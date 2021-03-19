package org.jetbrains.plugins.notebooks.editor.outputs

import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.EditorGutterComponentEx
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.util.Disposer
import com.intellij.ui.IdeBorderFactory
import com.intellij.util.castSafelyTo
import org.jetbrains.plugins.notebooks.editor.NotebookCellInlayController
import org.jetbrains.plugins.notebooks.editor.NotebookCellLines
import org.jetbrains.plugins.notebooks.editor.notebookAppearance
import org.jetbrains.plugins.notebooks.editor.outputs.NotebookOutputComponentFactory.Companion.gutterPainter
import org.jetbrains.plugins.notebooks.editor.outputs.NotebookOutputComponentFactory.WidthStretching.*
import org.jetbrains.plugins.notebooks.editor.ui.addComponentInlay
import org.jetbrains.plugins.notebooks.editor.ui.registerEditorSizeWatcher
import org.jetbrains.plugins.notebooks.editor.ui.textEditingAreaWidth
import org.jetbrains.plugins.notebooks.editor.ui.yOffsetFromEditor
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.math.max
import kotlin.math.min

private const val DEFAULT_INLAY_HEIGHT = 200

/**
 * Shows outputs for intervals using [NotebookOutputDataKeyExtractor] and [NotebookOutputComponentFactory].
 */
