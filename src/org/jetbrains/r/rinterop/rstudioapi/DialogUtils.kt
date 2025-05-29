package org.jetbrains.r.rinterop.rstudioapi

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.icons.AllIcons
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.EDT
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
  suspend fun askForPassword(args: RObject): RObject =
    withContext(Dispatchers.EDT) {
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

      if (dialog("", panel).showAndGet()) {
        password.password.joinToString("").toRString()
      }
      else {
        getRNull()
      }
    }

  suspend fun showQuestion(args: RObject): RObject =
    withContext(Dispatchers.EDT) {
      val (title, message, ok, cancel) = args.rString.stringsList
      val result = showOkCancelDialog(title, message, ok, cancel, AllIcons.General.QuestionDialog)
      (result == Messages.OK).toRBoolean()
    }

  suspend fun showPrompt(args: RObject): RObject =
    withContext(Dispatchers.EDT) {
      val (title, message, default) = args.rString.stringsList
      lateinit var textField: JBTextField
      val panel = panel {
        row { label(message) }
        row {
          textField = textField()
            .columns(30)
            .focused()
            .component
        }
      }
      textField.text = default

      if (dialog(title, panel).showAndGet()) {
        textField.text.toRString()
      }
      else getRNull()
    }

  suspend fun askForSecret(args: RObject): RObject = withContext(Dispatchers.EDT) {
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

    if (dialog(message, panel).showAndGet()) {
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
  }

  private fun createCredentialAttributes(key: String): CredentialAttributes {
    return CredentialAttributes(generateServiceName("rstudioapi", key))
  }

  suspend fun selectFile(rInterop: RInterop, args: RObject): RObject {
    return selectHelper(rInterop, args)
  }

  suspend fun selectDirectory(rInterop: RInterop, args: RObject): RObject {
    return selectHelper(rInterop, args, selectFolder = true)
  }

  private suspend fun selectHelper(rInterop: RInterop, args: RObject, selectFolder: Boolean = false): RObject {
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

    return withContext(Dispatchers.EDT) {
      if (rInterop.interpreter.isLocal() && existing) {
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

        return@withContext if (files.isEmpty()) getRNull()
        else files.first().path.toRString()
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

        fileChooser.isEditable = !existing
        if (dialog(caption, panel).showAndGet()) {
          fileChooser.text.toRString()
        }
        else getRNull()
      }
    }
  }

  suspend fun showDialog(args: RObject): RObject {
    val (title, message, url) = args.rString.stringsList
    val msg = RBundle.message("rstudioapi.show.dialog.message", message, url)
    withContext(Dispatchers.EDT) {
      Messages.showInfoMessage(msg, title)
    }
    return getRNull()
  }

  suspend fun updateDialog(args: RObject): RObject {
    TODO()
  }
}
