package org.jetbrains.r.rinterop.rstudioapi

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.showOkCancelDialog
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.dialog
import com.intellij.ui.layout.*
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.RBundle
import org.jetbrains.r.interpreter.isLocal
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RObject
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.getRNull
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.rError
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.toRBoolean
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.toRString
import javax.swing.JPasswordField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.credentialStore.Credentials


object DialogUtils {
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
      addValidateFieldNotEmpty(dialog, passwordField)
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

    val credentialAttributes = createCredentialAttributes(name)

    val secretField = JPasswordField()
    PasswordSafe.instance.getPassword(credentialAttributes)?.let {
      secretField.text = it
    }

    val keyringCheckbox = JBCheckBox(RBundle.message("rstudioapi.remember.with.keyring.checkbox"))
    keyringCheckbox.isSelected = true
    val panel = panel {
      noteRow(title)
      row { secretField().focused() }
      row { keyringCheckbox() }
      noteRow(RBundle.message("rstudioapi.remember.with.keyring.note"))
    }
    val promise = AsyncPromise<RObject>()
    runInEdt {
      val dialog = dialog(message, panel)
      dialog.isOKActionEnabled = secretField.password.isNotEmpty()
      addValidateFieldNotEmpty(dialog, secretField)
      val result = if (dialog.showAndGet()) {
        val password = secretField.password.joinToString("")
        if (keyringCheckbox.isSelected) {
          val credentials = Credentials("", password)
          PasswordSafe.instance[credentialAttributes] = credentials
        }
        password.toRString()
      }
      else rError("Ask for secret operation was cancelled.")
      promise.setResult(result)
    }
    return promise
  }

  private fun createCredentialAttributes(key: String): CredentialAttributes {
    return CredentialAttributes(generateServiceName("rstudioapi", key))
  }

  fun selectFile(rInterop: RInterop, args: RObject): Promise<RObject> {
    return selectHelper(rInterop, args)
  }

  fun selectDirectory(rInterop: RInterop, args: RObject): Promise<RObject> {
    return selectHelper(rInterop, args, true)
  }

  private fun selectHelper(rInterop: RInterop, args: RObject, selectFolder: Boolean = false): Promise<RObject> {
    val caption = args.list.getRObjects(0).rString.getStrings(0)
    val label = args.list.getRObjects(1).rString.getStrings(0)
    val path = args.list.getRObjects(2).rString.getStrings(0)
    val filter = if (selectFolder) null else args.list.getRObjects(3).rString.getStrings(0)
    val extension = when (filter) {
      "All Files (*)" -> null
      "R Files (*.R)" -> "R"
      "R Markdown Files (*.Rmd)" -> "Rmd"
      else -> null
    }
    val existing = if (selectFolder) true else args.list.getRObjects(4).rBoolean.getBooleans(0)
    val promise = AsyncPromise<RObject>()
    if (rInterop.interpreter.isLocal() && existing) {
      runInEdt {
        val descriptor = if (!selectFolder) {
          extension?.let {
            FileChooserDescriptorFactory.createSingleFileDescriptor(extension).withTitle(caption)
          } ?: FileChooserDescriptorFactory.createSingleFileDescriptor().withTitle(caption)
        }
        else {
          FileChooserDescriptorFactory.createSingleFolderDescriptor().withTitle(caption)
        }
        descriptor.isForcedToUseIdeaFileChooser = true
        val fileChooserDialog = FileChooserFactory.getInstance().createFileChooser(descriptor, rInterop.project, null)
        val toSelect = LocalFileSystem.getInstance().refreshAndFindFileByPath(path)
        val files = fileChooserDialog.choose(rInterop.project, toSelect!!)
        if (files.isEmpty()) {
          promise.setResult(getRNull())
        }
        else {
          promise.setResult(files.first().path.toRString())
        }
      }
    }
    else {
      val fileChooser = rInterop.interpreter.createFileChooserForHost(path, selectFolder)
      val panel = panel {
        if (!selectFolder) noteRow(filter!!)
        row { fileChooser().focused() }
      }
      runInEdt {
        val dialog = dialog(caption, panel, okActionEnabled = extension == null)
        val validate: () -> Unit = {
          dialog.isOKActionEnabled = (extension == null || fileChooser.text.endsWith(".$extension"))
        }
        fileChooser.isEditable = !existing
        if (!selectFolder) {
          fileChooser.textField.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent?) = validate()
            override fun insertUpdate(e: DocumentEvent?) = validate()
            override fun removeUpdate(e: DocumentEvent?) = validate()
          })
        }
        val result = if (dialog.showAndGet()) {
          fileChooser.text.toRString()
        }
        else getRNull()
        promise.setResult(result)
      }
    }
    return promise
  }

  fun showDialog(args: RObject): Promise<RObject> {
    val (title, message, url) = args.rString.stringsList
    val msg = RBundle.message("rstudioapi.show.dialog.message", message, url)
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

  private fun addValidateFieldNotEmpty(dialog: DialogWrapper, field: JPasswordField) {
    val validate: () -> Unit = {
      dialog.isOKActionEnabled = field.password.isNotEmpty()
    }
    field.document.addDocumentListener(object : DocumentListener {
      override fun changedUpdate(e: DocumentEvent?) = validate()
      override fun insertUpdate(e: DocumentEvent?) = validate()
      override fun removeUpdate(e: DocumentEvent?) = validate()
    })
  }
}