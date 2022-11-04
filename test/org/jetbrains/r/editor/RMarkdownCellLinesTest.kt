package org.jetbrains.r.editor

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.testFramework.common.ThreadLeakTracker
import org.jetbrains.plugins.notebooks.visualization.CodeCellLinesChecker
import org.jetbrains.plugins.notebooks.visualization.NotebookCellLines
import org.jetbrains.plugins.notebooks.visualization.NotebookCellLines.CellType.CODE
import org.jetbrains.plugins.notebooks.visualization.NotebookCellLines.CellType.MARKDOWN
import org.jetbrains.plugins.notebooks.visualization.edt
import org.junit.Before
import org.junit.Test

class RMarkdownCellLinesTest : RMarkdownEditorUiTestBase() {
  @Test
  fun `test notebook with text`(): Unit = edt {
    fixture.openNotebookTextInEditor("""
      line 1
      line 2
      line 3
    """.trimIndent())

    assertCodeCells {
      markers {
      }
      intervals {
        interval(MARKDOWN, 0..2)
      }
    }
  }

  @Test
  fun `test notebook with code`(): Unit = edt {
    fixture.openNotebookTextInEditor("""
      ```{r}
      code line
      ```
    """.trimIndent())

    assertCodeCells {
      markers {
        marker(CODE, 0, 7)
        marker(CODE, 17, 3)
      }
      intervals {
        interval(CODE, 0..2)
      }
    }
  }

