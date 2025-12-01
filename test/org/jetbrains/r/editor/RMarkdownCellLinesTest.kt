package org.jetbrains.r.editor

import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.r.psi.RLanguage
import com.intellij.testFramework.common.ThreadLeakTracker
import com.jetbrains.python.PythonLanguage
import org.intellij.plugins.markdown.lang.MarkdownLanguage
import org.jetbrains.r.visualization.RNotebookCellLines
import org.jetbrains.r.visualization.RNotebookCellLines.CellType.CODE
import org.jetbrains.r.visualization.RNotebookCellLines.CellType.MARKDOWN
import org.jetbrains.r.visualization.RNotebookCellLines.MarkersAtLines
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
      intervals {
        rmdInterval(MARKDOWN, 0..2, MarkdownLanguage.INSTANCE)
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
      intervals {
        rmdInterval(CODE, 0..2, RLanguage.INSTANCE)
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
      intervals {
        rmdInterval(CODE, 0..1, RLanguage.INSTANCE)
        rmdInterval(CODE, 2..3, PythonLanguage.INSTANCE)
        rmdInterval(CODE, 4..5, RLanguage.INSTANCE)
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
      intervals {
        rmdInterval(MARKDOWN, 0..2, MarkdownLanguage.INSTANCE)
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
      intervals {
        rmdInterval(MARKDOWN, 0..0, MarkdownLanguage.INSTANCE)
        rmdInterval(MARKDOWN, 1..4, MarkdownLanguage.INSTANCE)
      }
    }
  }

  @Test
  fun `add code cell after text`(): Unit = edt {
    fixture.openNotebookTextInEditor("text<caret>")
    assertCodeCells {
      intervals {
        rmdInterval(MARKDOWN, 0..0, MarkdownLanguage.INSTANCE)
      }
    }

    assertCodeCells {
      fixture.performEditorAction("RMarkdownNewChunk")

      intervals {
        rmdInterval(MARKDOWN, 0..0, MarkdownLanguage.INSTANCE)
        rmdInterval(CODE, 1..3, RLanguage.INSTANCE)
        rmdInterval(MARKDOWN, 4..4, MarkdownLanguage.INSTANCE)
      }
      intervalListenerCall(1) {
        before {
        }
        after {
          rmdInterval(CODE, 1..3, RLanguage.INSTANCE)
          rmdInterval(MARKDOWN, 4..4, MarkdownLanguage.INSTANCE)
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
      intervals {
        rmdInterval(CODE, 0..2, RLanguage.INSTANCE)
        rmdInterval(CODE, 3..5, RLanguage.INSTANCE)
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
      intervals {
        rmdInterval(CODE, 0..2, RLanguage.INSTANCE)
        rmdInterval(MARKDOWN, 3..3, MarkdownLanguage.INSTANCE)
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
      intervals {
        rmdInterval(MARKDOWN, 0..9, MarkdownLanguage.INSTANCE)
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
      intervals {
        rmdInterval(MARKDOWN, 0..0, MarkdownLanguage.INSTANCE)
        rmdInterval(MARKDOWN, 1..1, MarkdownLanguage.INSTANCE)
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
      intervals {
        rmdInterval(MARKDOWN, 0..3, MarkdownLanguage.INSTANCE)
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
      intervals {
        rmdInterval(MARKDOWN, 0..4, MarkdownLanguage.INSTANCE)
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
      intervals {
        rmdInterval(MARKDOWN, 0..4, MarkdownLanguage.INSTANCE)
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
      intervals {
        rmdInterval(MARKDOWN, 0..1, MarkdownLanguage.INSTANCE)
        rmdInterval(MARKDOWN, 2..3, MarkdownLanguage.INSTANCE)
      }
    }
  }

  @Test
  fun `test add text to empty document and delete`(): Unit = edt {
    fixture.openNotebookTextInEditor("")

    assertCodeCells("add text") {
      fixture.type("add\nmultiline text")
      intervals {
        rmdInterval(MARKDOWN, 0..1, MarkdownLanguage.INSTANCE)
      }
      intervalListenerCall(0) {
        before {
          rmdInterval(MARKDOWN, 0..0, MarkdownLanguage.INSTANCE)
        }
        after {
          rmdInterval(MARKDOWN, 0..1, MarkdownLanguage.INSTANCE)
        }
      }
    }

    assertCodeCells("remove all text") {
      fixture.performEditorAction(IdeActions.ACTION_SELECT_ALL)
      fixture.performEditorAction(IdeActions.ACTION_EDITOR_DELETE)
      intervals {
        rmdInterval(MARKDOWN, 0..0, MarkdownLanguage.INSTANCE)
      }
      intervalListenerCall(0) {
        before {
          rmdInterval(MARKDOWN, 0..1, MarkdownLanguage.INSTANCE)
        }
        after {
          rmdInterval(MARKDOWN, 0..0, MarkdownLanguage.INSTANCE)
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
      intervals {
        rmdInterval(CODE, 0..2, RLanguage.INSTANCE)
      }
      intervalListenerCall(0) {
        before {
          rmdInterval(MARKDOWN, 0..0, MarkdownLanguage.INSTANCE)
        }
        after {
          rmdInterval(MARKDOWN, 0..1, MarkdownLanguage.INSTANCE)
        }
      }
      intervalListenerCall(0) {
        before {
          rmdInterval(MARKDOWN, 0..1, MarkdownLanguage.INSTANCE)
        }
        after {
          rmdInterval(MARKDOWN, 0..2, MarkdownLanguage.INSTANCE)
        }
      }
      intervalListenerCall(0) {
        before {
          rmdInterval(MARKDOWN, 0..2, MarkdownLanguage.INSTANCE)
        }
        after {
          rmdInterval(CODE, 0..2, RLanguage.INSTANCE)
        }
      }
    }

    assertCodeCells("remove code") {
      fixture.performEditorAction(IdeActions.ACTION_SELECT_ALL)
      fixture.performEditorAction(IdeActions.ACTION_EDITOR_DELETE)
      intervals {
        rmdInterval(MARKDOWN, 0..0, MarkdownLanguage.INSTANCE)
      }
      intervalListenerCall(0) {
        before {
          rmdInterval(CODE, 0..2, RLanguage.INSTANCE)
        }
        after {
          rmdInterval(MARKDOWN, 0..0, MarkdownLanguage.INSTANCE)
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
      intervals {
        rmdInterval(MARKDOWN, 0..2, MarkdownLanguage.INSTANCE)
      }
    }

    assertCodeCells("insert ```") {
      fixture.type("```")
      // actually it types ```<caret>`,
      // ``` is valid start of cell, but ```` is invalid and ignored
      intervals {
        rmdInterval(MARKDOWN, 0..2, MarkdownLanguage.INSTANCE)
      }
    }

    assertCodeCells("complete start of the cell to ```{r}") {
      fixture.type("{r}")
      intervals {
        rmdInterval(MARKDOWN, 0..0, MarkdownLanguage.INSTANCE)
        rmdInterval(MARKDOWN, 1..2, MarkdownLanguage.INSTANCE)
      }
      intervalListenerCall(0) {
        before {
          rmdInterval(MARKDOWN, 0..2, MarkdownLanguage.INSTANCE)
        }
        after {
          rmdInterval(MARKDOWN, 0..0, MarkdownLanguage.INSTANCE)
          rmdInterval(MARKDOWN, 1..2, MarkdownLanguage.INSTANCE)
        }
      }
    }

    assertCodeCells("add code cell end") {
      // one ` was already typed and placed after caret.
      // line with ```{r}``` isn't a chunk
      fixture.type("``")
      intervals {
        rmdInterval(MARKDOWN, 0..0, MarkdownLanguage.INSTANCE)
        rmdInterval(MARKDOWN, 1..2, MarkdownLanguage.INSTANCE)
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
      intervals {
        rmdInterval(MARKDOWN, 0..0, MarkdownLanguage.INSTANCE)
        rmdInterval(CODE, 1..2, RLanguage.INSTANCE)
        rmdInterval(MARKDOWN, 3..3, MarkdownLanguage.INSTANCE)
      }
      intervalListenerCall(1) {
        before {
          rmdInterval(MARKDOWN, 1..2, MarkdownLanguage.INSTANCE)
        }
        after {
          rmdInterval(CODE, 1..2, RLanguage.INSTANCE)
          rmdInterval(MARKDOWN, 3..3, MarkdownLanguage.INSTANCE)
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
      intervals {
        rmdInterval(MARKDOWN, 0..0, MarkdownLanguage.INSTANCE)
        rmdInterval(CODE, 1..3, RLanguage.INSTANCE)
        rmdInterval(MARKDOWN, 4..4, MarkdownLanguage.INSTANCE)
      }
    }

    assertCodeCells {
      fixture.performEditorAction(IdeActions.ACTION_EDITOR_DELETE_LINE)

      intervals {
        rmdInterval(MARKDOWN, 0..3, MarkdownLanguage.INSTANCE)
      }
      intervalListenerCall(0) {
        before {
          rmdInterval(MARKDOWN, 0..0, MarkdownLanguage.INSTANCE)
          rmdInterval(CODE, 1..3, RLanguage.INSTANCE)
          rmdInterval(MARKDOWN, 4..4, MarkdownLanguage.INSTANCE)
        }
        after {
          rmdInterval(MARKDOWN, 0..3, MarkdownLanguage.INSTANCE)
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
      intervals {
        rmdInterval(CODE, 0..1, RLanguage.INSTANCE)
        rmdInterval(MARKDOWN, 2..2, MarkdownLanguage.INSTANCE)
      }
    }

    val attemptsCount = 3
    for (attempt in 1..attemptsCount) {
      assertCodeCells("add new code line: attempt ${attempt} of ${attemptsCount}") {
        fixture.type("\n")

        intervals {
          rmdInterval(CODE, 0..2, RLanguage.INSTANCE)
          rmdInterval(MARKDOWN, 3..3, MarkdownLanguage.INSTANCE)
        }

        intervalListenerCall(0) {
          before {
            rmdInterval(CODE, 0..1, RLanguage.INSTANCE)
          }
          after {
            rmdInterval(CODE, 0..2, RLanguage.INSTANCE)
          }
        }
      }

      assertCodeCells("remove code line: attempt ${attempt} of ${attemptsCount}") {
        fixture.type("\b")

        intervals {
          rmdInterval(CODE, 0..1, RLanguage.INSTANCE)
          rmdInterval(MARKDOWN, 2..2, MarkdownLanguage.INSTANCE)
        }

        intervalListenerCall(0) {
          before {
            rmdInterval(CODE, 0..2, RLanguage.INSTANCE)
          }
          after {
            rmdInterval(CODE, 0..1, RLanguage.INSTANCE)
          }
        }
      }
    }
  }

  @Test
  fun `test incorrect language`(): Unit = edt {
    fixture.openNotebookTextInEditor("""
      ```{rrr}
      not r cell
      ```
    """.trimIndent())

    assertCodeCells {
      intervals {
        rmdInterval(CODE, 0..2, PlainTextLanguage.INSTANCE)
      }
    }
  }

  private fun assertCodeCells(description: String = "", handler: RCodeCellLinesChecker.() -> Unit) {
    RCodeCellLinesChecker(description) { fixture.editor as EditorImpl }.invoke(handler)
  }

  @Before
  fun before() {
    ThreadLeakTracker.longRunningThreadCreated(ApplicationManager.getApplication(), "Timer-", "BaseDataReader", "rwrapper")
  }
}

private fun RCodeCellLinesChecker.IntervalsSetter.rmdInterval(
  cellType: RNotebookCellLines.CellType,
  lines: IntRange,
  language: Language,
) {
  val markersAtLines = when (cellType) {
    CODE -> MarkersAtLines.TOP_AND_BOTTOM
    else -> MarkersAtLines.NO
  }

  interval(cellType, lines, markersAtLines, language)
}