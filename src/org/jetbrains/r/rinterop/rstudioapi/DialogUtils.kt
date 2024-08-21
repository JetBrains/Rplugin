package org.jetbrains.r.rinterop.rstudioapi

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.icons.AllIcons
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.showOkCancelDialog
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.dialog
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
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


object DialogUtils {
  fun askForPassword(args: RObject): Promise<RObject> {
    val message = args.rString.getStrings(0)
    lateinit var password: JBPasswordField
    val panel = panel {
      row { label(message) }
      row {
        password = passwordField()
          .focused()
          .columns(30)
          .addValidationRule(RBundle.message("rstudioapi.show.dialog.password.not.empty")) { it.password.isEmpty() }
          .component
      }
    }
    val promise = AsyncPromise<RObject>()
    runInEdt {
      val result = if (dialog("", panel).showAndGet()) {
        password.password.joinToString("").toRString()
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
    lateinit var textField: JBTextField
    val panel = panel {
      row { label(message) }
      row { textField = textField()
        .columns(30)
        .focused()
        .component
      }
    }
    textField.text = default
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

    lateinit var secretField: JPasswordField
    lateinit var keyringCheckbox: JBCheckBox

    val panel = panel {
      row { label(title) }
      row {
        secretField = passwordField()
          .focused()
          .columns(30)
          .addValidationRule(RBundle.message("rstudioapi.show.dialog.secret.not.empty")) {
            it.password.isEmpty()
          }.component
      }
      row { keyringCheckbox = checkBox(RBundle.message("rstudioapi.remember.with.keyring.checkbox")).component }
      row { text(RBundle.message("rstudioapi.remember.with.keyring.note")) }
    }
    PasswordSafe.instance.getPassword(credentialAttributes)?.let {
      secretField.text = it
    }
    keyringCheckbox.isSelected = true
    val promise = AsyncPromise<RObject>()
    runInEdt {
      val result = if (dialog(message, panel).showAndGet()) {
        val password = secretField.password.joinToString("")
        if (keyringCheckbox.isSelected) {
          val credentials = Credentials("", password)
          PasswordSafe.instance[credentialAttributes] = credentials
        }
        else {
          PasswordSafe.instance[credentialAttributes] = null
        }
        password.toRString()
      }
      else {
        rError("Ask for secret operation was cancelled.")
      }
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
        if (!selectFolder) {
          row { label(filter!!) }
        }
        row {
          cell(fileChooser).also { fChooser ->
            if (!selectFolder && extension != null) {
              fChooser.addValidationRule(RBundle.message("rstudioapi.show.dialog.file.extension.should.match", extension)) {
                !it.text.endsWith(".$extension")
              }
            }
          }.columns(40).focused()
        }
      }
      runInEdt {
        fileChooser.isEditable = !existing
        val result = if (dialog(caption, panel).showAndGet()) {
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
}