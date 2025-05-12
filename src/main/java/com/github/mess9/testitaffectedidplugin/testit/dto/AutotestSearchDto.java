package com.github.mess9.testitaffectedidplugin.testit.dto;

@SuppressWarnings("unused")
public class AutotestSearchDto {

	private AutotestSearchFilterDto filter;
	private Object includes;

	public AutotestSearchDto(AutotestSearchFilterDto filter, Object includes) {
		this.filter = filter;
		this.includes = includes;
	}

	public AutotestSearchDto() {
	}

	public AutotestSearchFilterDto getFilter() {
		return filter;
	}

	public void setFilter(AutotestSearchFilterDto filter) {
		this.filter = filter;
	}

	public Object getIncludes() {
		return includes;
	}

	public void setIncludes(Object includes) {
		this.includes = includes;
	}

	@Override
	public String toString() {
		return "AutotestSearchDto{" +
				"filter=" + filter +
				", includes=" + includes +
				'}';
	}
}
