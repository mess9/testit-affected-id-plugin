package com.github.mess9.testitaffectedidplugin.testit.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@SuppressWarnings("unused")
public class Project {

	public static final String SERIALIZED_NAME_ID = "id";
	public static final String SERIALIZED_NAME_DESCRIPTION = "description";
	public static final String SERIALIZED_NAME_NAME = "name";
	public static final String SERIALIZED_NAME_IS_FAVORITE = "isFavorite";
	public static final String SERIALIZED_NAME_TEST_CASES_COUNT = "testCasesCount";
	public static final String SERIALIZED_NAME_SHARED_STEPS_COUNT = "sharedStepsCount";
	public static final String SERIALIZED_NAME_CHECK_LISTS_COUNT = "checkListsCount";
	public static final String SERIALIZED_NAME_AUTO_TESTS_COUNT = "autoTestsCount";
	public static final String SERIALIZED_NAME_IS_DELETED = "isDeleted";
	public static final String SERIALIZED_NAME_CREATED_DATE = "createdDate";
	public static final String SERIALIZED_NAME_MODIFIED_DATE = "modifiedDate";
	public static final String SERIALIZED_NAME_CREATED_BY_ID = "createdById";
	public static final String SERIALIZED_NAME_MODIFIED_BY_ID = "modifiedById";
	public static final String SERIALIZED_NAME_GLOBAL_ID = "globalId";
	/**
	 * -- GETTER --
	 * Unique ID of the project
	 */
	@SerializedName(SERIALIZED_NAME_ID)
	private UUID id;
	/**
	 * -- GETTER --
	 * Description of the project
	 */
	@SerializedName(SERIALIZED_NAME_DESCRIPTION)
	private String description;
	/**
	 * -- GETTER --
	 * Name of the project
	 */
	@SerializedName(SERIALIZED_NAME_NAME)
	private String name;
	/**
	 * -- GETTER --
	 * Indicates if the project is marked as favorite
	 */
	@SerializedName(SERIALIZED_NAME_IS_FAVORITE)
	private Boolean isFavorite;
	/**
	 * -- GETTER --
	 * Number of test cases in the project
	 */
	@SerializedName(SERIALIZED_NAME_TEST_CASES_COUNT)
	private Integer testCasesCount;
	/**
	 * -- GETTER --
	 * Number of shared steps in the project
	 */
	@SerializedName(SERIALIZED_NAME_SHARED_STEPS_COUNT)
	private Integer sharedStepsCount;
	/**
	 * -- GETTER --
	 * Number of checklists in the project
	 */
	@SerializedName(SERIALIZED_NAME_CHECK_LISTS_COUNT)
	private Integer checkListsCount;
	/**
	 * -- GETTER --
	 * Number of autotests in the project
	 */
	@SerializedName(SERIALIZED_NAME_AUTO_TESTS_COUNT)
	private Integer autoTestsCount;
	/**
	 * -- GETTER --
	 * Indicates if the project is deleted
	 */
	@SerializedName(SERIALIZED_NAME_IS_DELETED)
	private Boolean isDeleted;
	/**
	 * -- GETTER --
	 * Creation date of the project
	 */
	@SerializedName(SERIALIZED_NAME_CREATED_DATE)
	private OffsetDateTime createdDate;
	/**
	 * -- GETTER --
	 * Last modification date of the project
	 */
	@SerializedName(SERIALIZED_NAME_MODIFIED_DATE)
	private OffsetDateTime modifiedDate;
	/**
	 * -- GETTER --
	 * Unique ID of the project creator
	 */
	@SerializedName(SERIALIZED_NAME_CREATED_BY_ID)
	private UUID createdById;
	/**
	 * -- GETTER --
	 * Unique ID of the project last editor
	 */
	@SerializedName(SERIALIZED_NAME_MODIFIED_BY_ID)
	private UUID modifiedById;
	/**
	 * -- GETTER --
	 * Global ID of the project
	 */
	@SerializedName(SERIALIZED_NAME_GLOBAL_ID)
	private Long globalId;


	public Project() {
	}

	public Project id(UUID id) {

		this.id = id;
		return this;
	}


	public void setId(UUID id) {
		this.id = id;
	}


	public Project description(String description) {

		this.description = description;
		return this;
	}


	public void setDescription(String description) {
		this.description = description;
	}


	public Project name(String name) {

		this.name = name;
		return this;
	}


	public void setName(String name) {
		this.name = name;
	}


	public Project isFavorite(Boolean isFavorite) {

		this.isFavorite = isFavorite;
		return this;
	}


	public void setIsFavorite(Boolean isFavorite) {
		this.isFavorite = isFavorite;
	}


	public Project testCasesCount(Integer testCasesCount) {

		this.testCasesCount = testCasesCount;
		return this;
	}


