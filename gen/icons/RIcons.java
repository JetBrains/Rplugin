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
  /** 16x16 */ public static final @NotNull Icon ChangeWorkingDirectory = load("icons/changeWorkingDirectory.svg", -741597682, 2);

  public static final class Debug {
    /** 16x16 */ public static final @NotNull Icon StepIntoMyCode = load("icons/debug/StepIntoMyCode.svg", -301389101, 2);
  }

  public static final class FileTypes {
    /** 16x16 */ public static final @NotNull Icon HtmlNotebook = load("icons/fileTypes/htmlNotebook.svg", -1191597828, 0);
    /** 16x16 */ public static final @NotNull Icon Presentation = load("icons/fileTypes/presentation.svg", 555635250, 0);
    /** 16x16 */ public static final @NotNull Icon RMarkdown = load("icons/fileTypes/rMarkdown.svg", -1194404262, 0);
    /** 16x16 */ public static final @NotNull Icon Shiny = load("icons/fileTypes/shiny.svg", -1176787047, 0);
  }

  /** 16x16 */ public static final @NotNull Icon MachineLearning = load("icons/machineLearning.svg", -239923723, 0);

  public static final class Packages {
    /** 16x16 */ public static final @NotNull Icon DocumentsRoot = load("icons/packages/documentsRoot.svg", -2097008969, 0);
    /** 16x16 */ public static final @NotNull Icon RTest = load("icons/packages/rTest.svg", 1105927713, 0);
    /** 16x16 */ public static final @NotNull Icon UpgradeAll = load("icons/packages/upgradeAll.svg", 1967176985, 2);
  }

  /** 16x16 */ public static final @NotNull Icon R = load("icons/r.svg", 224739393, 0);
  /** 16x16 */ public static final @NotNull Icon Render = load("icons/render.svg", -967889038, 2);
  /** 16x16 */ public static final @NotNull Icon Render_dark1 = load("icons/render_dark-1.svg", -689555824, 0);
  /** 16x16 */ public static final @NotNull Icon RMarkdown = load("icons/rMarkdown.svg", -1194404262, 0);
  /** 16x16 */ public static final @NotNull Icon ROpenTest = load("icons/rOpenTest.svg", -334127438, 2);

  public static final class Run {
    /** 16x16 */ public static final @NotNull Icon DebugCurrent = load("icons/run/debugCurrent.svg", 1582534527, 2);
    /** 16x16 */ public static final @NotNull Icon RestartJob = load("icons/run/restartJob.svg", 834428329, 2);
    /** 16x16 */ public static final @NotNull Icon RunAbove = load("icons/run/runAbove.svg", 1524258236, 2);
    /** 16x16 */ public static final @NotNull Icon RunBelow = load("icons/run/runBelow.svg", -506207761, 2);
    /** 16x16 */ public static final @NotNull Icon RunCurrent = load("icons/run/runCurrent.svg", -813897266, 2);
    /** 16x16 */ public static final @NotNull Icon RunJob = load("icons/run/runJob.svg", -1903768841, 2);
  }

  public static final class ToolWindow {
    /** 13x13 */ public static final @NotNull Icon RConsole = load("icons/toolWindow/RConsole.svg", 1894395341, 2);
    /** 13x13 */ public static final @NotNull Icon RGraph = load("icons/toolWindow/RGraph.svg", 146929391, 2);
    /** 13x13 */ public static final @NotNull Icon RHtml = load("icons/toolWindow/RHtml.svg", 443209157, 2);
    /** 13x13 */ public static final @NotNull Icon RPackages = load("icons/toolWindow/RPackages.svg", 698210638, 2);
    /** 13x13 */ public static final @NotNull Icon RTable = load("icons/toolWindow/RTable.svg", -1514986702, 2);
  }
}
