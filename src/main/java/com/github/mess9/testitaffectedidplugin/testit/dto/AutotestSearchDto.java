package com.github.mess9.testitaffectedidplugin.testit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AutotestSearchDto {

	private AutotestSearchFilterDto filter;
	private Object includes;
}