class NotebookOutputInlayController private constructor(
  override val factory: NotebookCellInlayController.Factory,
  private val editor: EditorImpl,
  lines: IntRange,
) : NotebookCellInlayController {
  private val innerComponent = InnerComponent()
  private val outerComponent = SurroundingComponent.create(editor, innerComponent)

  override val inlay: Inlay<*> =
    editor.addComponentInlay(
      outerComponent,
      isRelatedToPrecedingText = true,
      showAbove = false,
      priority = editor.notebookAppearance.NOTEBOOK_OUTPUT_INLAY_PRIORITY,
      offset = editor.document.getLineEndOffset(lines.last),
    )

  init {
    innerComponent.maxHeight = if (!ApplicationManager.getApplication().isUnitTestMode) {
      (Toolkit.getDefaultToolkit().screenSize.height * 0.3).toInt()
    }
    else {
      DEFAULT_INLAY_HEIGHT
    }

    Disposer.register(inlay) {
      for (disposable in innerComponent.mainComponents) {
        if (disposable is Disposable) {
          Disposer.dispose(disposable)
        }
      }
    }

    val gutterComponentEx = (editor as EditorEx).gutterComponentEx
    if (gutterComponentEx.getClientProperty(OutputCollapsingGutterMouseListener) == null) {
      gutterComponentEx.putClientProperty(OutputCollapsingGutterMouseListener, Unit)
      gutterComponentEx.addMouseListener(OutputCollapsingGutterMouseListener)
      gutterComponentEx.addMouseMotionListener(OutputCollapsingGutterMouseListener)
    }
  }

  override fun paintGutter(editor: EditorImpl, g: Graphics, r: Rectangle, intervalIterator: ListIterator<NotebookCellLines.Interval>) {
    val yOffset = innerComponent.yOffsetFromEditor(editor) ?: return
    val bounds = Rectangle()
    val oldClip = g.clipBounds
    g.clip = Rectangle(oldClip.x, yOffset, oldClip.width, innerComponent.height).intersection(oldClip)
    for (collapsingComponent in innerComponent.components) {
      val mainComponent = (collapsingComponent as CollapsingComponent).mainComponent
      mainComponent.gutterPainter?.let { painter ->
        mainComponent.yOffsetFromEditor(editor)?.let { yOffset ->
          bounds.setBounds(r.x, yOffset, r.width, mainComponent.height)
          painter.paintGutter(editor, g, bounds)
        }
      }

      collapsingComponent.paintGutter(editor, yOffset, g)
    }
    g.clip = oldClip
  }

  private fun rankCompatibility(outputDataKeys: List<NotebookOutputDataKey>): Int =
    getComponentsWithFactories().zip(outputDataKeys).sumBy { (pair, outputDataKey) ->
      val (component, factory) = pair
      when (factory.match(component, outputDataKey)) {
        NotebookOutputComponentFactory.Match.NONE -> 0
        NotebookOutputComponentFactory.Match.COMPATIBLE -> 1
        NotebookOutputComponentFactory.Match.SAME -> 1000
      }
    }

  private fun getComponentsWithFactories() = mutableListOf<Pair<JComponent, NotebookOutputComponentFactory>>().also {
    for (component in innerComponent.mainComponents) {
      val factory = component.outputComponentFactory
      if (factory != null) {
        it += component to factory
      }
    }
  }

  private fun updateData(newDataKeys: List<NotebookOutputDataKey>): Boolean {
    val newDataKeyIterator = newDataKeys.iterator()
    val oldComponentsWithFactories = getComponentsWithFactories().iterator()
    var isFilled = false
    for ((idx, pair1) in newDataKeyIterator.zip(oldComponentsWithFactories).withIndex()) {
      val (newDataKey, pair2) = pair1
      val (oldComponent, oldFactory: NotebookOutputComponentFactory) = pair2
      isFilled =
        when (oldFactory.match(oldComponent, newDataKey)) {
          NotebookOutputComponentFactory.Match.NONE -> {
            innerComponent.remove(idx)
            if (oldComponent is Disposable) {
              Disposer.dispose(oldComponent)
            }
            val newComponent = createOutputGuessingFactory(newDataKey)
            if (newComponent != null) {
              innerComponent.add(
                CollapsingComponent(editor, newComponent.component),
                newComponent.fixedWidthLayoutConstraint,
                idx,
              )
              true
            }
            else false
          }
          NotebookOutputComponentFactory.Match.COMPATIBLE -> {
            oldFactory.updateComponent(editor, oldComponent, newDataKey)
            true
          }
          NotebookOutputComponentFactory.Match.SAME -> true
        } || isFilled
    }

    for (ignored in oldComponentsWithFactories) {
      val idx = innerComponent.componentCount - 1
      val old = innerComponent.getComponent(idx).let { if (it is CollapsingComponent) it.mainComponent else it }
      innerComponent.remove(idx)
      if (old is Disposable) {
        Disposer.dispose(old)
      }
    }

    for (outputDataKey in newDataKeyIterator) {
      val newComponent = createOutputGuessingFactory(outputDataKey)
      if (newComponent != null) {
        isFilled = true
        innerComponent.add(
          CollapsingComponent(editor, newComponent.component),
          newComponent.fixedWidthLayoutConstraint,
        )
      }
    }

    return isFilled
  }

  private fun createOutputGuessingFactory(outputDataKey: NotebookOutputDataKey): NotebookOutputComponentFactory.CreatedComponent? =
    NotebookOutputComponentFactory.EP_NAME.extensionList.asSequence()
      .mapNotNull { factory ->
        createOutput(factory, outputDataKey)
      }
      .firstOrNull()

  private fun createOutput(factory: NotebookOutputComponentFactory,
                           outputDataKey: NotebookOutputDataKey): NotebookOutputComponentFactory.CreatedComponent? =
    factory.createComponent(editor, outputDataKey, inlay)?.also {
      it.component.outputComponentFactory = factory
      it.component.gutterPainter = it.gutterPainter
    }

  class Factory : NotebookCellInlayController.Factory {
    override fun compute(
      editor: EditorImpl,
      currentControllers: Collection<NotebookCellInlayController>,
      intervalIterator: ListIterator<NotebookCellLines.Interval>,
    ): NotebookCellInlayController? {
      if (!NewOutputInlaysSwitch.useNewForAnything) return null

      val interval = intervalIterator.next()
      if (interval.type != NotebookCellLines.CellType.CODE) return null

      val outputDataKeys =
        NotebookOutputDataKeyExtractor.EP_NAME.extensionList.asSequence()
          .mapNotNull { it.extract(editor, interval) }
          .firstOrNull()
          ?.takeIf { it.isNotEmpty() }
        ?: return null

      val controller =
        currentControllers
          .filterIsInstance<NotebookOutputInlayController>()
          .maxByOrNull { it.rankCompatibility(outputDataKeys) }
        ?: NotebookOutputInlayController(this, editor, interval.lines)
      return controller.takeIf { it.updateData(outputDataKeys) }
    }
  }
}

