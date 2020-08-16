package org.jetbrains.r.rinterop.rstudioapi

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.showOkCancelDialog
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.dialog
import com.intellij.ui.layout.*
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.rinterop.RObject
import javax.swing.JPasswordField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

fun askForPassword(args: RObject): Promise<RObject> {
  val message = args.rString.getStrings(0)
  val passwordField = JPasswordField()
  val panel = panel {
    noteRow(message)
    row { passwordField().focused() }
  }
  val promise = AsyncPromise<RObject>()
  runInEdt {
    val dialog = dialog("", panel)
    dialog.isOKActionEnabled = false
    val validate: () -> Unit = {
      dialog.isOKActionEnabled = passwordField.password.isNotEmpty()
    }
    passwordField.document.addDocumentListener(object : DocumentListener {
      override fun changedUpdate(e: DocumentEvent?) = validate()
      override fun insertUpdate(e: DocumentEvent?) = validate()
      override fun removeUpdate(e: DocumentEvent?) = validate()
    })
    val result = if (dialog.showAndGet()) {
      passwordField.password.joinToString("").toRString()
    }
    else {
      getRNull()
    }
    promise.setResult(result)
  }
  return promise
}

fun showQuestion(args: RObject): Promise<RObject> {
  val (title, message, ok, cancel) = args.rString.stringsList
  val promise = AsyncPromise<RObject>()
  runInEdt {
    val result = showOkCancelDialog(title, message, ok, cancel, AllIcons.General.QuestionDialog)
    promise.setResult((result == Messages.OK).toRBoolean())
  }
  return promise
}

fun showPrompt(args: RObject): Promise<RObject> {
  val (title, message, default) = args.rString.stringsList
  val textField = JBTextField()
  textField.text = default
  val panel = panel {
    noteRow(message)
    row { textField().focused() }
  }
  val promise = AsyncPromise<RObject>()
  runInEdt {
    val result = if (dialog(title, panel).showAndGet()) {
      textField.text.toRString()
    }
    else getRNull()
    promise.setResult(result)
  }
  return promise
}

fun askForSecret(args: RObject): Promise<RObject> {
  val (name, message, title) = args.rString.stringsList
  val secretField = JPasswordField()
  val checkBox = JBCheckBox("Remember with keyring")
  val url = "https://support.rstudio.com/hc/en-us/articles/360000969634"
  val panel = panel {
    noteRow(title)
    row { secretField().focused() }
    row { checkBox() }
    noteRow("""<a href="$url">Using Keyring</a>""")
  }.withPreferredWidth(350)
  val promise = AsyncPromise<RObject>()
  runInEdt {
    val dialog = dialog(message, panel)
    dialog.isOKActionEnabled = false
    val validate: () -> Unit = {
      dialog.isOKActionEnabled = secretField.password.isNotEmpty()
    }
    secretField.document.addDocumentListener(object : DocumentListener {
      override fun changedUpdate(e: DocumentEvent?) = validate()
      override fun insertUpdate(e: DocumentEvent?) = validate()
      override fun removeUpdate(e: DocumentEvent?) = validate()
    })
    val result = if (dialog.showAndGet()) {
      if (checkBox.isSelected) {
        // TODO
      }
      secretField.password.joinToString("").toRString()
    }
    else rError("Ask for secret operation was cancelled.")
    promise.setResult(result)
  }
  return promise
}

fun selectFile(args: RObject): Promise<RObject> {
  TODO()
}

fun selectDirectory(args: RObject): Promise<RObject> {
  TODO()
}

fun showDialog(args: RObject): Promise<RObject> {
  val (title, message, url) = args.rString.stringsList
  val msg = "$message\n<a href=\"$url\">$url</a>"
  val promise = AsyncPromise<RObject>()
  runInEdt {
    Messages.showInfoMessage(msg, title)
    promise.setResult(getRNull())
  }
  return promise
}

fun updateDialog(args: RObject): Promise<RObject> {
  TODO()
}