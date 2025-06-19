package com.github.mess9.testitaffectedidplugin.action;

import com.github.mess9.testitaffectedidplugin.service.AffectedTestsFinderService;
import com.github.mess9.testitaffectedidplugin.service.TestItService;
import com.github.mess9.testitaffectedidplugin.testit.TestItClient;
import com.github.mess9.testitaffectedidplugin.testit.TestItSettings;
import com.github.mess9.testitaffectedidplugin.testit.dto.AutoTest;
import com.github.mess9.testitaffectedidplugin.window.AffectedTestsWindow;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class FetchTestIdsAction extends AnAction {

	private static final Logger LOG = Logger.getInstance(FetchTestIdsAction.class);


	private final AffectedTestsWindow window;

	public FetchTestIdsAction(AffectedTestsWindow window) {
		super("Fetch TestIT IDs",
				"Fetch test IDs from TestIT",
				AllIcons.Vcs.Fetch);
		this.window = window;
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		if (window != null) {
			fetchTestIds(window);
		} else {
			LOG.warn("FetchTestIdsAction: AffectedTestsWindow instance is null.");
			if (e.getProject() != null) {
				Messages.showErrorDialog(e.getProject(), "Cannot perform action: Plugin window not available.", "Error");
			}
		}
	}

	public void fetchTestIds(AffectedTestsWindow window) {
		Project project = window.project;
		Map<String, Set<Long>> testIds;
		TestItClient client = new TestItClient(project);
		TestItSettings settings = client.getSettings();
		TestItService testItService = new TestItService(client);

		LOG.info("Fetching test IDs with URL: " + settings.getUrl() + ", ProjectID: " + settings.getProjectId());

		testIds = window.testIds;
		List<CompletableFuture<Void>> futures = new ArrayList<>();

		for (AffectedTestsFinderService.TestMethodInfo test : window.currentTests) {
			String testName = test.displayName();
			String testitName = testName.replace("\\", "");
			testIds.put(testName, new HashSet<>());

			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
				List<AutoTest> foundTests = testItService.getTestsFromTestItByName(testitName);
				LOG.info("Found " + foundTests.size() + " tests for name: " + testName);

				for (AutoTest foundTest : foundTests) {
					if (foundTest.getName().equals(testitName)) {
						LOG.info("Adding test ID: " + foundTest.getGlobalId() + " for test: " + testName);
						testIds.get(testName).add(foundTest.getGlobalId());
					}
				}
			});
			futures.add(future);
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		window.updateTable(true);
	}
}
