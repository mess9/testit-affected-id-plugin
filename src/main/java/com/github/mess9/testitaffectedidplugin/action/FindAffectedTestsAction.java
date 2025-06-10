package com.github.mess9.testitaffectedidplugin.action;

import com.github.mess9.testitaffectedidplugin.service.AffectedTestsFinderService;
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
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

public class FindAffectedTestsAction extends AnAction {

	private static final Logger log = Logger.getInstance(FindAffectedTestsAction.class);

	private final AffectedTestsWindow window;

	public FindAffectedTestsAction(AffectedTestsWindow window) {
		super("Find Affected Tests",
				"Find tests affected by changes between current branch and master",
				AllIcons.Actions.Find);
		this.window = window;
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		if (window != null) {
			findAffectedTests(window);
		} else {
			log.warn("FetchTestIdsAction: AffectedTestsWindow instance is null.");
			if (e.getProject() != null) {
				Messages.showErrorDialog(e.getProject(), "Cannot perform action: Plugin window not available.", "Error");
			}
		}
	}

	private void findAffectedTests(AffectedTestsWindow window) {
		Project project = window.project;
		VirtualFile baseDir = ProjectUtil.guessProjectDir(project);

		if (baseDir == null) {
			return;
		}

		File projectDir = new File(baseDir.getPath());
		AffectedTestsFinderService service = new AffectedTestsFinderService();

		ProgressManager.getInstance().run(new Task.Backgroundable(project, "Finding affected tests") {
			@Override
			public void run(@NotNull ProgressIndicator indicator) {
				try {
					List<AffectedTestsFinderService.TestMethodInfo> affectedTests = service.findAffectedTests(projectDir);
					log.info("found affected tests - " + affectedTests.size());

					ApplicationManager.getApplication().invokeLater(() -> window.setTestResults(affectedTests));

				} catch (Exception ex) {
					log.error("Error finding affected tests", ex);
					ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(
							project,
							"Error finding affected tests: " + ex.getMessage(),
							"Affected Tests Finder Error"
					));
				}
			}
		});
	}
}