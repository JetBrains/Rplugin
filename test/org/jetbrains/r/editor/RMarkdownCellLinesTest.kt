package org.jetbrains.r.editor

import com.intellij.openapi.editor.impl.EditorImpl
import org.jetbrains.plugins.notebooks.editor.CodeCellLinesChecker
import org.jetbrains.plugins.notebooks.editor.NotebookCellLines.CellType.*
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
        marker(MARKDOWN, 0, 20)
      }
      intervals {
        interval(MARKDOWN, 0..2)
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
        marker(CODE, 0, 20)
      }
      intervals {
        interval(CODE, 0..2)
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
        marker(CODE, 0, 17)
        marker(CODE, 17, 16)
        marker(CODE, 33, 20)
      }
      intervals {
        interval(CODE, 0..1)
        interval(CODE, 2..3)
        interval(CODE, 4..5)
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
        marker(MARKDOWN, 0, 30)
      }
      intervals {
        interval(MARKDOWN, 0..2)
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
        marker(CODE, 0, 23)
        marker(CODE, 23, 22)
      }
      intervals {
        interval(CODE, 0..2)
        interval(CODE, 3..5)
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
        marker(MARKDOWN, 0, 74)
        marker(MARKDOWN, 74, 36)
      }
      intervals {
        interval(MARKDOWN, 0..5)
        interval(MARKDOWN, 6..9)
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
        marker(CODE, 0, 10)
        marker(CODE, 10, 15)
      }
      intervals {
        interval(CODE, 0..0)
        interval(CODE, 1..1)
      }
    }
  }

  private fun assertCodeCells(description: String = "", handler: CodeCellLinesChecker.() -> Unit) {
    CodeCellLinesChecker(description) { fixture.editor as EditorImpl }.invoke(handler)
  }
}