val EditorCustomElementRenderer.notebookInlayOutputComponent: JComponent?
  get() = castSafelyTo<JComponent>()?.components?.firstOrNull()?.castSafelyTo<SurroundingComponent>()

private val NotebookOutputComponentFactory.CreatedComponent.fixedWidthLayoutConstraint
  get() = FixedWidthMaxHeightLayout.Constraint(widthStretching, limitHeight)

private class SurroundingComponent private constructor(private val innerComponent: InnerComponent) : JPanel(
  BorderLayout()) {
  private var presetWidth = 0

  init {
    border = IdeBorderFactory.createEmptyBorder(Insets(10, 0, 10, 0))
    add(innerComponent, BorderLayout.CENTER)
  }

  override fun updateUI() {
    super.updateUI()
    isOpaque = false
  }

  override fun getPreferredSize(): Dimension = super.getPreferredSize().also {
    it.width = presetWidth
    // No need to show anything for the empty component
    if (innerComponent.preferredSize.height == 0) {
      it.height = 0
    }
  }

  companion object {
    @JvmStatic
    fun create(
      editor: EditorImpl,
      innerComponent: InnerComponent,
    ) = SurroundingComponent(innerComponent).also {
      registerEditorSizeWatcher(it) {
        val oldWidth = it.presetWidth
        it.presetWidth = editor.textEditingAreaWidth
        if (oldWidth != it.presetWidth) {
          innerComponent.revalidate()
        }
      }

      for (wrapper in NotebookOutputComponentWrapper.EP_NAME.extensionList) {
        wrapper.wrap(it)
      }
    }
  }
}

private class InnerComponent : JPanel() {
  var maxHeight: Int = Int.MAX_VALUE

  init {
    layout = FixedWidthMaxHeightLayout(widthGetter = { width }, maxHeightGetter = { maxHeight })
  }

  override fun updateUI() {
    super.updateUI()
    isOpaque = false
  }

  override fun add(comp: Component): Component = throw UnsupportedOperationException()

  override fun add(comp: Component, constraints: Any) {
    require(comp is CollapsingComponent)
    super.add(comp, constraints)
  }

  override fun add(comp: Component, constraints: Any, index: Int) {
    require(comp is CollapsingComponent)
    super.add(comp, constraints, index)
  }

  val mainComponents: List<JComponent>
    get() = ArrayList<JComponent>(componentCount).also {
      repeat(componentCount) { i ->
        it += (getComponent(i) as CollapsingComponent).mainComponent
      }
    }
}

private fun <A, B> Iterator<A>.zip(other: Iterator<B>): Iterator<Pair<A, B>> = object : Iterator<Pair<A, B>> {
  override fun hasNext(): Boolean = this@zip.hasNext() && other.hasNext()
  override fun next(): Pair<A, B> = this@zip.next() to other.next()
}

private var JComponent.outputComponentFactory: NotebookOutputComponentFactory?
  get() =
    getClientProperty(NotebookOutputComponentFactory::class.java) as NotebookOutputComponentFactory?
  set(value) {
    putClientProperty(NotebookOutputComponentFactory::class.java, value)
  }

