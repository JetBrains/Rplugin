/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote.ui;

import com.google.common.collect.Lists;
import com.intellij.icons.AllIcons;
import com.intellij.ide.ActivityTracker;
import com.intellij.ide.DataManager;
import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.ui.table.JBTable;
import com.intellij.util.CatchingConsumer;
import com.intellij.util.IJSwingUtilities;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.StatusText;
import com.intellij.util.ui.UIUtil;
import com.intellij.webcore.packaging.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.notebooks.visualization.r.ui.ToolbarUtil;
import org.jetbrains.r.RBundle;
import org.jetbrains.r.packages.RInstalledPackage;
import org.jetbrains.r.packages.remote.RPackageManagementService;
import org.jetbrains.r.rinterop.RInteropKt;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RInstalledPackagesPanelBase extends JPanel {
  private static final Logger LOG = Logger.getInstance(InstalledPackagesPanel.class);

  private static final String LOADING_PACKAGES_LIST_TITLE = "Loading Packages List";

  private static final String INSTALL_ACTION_ID = "org.jetbrains.r.packages.remote.ui.RInstallAction";
  private static final String UPGRADE_ACTION_ID = "org.jetbrains.r.packages.remote.ui.RUpgradeAction";
  private static final String LOAD_UNLOAD_ACTION_ID = "org.jetbrains.r.packages.remote.ui.RLoadUnloadAction";
  private static final String NAVIGATE_TO_DOCUMENTATION_ACTION_ID = "org.jetbrains.r.packages.remote.ui.RNavigateToDocumentationAction";
  private static final String OPEN_LINK_ACTION_ID = "org.jetbrains.r.packages.remote.ui.ROpenLinkAction";
  private static final String UNINSTALL_ACTION_ID = "org.jetbrains.r.packages.remote.ui.RUninstallAction";

  public static final int IS_LOADED_COLUMN = 0;
  public static final int PACKAGE_NAME_COLUMN = 1;
  public static final int DESCRIPTION_COLUMN = 2;
  public static final int VERSION_COLUMN = 3;
  public static final int BROWSE_COLUMN = 4;
  public static final int UNINSTALL_COLUMN = 5;
  public static final String TITLE = "Title";

  private final AnActionButton myUpgradeButton;
  protected final AnActionButton myInstallButton;

  private final SettableLinkLabel<?> myPackageNameLinkLabel = new SettableLinkLabel<>();
  private final JBLabel myLabel = new JBLabel();
  private final JBCheckBox myIsLoadedCheckBox = new JBCheckBox();

  protected final JBTable myPackagesTable;
  private final MyFilteringTableModel myPackageFilteringModel;
  private final DefaultTableModel myPackagesTableModel;
  // can be accessed from any thread
  protected volatile RPackageManagementService myPackageManagementService;
  protected final Project myProject;
  protected final PackagesNotificationPanel myNotificationArea;
  private final Set<String> myCurrentlyInstalling = new HashSet<>();
  private final Map<RInstalledPackage, String> myWaitingToUpgrade = new HashMap<>();

  public RInstalledPackagesPanelBase(@NotNull Project project, @NotNull PackagesNotificationPanel area) {
    super(new BorderLayout());
    myProject = project;
    myNotificationArea = area;

    myPackagesTableModel = new DefaultTableModel(new String[]{"", "Package", "Description", "Version", "", ""}, 0) {
      @Override
      public boolean isCellEditable(int i, int i1) {
        return false;
      }
    };
    myPackageFilteringModel = new MyFilteringTableModel(myPackagesTableModel);
    final TableCellRenderer tableCellRenderer = new MyTableCellRenderer();
    myPackagesTable = new JBTable(myPackageFilteringModel) {
      @Override
      public TableCellRenderer getCellRenderer(int row, int column) {
        return tableCellRenderer;
      }
    };
    myPackagesTable.setShowVerticalLines(false);
    myPackagesTable.setShowHorizontalLines(false);
    myPackagesTable.setTableHeader(null);
    myPackagesTable.setPreferredScrollableViewportSize(null);
    myPackagesTable.setCellSelectionEnabled(false);
    myPackagesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    initColumnWidth();

    MessageBusConnection connect = project.getMessageBus().connect();
    connect.subscribe(EditorColorsManager.TOPIC, scheme -> {
      IJSwingUtilities.updateComponentTreeUI(myLabel);
      IJSwingUtilities.updateComponentTreeUI(myPackageNameLinkLabel);
      IJSwingUtilities.updateComponentTreeUI(myIsLoadedCheckBox);
    });

    connect.subscribe(RInteropKt.getLOADED_LIBRARIES_UPDATED(), myPackagesTable::repaint);

    myUpgradeButton = ToolbarUtil.INSTANCE.createAnActionButton(UPGRADE_ACTION_ID, this::upgradeAction);
    myInstallButton = ToolbarUtil.INSTANCE.createAnActionButton(INSTALL_ACTION_ID, this::installAction);
    MyTextSearchField textSearchFieldAction = new MyTextSearchField();
    myInstallButton.setShortcut(CommonShortcuts.getNew());
    ToolbarDecorator decorator =
      ToolbarDecorator.createDecorator(myPackagesTable).disableUpDownActions().disableAddAction().disableRemoveAction()
        .addExtraAction(textSearchFieldAction)
        .addExtraAction(myInstallButton);
    decorator.setToolbarPosition(ActionToolbarPosition.TOP);

    DocumentAdapter listener = new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        String text = textSearchFieldAction.myField.getText();
        if (StringUtil.isEmpty(text)) {
          myPackageFilteringModel.setFilter(null);
          return;
        }

        //MinusculeMatcher matcher = NameUtil.buildMatcher("*" + mySearchField.getText(), NameUtil.MatchingCaseSensitivity.NONE);
        myPackageFilteringModel.setFilter(text);
      }
    };
    textSearchFieldAction.myField.getTextEditor().getDocument().addDocumentListener(listener);

    decorator.addExtraActions(getExtraActions());
    add(decorator.createPanel());
    myInstallButton.setEnabled(false);
    myUpgradeButton.setEnabled(false);

    myPackagesTable.getSelectionModel().addListSelectionListener(event -> updateUninstallUpgrade());

    myPackagesTable.addMouseMotionListener( new MouseMotionAdapter() {
      boolean hit = false;

      @Override
      public void mouseMoved(MouseEvent e) {
        Point columnRow = getMouseColumnRow(e.getLocationOnScreen(), myPackagesTable);
        if (columnRow.x == PACKAGE_NAME_COLUMN) {
          myPackagesTable.repaint();
          hit = true;
        } else if (hit) {
          myPackagesTable.repaint();
          hit = false;
        }

      }
    });

    myPackagesTable.addMouseListener(new MouseInputAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        Point columnRow = getMouseColumnRow(e.getLocationOnScreen(), myPackagesTable);
        if (columnRow.x == IS_LOADED_COLUMN) {
          doRecorded(LOAD_UNLOAD_ACTION_ID, e, () -> loadUnloadPackage(columnRow));
        }
        if (columnRow.x == PACKAGE_NAME_COLUMN) {
          doRecorded(NAVIGATE_TO_DOCUMENTATION_ACTION_ID, e, () -> navigateToDocumentation(columnRow));
        }
        if (columnRow.x == BROWSE_COLUMN) {
          doRecorded(OPEN_LINK_ACTION_ID, e, () -> openLink(columnRow));
        }
        if (columnRow.x == UNINSTALL_COLUMN) {
          // Note: recorded inside `uninstallPackage()`
          uninstallPackage(e, columnRow);
        }
      }

      private void loadUnloadPackage(Point columnRow) {
        RInstalledPackage aPackage = getInstalledPackageAt(columnRow.y);
        if (aPackage != null) {
          var packageName = aPackage.getName();
          if (myPackageManagementService.isPackageLoaded(packageName)) {
            myPackageManagementService.unloadPackage(packageName, false);
          } else {
            myPackageManagementService.loadPackage(packageName);
          }
        }
      }

      private void navigateToDocumentation(Point columnRow) {
        RInstalledPackage aPackage = getInstalledPackageAt(columnRow.y);
        if (aPackage != null) {
          myPackageManagementService.navigateToPackageDocumentation(aPackage);
        }
      }

      private void uninstallPackage(MouseEvent e, Point columnRow) {
        RInstalledPackage aPackage = getInstalledPackageAt(columnRow.y);
        if (aPackage != null && myPackageManagementService.canUninstallPackage(aPackage)) {
          int yesNo = Messages.showYesNoDialog(myPackagesTable,
                                               RBundle.INSTANCE
                                                 .message("install.package.dialog.message.are.you.sure.you.wish.to.uninstall.package",
                                                          aPackage.getName()),
                                               RBundle.INSTANCE.message("install.package.dialog.title.uninstall", aPackage.getName()),
                                               AllIcons.Diff.Remove);
          if (yesNo == Messages.YES) {
            doRecorded(UNINSTALL_ACTION_ID, e, () -> uninstallAction(Collections.singletonList(aPackage)));
          }
        }
      }

      private void openLink(Point columnRow) {
        RInstalledPackage installedPackage = getInstalledPackageAt(columnRow.y);
        if (installedPackage != null) {
          String url = installedPackage.getDescription().get("URL");
          String link = null;
          if (url != null && (url.startsWith("http"))) {
            int firstLinkEnded = url.indexOf(", ");
            if (firstLinkEnded == -1) {
              link = url;
            } else {
              link = url.substring(0, firstLinkEnded);
            }
          }
          if (link == null) {
            link = "https://cran.r-project.org/package=" + installedPackage.getName();
          }
          BrowserLauncher.getInstance().browse(link, null);
        }
      }
    });
  }

  private void initColumnWidth() {
    TableColumnModel model = myPackagesTable.getColumnModel();
    int singleIconWidth = JBUIScale.scale(20);
    int versionMinWidth = GraphicsUtil.stringWidth("1.2.3.4", getFont());
    int versionMaxWidth = GraphicsUtil.stringWidth("1.2.3.9999.1.2.3", getFont());
    int packageMinWidth = GraphicsUtil.stringWidth("ggplot2", getFont());
    int packageMaxWidth = GraphicsUtil.stringWidth("veryVeryLongPackageName", getFont());
    int descriptionMinWidth = GraphicsUtil.stringWidth("very short package description", getFont());
    model.getColumn(IS_LOADED_COLUMN).setMinWidth(singleIconWidth * 3 / 2);
    model.getColumn(IS_LOADED_COLUMN).setMaxWidth(singleIconWidth * 3 / 2);
    model.getColumn(PACKAGE_NAME_COLUMN).setMinWidth(packageMinWidth);
    model.getColumn(PACKAGE_NAME_COLUMN).setMaxWidth(packageMaxWidth);
    model.getColumn(DESCRIPTION_COLUMN).setMinWidth(descriptionMinWidth);
    model.getColumn(DESCRIPTION_COLUMN).setMaxWidth(descriptionMinWidth * 10);
    model.getColumn(VERSION_COLUMN).setMinWidth(versionMinWidth);
    model.getColumn(VERSION_COLUMN).setMaxWidth(versionMaxWidth);
    model.getColumn(BROWSE_COLUMN).setMinWidth(singleIconWidth * 3 / 2);
    model.getColumn(BROWSE_COLUMN).setMaxWidth(singleIconWidth * 3 / 2);
    model.getColumn(UNINSTALL_COLUMN).setMinWidth(singleIconWidth * 3 / 2);
    model.getColumn(UNINSTALL_COLUMN).setMaxWidth(singleIconWidth * 3 / 2);
  }

  @NotNull
  private static Point getMouseColumnRow(Point screen, JTable table) {
    Point location = new Point(screen);
    SwingUtilities.convertPointFromScreen(location, table);
    int columnAtPoint = table.columnAtPoint(location);
    int rowAtPoint = table.rowAtPoint(location);
    return new Point(columnAtPoint, rowAtPoint);
  }

  protected AnActionButton[] getExtraActions() {
    return new AnActionButton[0];
  }

  @NotNull
  protected ManagePackagesDialog createManagePackagesDialog() {
    return new ManagePackagesDialog(myProject,
                                    myPackageManagementService,
                                    new PackageManagementService.Listener() {
                                      @Override
                                      public void operationStarted(String packageName) {
                                        myNotificationArea.hide();
                                      }

                                      @Override
                                      public void operationFinished(String packageName,
                                                                    @Nullable PackageManagementService.ErrorDescription errorDescription) {
                                        myNotificationArea.showResult(packageName, errorDescription);
                                        myPackagesTable.clearSelection();
                                        doUpdatePackages(myPackageManagementService);
                                      }
                                    });
  }

  private void installAction() {
    if (myPackageManagementService != null) {
      ManagePackagesDialog dialog = createManagePackagesDialog();
      dialog.show();
    }
  }

  private void upgradeAction() {
    final int[] rows = myPackagesTable.getSelectedRows();
    if (myPackageManagementService != null) {
      final Set<String> upgradedPackages = new HashSet<>();
      final Set<String> packagesShouldBePostponed = getPackagesToPostpone();
      for (int row : rows) {
        final Object packageObj = myPackagesTableModel.getValueAt(row, 0);
        if (packageObj instanceof RInstalledPackage pkg) {
          final String packageName = pkg.getName();
          final String currentVersion = pkg.getVersion();
          final String availableVersion = (String)myPackagesTableModel.getValueAt(row, 2);

          if (packagesShouldBePostponed.contains(packageName)) {
            myWaitingToUpgrade.put(pkg, availableVersion);
          }
          else if (isUpdateAvailable(currentVersion, availableVersion)) {
            upgradePackage(pkg, availableVersion);
            upgradedPackages.add(packageName);
          }
        }
      }

      if (myCurrentlyInstalling.isEmpty() && upgradedPackages.isEmpty() && !myWaitingToUpgrade.isEmpty()) {
        upgradePostponedPackages();
      }
    }
  }

  private void upgradePostponedPackages() {
    final Iterator<Map.Entry<RInstalledPackage, String>> iterator = myWaitingToUpgrade.entrySet().iterator();
    final Map.Entry<RInstalledPackage, String> toUpgrade = iterator.next();
    iterator.remove();
    upgradePackage(toUpgrade.getKey(), toUpgrade.getValue());
  }

  protected Set<String> getPackagesToPostpone() {
    return Collections.emptySet();
  }

  private void upgradePackage(@NotNull final RInstalledPackage pkg, @Nullable final String toVersion) {
    final RPackageManagementService selectedPackageManagementService = myPackageManagementService;
    myPackageManagementService.fetchPackageVersions(pkg.getName(), new CatchingConsumer<>() {
      @Override
      public void consume(java.util.List<String> releases) {
        if (!releases.isEmpty() && !isUpdateAvailable(pkg.getVersion(), releases.get(0))) {
          return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {
          ModalityState modalityState = ModalityState.current();
          final PackageManagementService.Listener listener = new PackageManagementService.Listener() {
            @Override
            public void operationStarted(final String packageName) {
              ApplicationManager.getApplication().invokeLater(() -> myCurrentlyInstalling.add(packageName), modalityState);
            }

            @Override
            public void operationFinished(final String packageName,
                                          @Nullable final PackageManagementService.ErrorDescription errorDescription) {
              ApplicationManager.getApplication().invokeLater(() -> {
                myPackagesTable.clearSelection();
                updatePackages(selectedPackageManagementService);
                myCurrentlyInstalling.remove(packageName);
                if (errorDescription == null) {
                  myNotificationArea.showSuccess("Package " + packageName + " successfully upgraded");
                }
                else {
                  myNotificationArea.showError("Upgrade packages failed. <a href=\"xxx\">Details...</a>",
                                               RBundle.INSTANCE.message("install.package.dialog.title.upgrade.packages.failed"),
                                               errorDescription);
                }

                if (myCurrentlyInstalling.isEmpty() && !myWaitingToUpgrade.isEmpty()) {
                  upgradePostponedPackages();
                }
              }, modalityState);
            }
          };
          PackageManagementServiceEx serviceEx = getServiceEx();
          if (serviceEx != null) {
            serviceEx.updatePackage(pkg, toVersion, listener);
          }
          else {
            myPackageManagementService.installPackage(new RepoPackage(pkg.getName(), null /* TODO? */), null, true, null, listener, false);
          }
          myUpgradeButton.setEnabled(false);
        }, ModalityState.any());
      }

      @Override
      public void consume(Exception e) {
        ApplicationManager.getApplication().invokeLater(() -> Messages
          .showErrorDialog(RBundle.INSTANCE.message("install.package.dialog.message.error.occurred.please.check.your.internet.connection"),
                           RBundle.INSTANCE.message("install.package.dialog.title.upgrade.package.failed")), ModalityState.any());
      }
    });
  }

  @Nullable
  private PackageManagementServiceEx getServiceEx() {
    return ObjectUtils.tryCast(myPackageManagementService, PackageManagementServiceEx.class);
  }

  protected void updateUninstallUpgrade() {
    final int[] selected = myPackagesTable.getSelectedRows();
    boolean upgradeAvailable = false;
    boolean canUninstall = selected.length != 0;
    boolean canInstall = installEnabled();
    boolean canUpgrade = true;
    if (myPackageManagementService != null && selected.length != 0) {
      for (int i = 0; i != selected.length; ++i) {
        final int index = selected[i];
        if (index >= myPackagesTable.getRowCount()) continue;
        final Object value = myPackagesTable.getValueAt(index, 0);
        if (value instanceof RInstalledPackage pkg) {
          if (!canUninstallPackage(pkg)) {
            canUninstall = false;
          }
          canInstall = canInstallPackage(pkg);
          if (!canUpgradePackage(pkg)) {
            canUpgrade = false;
          }
          final String pyPackageName = pkg.getName();
          final String availableVersion = (String)myPackagesTable.getValueAt(index, 2);
          if (!upgradeAvailable) {
            upgradeAvailable = isUpdateAvailable(pkg.getVersion(), availableVersion) &&
                               !myCurrentlyInstalling.contains(pyPackageName);
          }
          if (!canUninstall && !canUpgrade) break;
        }
      }
    }
    myInstallButton.setEnabled(canInstall);
    myUpgradeButton.setEnabled(upgradeAvailable && canUpgrade);
  }

  protected boolean canUninstallPackage(RInstalledPackage pyPackage) {
    return true;
  }

  protected boolean canInstallPackage(@NotNull final RInstalledPackage pyPackage) {
    return true;
  }

  protected boolean installEnabled() {
    return true;
  }

  protected boolean canUpgradePackage(RInstalledPackage pyPackage) {
    return true;
  }

  private void uninstallAction(@NotNull List<RInstalledPackage> packages) {
    final RPackageManagementService selectedPackageManagementService = myPackageManagementService;
    if (selectedPackageManagementService != null) {
      ModalityState modalityState = ModalityState.current();
      RPackageManagementService.Listener listener = new PackageManagementService.Listener() {
        @Override
        public void operationStarted(String packageName) {
        }

        @Override
        public void operationFinished(final String packageName,
                                      @Nullable final PackageManagementService.ErrorDescription errorDescription) {
          ApplicationManager.getApplication().invokeLater(() -> {
            myPackagesTable.clearSelection();
            updatePackages(selectedPackageManagementService);
            if (errorDescription == null) {
              if (packageName != null) {
                myNotificationArea.showSuccess("Package '" + packageName + "' successfully uninstalled");
              }
              else {
                myNotificationArea.showSuccess("Packages successfully uninstalled");
              }
            }
            else {
              myNotificationArea.showError("Uninstall packages failed. <a href=\"xxx\">Details...</a>",
                                           RBundle.INSTANCE.message("install.package.dialog.title.uninstall.packages.failed"),
                                           errorDescription);
            }
          }, modalityState);
        }
      };
      myPackageManagementService.uninstallPackages(packages, listener);
    }
  }

  public void updatePackages(@Nullable RPackageManagementService packageManagementService) {
    myPackageManagementService = packageManagementService;
    myPackagesTable.clearSelection();
    myPackagesTableModel.getDataVector().clear();
    myPackagesTableModel.fireTableDataChanged();
    if (packageManagementService != null) {
      doUpdatePackages(packageManagementService);
    }
  }

  private void onUpdateStarted() {
    myPackagesTable.getEmptyText().setText(RBundle.INSTANCE.message("install.package.status.text.loading"));
  }

  private void onUpdateFinished() {
    myPackagesTable.getEmptyText().setText(StatusText.getDefaultEmptyText());
    updateUninstallUpgrade();
    // Action button presentations won't be updated if no events occur (e.g. mouse isn't moving, keys aren't being pressed).
    // In that case emulating activity will help:
    ActivityTracker.getInstance().inc();
  }

  public void doUpdatePackages(@NotNull final RPackageManagementService packageManagementService) {
    onUpdateStarted();
    ProgressManager.getInstance().run(new Task.Backgroundable(myProject,
                                                              LOADING_PACKAGES_LIST_TITLE,
                                                              true,
                                                              PerformInBackgroundOption.ALWAYS_BACKGROUND) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        List<RInstalledPackage> packages = List.of();
        try {
          packages = packageManagementService.getInstalledPackagesList();
        }
        finally {
          List<Object[]> rows = ContainerUtil.map(packages,
                                                  pkg -> new Object[]{
                                                    "",
                                                    pkg,
                                                    pkg.getDescription().get(TITLE),
                                                    pkg.getVersion(),
                                                    "",
                                                    ""
                                                  });

          final boolean shouldFetchLatestVersionsForOnlyInstalledPackages = shouldFetchLatestVersionsForOnlyInstalledPackages();
          UIUtil.invokeLaterIfNeeded(() -> {
            if (packageManagementService == myPackageManagementService) {
              myPackagesTableModel.getDataVector().clear();
              for (Object[] row : rows) {
                myPackagesTableModel.addRow(row);
              }
              onUpdateFinished();
              if (shouldFetchLatestVersionsForOnlyInstalledPackages) {
                setLatestVersionsForInstalledPackages();
              }
            }
          });
        }
      }
    });
  }

  @Nullable
  private RInstalledPackage getInstalledPackageAt(int index) {
    return (RInstalledPackage) myPackageFilteringModel.getValueAt(index, PACKAGE_NAME_COLUMN);
  }

  private void setLatestVersionsForInstalledPackages() {
    final PackageManagementServiceEx serviceEx = getServiceEx();
    if (serviceEx == null) {
      return;
    }
    int packageCount = myPackagesTableModel.getRowCount();
    if (packageCount == 0) {
      onUpdateFinished();
    }
    final AtomicInteger inProgressPackageCount = new AtomicInteger(packageCount);
    for (int i = 0; i < packageCount; ++i) {
      final int finalIndex = i;
      final RInstalledPackage pkg = getInstalledPackageAt(finalIndex);
      if (pkg != null) {
        serviceEx.fetchLatestVersion(pkg, new CatchingConsumer<>() {

          private void decrement() {
            if (inProgressPackageCount.decrementAndGet() == 0) {
              onUpdateFinished();
            }
          }

          @Override
          public void consume(Exception e) {
            UIUtil.invokeLaterIfNeeded(this::decrement);
            LOG.warn("Cannot fetch the latest version of the installed package " + pkg, e);
          }

          @Override
          public void consume(@Nullable final String latestVersion) {
            UIUtil.invokeLaterIfNeeded(() -> {
              if (finalIndex < myPackagesTableModel.getRowCount()) {
                RInstalledPackage p = getInstalledPackageAt(finalIndex);
                if (pkg == p) {
                  myPackagesTableModel.setValueAt(latestVersion, finalIndex, 2);
                }
              }
              decrement();
            });
          }
        });
      }
    }
  }

  private boolean shouldFetchLatestVersionsForOnlyInstalledPackages() {
    PackageManagementServiceEx serviceEx = getServiceEx();
    if (serviceEx != null) {
      return serviceEx.shouldFetchLatestVersionsForOnlyInstalledPackages();
    }
    return false;
  }

  private boolean isUpdateAvailable(@Nullable String currentVersion, @Nullable String availableVersion) {
    if (availableVersion == null) {
      return false;
    }
    if (currentVersion == null) {
      return true;
    }
    PackageManagementService service = myPackageManagementService;
    if (service != null) {
      return service.compareVersions(currentVersion, availableVersion) < 0;
    }
    return PackageVersionComparator.VERSION_COMPARATOR.compare(currentVersion, availableVersion) < 0;
  }

  private static void doRecorded(@NotNull String actionId, @NotNull MouseEvent mouseEvent, @NotNull Runnable runnable) {
    AnAction action = ActionManager.getInstance().getAction(actionId);
    if (action == null) throw new AssertionError("'" + actionId + "' action not found");
    DataContext dataContext = DataManager.getInstance().getDataContext(mouseEvent.getComponent());
    AnActionEvent event = AnActionEvent.createFromAnAction(action, mouseEvent, "RInstalledPackagesPanel", dataContext);
    ActionUtil.performDumbAwareWithCallbacks(action, event, runnable);
  }

  private final class MyTableCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                   final boolean hasFocus, final int row, final int column) {
      RInstalledPackage aPackage = getInstalledPackageAt(table, row);
      if (aPackage == null) return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if (column == PACKAGE_NAME_COLUMN) {
        PointerInfo info = MouseInfo.getPointerInfo();
        Point columnRow = (info != null) ? getMouseColumnRow(info.getLocation(), table) : new Point(-1, -1);
        myPackageNameLinkLabel.setText(aPackage.getName());
        myPackageNameLinkLabel.setUnderline(columnRow.x == column && columnRow.y == row);
        return myPackageNameLinkLabel;
      }
      if (column == BROWSE_COLUMN) {
        myLabel.setText("");
        myLabel.setIcon(AllIcons.General.Web);
        return myLabel;
      }
      if (column == UNINSTALL_COLUMN) {
        myLabel.setText("");
        var canUninstall = myPackageManagementService.canUninstallPackage(aPackage);
        var icon = canUninstall ? AllIcons.Diff.Remove : null;
        myLabel.setIcon(icon);
        return myLabel;
      }
      if (column == IS_LOADED_COLUMN) {
        myIsLoadedCheckBox.setText("");
        myIsLoadedCheckBox.setBackground(myPackagesTable.getBackground());
        myIsLoadedCheckBox.setSelected(myPackageManagementService.isPackageLoaded(aPackage.getName()));
        return myIsLoadedCheckBox;
      }
      return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

    @Nullable
    private RInstalledPackage getInstalledPackageAt(final JTable table, final int row) {
      final Object o = table.getValueAt(row, PACKAGE_NAME_COLUMN);
      if (o instanceof RInstalledPackage) {
        return (RInstalledPackage) o;
      } else {
        return null;
      }
    }
  }

  private static final class SettableLinkLabel<T> extends LinkLabel<T> {
    SettableLinkLabel() {
      super("", null);
    }

    @Override
    protected @NotNull Rectangle getTextBounds() {
      int width = GraphicsUtil.stringWidth(getText(), getFont());
      return new Rectangle(0, 0, width, 0);
    }

    public void setUnderline(boolean value) {
      myUnderline = value;
    }
  }

  private static final class MyTextSearchField extends AnActionButton implements CustomComponentAction, DumbAware {
    private final SearchTextField myField;

    @Override
    public boolean isDumbAware() {
      return true;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
    }

    private MyTextSearchField() {
      super("", "", null);
      myField = new SearchTextField();
    }

    @NotNull
    @Override
    public JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
      return myField;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
      return ActionUpdateThread.BGT;
    }
  }

  private static class MyFilteringTableModel extends AbstractTableModel {
    private final TableModel myOriginalModel;
    private final List<List<?>> myData = new ArrayList<>();
    private final ArrayList<Integer> myIndex = new ArrayList<>();
    private String myText = null;

    private final TableModelListener myListDataListener = e -> refilter();

    private MyFilteringTableModel(TableModel originalModel) {
      myOriginalModel = originalModel;
      myOriginalModel.addTableModelListener(myListDataListener);
    }

    public void dispose() {
      myOriginalModel.removeTableModelListener(myListDataListener);
    }

    public void setFilter(@Nullable String text) {
      myText = text;
      refilter();
    }

    private void removeAllElements() {
      int index1 = myData.size() - 1;
      if (index1 >= 0) {
        myData.clear();
        fireTableRowsDeleted(0, index1);
      }
      myIndex.clear();
    }

    public void refilter() {
      removeAllElements();
      int count = 0;
      for (int i = 0; i < myOriginalModel.getRowCount(); i++) {
        RInstalledPackage aPackage = (RInstalledPackage)myOriginalModel.getValueAt(i, PACKAGE_NAME_COLUMN);
        String title = aPackage.getDescription().get(TITLE);
        if (myText == null ||
            StringUtil.containsIgnoreCase(aPackage.getName(), myText) ||
            StringUtil.containsIgnoreCase(aPackage.getVersion(), myText) ||
            (title != null && StringUtil.containsIgnoreCase(title, myText))) {
          List<Object> elements = Lists.newArrayListWithCapacity(myOriginalModel.getColumnCount());
          for (int col = 0; col < myOriginalModel.getColumnCount(); col++) {
            elements.add(myOriginalModel.getValueAt(i, col));
          }
          addToFiltered(elements);
          myIndex.add(i);
          count++;
        }
      }

      if (count > 0) {
        fireTableRowsInserted(0, count - 1);
      }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return myOriginalModel.isCellEditable(myIndex.get(rowIndex), columnIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      myOriginalModel.setValueAt(aValue, myIndex.get(rowIndex), columnIndex);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return myOriginalModel.getColumnClass(columnIndex);
    }

    @Override
    public String getColumnName(int column) {
      return myOriginalModel.getColumnName(column);
    }

    protected void addToFiltered(List<?> elt) {
      myData.add(elt);
    }

    public int getSize() {
      return myData.size();
    }

    @Override
    public int getRowCount() {
      return myData.size();
    }

    @Override
    public int getColumnCount() {
      return myOriginalModel.getColumnCount();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      if (rowIndex >= myData.size() || rowIndex < 0 || columnIndex < 0 || columnIndex >= getColumnCount()) {
        return null;
      }
      return myData.get(rowIndex).get(columnIndex);
    }
  }
}