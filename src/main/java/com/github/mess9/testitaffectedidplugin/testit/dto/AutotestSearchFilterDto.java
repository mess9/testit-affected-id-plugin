package com.github.mess9.testitaffectedidplugin.testit.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

@SuppressWarnings("unused")
public class AutotestSearchFilterDto {

	private List<String> projectIds;
	private List<String> externalIds;
	private List<String> globalIds;
	private String name;
	private String className;
	private Boolean isFlaky;
	private Boolean isDeleted;

	// Используем строку вместо объекта для дат
	@SerializedName("createdDate")
	private String createdDateString;

	private List<String> createdByIds;

	@SerializedName("modifiedDate")
	private String modifiedDateString;

	private List<String> modifiedByIds;

	public AutotestSearchFilterDto(List<String> projectIds, List<String> externalIds, List<String> globalIds, String name, String className, Boolean isFlaky, Boolean isDeleted, String createdDateString, List<String> createdByIds, String modifiedDateString, List<String> modifiedByIds) {
		this.projectIds = projectIds;
		this.externalIds = externalIds;
		this.globalIds = globalIds;
		this.name = name;
		this.className = className;
		this.isFlaky = isFlaky;
		this.isDeleted = isDeleted;
		this.createdDateString = createdDateString;
		this.createdByIds = createdByIds;
		this.modifiedDateString = modifiedDateString;
		this.modifiedByIds = modifiedByIds;
	}

	public AutotestSearchFilterDto() {
	}

	public List<String> getProjectIds() {
		return projectIds;
	}

	public void setProjectIds(List<String> projectIds) {
		this.projectIds = projectIds;
	}

	public List<String> getExternalIds() {
		return externalIds;
	}

	public void setExternalIds(List<String> externalIds) {
		this.externalIds = externalIds;
	}

	public List<String> getGlobalIds() {
		return globalIds;
	}

	public void setGlobalIds(List<String> globalIds) {
		this.globalIds = globalIds;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public Boolean getFlaky() {
		return isFlaky;
	}

	public void setFlaky(Boolean flaky) {
		isFlaky = flaky;
	}

	public Boolean getDeleted() {
		return isDeleted;
	}

	public void setDeleted(Boolean deleted) {
		isDeleted = deleted;
	}

	public String getCreatedDateString() {
		return createdDateString;
	}

	public void setCreatedDateString(String createdDateString) {
		this.createdDateString = createdDateString;
	}

	public List<String> getCreatedByIds() {
		return createdByIds;
	}

	public void setCreatedByIds(List<String> createdByIds) {
		this.createdByIds = createdByIds;
	}

	public String getModifiedDateString() {
		return modifiedDateString;
	}

	public void setModifiedDateString(String modifiedDateString) {
		this.modifiedDateString = modifiedDateString;
	}

	public List<String> getModifiedByIds() {
		return modifiedByIds;
	}

	public void setModifiedByIds(List<String> modifiedByIds) {
		this.modifiedByIds = modifiedByIds;
	}

	@Override
	public String toString() {
		return "AutotestSearchFilterDto{" +
				"projectIds=" + projectIds +
				", externalIds=" + externalIds +
				", globalIds=" + globalIds +
				", name='" + name + '\'' +
				", className='" + className + '\'' +
				", isFlaky=" + isFlaky +
				", isDeleted=" + isDeleted +
				", createdDateString='" + createdDateString + '\'' +
				", createdByIds=" + createdByIds +
				", modifiedDateString='" + modifiedDateString + '\'' +
				", modifiedByIds=" + modifiedByIds +
				'}';
	}
}
