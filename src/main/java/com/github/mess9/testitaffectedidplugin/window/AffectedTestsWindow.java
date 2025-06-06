package com.github.mess9.testitaffectedidplugin.window;

import com.github.mess9.testitaffectedidplugin.action.CopyIdsAction;
import com.github.mess9.testitaffectedidplugin.action.FetchTestIdsAction;
import com.github.mess9.testitaffectedidplugin.action.FindAffectedTestsAction;
import com.github.mess9.testitaffectedidplugin.action.MouseActions;
import com.github.mess9.testitaffectedidplugin.service.AffectedTestsFinderService;
import com.github.mess9.testitaffectedidplugin.testit.TestItSettings;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AffectedTestsWindow extends SimpleToolWindowPanel {

	private static final Logger LOG = Logger.getInstance(AffectedTestsWindow.class);
	public static final int PACKAGE_COLUMN_INDEX = 0;
	public static final int CLASS_NAME_COLUMN_INDEX = 1;
	public static final int TEST_NAME_COLUMN_INDEX = 2;
	public static final int METHOD_NAME_COLUMN_INDEX = 3;
	public static final int TEST_ID_COLUMN_INDEX = 4;
	public static final int TEST_IT_URL_COLUMN_INDEX = 5;
	private static final String PACKAGE_COLUMN = "package";
	private static final String CLASS_NAME_COLUMN = "class";
	private static final String TEST_NAME_COLUMN = "test name";
	private static final String METHOD_NAME_COLUMN = "method";
	private static final String TEST_ID_COLUMN = "test ids";
	private static final String TEST_IT_URL = "testIT link";
	private final DefaultTableModel tableModel;
	public final Project project;
	public List<AffectedTestsFinderService.TestMethodInfo> currentTests = new ArrayList<>();
	public Map<String, Set<Long>> testIds = new HashMap<>();

	// region инициализация
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

		MouseActions mouseActions = new MouseActions();
		mouseActions.addMouseListenerToTable(resultsTable, project);

		// Добавляем кнопки управления
		DefaultActionGroup actionGroup = new DefaultActionGroup();
		actionGroup.add(new FindAffectedTestsAction(this));
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

	// endregion инициализация


	// region таблица

	public void setTestResults(List<AffectedTestsFinderService.TestMethodInfo> tests) {
		this.currentTests = new ArrayList<>(tests);
		updateTable(false);
	}

	public void updateTable(boolean withIds) {
		TestItSettings settings = project.getService(TestItSettings.class);
		ApplicationManager.getApplication().invokeLater(() -> {
			tableModel.setRowCount(0);

			List<AffectedTestsFinderService.TestMethodInfo> sortedTests = new ArrayList<>(currentTests);
			sortedTests.sort(Comparator.comparing(AffectedTestsFinderService.TestMethodInfo::className)
					.thenComparing(AffectedTestsFinderService.TestMethodInfo::displayName));

			for (AffectedTestsFinderService.TestMethodInfo test : sortedTests) {
				Object[] row;
				if (withIds && testIds.containsKey(test.displayName())) {
					String ids = testIds.get(test.displayName()).stream()
							.map(Object::toString)
							.collect(Collectors.joining(", "));
					row = new Object[]{test.packageName(), test.className(), test.displayName(), test.methodName(), ids, getLink(ids, settings)};
				} else {
					row = new Object[]{test.packageName(), test.className(), test.displayName(), test.methodName(), "", ""};
				}
				tableModel.addRow(row);
			}
		});
	}

	// endregion таблица

	private static @NotNull String getLink(String ids, TestItSettings settings) {
		return !ids.isEmpty() ? settings.getUrl() + "/projects/" + settings.getProjectUrlId() + "/autotests/" + ids : "";
	}
}