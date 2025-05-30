package icons;

import com.intellij.ui.IconManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * NOTE THIS FILE IS AUTO-GENERATED
 * DO NOT EDIT IT BY HAND, run "Generate icon classes" configuration instead
 */
public final class RIcons {
  private static @NotNull Icon load(@NotNull String path, int cacheKey, int flags) {
    return IconManager.getInstance().loadRasterizedIcon(path, RIcons.class.getClassLoader(), cacheKey, flags);
  }
  private static @NotNull Icon load(@NotNull String expUIPath, @NotNull String path, int cacheKey, int flags) {
    return IconManager.getInstance().loadRasterizedIcon(path, expUIPath, RIcons.class.getClassLoader(), cacheKey, flags);
  }
  /** 16x16 */ public static final @NotNull Icon ChangeWorkingDirectory = load("icons/expui/setCurrentDirectory.svg", "icons/changeWorkingDirectory.svg", -1651105754, 2);

  public static final class Debug {
    /** 16x16 */ public static final @NotNull Icon StepIntoMyCode = load("icons/debug/StepIntoMyCode.svg", 1496128495, 2);
  }

  public static final class FileTypes {
    /** 16x16 */ public static final @NotNull Icon HtmlNotebook = load("icons/fileTypes/htmlNotebook.svg", 1421443285, 0);
    /** 16x16 */ public static final @NotNull Icon Presentation = load("icons/fileTypes/presentation.svg", -262024095, 0);
    /** 16x16 */ public static final @NotNull Icon Quarto = load("icons/fileTypes/quarto.svg", -1658626261, 0);
    /** 16x16 */ public static final @NotNull Icon RMarkdown = load("icons/fileTypes/rMarkdown.svg", 1137740664, 0);
    /** 16x16 */ public static final @NotNull Icon Shiny = load("icons/fileTypes/shiny.svg", -2101833744, 0);
  }

  public static final class Graphics {
    /** 16x16 */ public static final @NotNull Icon ConstraintProportions = load("icons/graphics/constraintProportions.svg", -501427623, 2);
  }

  /** 16x16 */ public static final @NotNull Icon MachineLearning = load("icons/machineLearning.svg", 667706227, 0);

  public static final class Packages {
    /** 16x16 */ public static final @NotNull Icon DocumentsRoot = load("icons/packages/documentsRoot.svg", -1341931145, 0);
    /** 16x16 */ public static final @NotNull Icon RTest = load("icons/packages/rTest.svg", -1872272236, 0);
    /** 16x16 */ public static final @NotNull Icon UpgradeAll = load("icons/expui/upgradeAll.svg", "icons/packages/upgradeAll.svg", -231640937, 2);
  }

  /** 16x16 */ public static final @NotNull Icon R = load("icons/r.svg", 521006611, 0);
  /** 16x16 */ public static final @NotNull Icon Render = load("icons/render.svg", -1518683048, 2);
  /** 16x16 */ public static final @NotNull Icon Render_dark1 = load("icons/render_dark-1.svg", 2140524824, 0);
  /** 16x16 */ public static final @NotNull Icon RMarkdown = load("icons/expui/rMarkdownFile.svg", "icons/rMarkdown.svg", 1137740664, 0);
  /** 16x16 */ public static final @NotNull Icon ROpenTest = load("icons/rOpenTest.svg", 975755591, 2);

  public static final class Run {
    /** 16x16 */ public static final @NotNull Icon DebugCurrent = load("icons/expui/debugSelection.svg", "icons/run/debugCurrent.svg", -1599735811, 2);
    /** 16x16 */ public static final @NotNull Icon RestartJob = load("icons/expui/restartJob.svg", "icons/run/restartJob.svg", -1815991887, 2);
    /** 16x16 */ public static final @NotNull Icon RunAbove = load("icons/run/runAbove.svg", -1227463414, 2);
    /** 16x16 */ public static final @NotNull Icon RunBelow = load("icons/run/runBelow.svg", -1826950222, 2);
    /** 16x16 */ public static final @NotNull Icon RunCurrent = load("icons/expui/runCursor.svg", "icons/run/runCurrent.svg", 1194380704, 2);
    /** 16x16 */ public static final @NotNull Icon RunJob = load("icons/expui/runRJob.svg", "icons/run/runJob.svg", -1705121289, 2);
  }

  public static final class Table {
    /** 16x16 */ public static final @NotNull Icon Pagination = load("icons/table/pagination.svg", -1428227815, 2);
  }

  public static final class ToolWindow {
    /** 13x13 */ public static final @NotNull Icon RConsole = load("icons/expui/rEngineToolWindow.svg", "icons/toolWindow/RConsole.svg", -789591796, 2);
    /** 13x13 */ public static final @NotNull Icon RGraph = load("icons/expui/toolWindowRGraph.svg", "icons/toolWindow/RGraph.svg", 66042680, 2);
    /** 13x13 */ public static final @NotNull Icon RHtml = load("icons/expui/toolWindowRHtml.svg", "icons/toolWindow/RHtml.svg", 2010807116, 2);
    /** 13x13 */ public static final @NotNull Icon RPackages = load("icons/expui/toolWindowRPackages.svg", "icons/toolWindow/RPackages.svg", -1018126418, 2);
    /** 13x13 */ public static final @NotNull Icon RTable = load("icons/toolWindow/RTable.svg", -749604689, 2);
  }
}
