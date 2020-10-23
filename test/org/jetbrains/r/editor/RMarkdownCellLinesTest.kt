package org.jetbrains.r.editor

import com.intellij.openapi.editor.impl.EditorImpl
import org.jetbrains.plugins.notebooks.editor.CodeCellLinesChecker
import org.jetbrains.plugins.notebooks.editor.NotebookCellLines
import org.jetbrains.plugins.notebooks.editor.edt
import org.junit.Test

class RMarkdownCellLinesTest: RMarkdownEditorUiTestBase() {
  @Test
  fun `test notebook with text`(): Unit = edt{
    fixture.openNotebookTextInEditor("""
      line 1
      line 2
      line 3
    """.trimIndent())

    assertCodeCells {
      markers {
        marker(NotebookCellLines.CellType.MARKDOWN, 0, 20)
      }
      intervals {
        interval(NotebookCellLines.CellType.MARKDOWN, 0..2)
      }
    }
  }

  @Test
  fun `test notebook with code`(): Unit = edt{
    fixture.openNotebookTextInEditor("""
      ```{r}
      code line
      ```
    """.trimIndent())

    assertCodeCells {
      markers {
        marker(NotebookCellLines.CellType.CODE, 0, 20)
      }
      intervals {
        interval(NotebookCellLines.CellType.CODE, 0..2)
      }
    }
  }

  @Test
  fun `test code options`(): Unit = edt{
    fixture.openNotebookTextInEditor("""
      ```{r setup}
      ```
      ```{python}
      ```
      ```{r chunk-one}
      ```
    """.trimIndent())

    assertCodeCells {
      markers {
        marker(NotebookCellLines.CellType.CODE, 0, 17)
        marker(NotebookCellLines.CellType.CODE, 17, 16)
        marker(NotebookCellLines.CellType.CODE, 33, 20)
      }
      intervals {
        interval(NotebookCellLines.CellType.CODE, 0..1)
        interval(NotebookCellLines.CellType.CODE, 2..3)
        interval(NotebookCellLines.CellType.CODE, 4..5)
      }
    }
  }

  @Test
  fun `test uncompleted code cell`(): Unit = edt{
    fixture.openNotebookTextInEditor("""
      ```{r setup}
      actual markdown
      `
    """.trimIndent())

    assertCodeCells {
      markers {
        marker(NotebookCellLines.CellType.MARKDOWN, 0, 30)
      }
      intervals {
        interval(NotebookCellLines.CellType.MARKDOWN, 0..2)
      }
    }
  }

  @Test
  fun `two code cells`(): Unit = edt{
    fixture.openNotebookTextInEditor("""
      ```{r}
      code cell 1
      ```
      ```{r}
      code cell 2
      ```
    """.trimIndent())

    assertCodeCells {
      markers {
        marker(NotebookCellLines.CellType.CODE, 0, 23)
        marker(NotebookCellLines.CellType.CODE, 23, 22)
      }
      intervals {
        interval(NotebookCellLines.CellType.CODE, 0..2)
        interval(NotebookCellLines.CellType.CODE, 3..5)
      }
    }
  }

  @Test
  fun `rmd notebook header`(): Unit = edt{
    fixture.openNotebookTextInEditor("""
      ---
      title: "title"
      author: author
      date: 9/11/20
      output: html_notebook
      ---
      markdown_cell_1
      ---
      still_cell_1
      ---
    """.trimIndent())

    assertCodeCells {
      markers {
        marker(NotebookCellLines.CellType.MARKDOWN, 0, 74)
        marker(NotebookCellLines.CellType.MARKDOWN, 74, 36)
      }
      intervals {
        interval(NotebookCellLines.CellType.MARKDOWN, 0..5)
        interval(NotebookCellLines.CellType.MARKDOWN, 6..9)
      }
    }
  }

  @Test
  fun `single line code cells`(): Unit = edt {
    fixture.openNotebookTextInEditor("""
      ```{r}```
      ```{r} code ```
    """.trimIndent())

    assertCodeCells {
      markers {
        marker(NotebookCellLines.CellType.CODE, 0, 10)
        marker(NotebookCellLines.CellType.CODE, 10, 15)
      }
      intervals {
        interval(NotebookCellLines.CellType.CODE, 0..0)
        interval(NotebookCellLines.CellType.CODE, 1..1)
      }
    }
  }

  private fun assertCodeCells(description: String = "", handler: CodeCellLinesChecker.() -> Unit) {
    CodeCellLinesChecker(description) { fixture.editor as EditorImpl }.invoke(handler)
  }
}