	public void setTestCasesCount(Integer testCasesCount) {
		this.testCasesCount = testCasesCount;
	}


	public Project sharedStepsCount(Integer sharedStepsCount) {

		this.sharedStepsCount = sharedStepsCount;
		return this;
	}


	public void setSharedStepsCount(Integer sharedStepsCount) {
		this.sharedStepsCount = sharedStepsCount;
	}


	public Project checkListsCount(Integer checkListsCount) {

		this.checkListsCount = checkListsCount;
		return this;
	}


	public void setCheckListsCount(Integer checkListsCount) {
		this.checkListsCount = checkListsCount;
	}


	public Project autoTestsCount(Integer autoTestsCount) {

		this.autoTestsCount = autoTestsCount;
		return this;
	}


	public void setAutoTestsCount(Integer autoTestsCount) {
		this.autoTestsCount = autoTestsCount;
	}


	public Project isDeleted(Boolean isDeleted) {

		this.isDeleted = isDeleted;
		return this;
	}


	public void setIsDeleted(Boolean isDeleted) {
		this.isDeleted = isDeleted;
	}


	public Project createdDate(OffsetDateTime createdDate) {

		this.createdDate = createdDate;
		return this;
	}


	public void setCreatedDate(OffsetDateTime createdDate) {
		this.createdDate = createdDate;
	}


	public Project modifiedDate(OffsetDateTime modifiedDate) {

		this.modifiedDate = modifiedDate;
		return this;
	}


	public void setModifiedDate(OffsetDateTime modifiedDate) {
		this.modifiedDate = modifiedDate;
	}


	public Project createdById(UUID createdById) {

		this.createdById = createdById;
		return this;
	}


	public void setCreatedById(UUID createdById) {
		this.createdById = createdById;
	}


	public Project modifiedById(UUID modifiedById) {

		this.modifiedById = modifiedById;
		return this;
	}


	public void setModifiedById(UUID modifiedById) {
		this.modifiedById = modifiedById;
	}


	public Project globalId(Long globalId) {

		this.globalId = globalId;
		return this;
	}


	public void setGlobalId(Long globalId) {
		this.globalId = globalId;
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Project projectModel = (Project) o;
		return Objects.equals(this.id, projectModel.id) &&
				Objects.equals(this.description, projectModel.description) &&
				Objects.equals(this.name, projectModel.name) &&
				Objects.equals(this.isFavorite, projectModel.isFavorite) &&
				Objects.equals(this.testCasesCount, projectModel.testCasesCount) &&
				Objects.equals(this.sharedStepsCount, projectModel.sharedStepsCount) &&
				Objects.equals(this.checkListsCount, projectModel.checkListsCount) &&
				Objects.equals(this.autoTestsCount, projectModel.autoTestsCount) &&
				Objects.equals(this.isDeleted, projectModel.isDeleted) &&
				Objects.equals(this.createdDate, projectModel.createdDate) &&
				Objects.equals(this.modifiedDate, projectModel.modifiedDate) &&
				Objects.equals(this.createdById, projectModel.createdById) &&
				Objects.equals(this.modifiedById, projectModel.modifiedById) &&
				Objects.equals(this.globalId, projectModel.globalId);
	}


	@Override
	public int hashCode() {
		return Objects.hash(id, description, name, isFavorite, testCasesCount, sharedStepsCount, checkListsCount, autoTestsCount, isDeleted, createdDate, modifiedDate, createdById, modifiedById, globalId);
	}

	@Override
	public String toString() {
		return "class ProjectModel {\n" +
				"    id: " + toIndentedString(id) + "\n" +
				"    description: " + toIndentedString(description) + "\n" +
				"    name: " + toIndentedString(name) + "\n" +
				"    isFavorite: " + toIndentedString(isFavorite) + "\n" +
				"    testCasesCount: " + toIndentedString(testCasesCount) + "\n" +
				"    sharedStepsCount: " + toIndentedString(sharedStepsCount) + "\n" +
				"    checkListsCount: " + toIndentedString(checkListsCount) + "\n" +
				"    autoTestsCount: " + toIndentedString(autoTestsCount) + "\n" +
				"    isDeleted: " + toIndentedString(isDeleted) + "\n" +
				"    createdDate: " + toIndentedString(createdDate) + "\n" +
				"    modifiedDate: " + toIndentedString(modifiedDate) + "\n" +
				"    createdById: " + toIndentedString(createdById) + "\n" +
				"    modifiedById: " + toIndentedString(modifiedById) + "\n" +
				"    globalId: " + toIndentedString(globalId) + "\n" +
				"}";
	}

	/**
	 * Convert the given object to string with each line indented by 4 spaces
	 * (except the first line).
	 */
	private String toIndentedString(Object o) {
		if (o == null) {
			return "null";
		}
		return o.toString().replace("\n", "\n    ");
	}
}