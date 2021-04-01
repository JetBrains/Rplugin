package org.jetbrains.plugins.notebooks.editor.outputs.impl

import com.intellij.openapi.editor.ex.EditorGutterComponentEx
import org.jetbrains.plugins.notebooks.editor.SwingClientProperty
import javax.swing.JComponent

internal var JComponent.layoutConstraints: FixedWidthMaxHeightLayout.Constraint? by SwingClientProperty()

internal var EditorGutterComponentEx.hoveredCollapsingComponentRect: CollapsingComponent? by SwingClientProperty()