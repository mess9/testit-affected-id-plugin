package com.github.mess9.testitaffectedidplugin.action;

import com.github.mess9.testitaffectedidplugin.service.AffectedTestsFinderService;
import com.github.mess9.testitaffectedidplugin.window.AffectedTestsWindow;
import com.github.mess9.testitaffectedidplugin.window.AffectedTestsWindowFactory;
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
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

public class FindAffectedTestsAction extends AnAction {

	private static final Logger LOG = Logger.getInstance(FindAffectedTestsAction.class);

	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		Project project = e.getProject();
		if (project == null) return;

		VirtualFile baseDir = ProjectUtil.guessProjectDir(project);

		if (baseDir == null) return;

		File projectDir = new File(baseDir.getPath());
		AffectedTestsFinderService service = new AffectedTestsFinderService();

		LOG.info("Начинаем поиск затронутых тестов");
		LOG.debug("Анализируем директорию: " + projectDir.getAbsolutePath());

		ProgressManager.getInstance().run(new Task.Backgroundable(project, "Finding affected tests") {
			@Override
			public void run(@NotNull ProgressIndicator indicator) {
				try {
					List<AffectedTestsFinderService.TestMethodInfo> affectedTests = service.findAffectedTests(projectDir);

					// Обновить UI в EDT потоке
					ApplicationManager.getApplication().invokeLater(() -> {
						ToolWindow toolWindow = ToolWindowManager.getInstance(project)
								.getToolWindow("Affected Tests");
						if (toolWindow != null) {
							// Получаем экземпляр окна из фабрики напрямую
							AffectedTestsWindow testsWindow = AffectedTestsWindowFactory.getInstance(project);
							if (testsWindow == null) {
								LOG.warn("Could not get instance of AffectedTestsWindow from factory");
								testsWindow = AffectedTestsWindow.getInstance(project);
							}

							if (testsWindow != null) {
								testsWindow.setTestResults(affectedTests);
								toolWindow.show();
							} else {
								LOG.error("AffectedTestsWindow instance is null");
								Messages.showErrorDialog(
										project,
										"Не удалось найти окно для отображения результатов",
										"Affected Tests Finder Error"
								);
							}
						}
					});
				} catch (Exception ex) {
					LOG.error("Error finding affected tests", ex);
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