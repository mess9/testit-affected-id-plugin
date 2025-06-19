package com.github.mess9.testitaffectedidplugin.action;

import com.github.mess9.testitaffectedidplugin.service.AffectedTestsFinderService;
import com.github.mess9.testitaffectedidplugin.service.ProjectTestService;
import com.github.mess9.testitaffectedidplugin.service.TestItService;
import com.github.mess9.testitaffectedidplugin.testit.TestItClient;
import com.github.mess9.testitaffectedidplugin.testit.dto.AutoTest;
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
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FindGhostTestItAction extends AnAction {

	private static final Logger log = Logger.getInstance(FindGhostTestItAction.class);
	private final AffectedTestsWindow window;

	public FindGhostTestItAction(AffectedTestsWindow window) {
		super("Find Ghost Tests",
				"Find ghost tests in testIt",
				AllIcons.Actions.Preview);
		this.window = window;
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		if (window != null) {
			foundGhostTestItTests(window);
		} else {
			log.warn("FetchTestIdsAction: AffectedTestsWindow instance is null.");
			if (e.getProject() != null) {
				Messages.showErrorDialog(e.getProject(), "Cannot perform action: Plugin window not available.", "Error");
			}
		}
	}

	public void foundGhostTestItTests(AffectedTestsWindow window) {
		Project project = window.project;
		TestItClient client = new TestItClient(project);
		ProjectTestService projectService = new ProjectTestService(project);
		TestItService testitService = new TestItService(client); // todo меня эта портянка из конструкторов тоже не радует

		ProgressManager.getInstance().run(new Task.Backgroundable(project, "Finding ghost tests") {
			@Override
			public void run(@NotNull ProgressIndicator indicator) {
				try {
					// find tests
					Map<String, PsiMethod> testsInCode = ApplicationManager.getApplication()
							.runReadAction((Computable<Map<String, PsiMethod>>) projectService::collectProjectTests);
					List<AutoTest> testInTestIt = testitService.getAllTestsFromTestIt();

					Set<String> codeIds = testsInCode.keySet().stream()
							.map(e -> e.startsWith("betcity.") ? e.substring("betcity.".length()) : e)
							.collect(Collectors.toSet());
					Set<String> tmsIds = testInTestIt.stream()
							.map(AutoTest::getExternalId)
							.map(e -> e.startsWith("betcity.") ? e.substring("betcity.".length()) : e)
							.collect(Collectors.toSet());
					Set<String> onlyInCode = codeIds.stream()
							.filter(e -> !tmsIds.contains(e))
							.collect(Collectors.toSet());
					Set<String> onlyInTms = tmsIds.stream()
							.filter(e -> !codeIds.contains(e))
							.collect(Collectors.toSet());
					Set<String> both = codeIds.stream()
							.filter(tmsIds::contains)
							.collect(Collectors.toSet());

					log.info("▶️ Тесты только в коде: " + onlyInCode.size());
					onlyInCode.stream().limit(10).forEach(id -> log.info(" - " + id));
					log.info("📦 Тесты только в TMS: " + onlyInTms.size());
					onlyInTms.stream().limit(10).forEach(id -> log.info(" - " + id));
					log.info("✅ Тесты и в коде, и в TMS: " + both.size());

					List<AutoTest> ghostTests = testInTestIt.stream()
							.filter(e -> onlyInTms.contains(e.getExternalId())).toList();


					// fill table
					List<AffectedTestsFinderService.TestMethodInfo> missingTests = ghostTests.stream()
							.map(e -> new AffectedTestsFinderService.TestMethodInfo(
									"betcity." + e.getNamespace(),
									e.getClassname(),
									e.getName(),
									e.getExternalId().substring(e.getExternalId().lastIndexOf('.') + 1),
									0, 0, List.of() // todo переделать на нормальную дто для таблицы
							))
							.toList();
					window.testIds = ghostTests.stream()
							.collect(Collectors.groupingBy(
									AutoTest::getName,
									Collectors.mapping(AutoTest::getGlobalId, Collectors.toSet())
							));
					ApplicationManager.getApplication().invokeLater(() -> window.setTestResultsWithIds(missingTests));

				} catch (Exception ex) {
					log.error("Error finding ghost tests", ex);
					ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(
							project,
							"Error finding ghost tests: " + ex.getMessage(),
							"Ghost Tests Finder Error"
					));
				}
			}
		});
	}
}
