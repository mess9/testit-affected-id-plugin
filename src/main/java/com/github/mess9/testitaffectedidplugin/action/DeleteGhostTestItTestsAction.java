package com.github.mess9.testitaffectedidplugin.action;

import com.github.mess9.testitaffectedidplugin.service.TestItService;
import com.github.mess9.testitaffectedidplugin.testit.TestItClient;
import com.github.mess9.testitaffectedidplugin.testit.TestItSettings;
import com.github.mess9.testitaffectedidplugin.window.AffectedTestsWindow;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class DeleteGhostTestItTestsAction extends AnAction {

	private static final Logger log = Logger.getInstance(DeleteGhostTestItTestsAction.class);


	private final AffectedTestsWindow window;

	public DeleteGhostTestItTestsAction(AffectedTestsWindow window) {
		super("Delete in TestIt",
				"Delete selected or all tests in TestIt",
				AllIcons.General.Delete);
		this.window = window;
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		if (window != null) {
			deleteGhostTestsFromTestTi(window);
		} else {
			log.warn("DeleteGhostTestItTestsAction: AffectedTestsWindow instance is null.");
			if (e.getProject() != null) {
				Messages.showErrorDialog(e.getProject(), "Cannot perform action: Plugin window not available.", "Error");
			}
		}
	}

	public void deleteGhostTestsFromTestTi(AffectedTestsWindow window) {
		Project project = window.project;
		List<Long> testIds = window.testIds.values().stream().flatMap(Collection::stream).toList();
		TestItClient client = new TestItClient(project);
		TestItSettings settings = client.getSettings();
		TestItService testItService = new TestItService(client);

		log.info("Deleting test IDs with URL: " + settings.getUrl() + ", ProjectID: " + settings.getProjectId());

		ProgressManager.getInstance().run(new Task.Backgroundable(project, "Finding ghost tests") {
			@Override
			public void run(@NotNull ProgressIndicator indicator) {
				try {
					testItService.deleteTestById(testIds);

					window.testIds.clear();
					window.currentTests.clear();

					window.updateTable(false);

				} catch (Exception ex) {
					log.error("Error delete ghost tests", ex);
					ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(
							project,
							"Error delete ghost tests: " + ex.getMessage(),
							"Ghost Tests Deleter Error"
					));
				}
			}
		});
	}
}