private class FixedWidthMaxHeightLayout(
  private val widthGetter: (Container) -> Int,
  private val maxHeightGetter: () -> Int,
) : LayoutManager2 {
  data class Constraint(val widthStretching: NotebookOutputComponentFactory.WidthStretching, val limitedHeight: Boolean)

  override fun addLayoutComponent(comp: Component, constraints: Any) {
    require(comp is JComponent)
    comp.constraints = constraints as Constraint
  }

  override fun addLayoutComponent(name: String?, comp: Component) {
    error("Should not be called")
  }

  override fun removeLayoutComponent(comp: Component) {
    comp.constraints = null
  }

  override fun preferredLayoutSize(parent: Container): Dimension =
    foldSize(parent) { preferredSize }

  override fun minimumLayoutSize(parent: Container): Dimension =
    foldSize(parent) { minimumSize }

  override fun maximumLayoutSize(target: Container): Dimension =
    foldSize(target) { maximumSize }

  override fun layoutContainer(parent: Container) {
    val parentInsets = parent.insets
    var totalY = parentInsets.top
    forEveryComponent(parent, Component::getPreferredSize) { component, newWidth, newHeight ->
      component.setBounds(
        parentInsets.left,
        totalY,
        newWidth,
        newHeight,
      )
      totalY += newHeight
    }
  }

  override fun getLayoutAlignmentX(target: Container): Float = 0f

  override fun getLayoutAlignmentY(target: Container): Float = 0f

  override fun invalidateLayout(target: Container): Unit = Unit

  private inline fun foldSize(parent: Container, crossinline handler: Component.() -> Dimension): Dimension {
    val acc = Dimension(0, parent.insets.run { top + bottom })

    forEveryComponent(parent, handler) { _, newWidth, newHeight ->
      acc.width = max(acc.width, newWidth)
      acc.height += newHeight
    }

    return acc
  }

  private inline fun forEveryComponent(
    parent: Container,
    crossinline sizeProposer: Component.() -> Dimension,
    crossinline handleComponent: (component: Component, newWidth: Int, newHeight: Int) -> Unit,
  ) {
    val parentInsets = parent.insets
    val parentDesiredWidth = widthGetter(parent)
    val maxHeight = maxHeightGetter()
    for (component in parent.components) {
      val proposedSize = component.sizeProposer()
      val newWidth = getComponentWidthByConstraint(parentDesiredWidth, parentInsets, component.constraints, proposedSize.width)
      val newHeight =
        if (component.constraints?.limitedHeight == true) min(maxHeight, proposedSize.height)
        else proposedSize.height
      handleComponent(component, newWidth, newHeight)
    }
  }

  private fun getComponentWidthByConstraint(parentWidth: Int,
                                            parentInsets: Insets,
                                            componentConstraints: Constraint?,
                                            componentDesiredWidth: Int): Int =
    (parentWidth - parentInsets.left - parentInsets.right).let {
      when (componentConstraints?.widthStretching) {
        STRETCH_AND_SQUEEZE -> it
        STRETCH -> max(it, componentDesiredWidth)
        SQUEEZE -> min(it, componentDesiredWidth)
        NOTHING -> componentDesiredWidth
        else -> error(componentConstraints.toString())
      }
    }

  private var Component.constraints: Constraint?
    get() = castSafelyTo<JComponent>()?.getClientProperty(Constraint::class.java) as Constraint?
    set(value) {
      castSafelyTo<JComponent>()?.putClientProperty(Constraint::class.java, value)
    }
}

private object OutputCollapsingGutterMouseListener : MouseListener, MouseMotionListener {
  override fun mouseClicked(e: MouseEvent) {
    val gutterComponentEx = e.source as? EditorGutterComponentEx ?: return
    if (!isAtCollapseRegion(e, gutterComponentEx)) return
    val component = getCollapsingComponent(e, gutterComponentEx) ?: return

    e.consume()
    component.isSeen = !component.isSeen
  }

  override fun mousePressed(e: MouseEvent) : Unit = Unit

  override fun mouseReleased(e: MouseEvent): Unit = Unit

  override fun mouseEntered(e: MouseEvent): Unit = Unit

  override fun mouseExited(e: MouseEvent) {
    val gutterComponentEx = e.source as? EditorGutterComponentEx ?: return
    gutterComponentEx.cursor = Cursor(Cursor.DEFAULT_CURSOR)
  }

  override fun mouseMoved(e: MouseEvent) {
    val gutterComponentEx = e.source as? EditorGutterComponentEx ?: return
    when {
      !isAtCollapseRegion(e, gutterComponentEx) -> {
        gutterComponentEx.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
      }
      getCollapsingComponent(e, gutterComponentEx) != null -> {
        gutterComponentEx.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
      }
    }
  }

  override fun mouseDragged(e: MouseEvent): Unit = Unit

  private fun isAtCollapseRegion(e: MouseEvent, gutterComponentEx: EditorGutterComponentEx): Boolean =
    CollapsingComponent.collapseRectHorizontalLeft(gutterComponentEx.editor as EditorEx).let {
      e.point.x in it until it + CollapsingComponent.COLLAPSING_RECT_WIDTH
    }

