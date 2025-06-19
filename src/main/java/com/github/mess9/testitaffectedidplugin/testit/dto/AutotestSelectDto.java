package com.github.mess9.testitaffectedidplugin.testit.dto;

import java.util.List;

public class AutotestSelectDto {

	private AutoTestSelect autoTestSelect;

	public void setAutoTestSelect(AutoTestSelect autoTestSelect) {
		this.autoTestSelect = autoTestSelect;
	}

	public static class AutoTestSelect {

		private Filter filter;

		public void setFilter(Filter filter) {
			this.filter = filter;
		}

		public static class Filter {

			private List<String> projectIds;
			private List<Long> globalIds;

			public void setProjectIds(List<String> projectIds) {
				this.projectIds = projectIds;
			}

			public void setGlobalIds(List<Long> globalIds) {
				this.globalIds = globalIds;
			}
		}
	}
}
