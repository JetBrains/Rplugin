package org.jetbrains.r.actions

import com.intellij.ide.IdeView
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.ide.fileTemplates.actions.AttributesDefaults
import com.intellij.ide.fileTemplates.actions.CreateFromTemplateAction
import com.intellij.ide.impl.ProjectViewSelectInTarget
import com.intellij.ide.projectView.impl.ProjectViewPane
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.RFileType
import com.intellij.r.psi.icons.RIcons
import com.intellij.r.psi.packages.build.RPackageBuildUtil

class CreateRTestFileAction : CreateFromTemplateAction(FileTemplateManager.getDefaultInstance().getInternalTemplate(TEST_FILE_TEMPLATE)) {

  init {
    templatePresentation.text = RBundle.message("rtest.create")
    templatePresentation.description = RBundle.message("rtest.description")
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isEnabledAndVisible = e.project != null && isPsiFileForTest(e.psiFile)
    e.presentation.icon = RIcons.ROpenTest
    e.presentation.text = if (getTestFile(e) == null) RBundle.message("rtest.create") else RBundle.message("rtest.open")
  }

  override fun getAttributesDefaults(dataContext: DataContext?): AttributesDefaults? {
    dataContext ?: return null
    val file = CommonDataKeys.PSI_FILE.getData(dataContext) ?: return null
    val defaults = AttributesDefaults(getTestFileName(file))
      .withFixedName(true)
    defaults.addPredefined("FileName", getFileName(file))
    return defaults
  }

  override fun getTargetDirectory(dataContext: DataContext?, view: IdeView?): PsiDirectory? {
    return getOrCreateTestsDirectory(dataContext, "testthat")
  }

  private fun getTestFile(e: AnActionEvent): VirtualFile? {
    val fileName = getTestFileName(e.psiFile ?: return null)
    return RPackageBuildUtil.findTestFile(e.project!!, fileName)
  }

  private fun getOrCreateTestsDirectory(dataContext: DataContext?, childDir: String? = null): PsiDirectory? {
    dataContext ?: return null
    val project = LangDataKeys.PROJECT.getData(dataContext) ?: return null
    val sourceRoots = project.guessProjectDir() ?: return null
    var targetDir = sourceRoots.findOrCreateChildDirectory("tests")
    createTestThatFile(PsiManager.getInstance(project).findDirectory(targetDir))
    if (childDir != null) targetDir = targetDir.findOrCreateChildDirectory(childDir)
    return PsiManager.getInstance(project).findDirectory(targetDir)
  }

  private fun VirtualFile.findOrCreateChildDirectory(name: String): VirtualFile {
    val child = findChild(name)
    if (child != null) return child
    return runWriteAction { createChildDirectory(this, name) }
  }

  override fun getReplacedAction(selectedTemplate: FileTemplate?) = object : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
      val dataContext = e.dataContext
      val view = LangDataKeys.IDE_VIEW.getData(dataContext) ?: return
      val dir = getTargetDirectory(dataContext, view) ?: return
      val project = dir.project
      FileTemplateManager.getInstance(project).addRecentName(selectedTemplate!!.name)
      val defaults = getAttributesDefaults(dataContext)
      val psiFile = dir.findFile(defaults?.defaultFileName!!)
      if (psiFile == null) {
        LOG.info("Creating test file ${defaults.defaultFileName}")
        val createdElement = FileTemplateUtil.createFromTemplate(selectedTemplate, defaults.defaultFileName, defaults.defaultProperties, dir)
        openFile(createdElement.containingFile)
        if (selectedTemplate.isLiveTemplateEnabled && createdElement is PsiFile) {
          val defaultValues = getLiveTemplateDefaults(dataContext, createdElement)
          startLiveTemplate(createdElement, defaultValues ?: emptyMap<String?, String>())
        }
      } else {
        openFile(psiFile)
      }
    }
  }

  fun openFile(file: PsiFile) {
    ProjectViewSelectInTarget.select(file.project, file, ProjectViewPane.ID, null, file.virtualFile, true)
    if (PsiNavigationSupport.getInstance().canNavigate(file))
      (file as Navigatable).navigate(true)
  }

  private fun createTestThatFile(dir: PsiDirectory?): Boolean? {
    dir ?: return null
    val selectedTemplate = FileTemplateManager.getDefaultInstance().getInternalTemplate(TESTTHAT_FILE_TEMPLATE)
    FileTemplateManager.getInstance(dir.project).addRecentName(selectedTemplate.name)
    val defaults = AttributesDefaults("testthat.R")
      .withFixedName(true)
    defaults.addPredefined("PackageName", RPackageBuildUtil.getPackageName(dir.project) ?: "package")
    if (dir.findFile("testthat.R") == null) {
      LOG.info("Creating testthat file")
      FileTemplateUtil.createFromTemplate(selectedTemplate, defaults.defaultFileName, defaults.defaultProperties, dir)
      return true
    }
    return false
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  private fun getFileName(file: PsiFile): String {
    return file.virtualFile.nameWithoutExtension
  }
  private fun getTestFileName(file: PsiFile): String  = "test-${getFileName(file)}.R"

  companion object {
    private val LOG = Logger.getInstance(CreateRTestFileAction::class.java)
    private const val TEST_FILE_TEMPLATE = "R Test"
    private const val TESTTHAT_FILE_TEMPLATE = "testthat"
  }
}

fun isPsiFileForTest(file: PsiFile?): Boolean {
  return file != null
         && isVirtualFileForTest(file.virtualFile, file.project)
}

fun isVirtualFileForTest(file: VirtualFile, project: Project): Boolean {
  return FileTypeRegistry.getInstance().isFileOfType(file, RFileType)
         && RPackageBuildUtil.isPackage(project)
         // According to https://cran.r-project.org/doc/manuals/R-exts.html#Package-subdirectories there could be only unix and windows
         // subdirectories in R package
         && file.parent?.name == "R"
}