package com.github.mess9.testitaffectedidplugin.service;

enum TestPath {

	JAVA_TEST_PATH("src/test/java");

	private final String testPath;

	TestPath(String testPath) {
		this.testPath = testPath;
	}

	public String getTestPath() {
		return testPath;
	}
}
