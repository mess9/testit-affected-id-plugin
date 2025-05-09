package com.github.mess9.testitaffectedidplugin;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.mess9.testitaffectedidplugin.service.AffectedTestsFinderService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeepSeekTests {

	private AffectedTestsFinderService service;

	@Mock
	private Repository mockRepository;
	@Mock
	private Git mockGit;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		service = new AffectedTestsFinderService();
	}

	// Тест 1: Добавление новой аннотации к существующему тесту
	@Test
	void shouldDetectAddedAnnotation() {
		String oldCode = """
				@Test
				@DisplayName("Old test")
				void testMethod() {}
				""";

		String newCode = """
				@Test
				@DisplayName("Old test")
				@Tag("new")
				void testMethod() {}
				""";

		List<AffectedTestsFinderService.TestMethodInfo> result = service.checkDeletedTests(oldCode, newCode, "Test.java");
		assertEquals(1, result.size());
		assertTrue(result.get(0).annotations().contains("Tag"));
	}

	// Тест 2: Удаление аннотации
	@Test
	void shouldDetectRemovedAnnotation() {
		String oldCode = """
				@Test
				@DisplayName("Test")
				@Tag("old")
				void testMethod() {}
				""";

		String newCode = """
				@Test
				@DisplayName("Test")
				void testMethod() {}
				""";

		List<AffectedTestsFinderService.TestMethodInfo> affected = service.checkDeletedTests(oldCode, newCode, "Test.java");
		assertTrue(affected.stream().anyMatch(t -> t.methodName().equals("testMethod")));
	}

	// Тест 3: Изменение параметров аннотации
	@Test
	void shouldDetectChangedAnnotationParams() {
		String oldCode = """
				@Test
				@DisplayName("Old name")
				void testMethod() {}
				""";

		String newCode = """
				@Test
				@DisplayName("New name")
				void testMethod() {}
				""";

		List<AffectedTestsFinderService.TestMethodInfo> affected = service.checkDeletedTests(oldCode, newCode, "Test.java");
		assertEquals(1, affected.size());
	}

	// Тест 4: Переименование метода
	@Test
	void shouldDetectRenamedMethod() {
		String oldCode = """
				@Test
				void oldMethodName() {}
				""";

		String newCode = """
				@Test
				void newMethodName() {}
				""";

		List<AffectedTestsFinderService.TestMethodInfo> affected = service.checkDeletedTests(oldCode, newCode, "Test.java");
		assertEquals(2, affected.size()); // 1 deleted, 1 new
	}

	// Тест 5: Изменение тела метода без изменения аннотаций
	@Test
	void shouldNotDetectBodyChanges() {
		String oldCode = """
				@Test
				void testMethod() { int a = 1; }
				""";

		String newCode = """
				@Test
				void testMethod() { int b = 2; }
				""";

		List<AffectedTestsFinderService.TestMethodInfo> affected = service.checkDeletedTests(oldCode, newCode, "Test.java");
		assertTrue(affected.isEmpty());
	}

	// Тест 6: Новый тестовый метод
	@Test
	void shouldDetectNewTestMethod() throws IOException, GitAPIException {
		DiffEntry diffEntry = mock(DiffEntry.class);
		when(diffEntry.getChangeType()).thenReturn(DiffEntry.ChangeType.ADD);
		when(diffEntry.getNewPath()).thenReturn("NewTest.java");

		ObjectId headTree = ObjectId.fromString("d670460b4b4aece5915caf5c68d12f560a9fe3e4");

		// Мокируем получение содержимого файлов
		when(mockRepository.resolve(any())).thenReturn(headTree);
		when(service.getFileContent(mockRepository, headTree, "NewTest.java"))
				.thenReturn("""
						@Test
						void newTest() {}
						""");

		service.handleDiffs(mockRepository, diffEntry, headTree);

		assertFalse(service.affectedTests.isEmpty());
	}

	// Тест 7: Удаление тестового метода
	@Test
	void shouldDetectDeletedTestMethod() {
		String oldCode = """
				@Test
				void deletedTest() {}
				""";

		String newCode = "";

		List<AffectedTestsFinderService.TestMethodInfo> affected = service.checkDeletedTests(oldCode, newCode, "Test.java");
		assertEquals(1, affected.size());
		assertEquals("deletedTest", affected.get(0).methodName());
	}

	// Тест 8: Изменение порядка аннотаций
	@Test
	void shouldIgnoreAnnotationOrder() {
		String oldCode = """
				@Test
				@DisplayName("Test")
				void testMethod() {}
				""";

		String newCode = """
				@DisplayName("Test")
				@Test
				void testMethod() {}
				""";

		List<AffectedTestsFinderService.TestMethodInfo> affected = service.checkDeletedTests(oldCode, newCode, "Test.java");
		assertTrue(affected.isEmpty());
	}

	// Тест 9: Комбинированные изменения (аннотации + тело метода)
	@Test
	void shouldDetectCombinedChanges() {
		String oldCode = """
				@Test
				void testMethod() {}
				""";

		String newCode = """
				@Test
				@Tag("new")
				void testMethod() { int x = 5; }
				""";

		List<AffectedTestsFinderService.TestMethodInfo> affected = service.checkDeletedTests(oldCode, newCode, "Test.java");
		assertEquals(1, affected.size());
	}

	// Тест 10: Не тестовый метод (должен игнорироваться)
	@Test
	void shouldIgnoreNonTestMethods() {
		String code = """
				void helperMethod() {}
				""";

		// Используем новый метод для тестирования парсинга
		JavaParser parser = new JavaParser(new ParserConfiguration()
				.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17));

		ParseResult<CompilationUnit> result = parser.parse(code);
		assertTrue(result.isSuccessful());

		CompilationUnit cu = result.getResult().orElseThrow();
		List<AffectedTestsFinderService.TestMethodInfo> tests = service.findTestMethods(cu);

		assertTrue(tests.isEmpty());
	}
}
