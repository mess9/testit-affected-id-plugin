package com.github.mess9.testitaffectedidplugin.action;

import com.github.mess9.testitaffectedidplugin.window.AffectedTestsWindow;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.bouncycastle.util.Arrays;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.mess9.testitaffectedidplugin.window.AffectedTestsWindow.TEST_NAME_COLUMN_INDEX;

public class CopyIdsAction extends AnAction {

	private static final Logger LOG = Logger.getInstance(CopyIdsAction.class);

	private final AffectedTestsWindow window;

	public CopyIdsAction(AffectedTestsWindow window) {
		super("Copy IDs",
				"Copy test IDs to clipboard",
				AllIcons.Actions.Copy);
		this.window = window;
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		if (window != null) {
			copyIds(window);
		} else {
			LOG.warn("CopyIdsAction: AffectedTestsWindow instance is null.");
			if (e.getProject() != null) {
				Messages.showErrorDialog(e.getProject(), "Cannot perform action: Plugin window not available.", "Error");
			}
		}
	}

	public void copyIds(AffectedTestsWindow window) {
		Project project = window.project;
		Map<String, Set<Long>> testIds = window.testIds;
		JTable table = window.resultsTable;
		int[] selectedRows = table.getSelectedRows();

		if (testIds.isEmpty()) {
			Messages.showInfoMessage(project, "No test IDs to copy. Please fetch test IDs first.", "No Data");
			return;
		}

		StringSelection selection = new StringSelection("");
		if (Arrays.isNullOrEmpty(selectedRows)) {
			selection = new StringSelection(getResultIdsStringFromMap(testIds));
		} else {
			ArrayList<Set<Long>> sets = new ArrayList<>();
			for (int row : selectedRows) {
				String testName = (String) table.getValueAt(row, TEST_NAME_COLUMN_INDEX);
				Set<Long> longs = testIds.get(testName);
				sets.add(longs);
				selection = new StringSelection(getResultsIdsFromCollection(sets));
			}
		}

		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(selection, null);
		Messages.showInfoMessage(project, "Test IDs have been copied to clipboard", "Success");
	}

	private String getResultIdsStringFromMap(Map<String, Set<Long>> testIds) {
		Collection<Set<Long>> values = testIds.values();
		return getResultsIdsFromCollection(values);
	}

	private String getResultsIdsFromCollection(Collection<Set<Long>> ids) {
		StringBuilder sb = new StringBuilder();
		ids.forEach(e -> {
			Set<Long> idSet = e.isEmpty() ? Set.of(0L) : e;
			String idsStr = idSet.stream()
					.map(Object::toString)
					.collect(Collectors.joining(","));
			sb.append(idsStr).append(",");
		});
		if (!sb.isEmpty()) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

}