  @Test
  fun `test code options`(): Unit = edt {
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
        marker(CODE, 0, 13)
        marker(CODE, 13, 4)
        marker(CODE, 17, 12)
        marker(CODE, 29, 4)
        marker(CODE, 33, 17)
        marker(CODE, 50, 3)
      }
      intervals {
        interval(CODE, 0..1)
        interval(CODE, 2..3)
        interval(CODE, 4..5)
      }
    }
  }

  @Test
  fun `test uncompleted code cell`(): Unit = edt {
    fixture.openNotebookTextInEditor("""
      ```{r setup}
      actual markdown
      `
    """.trimIndent())

    assertCodeCells {
      markers {
      }
      intervals {
        interval(MARKDOWN, 0..2)
      }
    }
  }

  @Test
  fun `test uncompleted code cell surrounded by markdown`(): Unit = edt {
    fixture.openNotebookTextInEditor("""
      markdown
      ```{r setup}
      uncompleted cell
      `
      markdown
    """.trimIndent())

    assertCodeCells {
      markers {
      }
      intervals {
        interval(MARKDOWN, 0..0)
        interval(MARKDOWN, 1..4)
      }
    }
  }

  @Test
  fun `add code cell after text`(): Unit = edt {
    fixture.openNotebookTextInEditor("text<caret>")
    assertCodeCells {
      markers {
      }
      intervals {
        interval(MARKDOWN, 0..0)
      }
    }

    assertCodeCells {
      fixture.performEditorAction("RMarkdownNewChunk")

      markers {
        marker(CODE, 5, 7)
        marker(CODE, 13, 4)
      }
      intervals {
        interval(MARKDOWN, 0..0)
        interval(CODE, 1..3)
        interval(MARKDOWN, 4..4)
      }
      intervalListenerCall(1) {
        before {
        }
        after {
          interval(CODE, 1..3)
          interval(MARKDOWN, 4..4)
        }
      }
    }
  }

  @Test
  fun `two code cells`(): Unit = edt {
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
        marker(CODE, 0, 7)
        marker(CODE, 19, 4)
        marker(CODE, 23, 7)
        marker(CODE, 42, 3)
      }
      intervals {
        interval(CODE, 0..2)
        interval(CODE, 3..5)
      }
    }
  }

  @Test
  fun `code cell with newline at the end of file`(): Unit = edt {
    fixture.openNotebookTextInEditor("""
      ```{r}
      code
      ```
      
    """.trimIndent())

    assertCodeCells {
      markers {
        marker(CODE, 0, 7)
        marker(CODE, 12, 4)
        // last line has zero symbols
      }
      intervals {
        interval(CODE, 0..2)
        interval(MARKDOWN, 3..3)
      }
    }
  }

  @Test
  fun `rmd notebook header`(): Unit = edt {
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
      }
      intervals {
        interval(MARKDOWN, 0..9)
      }
    }
  }

  @Test
  fun `single line code cells isnt chunks`(): Unit = edt {
    fixture.openNotebookTextInEditor("""
      ```{r}```
      ```{r} code ```
    """.trimIndent())

    assertCodeCells {
      markers {
      }
      intervals {
        interval(MARKDOWN, 0..0)
        interval(MARKDOWN, 1..1)
      }
    }
  }

  @Test
  fun `test quoted cell`(): Unit = edt {
    fixture.openNotebookTextInEditor("""
      markdown
      > ```{r}
      > commented code
      > ```
    """.trimIndent())

    assertCodeCells {
      markers {
      }
      intervals {
        interval(MARKDOWN, 0..3)
      }
    }
  }

  @Test
  fun `test quoted code cell surrounded by markdown`(): Unit = edt {
    fixture.openNotebookTextInEditor("""
      markdown line
      > ```{r}
      > commented code
      > ```
      last line
    """.trimIndent())

    assertCodeCells {
      markers {
      }
      intervals {
        interval(MARKDOWN, 0..4)
      }
    }
  }

  @Test
  fun `test invalid code cell with leading whitespaces`(): Unit = edt {
    fixture.openNotebookTextInEditor("""
      markdown line
        ```{r}
      invalid code cell
      ```
      last line
    """.trimIndent())

    assertCodeCells {
      markers {
      }
      intervals {
        interval(MARKDOWN, 0..4)
      }
    }
  }

  @Test
  fun `test ignore code cell with leading markdown`(): Unit = edt {
    fixture.openNotebookTextInEditor("""
      valid markdown
        ```{r}invalid``` ```{r}cells```
      ```{r}invalid code cell``` invalid markdown ```{r} invalid code cell ``` 
      valid markdown
    """.trimIndent())

    assertCodeCells {
      markers {
      }
      intervals {
        interval(MARKDOWN, 0..1)
        interval(MARKDOWN, 2..3)
      }
    }
  }

  @Test
  fun `test add text to empty document and delete`(): Unit = edt {
    fixture.openNotebookTextInEditor("")

    assertCodeCells("add text") {
      fixture.type("add\nmultiline text")
      markers {
      }
      intervals {
        interval(MARKDOWN, 0..1)
      }
      intervalListenerCall(0) {
        before {
          interval(MARKDOWN, 0..0)
        }
        after {
          interval(MARKDOWN, 0..1)
        }
      }
    }

    assertCodeCells("remove all text") {
      fixture.performEditorAction(IdeActions.ACTION_SELECT_ALL)
      fixture.performEditorAction(IdeActions.ACTION_EDITOR_DELETE)
      markers {
      }
      intervals {
        interval(MARKDOWN, 0..0)
      }
      intervalListenerCall(0) {
        before {
          interval(MARKDOWN, 0..1)
        }
        after {
          interval(MARKDOWN, 0..0)
        }
      }
    }
  }

  @Test
  fun `test add code to empty document`(): Unit = edt {
    fixture.openNotebookTextInEditor("")

    assertCodeCells("add code") {
      // for first ` ide actually types `<caret>`
      fixture.type("""
        ```{r}
        code
        ``
      """.trimIndent())
      markers {
        marker(CODE, 0, 7)
        marker(CODE, 12, 3)
      }
      intervals {
        interval(CODE, 0..2)
      }
      intervalListenerCall(0) {
        before {
          interval(MARKDOWN, 0..0)
        }
        after {
          interval(MARKDOWN, 0..1)
        }
      }
      intervalListenerCall(0) {
        before {
          interval(MARKDOWN, 0..1)
        }
        after {
          interval(MARKDOWN, 0..2)
        }
      }
      intervalListenerCall(0) {
        before {
          interval(MARKDOWN, 0..2)
        }
        after {
          interval(CODE, 0..2)
        }
      }
    }

    assertCodeCells("remove code") {
      fixture.performEditorAction(IdeActions.ACTION_SELECT_ALL)
      fixture.performEditorAction(IdeActions.ACTION_EDITOR_DELETE)
      markers {
      }
      intervals {
        interval(MARKDOWN, 0..0)
      }
      intervalListenerCall(0) {
        before {
          interval(CODE, 0..2)
        }
        after {
          interval(MARKDOWN, 0..0)
        }
      }
    }
  }

  @Test
  fun `test insert invalid code cell in one line`(): Unit = edt {
    fixture.openNotebookTextInEditor("""
      markdown line 1
      <caret>
      markdown line 2
    """.trimIndent())

    assertCodeCells {
      markers {
      }
      intervals {
        interval(MARKDOWN, 0..2)
      }
    }

    assertCodeCells("insert ```") {
      fixture.type("```")
      // actually it types ```<caret>`,
      // ``` is valid start of cell, but ```` is invalid and ignored
      markers {
      }
      intervals {
        interval(MARKDOWN, 0..2)
      }
    }

    assertCodeCells("complete start of the cell to ```{r}") {
      fixture.type("{r}")
      markers {
      }
      intervals {
        interval(MARKDOWN, 0..0)
        interval(MARKDOWN, 1..2)
      }
      intervalListenerCall(0) {
        before {
          interval(MARKDOWN, 0..2)
        }
        after {
          interval(MARKDOWN, 0..0)
          interval(MARKDOWN, 1..2)
        }
      }
    }

    assertCodeCells("add code cell end") {
      // one ` was already typed and placed after caret.
      // line with ```{r}``` isn't a chunk
      fixture.type("``")
      markers {
      }
      intervals {
        interval(MARKDOWN, 0..0)
        interval(MARKDOWN, 1..2)
      }
    }
  }

  @Test
  fun `test insert code chunk`(): Unit = edt {
    fixture.openNotebookTextInEditor("""
      markdown line 1
      ```{r}<caret>```
      markdown line 2
    """.trimIndent())

    assertCodeCells("make valid code chunk") {
      fixture.type("\n")
      markers {
        marker(CODE, 16, 7)
        marker(CODE, 23, 4)
      }
      intervals {
        interval(MARKDOWN, 0..0)
        interval(CODE, 1..2)
        interval(MARKDOWN, 3..3)
      }
      intervalListenerCall(1) {
        before {
          interval(MARKDOWN, 1..2)
        }
        after {
          interval(CODE, 1..2)
          interval(MARKDOWN, 3..3)
        }
      }
    }
  }

  @Test
  fun `test markdown merge when remove code cell`(): Unit = edt {
    fixture.openNotebookTextInEditor("""
      line 0
      ```{r}<caret>
      code
      ```
      line 1
    """.trimIndent())

    assertCodeCells {
      markers {
        marker(CODE, 7, 7)
        marker(CODE, 19, 4)
      }
      intervals {
        interval(MARKDOWN, 0..0)
        interval(CODE, 1..3)
        interval(MARKDOWN, 4..4)
      }
    }

    assertCodeCells {
      fixture.performEditorAction(IdeActions.ACTION_EDITOR_DELETE_LINE)

      markers {
      }
      intervals {
        interval(MARKDOWN, 0..3)
      }
      intervalListenerCall(0) {
        before {
          interval(MARKDOWN, 0..0)
          interval(CODE, 1..3)
          interval(MARKDOWN, 4..4)
        }
        after {
          interval(MARKDOWN, 0..3)
        }
      }
    }
  }

  @Test
  fun `test edit code cell`(): Unit = edt {
    fixture.openNotebookTextInEditor("""
      ```{r}<caret>
      ```
      last line
    """.trimIndent())

    assertCodeCells {
      markers {
        marker(CODE, 0, 7)
        marker(CODE, 7, 4)
      }
      intervals {
        interval(CODE, 0..1)
        interval(MARKDOWN, 2..2)
      }
    }

    val attemptsCount = 3
    for (attempt in 1..attemptsCount) {
      assertCodeCells("add new code line: attempt ${attempt} of ${attemptsCount}") {
        fixture.type("\n")

        markers {
          marker(CODE, 0, 7)
          marker(CODE, 8, 4)
        }
        intervals {
          interval(CODE, 0..2)
          interval(MARKDOWN, 3..3)
        }

        intervalListenerCall(0) {
          before {
            interval(CODE, 0..1)
          }
          after {
            interval(CODE, 0..2)
          }
        }
      }

      assertCodeCells("remove code line: attempt ${attempt} of ${attemptsCount}") {
        fixture.type("\b")

        markers {
          marker(CODE, 0, 7)
          marker(CODE, 7, 4)
        }
        intervals {
          interval(CODE, 0..1)
          interval(MARKDOWN, 2..2)
        }

        intervalListenerCall(0) {
          before {
            interval(CODE, 0..2)
          }
          after {
            interval(CODE, 0..1)
          }
        }
      }
    }
  }

  private fun assertCodeCells(description: String = "", handler: CodeCellLinesChecker.() -> Unit) {
    CodeCellLinesChecker(description) { fixture.editor as EditorImpl }.invoke(handler)
  }

  @Before
  fun before() {
    ThreadLeakTracker.longRunningThreadCreated(ApplicationManager.getApplication(), "Timer-", "BaseDataReader", "rwrapper")
  }
}

private fun CodeCellLinesChecker.IntervalsSetter.interval(cellType: NotebookCellLines.CellType, lines: IntRange) {
  val markersAtLines = when (cellType) {
    CODE -> NotebookCellLines.MarkersAtLines.TOP_AND_BOTTOM
    else -> NotebookCellLines.MarkersAtLines.NO
  }

  interval(cellType, lines, markersAtLines)
}