package com.github.mess9.testitaffectedidplugin.testit;


import com.github.mess9.testitaffectedidplugin.testit.dto.AutoTest;
import com.github.mess9.testitaffectedidplugin.testit.dto.AutotestSearchDto;
import com.github.mess9.testitaffectedidplugin.testit.dto.Project;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public class TestItClient {

	private static final Logger LOG = Logger.getInstance(TestItClient.class);
	private static final String PROJECT_URL_ID_TEMPLATE = "/api/v2/projects/%s";
	private static final String AUTOTEST_SEARCH_URL = "/api/v2/autoTests/search";
	private final HttpClient httpClient;
	private final Gson gson;
	private final TestItSettings settings;

	public TestItClient(TestItSettings settings) {
		this.settings = settings;
		this.httpClient = HttpClient.newBuilder().build();
		this.gson = new GsonBuilder()
				.registerTypeAdapter(
						OffsetDateTime.class,
						(JsonDeserializer<OffsetDateTime>) (json, typeOfT, context) ->
								OffsetDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME))
				.create();

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
				LOG.error("Error searching test cases: " + response.statusCode() + " " + response.body());
			}
		} catch (IOException | InterruptedException e) {
			LOG.error("Error calling TestIT API", e);
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
				Project project = gson.fromJson(response.body(), Project.class);
				settings.setProjectUrlId(String.valueOf(project.getGlobalId()));
				LOG.info("Project URL ID extracted: " + settings.getProjectUrlId());
			} else {
				LOG.error("Error searching test cases: " + response.statusCode() + " " + response.body());
			}
		} catch (IOException | InterruptedException e) {
			LOG.error("Error calling TestIT API", e);
		}
	}
}

