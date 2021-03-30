package org.jetbrains.plugins.notebooks.editor.outputs

import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.mouseButton
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.EditorGutterComponentEx
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.wm.impl.IdeGlassPaneImpl
import com.intellij.ui.IdeBorderFactory
import com.intellij.util.castSafelyTo
import com.intellij.util.ui.JBUI
import icons.VisualisationIcons
import org.intellij.datavis.r.inlays.ResizeController
import org.jetbrains.plugins.notebooks.editor.NotebookCellInlayController
import org.jetbrains.plugins.notebooks.editor.NotebookCellLines
import org.jetbrains.plugins.notebooks.editor.SwingClientProperty
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

    OutputCollapsingGutterMouseListener.ensureInstalled((editor as EditorEx).gutterComponentEx)
  }

  override fun paintGutter(editor: EditorImpl, g: Graphics, r: Rectangle, intervalIterator: ListIterator<NotebookCellLines.Interval>) {
    val yOffset = innerComponent.yOffsetFromEditor(editor) ?: return
    val bounds = Rectangle()
    val oldClip = g.clipBounds
    g.clip = Rectangle(oldClip.x, yOffset, oldClip.width, innerComponent.height).intersection(oldClip)
    for (collapsingComponent in innerComponent.components) {
      val mainComponent = (collapsingComponent as CollapsingComponent).mainComponent

      collapsingComponent.paintGutter(editor, yOffset, g)

      mainComponent.gutterPainter?.let { painter ->
        mainComponent.yOffsetFromEditor(editor)?.let { yOffset ->
          bounds.setBounds(r.x, yOffset, r.width, mainComponent.height)
          painter.paintGutter(editor, g, bounds)
        }
      }
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
              addIntoInnerComponent(newComponent, idx)
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
        addIntoInnerComponent(newComponent)
      }
    }

    return isFilled
  }

  private fun addIntoInnerComponent(newComponent: NotebookOutputComponentFactory.CreatedComponent, pos: Int = -1) {
    val collapsingComponent = CollapsingComponent(
      editor,
      newComponent.component,
      newComponent.limitHeight,
      newComponent.collapsedTextSupplier,
    )
    innerComponent.add(collapsingComponent, newComponent.fixedWidthLayoutConstraint, pos)
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
    border = IdeBorderFactory.createEmptyBorder(Insets(10, 0, 0, 0))
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
    layout = FixedWidthMaxHeightLayout(this)
  }

  override fun updateUI() {
    super.updateUI()
    isOpaque = false
  }

  override fun add(comp: Component): Component = throw UnsupportedOperationException()

  override fun add(comp: Component, constraints: Any) {
    add(comp, constraints, -1)
  }

  override fun add(comp: Component, constraints: Any, index: Int) {
    require(comp is CollapsingComponent)
    // It'd rather the constraint was set in the layout manager, but it's broken. See DS-1566.
    comp.layoutConstraints = constraints as FixedWidthMaxHeightLayout.Constraint
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

private var JComponent.outputComponentFactory: NotebookOutputComponentFactory? by SwingClientProperty()

private class FixedWidthMaxHeightLayout(private val innerComponent: InnerComponent) : LayoutManager2 {
  data class Constraint(val widthStretching: NotebookOutputComponentFactory.WidthStretching, val limitedHeight: Boolean)

  override fun addLayoutComponent(comp: Component, constraints: Any) {
    // Can't rely on this method. See DS-1566.
  }

  override fun addLayoutComponent(name: String?, comp: Component) {
    error("Should not be called")
  }

  override fun removeLayoutComponent(comp: Component) {
    require(comp is JComponent)
    comp.layoutConstraints = null
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
    val parentDesiredWidth = innerComponent.width
    val maxHeight = innerComponent.maxHeight
    for (component in parent.components) {
      check(component is CollapsingComponent) { "$component is not CollapsingComponent" }
      val proposedSize = component.sizeProposer()
      val newWidth = getComponentWidthByConstraint(parentDesiredWidth, parentInsets, component, proposedSize.width)
      val newHeight =
        if (!component.isPreferredSizeSet && component.layoutConstraints?.limitedHeight == true) min(maxHeight, proposedSize.height)
        else proposedSize.height
      handleComponent(component, newWidth, newHeight)
    }
  }

  private fun getComponentWidthByConstraint(parentWidth: Int,
                                            parentInsets: Insets,
                                            component: JComponent,
                                            componentDesiredWidth: Int): Int =
    (parentWidth - parentInsets.left - parentInsets.right).let {
      when (component.layoutConstraints?.widthStretching) {
        STRETCH_AND_SQUEEZE -> it
        STRETCH -> max(it, componentDesiredWidth)
        SQUEEZE -> min(it, componentDesiredWidth)
        NOTHING -> componentDesiredWidth
        null -> {
          LOG.error("The component $component has no constraints")
          componentDesiredWidth
        }
      }
    }
}

private var JComponent.layoutConstraints: FixedWidthMaxHeightLayout.Constraint? by SwingClientProperty()

private var EditorGutterComponentEx.hoveredCollapsingComponentRect: CollapsingComponent? by SwingClientProperty()

private class OutputCollapsingGutterMouseListener(
  private val gutterComponentEx: EditorGutterComponentEx,
) : MouseListener, MouseMotionListener {
  companion object {
    @JvmStatic
    fun ensureInstalled(gutterComponentEx: EditorGutterComponentEx) {
      if (gutterComponentEx.getClientProperty(OutputCollapsingGutterMouseListener::class.java) == null) {
        gutterComponentEx.putClientProperty(OutputCollapsingGutterMouseListener::class.java, Unit)

        val instance = OutputCollapsingGutterMouseListener(gutterComponentEx)
        gutterComponentEx.addMouseListener(instance)
        gutterComponentEx.addMouseMotionListener(instance)
      }
    }
  }

  override fun mouseClicked(e: MouseEvent) {
    if (e.mouseButton != MouseButton.Left) return

    val point = e.point
    if (!isAtCollapseVerticalStripe(point)) return
    val component = gutterComponentEx.hoveredCollapsingComponentRect ?: return

    component.isSeen = !component.isSeen
    e.consume()
    SwingUtilities.invokeLater {  // Being invoked without postponing, it would access old states of layouts and get the same results.
      if (gutterComponentEx.editor?.isDisposed == false) {
        updateState(point)
      }
    }
  }

  override fun mousePressed(e: MouseEvent): Unit = Unit

  override fun mouseReleased(e: MouseEvent): Unit = Unit

  override fun mouseEntered(e: MouseEvent): Unit = Unit

  override fun mouseExited(e: MouseEvent) {
    updateState(null)
  }

  override fun mouseMoved(e: MouseEvent) {
    updateState(e.point)
  }

  private fun updateState(point: Point?) {
    if (point == null || !isAtCollapseVerticalStripe(point)) {
      IdeGlassPaneImpl.forgetPreProcessedCursor(gutterComponentEx)
      gutterComponentEx.cursor = @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") null  // Huh? It's a valid operation!
      updateHoveredComponent(null)
    }
    else {
      val collapsingComponent = getCollapsingComponent(point)
      updateHoveredComponent(collapsingComponent)
      if (collapsingComponent != null) {
        val cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        IdeGlassPaneImpl.savePreProcessedCursor(gutterComponentEx, cursor)
        gutterComponentEx.cursor = cursor
      }
      else {
        IdeGlassPaneImpl.forgetPreProcessedCursor(gutterComponentEx)
      }
    }
  }

  override fun mouseDragged(e: MouseEvent): Unit = Unit

  private fun isAtCollapseVerticalStripe(point: Point): Boolean =
    CollapsingComponent.collapseRectHorizontalLeft(gutterComponentEx.editor as EditorEx).let {
      point.x in it until it + CollapsingComponent.COLLAPSING_RECT_WIDTH
    }

  private fun getCollapsingComponent(point: Point): CollapsingComponent? {
    val editor = gutterComponentEx.editor ?: return null

    val surroundingComponent: SurroundingComponent =
      editor.contentComponent.getComponentAt(0, point.y).castSafelyTo<JComponent>()?.getComponent(0)?.castSafelyTo()
      ?: return null

    val innerComponent: InnerComponent =
      (surroundingComponent.layout as BorderLayout).getLayoutComponent(BorderLayout.CENTER).castSafelyTo()
      ?: return null

    val y = point.y - SwingUtilities.convertPoint(innerComponent, 0, 0, editor.contentComponent).y

    val collapsingComponent: CollapsingComponent =
      innerComponent.getComponentAt(0, y).castSafelyTo()
      ?: return null

    if (!collapsingComponent.isWorthCollapsing) return null
    return collapsingComponent
  }

  private fun updateHoveredComponent(collapsingComponent: CollapsingComponent?) {
    val old = gutterComponentEx.hoveredCollapsingComponentRect
    if (old !== collapsingComponent) {
      gutterComponentEx.hoveredCollapsingComponentRect = collapsingComponent
      gutterComponentEx.repaint()
    }
  }
}

private class CollapsingComponent(
  editor: EditorImpl,
  child: JComponent,
  private val resizable: Boolean,
  private val collapsedTextSupplier: () -> @NlsSafe String,
) : JPanel(BorderLayout()) {
  private val resizeController by lazy { ResizeController(this, editor) }
  private var oldPredefinedPreferredSize: Dimension? = null

  var isSeen: Boolean
    get() = mainComponent.isVisible
    set(value) {
      mainComponent.isVisible = value
      stubComponent.isVisible = !value

      if (resizable) {
        if (value) {
          addMouseListener(resizeController)
          addMouseMotionListener(resizeController)
          preferredSize = oldPredefinedPreferredSize
          oldPredefinedPreferredSize = null
        }
        else {
          removeMouseListener(resizeController)
          removeMouseMotionListener(resizeController)
          oldPredefinedPreferredSize = if (isPreferredSizeSet) preferredSize else null
          preferredSize = null
        }
      }

      if (!value) {
        (stubComponent as StubComponent).text = collapsedTextSupplier()
      }
    }

  init {
    add(child, BorderLayout.CENTER)
    add(StubComponent(editor), BorderLayout.NORTH)
    border = IdeBorderFactory.createEmptyBorder(Insets(0, 0, 10, 0))  // It's used as a grip for resizing.
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
    val notebookAppearance = editor.notebookAppearance
    val backgroundColor = notebookAppearance.getCodeCellBackground(editor.colorsScheme)
    if (backgroundColor != null && isWorthCollapsing) {
      val x = collapseRectHorizontalLeft(editor)

      val (rectTop, rectHeight) = insets.let {
        yOffset + y + it.top to height - it.top - it.bottom
      }

      g.color = backgroundColor
      if (editor.gutterComponentEx.hoveredCollapsingComponentRect === this) {
        g.fillRect(x, rectTop, COLLAPSING_RECT_WIDTH, rectHeight)
      }

      if (!isSeen) {
        val outputAdjacentRectWidth = notebookAppearance.getLeftBorderWidth()
        g.fillRect(
          editor.gutterComponentEx.width - outputAdjacentRectWidth,
          rectTop,
          outputAdjacentRectWidth,
          rectHeight,
        )
      }

      val icon = if (isSeen) VisualisationIcons.OutputCollapse else VisualisationIcons.OutputExpand
      val iconOffset = (COLLAPSING_RECT_WIDTH - icon.iconWidth) / 2
      icon.paintIcon(this, g, x + iconOffset, yOffset + y + COLLAPSING_RECT_MARGIN_Y_BOTTOM + iconOffset)
    }
  }

  private class StubComponent(private val editor: EditorImpl) : JLabel("...") {
    init {
      border = IdeBorderFactory.createEmptyBorder(Insets(7, 0, 7, 0))
      updateUIFromEditor()
    }

    override fun updateUI() {
      super.updateUI()
      isOpaque = true
      if (@Suppress("SENSELESS_COMPARISON") (editor != null)) {
        updateUIFromEditor()
      }
    }

    private fun updateUIFromEditor() {
      val attrs = editor.colorsScheme.getAttributes(EditorColors.FOLDED_TEXT_ATTRIBUTES)
      foreground = JBUI.CurrentTheme.ActionsList.MNEMONIC_FOREGROUND
      background = editor.notebookAppearance.getCodeCellBackground(editor.colorsScheme) ?: editor.colorsScheme.defaultBackground
      font = EditorUtil.fontForChar(text.first(), attrs.fontType, editor).font
    }
  }

  companion object {
    const val MIN_HEIGHT_TO_COLLAPSE = 50
    const val COLLAPSING_RECT_WIDTH = 22
    private const val COLLAPSING_RECT_MARGIN_Y_BOTTOM = 5

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

// Used only in exceptional, very rare cases, therefore covered by "lazy".
private val LOG by lazy { logger<NotebookOutputInlayController>() }