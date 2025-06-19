package com.github.mess9.testitaffectedidplugin.service;

public enum TestAnnotation {

	JUNIT_TEST("org.junit.jupiter.api.Test"),
	JUNIT_PARAMETRIZED_TEST("org.junit.jupiter.params.ParameterizedTest"),
	TESTNG_TEST("org.testng.annotations.Test");

	private final String annotation;

	TestAnnotation(String annotation) {
		this.annotation = annotation;
	}

	public String getAnnotation() {
		return annotation;
	}

	public String getAnnotationName() {
		String[] split = annotation.split("\\.");
		return split[split.length - 1];
	}
}
