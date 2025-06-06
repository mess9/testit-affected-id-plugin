package com.github.mess9.testitaffectedidplugin.action;

import com.github.mess9.testitaffectedidplugin.window.AffectedTestsWindow;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CopyIdsAction extends AnAction {

	private static final Logger LOG = Logger.getInstance(CopyIdsAction.class);

	private final AffectedTestsWindow window;

	public CopyIdsAction(AffectedTestsWindow window) {
		super("Copy IDs", "Copy test IDs to clipboard", AllIcons.Actions.Copy);
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

}
