package com.github.mess9.testitaffectedidplugin.service;

import com.github.mess9.testitaffectedidplugin.testit.TestItClient;
import com.github.mess9.testitaffectedidplugin.testit.dto.AutoTest;
import com.github.mess9.testitaffectedidplugin.testit.dto.AutotestSearchDto;
import com.github.mess9.testitaffectedidplugin.testit.dto.AutotestSearchFilterDto;
import com.github.mess9.testitaffectedidplugin.testit.dto.AutotestSelectDto;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;

import java.util.ArrayList;
import java.util.List;

public class TestItService {

	private static final Logger log = Logger.getInstance(TestItService.class);
	private final TestItClient client;

	public TestItService(TestItClient client) {
		this.client = client;
	}

	public List<AutoTest> getTestsFromTestItByName(String name) {
		List<AutoTest> autoTests = new ArrayList<>();
		var filter = new AutotestSearchFilterDto();
		filter.setProjectIds(List.of(client.getSettings().getProjectId()));
		filter.setName(name);
		filter.setDeleted(false);
		var searchDto = new AutotestSearchDto();
		searchDto.setFilter(filter);

		try {
			autoTests = client.searchTestCases(searchDto);

		} catch (Exception e) {
			log.error("Error fetching test IDs from TestIT", e);
			String errorDetails = e.getMessage();
			if (e.getCause() != null) {
				errorDetails += "\nПричина: " + e.getCause().getMessage();
			}

			Messages.showErrorDialog(client.getProject(),
					"Ошибка при получении ID тестов из TestIT:\n" + errorDetails,
					"TestIT API Error");
		}
		return autoTests;
	}

	public List<AutoTest> getAllTestsFromTestIt() {
		List<AutoTest> autoTests = new ArrayList<>();
		var filter = new AutotestSearchFilterDto();
		filter.setProjectIds(List.of(client.getSettings().getProjectId()));
		filter.setDeleted(false);
		var searchDto = new AutotestSearchDto();
		searchDto.setFilter(filter);

		try {
			autoTests = client.searchTestCases(searchDto);

		} catch (Exception e) {
			log.error("Error fetching data from TestIT", e);
			String errorDetails = e.getMessage();
			if (e.getCause() != null) {
				errorDetails += "\nПричина: " + e.getCause().getMessage();
			}
			Messages.showErrorDialog(client.getProject(),
					"Ошибка при получении тестов из TestIT:\n" + errorDetails,
					"TestIT API Error");
		}
		return autoTests;
	}

	public void deleteTestById(List<Long> ids) {
		var request = new AutotestSelectDto();
		var select = new AutotestSelectDto.AutoTestSelect();
		var filter = new AutotestSelectDto.AutoTestSelect.Filter();
		filter.setProjectIds(List.of(client.getSettings().getProjectId()));
		filter.setGlobalIds(ids);
		select.setFilter(filter);
		request.setAutoTestSelect(select);

		log.info("ids for delete: " + ids.size());
		ids.forEach(e -> log.info(e.toString()));

		try {
			client.deleteTestById(request);
		} catch (Exception e) {
			log.error("Error deleting tests in TestIT", e);
			String errorDetails = e.getMessage();
			if (e.getCause() != null) {
				errorDetails += "\nПричина: " + e.getCause().getMessage();
			}
			Messages.showErrorDialog(client.getProject(),
					"Ошибка при удалении тестов из TestIT:\n" + errorDetails,
					"TestIT API Error");
		}
	}

}
