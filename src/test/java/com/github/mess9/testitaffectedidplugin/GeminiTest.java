package com.github.mess9.testitaffectedidplugin;

import com.github.mess9.testitaffectedidplugin.service.AffectedTestsFinderService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


public class GeminiTest {

	private static final String TEST_FILE_PATH = "src/test/java/com/example/MyTest.java";
	private static final String DEFAULT_AUTHOR_NAME = "Test Author";
	private static final String DEFAULT_AUTHOR_EMAIL = "test@example.com";
	@TempDir
	Path tempDir;
	private AffectedTestsFinderService finderService;
	private Git git;
	private File projectDir;
	private Repository repository;

	@BeforeEach
	void setUp() throws IOException, GitAPIException {
		projectDir = tempDir.toFile();
		finderService = new AffectedTestsFinderService();

		git = Git.init().setDirectory(projectDir).call();
		repository = git.getRepository();

		Path testSrcPath = projectDir.toPath().resolve("src/test/java/com/example");
		Files.createDirectories(testSrcPath);

		RevCommit initialCommit = commitFile(TEST_FILE_PATH, "// Initial empty master file", "Initial commit");

		// HEAD уже в master, просто делаем коммит и обновляем origin/master
		RefUpdate ruOriginMaster = repository.updateRef("refs/remotes/origin/master");
		ruOriginMaster.setNewObjectId(initialCommit.getId());
		RefUpdate.Result result = ruOriginMaster.forceUpdate();
		assertThat(result).isIn(RefUpdate.Result.NEW, RefUpdate.Result.FORCED, RefUpdate.Result.NO_CHANGE, RefUpdate.Result.FAST_FORWARD);

		recreateFeatureBranch(initialCommit);
	}

