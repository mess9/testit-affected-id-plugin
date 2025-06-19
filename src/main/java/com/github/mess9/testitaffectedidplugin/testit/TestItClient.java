package com.github.mess9.testitaffectedidplugin.testit;


import com.github.mess9.testitaffectedidplugin.testit.dto.AutoTest;
import com.github.mess9.testitaffectedidplugin.testit.dto.AutotestSearchDto;
import com.github.mess9.testitaffectedidplugin.testit.dto.AutotestSelectDto;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TestItClient {

	private static final Logger log = Logger.getInstance(TestItClient.class);
	private static final String PROJECT_URL_ID_TEMPLATE = "/api/v2/projects/%s";
	private static final String AUTOTEST_SEARCH_URL = "/api/v2/autoTests/search";
	private static final String AUTOTEST_URL = "/api/v2/autoTests";
	private final Project project;
	private HttpClient httpClient;
	private Gson gson;
	private TestItSettings settings;

	public TestItClient(Project project) {
		this.project = project;
		TestItSettings settings = project.getService(TestItSettings.class);
		if (settings.getUrl().isEmpty() || settings.getPrivateToken().isEmpty() || settings.getProjectId().isEmpty()) {
			Path propertiesPath = Paths.get(Objects.requireNonNull(project.getBasePath()), "src", "test", "resources", "testit.properties");
			boolean propertiesFileExists = Files.exists(propertiesPath);
			Messages.showErrorDialog(project, errorMessage(propertiesFileExists), "TestIT Settings Error");
			return;
		}
		settings.initProjectContext(project);
		settings.loadFromPropertiesFile();
		this.settings = settings;
		this.httpClient = HttpClient.newBuilder().build();
		this.gson = new GsonBuilder()
				.registerTypeAdapter(
						OffsetDateTime.class,
						(JsonDeserializer<OffsetDateTime>) (json, typeOfT, context) ->
								OffsetDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME))
				.create();
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

	public void deleteTestById(AutotestSelectDto request) {
		try {

			String url = settings.getUrl() + AUTOTEST_URL;
			String requestBody = gson.toJson(request);

			log.info("request - " + requestBody);

			HttpRequest httpRequest = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.header("Content-Type", "application/json")
					.header("Authorization", "PrivateToken " + settings.getPrivateToken())
					.method("DELETE", HttpRequest.BodyPublishers.ofString(requestBody))
					.build();

			HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

			log.info("delete response:\n" + response.body());

			if (response.statusCode() != 200) {
				log.error("Error searching test cases: " + response.statusCode() + " " + response.body());
			}
		} catch (IOException | InterruptedException e) {
			log.error("Error calling TestIT API", e);
		}
	}

	public List<AutoTest> searchTestCases(AutotestSearchDto request) {
		try {
			String url = settings.getUrl() + AUTOTEST_SEARCH_URL;
			String requestBody = gson.toJson(request);

			HttpRequest httpRequest = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.header("Content-Type", "application/json")
					.header("Authorization", "PrivateToken " + settings.getPrivateToken())
					.POST(HttpRequest.BodyPublishers.ofString(requestBody))
					.build();

			HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() == 200) {
				Type listType = new TypeToken<List<AutoTest>>() {
				}.getType();
				List<AutoTest> autoTests = gson.fromJson(response.body(), listType);
				autoTests.stream().map(AutoTest::getProjectId).findAny().ifPresent(p -> extractProjectUrlId(p.toString()));
				return autoTests;
			} else {
				log.error("Error searching test cases: " + response.statusCode() + " " + response.body());
			}
		} catch (IOException | InterruptedException e) {
			log.error("Error calling TestIT API", e);
		}
		return Collections.emptyList();
	}

	private void extractProjectUrlId(String projectId) {
		String url = settings.getUrl() + PROJECT_URL_ID_TEMPLATE.formatted(projectId);
		try {
			HttpRequest httpRequest = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.header("Content-Type", "application/json")
					.header("Authorization", "PrivateToken " + settings.getPrivateToken())
					.GET()
					.build();

			HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() == 200) {
				com.github.mess9.testitaffectedidplugin.testit.dto.Project project =
						gson.fromJson(response.body(), com.github.mess9.testitaffectedidplugin.testit.dto.Project.class);
				settings.setProjectUrlId(String.valueOf(project.getGlobalId()));
				log.info("Project URL ID extracted: " + settings.getProjectUrlId());
			} else {
				log.error("Error searching test cases: " + response.statusCode() + " " + response.body());
			}
		} catch (IOException | InterruptedException e) {
			log.error("Error calling TestIT API", e);
		}
	}

	public TestItSettings getSettings() {
		return settings;
	}

	public Project getProject() {
		return project;
	}
}

