package com.github.mess9.testitaffectedidplugin.window;

import com.github.mess9.testitaffectedidplugin.service.AffectedTestsFinderService;
import com.github.mess9.testitaffectedidplugin.testit.TestItClient;
import com.github.mess9.testitaffectedidplugin.testit.TestItSettings;
import com.github.mess9.testitaffectedidplugin.testit.dto.AutoTest;
import com.github.mess9.testitaffectedidplugin.testit.dto.AutotestSearchDto;
import com.github.mess9.testitaffectedidplugin.testit.dto.AutotestSearchFilterDto;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.intellij.openapi.ui.Messages.showWarningDialog;

public class AffectedTestsWindow extends SimpleToolWindowPanel {

	private static final Logger LOG = Logger.getInstance(AffectedTestsWindow.class);
	private static final int PACKAGE_COLUMN_INDEX = 0;
	private static final int CLASS_NAME_COLUMN_INDEX = 1;
	private static final int TEST_NAME_COLUMN_INDEX = 2;
	private static final int METHOD_NAME_COLUMN_INDEX = 3;
	private static final int TEST_ID_COLUMN_INDEX = 4;
	private static final int TEST_IT_URL_COLUMN_INDEX = 5;
	private static final String PACKAGE_COLUMN = "package";
	private static final String CLASS_NAME_COLUMN = "class";
	private static final String TEST_NAME_COLUMN = "test name";
	private static final String METHOD_NAME_COLUMN = "method";
	private static final String TEST_ID_COLUMN = "test ids";
	private static final String TEST_IT_URL = "testIT link";
	private final DefaultTableModel tableModel;
	private final Project project;
	private List<AffectedTestsFinderService.TestMethodInfo> currentTests = new ArrayList<>();
	private Map<String, Set<Long>> testIds = new HashMap<>();