	private RevCommit commitFile(String relativePath, String content, String message) throws IOException, GitAPIException {
		Path filePath = projectDir.toPath().resolve(relativePath);
		Files.createDirectories(filePath.getParent());
		Files.writeString(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		git.add().addFilepattern(relativePath.replace(File.separatorChar, '/')).call();
		return git.commit()
				.setAuthor(DEFAULT_AUTHOR_NAME, DEFAULT_AUTHOR_EMAIL)
				.setMessage(message)
				.call();
	}

	private void recreateFeatureBranch(RevCommit baseCommit) throws GitAPIException {
		try {
			git.branchDelete().setBranchNames("feature-branch").setForce(true).call();
		} catch (Exception ignored) {
		}

		git.checkout()
				.setCreateBranch(true)
				.setName("feature-branch")
				.setStartPoint(baseCommit)
				.call();
	}

	@AfterEach
	void tearDown() {
		if (git != null) {
			repository = null;
			git.close();
		}
	}

	@Test
	void findAffectedTests_annotationAddedToMethod_shouldAffectTest() throws GitAPIException, IOException {
		String masterContent = """
				package com.example;
				import org.junit.jupiter.params.ParameterizedTest;
				class MyTest {
				    @ParameterizedTest // Line 4
				    void testOne() {} // Line 5
				}
				""";
		String featureContent = """
				package com.example;
				import org.junit.jupiter.params.ParameterizedTest;
				import ru.testit.annotations.DisplayName; // New import - Line 3
				class MyTest {
				    @DisplayName("Test One Display Name") // New annotation - Line 5
				    @ParameterizedTest // Line 6
				    void testOne() {} // Line 7, method name: testOne
				}
				""";
		setupMasterAndFeature(masterContent, featureContent);

		List<AffectedTestsFinderService.TestMethodInfo> affectedTests = finderService.findAffectedTests(projectDir);

		assertThat(affectedTests).asList().hasSize(1);
		AffectedTestsFinderService.TestMethodInfo testInfo = affectedTests.get(0);
		assertThat(testInfo.displayName()).isEqualTo("Test One Display Name");
		assertThat(testInfo.methodName()).isEqualTo("testOne");
		assertThat(testInfo.startLine()).isEqualTo(5); // Строка @DisplayName в новом файле
	}

	private void setupMasterAndFeature(String masterContent, String featureContent) throws GitAPIException, IOException {
		setupMasterAndFeature(masterContent, featureContent, TEST_FILE_PATH);
	}


	// --- Тестовые методы ---

	private void setupMasterAndFeature(String masterContent, String featureContent, String filePath) throws GitAPIException, IOException {
		git.checkout().setName("master").call();
		RevCommit masterCommit = commitFile(filePath, masterContent, "Master state for file");

		RefUpdate ruOriginMaster = repository.updateRef("refs/remotes/origin/master");
		ruOriginMaster.setNewObjectId(masterCommit.getId());
		RefUpdate.Result result = ruOriginMaster.forceUpdate();
		assertThat(result).isIn(RefUpdate.Result.NEW, RefUpdate.Result.FORCED, RefUpdate.Result.NO_CHANGE, RefUpdate.Result.FAST_FORWARD);

		recreateFeatureBranch(masterCommit);
		commitFile(filePath, featureContent, "Feature state for file");
	}

	@Test
	void findAffectedTests_annotationRemovedFromMethod_shouldAffectTest() throws GitAPIException, IOException {
		String masterContent = """
				package com.example;
				import org.junit.jupiter.params.ParameterizedTest;
				import ru.testit.annotations.DisplayName;
				class MyTest {
				    @DisplayName("Test One Original") // Line 5
				    @ParameterizedTest // Line 6
				    void testOne() {} // Line 7
				}
				""";
		String featureContent = """
				package com.example;
				import org.junit.jupiter.params.ParameterizedTest;
				// import ru.testit.annotations.DisplayName; // Removed import
				class MyTest {
				    @ParameterizedTest // Line 4 (смещение из-за удаления import)
				    void testOne() {} // Line 5, method name: testOne, display name now also testOne
				}
				""";
		// Корректируем ожидаемые номера строк с учетом смещения из-за удаления import
		// Если import ru.testit.annotations.DisplayName; был на строке 3, а теперь его нет,
		// то все последующие строки смещаются на -1.
		// В featureContent @ParameterizedTest будет на строке 4.

		setupMasterAndFeature(masterContent, featureContent);

		List<AffectedTestsFinderService.TestMethodInfo> affectedTests = finderService.findAffectedTests(projectDir);

		assertThat(affectedTests).asList().hasSize(1);
		AffectedTestsFinderService.TestMethodInfo testInfo = affectedTests.get(0);
		assertThat(testInfo.displayName()).isEqualTo("testOne");
		assertThat(testInfo.methodName()).isEqualTo("testOne");
		assertThat(testInfo.startLine()).isEqualTo(4); // Строка @ParameterizedTest в новом файле
	}

	@Test
	void findAffectedTests_testMethodBodyChanged_shouldAffectTest() throws GitAPIException, IOException {
		String masterContent = """
				package com.example;
				import org.junit.jupiter.params.ParameterizedTest;
				class MyTest {
				    @ParameterizedTest // Line 4
				    void testOne() { // Line 5
				        System.out.println("Old Body"); // Line 6
				    } // Line 7
				}
				""";
		String featureContent = """
				package com.example;
				import org.junit.jupiter.params.ParameterizedTest;
				class MyTest {
				    @ParameterizedTest // Line 4
				    void testOne() { // Line 5
				        System.out.println("New Body"); // Line 6
				        // Added comment // Line 7
				    } // Line 8
				}
				""";
		setupMasterAndFeature(masterContent, featureContent);

		List<AffectedTestsFinderService.TestMethodInfo> affectedTests = finderService.findAffectedTests(projectDir);

		assertThat(affectedTests).asList().hasSize(1);
		AffectedTestsFinderService.TestMethodInfo testInfo = affectedTests.get(0);
		assertThat(testInfo.methodName()).isEqualTo("testOne");
		assertThat(testInfo.startLine()).isEqualTo(4); // Строка @ParameterizedTest
	}


	@Test
	void findAffectedTests_newTestMethodAddedInExistingFile_shouldAffectNewTest() throws GitAPIException, IOException {
		String masterContent = """
				package com.example;
				import org.junit.jupiter.params.ParameterizedTest;
				class MyTest { // Line 3
				    @ParameterizedTest // Line 4
				    void existingTest() {} // Line 5
				}
				""";
		String featureContent = """
				package com.example;
				import org.junit.jupiter.params.ParameterizedTest;
				import ru.testit.annotations.DisplayName; // Line 3
				class MyTest { // Line 4
				    @ParameterizedTest // Line 5
				    void existingTest() {} // Line 6
				
				    @ParameterizedTest // Line 8
				    @DisplayName("Newly Added Test") // Line 9
				    void newTest() {} // Line 10
				}
				""";
		setupMasterAndFeature(masterContent, featureContent);

		List<AffectedTestsFinderService.TestMethodInfo> affectedTests = finderService.findAffectedTests(projectDir);

		assertThat(affectedTests).asList().hasSize(1);
		AffectedTestsFinderService.TestMethodInfo testInfo = affectedTests.get(0);
		assertThat(testInfo.displayName()).isEqualTo("Newly Added Test");
		assertThat(testInfo.methodName()).isEqualTo("newTest");
		assertThat(testInfo.startLine()).isEqualTo(8); // Строка @ParameterizedTest для newTest
	}

	@Test
	void findAffectedTests_testMethodRemovedInExistingFile_shouldAffectRemovedTest() throws GitAPIException, IOException {
		String masterContent = """
				package com.example;
				import org.junit.jupiter.params.ParameterizedTest;
				import ru.testit.annotations.DisplayName;
				class MyTest {
				    @ParameterizedTest
				    void existingTest() {}
				
				    @ParameterizedTest
				    @DisplayName("Test To Be Removed")
				    void doomedTest() {} // Этот будет удален
				}
				""";
		String featureContent = """
				package com.example;
				import org.junit.jupiter.params.ParameterizedTest;
				// import ru.testit.annotations.DisplayName; // Мог быть удален, если больше не нужен
				class MyTest {
				    @ParameterizedTest
				    void existingTest() {}
				    // doomedTest отсутствует
				}
				""";
		setupMasterAndFeature(masterContent, featureContent);

		List<AffectedTestsFinderService.TestMethodInfo> affectedTests = finderService.findAffectedTests(projectDir);

		assertThat(affectedTests).asList().hasSize(1);
		AffectedTestsFinderService.TestMethodInfo testInfo = affectedTests.get(0);
		assertThat(testInfo.displayName()).isEqualTo("Test To Be Removed");
		assertThat(testInfo.methodName()).isEqualTo("doomedTest");
	}

	@Test
	void findAffectedTests_nonTestMethodChanged_noEffectOnExistingUndisturbedTest() throws GitAPIException, IOException {
		String masterContent = """
				 package com.example;
				 import org.junit.jupiter.params.ParameterizedTest; // Line 2
				 class MyTest { // Line 3
				     private void helperMethod() { System.out.println("Old Helper"); } // Line 4
				    \s
				     @ParameterizedTest // Line 6
				     void actualTest() { helperMethod(); } // Line 7
				 }
				\s""";
		String featureContent = """
				 package com.example;
				 import org.junit.jupiter.params.ParameterizedTest; // Line 2
				 class MyTest { // Line 3
				     private void helperMethod() { // Line 4
				         System.out.println("New Helper Code"); // Line 5 - это изменение
				     } // Line 6
				     // Пустая строка для смещения, чтобы тест ниже не попал в диапазон правок helperMethod
				    \s
				     @ParameterizedTest // Line 9 (было 6)
				     void actualTest() { helperMethod(); } // Line 10 (было 7)
				 }
				\s""";
		setupMasterAndFeature(masterContent, featureContent);

		List<AffectedTestsFinderService.TestMethodInfo> affectedTests = finderService.findAffectedTests(projectDir);
		// Ожидаем, что тест actualTest не будет затронут, если правки в helperMethod
		// и смещение строк не привели к тому, что строки actualTest попали в EditList.
		// Это чувствительный тест, зависит от того, как JGit формирует дифф.
		assertThat(affectedTests).asList().isEmpty();
	}

	@Test
	void findAffectedTests_fileSkippedByFilter_BaseTest() throws GitAPIException, IOException {
		String skippedFilePath = "src/test/java/com/example/MyBaseTest.java";
		String masterContent = "// Base master";
		String featureContent = """
				package com.example;
				import org.junit.jupiter.params.ParameterizedTest;
				class MyBaseTest { // Имя файла соответствует фильтру *BaseTest.java
				    @ParameterizedTest
				    void testInBase() {}
				}
				""";
		setupMasterAndFeature(masterContent, featureContent, skippedFilePath);

		List<AffectedTestsFinderService.TestMethodInfo> affectedTests = finderService.findAffectedTests(projectDir);
		assertThat(affectedTests).asList().isEmpty();
	}

}
