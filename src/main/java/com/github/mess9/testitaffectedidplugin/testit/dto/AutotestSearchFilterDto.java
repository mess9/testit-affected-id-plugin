package com.github.mess9.testitaffectedidplugin.testit.dto;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
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
}