	public AffectedTestsWindow(Project project) {
		super(true, true);
		this.project = project;

		// Модель таблицы
		tableModel = new DefaultTableModel(
				new Object[]{PACKAGE_COLUMN, CLASS_NAME_COLUMN, TEST_NAME_COLUMN, METHOD_NAME_COLUMN, TEST_ID_COLUMN, TEST_IT_URL}, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		// Настраиваем таблицу
		JTable resultsTable = new JBTable(tableModel);

		resultsTable.getColumnModel().getColumn(PACKAGE_COLUMN_INDEX).setPreferredWidth(50);
		resultsTable.getColumnModel().getColumn(CLASS_NAME_COLUMN_INDEX).setPreferredWidth(30);
		resultsTable.getColumnModel().getColumn(TEST_NAME_COLUMN_INDEX).setPreferredWidth(110);
		resultsTable.getColumnModel().getColumn(METHOD_NAME_COLUMN_INDEX).setPreferredWidth(150);
		resultsTable.getColumnModel().getColumn(TEST_ID_COLUMN_INDEX).setPreferredWidth(50);
		resultsTable.getColumnModel().getColumn(TEST_ID_COLUMN_INDEX).setPreferredWidth(10);
		resultsTable.getColumnModel().getColumn(TEST_IT_URL_COLUMN_INDEX).setPreferredWidth(100);

		addMouseListenerToTable(resultsTable);

		// Добавляем кнопки управления
		DefaultActionGroup actionGroup = new DefaultActionGroup();
		actionGroup.add(ActionManager.getInstance().getAction("AffectedTestsFinder.FindTests"));
		actionGroup.add(new FetchTestIdsAction(this));
		actionGroup.add(new CopyIdsAction(this));

		ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(
				"AffectedTestsToolbar", actionGroup, true);
		toolbar.setTargetComponent(resultsTable);

		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(toolbar.getComponent(), BorderLayout.NORTH);
		mainPanel.add(new JBScrollPane(resultsTable), BorderLayout.CENTER);

		setContent(mainPanel);
	}

	private void addMouseListenerToTable(JTable table) {
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {

					int row = table.rowAtPoint(e.getPoint());
					int col = table.columnAtPoint(e.getPoint());

					if (row == -1 || col == -1) return;

					String packageName = table.getValueAt(row, PACKAGE_COLUMN_INDEX).toString();
					String className = table.getValueAt(row, CLASS_NAME_COLUMN_INDEX).toString();
					String fqn = packageName + "." + className;

					if (col == CLASS_NAME_COLUMN_INDEX || col == PACKAGE_COLUMN_INDEX) {
						// Клик по классу — открыть класс
						openElementInEditor(fqn, null, project);
					} else if ((col == METHOD_NAME_COLUMN_INDEX || col == TEST_NAME_COLUMN_INDEX || col == TEST_ID_COLUMN_INDEX) && table.getValueAt(row, METHOD_NAME_COLUMN_INDEX) != null) {
						// Клик по методу — открыть метод
						String methodName = table.getValueAt(row, METHOD_NAME_COLUMN_INDEX).toString();
						openElementInEditor(fqn, methodName, project);
					} else if (col == TEST_IT_URL_COLUMN_INDEX && table.getValueAt(row, TEST_IT_URL_COLUMN_INDEX) != null && table.getValueAt(row, TEST_ID_COLUMN_INDEX) != null) {
						String url = table.getValueAt(row, TEST_IT_URL_COLUMN_INDEX).toString();
						if (!url.isBlank()) {
							BrowserUtil.browse(url);
						}
					}
				}
			}
		});
	}

	private void openElementInEditor(String fqn, String methodName, Project project) {
		ApplicationManager.getApplication().invokeLater(() -> {
			GlobalSearchScope scope = GlobalSearchScope.allScope(project);
			PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(fqn, scope);

			if (psiClass == null) {
				showWarningDialog(project,
						"Не удалось найти класс: " + fqn,
						"Ошибка");
				return;
			}

			if (methodName == null || methodName.isEmpty()) {
				psiClass.navigate(true);
			} else {
				// Ищем метод по имени
				for (PsiMethod method : psiClass.getMethods()) {
					if (method.getName().equals(methodName)) {
						method.navigate(true);
						return;
					}
				}
				// Если не нашли метод — просто открываем класс
				psiClass.navigate(true);
			}
		});
	}

	public static AffectedTestsWindow getInstance(Project project) {
		// Сначала пробуем получить экземпляр из фабрики
		AffectedTestsWindow window = AffectedTestsWindowFactory.getInstance(project);
		if (window != null) {
			return window;
		}

		// Если не удалось, пробуем получить из ToolWindow
		ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Affected Tests");
		if (toolWindow != null) {
			// Попытка получить контент, если он является AffectedTestsWindow
			if (toolWindow.getContentManager().getContent(0) != null &&
					Objects.requireNonNull(toolWindow.getContentManager().getContent(0)).getComponent() instanceof AffectedTestsWindow) {
				return (AffectedTestsWindow) Objects.requireNonNull(toolWindow.getContentManager().getContent(0)).getComponent();
			}
		}
		LOG.warn("Could not get instance of AffectedTestsWindow for project: " + project.getName());
		return null;
	}

	public void setTestResults(List<AffectedTestsFinderService.TestMethodInfo> tests) {
		this.currentTests = new ArrayList<>(tests);
		updateTable(false);
	}

	private void updateTable(boolean withIds) {
		TestItSettings settings = project.getService(TestItSettings.class);
		ApplicationManager.getApplication().invokeLater(() -> {
			tableModel.setRowCount(0);

			List<AffectedTestsFinderService.TestMethodInfo> sortedTests = new ArrayList<>(currentTests);
			sortedTests.sort(Comparator.comparing(AffectedTestsFinderService.TestMethodInfo::className)
					.thenComparing(AffectedTestsFinderService.TestMethodInfo::displayName));

			// Iterate through the sorted list
			for (AffectedTestsFinderService.TestMethodInfo test : sortedTests) {
				Object[] row;
				if (withIds && testIds.containsKey(test.displayName())) {
					String ids = testIds.get(test.displayName()).stream()
							.map(Object::toString)
							.collect(Collectors.joining(", "));
					String link = "";
					if (!ids.isEmpty()) {
						link = settings.getUrl() + "/projects/" + settings.getProjectUrlId() + "/autotests/" + ids;
					}
					row = new Object[]{test.packageName(), test.className(), test.displayName(), test.methodName(), ids, link};
				} else {
					row = new Object[]{test.packageName(), test.className(), test.displayName(), test.methodName(), "", ""};
				}
				tableModel.addRow(row);
			}
		});
	}

	public void fetchTestIds() {
		TestItSettings settings = project.getService(TestItSettings.class);
		if (settings.getUrl().isEmpty() || settings.getPrivateToken().isEmpty() || settings.getProjectId().isEmpty()) {
			Path propertiesPath = Paths.get(Objects.requireNonNull(project.getBasePath()), "src", "test", "resources", "testit.properties");
			boolean propertiesFileExists = Files.exists(propertiesPath);

			Messages.showErrorDialog(project, errorMessage(propertiesFileExists), "TestIT Settings Error");
			return;
		}

		try {
			// Инициализируем контекст проекта в настройках, если он не был установлен
			settings.initProjectContext(project);

			// Пробуем загрузить настройки из файла свойств
			settings.loadFromPropertiesFile();

			LOG.info("Fetching test IDs with URL: " + settings.getUrl() + ", ProjectID: " + settings.getProjectId());

			TestItClient client = new TestItClient(settings);
			testIds = new HashMap<>();
			List<CompletableFuture<Void>> futures = new ArrayList<>();

			for (AffectedTestsFinderService.TestMethodInfo test : currentTests) {
				String testName = test.displayName();
				testIds.put(testName, new HashSet<>());

				CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
					try {
						AutotestSearchFilterDto filter = AutotestSearchFilterDto.builder()
								.projectIds(List.of(settings.getProjectId()))
								.isDeleted(false)
								.name(testName)
								.build();

						AutotestSearchDto searchDto = AutotestSearchDto.builder()
								.filter(filter)
								.build();

						List<AutoTest> foundTests = client.searchTestCases(searchDto);
						LOG.debug("Found " + foundTests.size() + " tests for name: " + testName);

						for (AutoTest foundTest : foundTests) {
							if (foundTest.getName().equals(testName)) {
								LOG.debug("Adding test ID: " + foundTest.getGlobalId() + " for test: " + testName);
								testIds.get(testName).add(foundTest.getGlobalId());
							}
						}
					} catch (Exception e) {
						LOG.error("Error searching for test: " + testName, e);
					}
				});

				futures.add(future);
			}

			CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
			updateTable(true);

		} catch (Exception e) {
			LOG.error("Error fetching test IDs from TestIT", e);

			// Более детальное сообщение об ошибке
			String errorDetails = e.getMessage();
			if (e.getCause() != null) {
				errorDetails += "\nПричина: " + e.getCause().getMessage();
			}

			Messages.showErrorDialog(project,
					"Ошибка при получении ID тестов из TestIT:\n" + errorDetails,
					"TestIT API Error");
		}
	}

	private static @NotNull String errorMessage(boolean propertiesFileExists) {
		String message;
		if (propertiesFileExists) {
			message = "Необходимые настройки TestIT (URL, Project ID, Private Token) не заданы или неполны.\n" +
					"Пожалуйста, проверьте файл testit.properties в директории src/test/resources " +
					"или настройте плагин через Settings > Tools > TestIT Settings.";
		} else {
			message = "Файл testit.properties не найден в директории src/test/resources, и настройки плагина неполны.\n" +
					"Пожалуйста, создайте и заполните testit.properties " +
					"или настройте плагин через Settings > Tools > TestIT Settings.";
		}
		return message;
	}

	public void copyIds() {
		if (testIds.isEmpty()) {
			Messages.showInfoMessage(project, "No test IDs to copy. Please fetch test IDs first.", "No Data");
			return;
		}

		StringBuilder sb = new StringBuilder();
		testIds.forEach((name, ids) -> {
			Set<Long> idSet = ids.isEmpty() ? Set.of(0L) : ids;
			String idsStr = idSet.stream()
					.map(Object::toString)
					.collect(Collectors.joining(","));
			sb.append(idsStr).append(",");
		});

		if (!sb.isEmpty()) {
			sb.deleteCharAt(sb.length() - 1);
		}

		StringSelection selection = new StringSelection(sb.toString());
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(selection, null);

		Messages.showInfoMessage(project, "Test IDs have been copied to clipboard", "Success");
	}

	private static class FetchTestIdsAction extends AnAction {

		private final AffectedTestsWindow window;

		public FetchTestIdsAction(AffectedTestsWindow window) {
			super("Fetch TestIT IDs", "Fetch test IDs from TestIT", AllIcons.Vcs.Fetch);
			this.window = window;
		}

		@Override
		public void actionPerformed(@NotNull AnActionEvent e) {
			if (window != null) {
				window.fetchTestIds();
			} else {
				LOG.warn("FetchTestIdsAction: AffectedTestsWindow instance is null.");
				if (e.getProject() != null) {
					Messages.showErrorDialog(e.getProject(), "Cannot perform action: Plugin window not available.", "Error");
				}
			}
		}
	}

	private static class CopyIdsAction extends AnAction {

		private final AffectedTestsWindow window;

		public CopyIdsAction(AffectedTestsWindow window) {
			super("Copy IDs", "Copy test IDs to clipboard", AllIcons.Actions.Copy);
			this.window = window;
		}

		@Override
		public void actionPerformed(@NotNull AnActionEvent e) {
			if (window != null) {
				window.copyIds();
			} else {
				LOG.warn("CopyIdsAction: AffectedTestsWindow instance is null.");
				if (e.getProject() != null) {
					Messages.showErrorDialog(e.getProject(), "Cannot perform action: Plugin window not available.", "Error");
				}
			}
		}
	}
}