  private fun getCollapsingComponent(e: MouseEvent, gutterComponentEx: EditorGutterComponentEx): CollapsingComponent? {
    val editor = gutterComponentEx.editor ?: return null

    val surroundingComponent: SurroundingComponent =
      editor.contentComponent.getComponentAt(0, e.y).castSafelyTo<JComponent>()?.getComponent(0)?.castSafelyTo()
      ?: return null

    val innerComponent: InnerComponent =
      (surroundingComponent.layout as BorderLayout).getLayoutComponent(BorderLayout.CENTER).castSafelyTo()
      ?: return null

    val y = e.y - SwingUtilities.convertPoint(innerComponent, 0, 0, editor.contentComponent).y

    val collapsingComponent: CollapsingComponent =
      innerComponent.getComponentAt(0, y).castSafelyTo()
      ?: return null

    if (!collapsingComponent.isWorthCollapsing) return null
    return collapsingComponent
  }
}

private class CollapsingComponent(editor: EditorImpl, child: JComponent) : JPanel(BorderLayout()) {
  var isSeen: Boolean
    get() = mainComponent.isVisible
    set(value) {
      mainComponent.isVisible = value
      stubComponent.isVisible = !value
    }

  init {
    add(child, BorderLayout.CENTER)
    add(StubComponent(editor), BorderLayout.NORTH)
    isSeen = true
  }

  override fun updateUI() {
    super.updateUI()
    isOpaque = false
  }

  val mainComponent: JComponent get() = getComponent(0) as JComponent
  val stubComponent: JComponent get() = getComponent(1) as JComponent

  val isWorthCollapsing: Boolean get() = !isSeen || mainComponent.height >= MIN_HEIGHT_TO_COLLAPSE

  fun paintGutter(editor: EditorEx, yOffset: Int, g: Graphics) {
    val backgroundColor = editor.notebookAppearance.getCollapseOutputAreaBackground(editor.colorsScheme)
    if (backgroundColor != null && isWorthCollapsing) {
      g.color = backgroundColor
      g.fillRoundRect(
        collapseRectHorizontalLeft(editor),
        yOffset + y + COLLAPSING_RECT_MARGIN_Y,
        COLLAPSING_RECT_WIDTH,
        height - COLLAPSING_RECT_MARGIN_Y * 2,
        COLLAPSING_RECT_ARC_RADIUS,
        COLLAPSING_RECT_ARC_RADIUS,
      )
    }
  }

  private class StubComponent(private val editor: EditorImpl) : JLabel("...") {
    init {
      border = IdeBorderFactory.createEmptyBorder(Insets(5, 10, 5, 0))
      updateUIFromEditor()
    }

    override fun updateUI() {
      super.updateUI()
      if (@Suppress("SENSELESS_COMPARISON") (editor != null)) {
        updateUIFromEditor()
      }
    }

    private fun updateUIFromEditor() {
      val attrs = editor.colorsScheme.getAttributes(EditorColors.DELETED_TEXT_ATTRIBUTES)
      foreground = attrs.foregroundColor
      background = attrs.backgroundColor
      font = EditorUtil.fontForChar(text.first(), attrs.fontType, editor).font
    }
  }

  companion object {
    const val MIN_HEIGHT_TO_COLLAPSE = 50
    const val COLLAPSING_RECT_WIDTH = 20
    private const val COLLAPSING_RECT_MARGIN_Y = 5
    private const val COLLAPSING_RECT_ARC_RADIUS = 7

    @JvmStatic
    fun collapseRectHorizontalLeft(editor: EditorEx): Int =
      (editor.gutterComponentEx.width
       - COLLAPSING_RECT_WIDTH
       - editor.notebookAppearance.LINE_NUMBERS_MARGIN
       - editor.notebookAppearance.CODE_CELL_LEFT_LINE_PADDING)
  }
}

private val EditorGutterComponentEx.editor: Editor?
  get() = PlatformDataKeys.EDITOR.getData(DataManager.getInstance().getDataContext(this))