package com.github.mess9.testitaffectedidplugin.action;

import com.github.mess9.testitaffectedidplugin.service.AffectedTestsFinderService;
import com.github.mess9.testitaffectedidplugin.testit.TestItClient;
import com.github.mess9.testitaffectedidplugin.testit.TestItSettings;
import com.github.mess9.testitaffectedidplugin.testit.dto.AutoTest;
import com.github.mess9.testitaffectedidplugin.testit.dto.AutotestSearchDto;
import com.github.mess9.testitaffectedidplugin.testit.dto.AutotestSearchFilterDto;
import com.github.mess9.testitaffectedidplugin.window.AffectedTestsWindow;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class FetchTestIdsAction extends AnAction {

	private static final Logger LOG = Logger.getInstance(FetchTestIdsAction.class);


	private final AffectedTestsWindow window;

	public FetchTestIdsAction(AffectedTestsWindow window) {
		super("Fetch TestIT IDs", "Fetch test IDs from TestIT", AllIcons.Vcs.Fetch);
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

			for (AffectedTestsFinderService.TestMethodInfo test : window.currentTests) {
				String testName = test.displayName();
				testIds.put(testName, new HashSet<>());

				CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
					try {
						var filter = new AutotestSearchFilterDto();
						filter.setProjectIds(List.of(settings.getProjectId()));
						filter.setName(testName);
						filter.setDeleted(false);

						var searchDto = new AutotestSearchDto();
						searchDto.setFilter(filter);

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
			window.updateTable(true);

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


}
