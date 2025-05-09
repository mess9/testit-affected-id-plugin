package com.github.mess9.testitaffectedidplugin.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.intellij.openapi.diagnostic.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class AffectedTestsFinderService {

	private static final Logger log = Logger.getInstance(AffectedTestsFinderService.class);
	private static final String TEST_PATH = "src/test/java";
	private static final JavaParser JAVA_PARSER = new JavaParser(new ParserConfiguration()
			.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));

	public final List<TestMethodInfo> affectedTests = new ArrayList<>();

	public List<TestMethodInfo> findAffectedTests(File projectDir) throws IOException, GitAPIException {
		affectedTests.clear();
		log.info("Repo path: " + projectDir.getAbsolutePath());

		try (Git git = openRepository(projectDir)) {
			Repository repository = git.getRepository();
			log.info("Current branch: " + repository.getBranch());
			ObjectId headTree = repository.resolve("HEAD^{tree}");
			ObjectId masterTree = repository.resolve("origin/master^{tree}");
			if (masterTree == null) {
				log.error("Cannot find origin/master branch");
				return affectedTests;
			}
			log.info("origin/master tree hash: " + masterTree.getName());
			log.info("HEAD tree hash: " + headTree.getName());

			List<DiffEntry> diffs = git.diff()
					.setOldTree(prepareTreeParser(repository, masterTree))
					.setNewTree(prepareTreeParser(repository, headTree))
					.setPathFilter(PathFilter.create(TEST_PATH))
					.call();
			if (diffs.isEmpty()) {
				log.info("No changes found in test files");
				return affectedTests;
			}
			for (DiffEntry diff : diffs) {
				handleDiffs(repository, diff, headTree);
			}
		}

		return affectedTests.stream().distinct().collect(Collectors.toList());
	}

	private Git openRepository(File repoDir) throws IOException {
		try (Repository repository = new FileRepositoryBuilder()
				.setGitDir(new File(repoDir, ".git"))
				.build()) {
			return new Git(repository);
		}
	}

	private CanonicalTreeParser prepareTreeParser(Repository repository, ObjectId treeId) throws IOException {
		try (RevWalk walk = new RevWalk(repository)) {
			RevTree tree = walk.parseTree(treeId);
			CanonicalTreeParser treeParser = new CanonicalTreeParser();
			try (ObjectReader reader = repository.newObjectReader()) {
				treeParser.reset(reader, tree);
			}
			return treeParser;
		}
	}

	public void handleDiffs(Repository repository, DiffEntry diff, ObjectId headTree) throws IOException {
		String filePath = diff.getNewPath();
		if (skipNoTestClasses(filePath)) {
			log.info(format("Skipping file: %s", filePath));
			return;
		}

		ObjectId oldTree = repository.resolve("origin/master^{tree}");
		String oldFileContent = getFileContent(repository, oldTree, diff.getOldPath());
		String newFileContent = getFileContent(repository, headTree, filePath);

		log.info(format("\nProcessing file: %s", filePath));
		log.info(format("Change type: %s", diff.getChangeType()));

		List<TestMethodInfo> newTests = checkDeletedTests(oldFileContent, newFileContent, filePath);
		if (newTests == null) return;
		checkExistTests(repository, diff, newTests, filePath);
	}

	private boolean skipNoTestClasses(String filePath) {
		return filePath.endsWith("Suite.java") ||
				filePath.matches(".*(Base|Abstract).*Test\\.java") ||
				!filePath.endsWith("Test.java");
	}

	public String getFileContent(Repository repository, ObjectId treeId, String filePath) throws IOException {
		try (TreeWalk treeWalk = TreeWalk.forPath(repository, filePath, treeId)) {
			if (treeWalk == null) {
				return "";
			}
			ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
			return new String(loader.getBytes(), StandardCharsets.UTF_8);
		}
	}

	public List<TestMethodInfo> checkDeletedTests(String oldFileContent, String newFileContent, String filePath) {
		Optional<CompilationUnit> oldCompilationUnit = parseJavaFile(oldFileContent);
		Optional<CompilationUnit> newCompilationUnit = parseJavaFile(newFileContent);

		if (oldCompilationUnit.isEmpty() || newCompilationUnit.isEmpty()) {
			log.error("Failed to parse file: {}", filePath);
			return null;
		}

		List<TestMethodInfo> oldTests = findTestMethods(oldCompilationUnit.get());
		List<TestMethodInfo> newTests = findTestMethods(newCompilationUnit.get());

		for (TestMethodInfo oldTest : oldTests) {
			Optional<TestMethodInfo> newTestOpt = newTests.stream()
					.filter(t -> t.methodName().equals(oldTest.methodName()))
					.findFirst();
			if (newTestOpt.isEmpty()) {
				affectedTests.add(oldTest);
				log.info(format(">>> Affected test (DELETED): %s (method: %s)", oldTest.displayName(), oldTest.methodName()));
			} else {
				TestMethodInfo newTest = newTestOpt.get();
				// Проверяем изменения в аннотациях
				if (!newTest.annotations().equals(oldTest.annotations())) {
					affectedTests.add(newTest);
					log.info(">>> Affected test (ANNOTATIONS CHANGED): " + newTest.displayName());
				}
			}
		}
		return newTests;
	}

	private void checkExistTests(Repository repository, DiffEntry diff, List<TestMethodInfo> newTests, String filePath) throws IOException {
		if (!newTests.isEmpty()) {
			findAffectedTestsInHelperMethods(repository, diff, newTests, filePath);
			try (DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
				df.setRepository(repository);
				df.setPathFilter(PathFilter.create(filePath));
				EditList edits = df.toFileHeader(diff).toEditList();

				log.info(format("Found %s edits in file", edits.size()));
				edits.forEach(e -> log.info(format("Edit: %s (lines %d-%d)", e, e.getBeginB() + 1, e.getEndB() + 1)));

				for (TestMethodInfo test : newTests) {
					log.info(format("Test candidate: %s [%d - %d]", test.displayName(), test.startLine(), test.endLine()));

					if (isTestAffected(test, edits, diff.getChangeType())) {
						affectedTests.add(test);
						log.info(format(">>> Affected test: %s (method: %s)", test.displayName(), test.methodName()));
					}
				}
			}
		}
	}

	public Optional<CompilationUnit> parseJavaFile(String content) {
		ParseResult<CompilationUnit> result = JAVA_PARSER.parse(content);
		if (result.isSuccessful()) return result.getResult();

		result.getProblems().forEach(p -> log.error("Parse error {}", p.getMessage()));
		return Optional.empty();
	}

	public List<TestMethodInfo> findTestMethods(CompilationUnit cu) {
		List<TestMethodInfo> testMethods = new ArrayList<>();
		cu.accept(new VoidVisitorAdapter<Void>() {
			@Override
			public void visit(MethodDeclaration method, Void arg) {
				super.visit(method, arg);
				if (isTestMethod(method)) {
					String displayName = method.getAnnotationByName("DisplayName")
							.flatMap(this::parseDisplayName)
							.orElse(method.getNameAsString());
					List<String> annotations = method.getAnnotations().stream()
							.map(Node::toString) // включая параметры
							.collect(Collectors.toList());

					method.getRange().ifPresent(range -> {
						int fullStartLine = method.getAnnotations().stream()
								.map(a -> a.getBegin().map(pos -> pos.line).orElse(range.begin.line))
								.min(Integer::compareTo)
								.orElse(range.begin.line);

						testMethods.add(new TestMethodInfo(
								getPackageName(method),
								getClassName(method),
								displayName,
								method.getNameAsString(),
								fullStartLine,
								range.end.line,
								annotations
						));
					});
				}
			}

			private Optional<String> parseDisplayName(AnnotationExpr annotation) {
				try {
					if (annotation.isNormalAnnotationExpr()) {
						return Optional.of(annotation.asNormalAnnotationExpr()
								.getPairs().getFirst().orElseThrow()
								.getValue().asStringLiteralExpr().getValue());
					} else if (annotation.isSingleMemberAnnotationExpr()) {
						return Optional.of(annotation.asSingleMemberAnnotationExpr()
								.getMemberValue().asStringLiteralExpr().getValue());
					}
				} catch (Exception e) {
					log.error("Error parsing DisplayName: {}", e.getMessage());
				}
				return Optional.empty();
			}
		}, null);
		return testMethods;
	}

	private void findAffectedTestsInHelperMethods(Repository repository, DiffEntry diff, List<TestMethodInfo> newTests, String filePath) throws IOException {
		// --- Считываем содержимое файла (старое и новое) ---
		ObjectId oldTree = repository.resolve("origin/master^{tree}");
		ObjectId headTree = repository.resolve("HEAD^{tree}");
		String oldFileContent = getFileContent(repository, oldTree, diff.getOldPath());
		String newFileContent = getFileContent(repository, headTree, filePath);

		// --- Парсим обе версии файла ---
		Optional<CompilationUnit> oldCuOpt = parseJavaFile(oldFileContent);
		Optional<CompilationUnit> newCuOpt = parseJavaFile(newFileContent);

		if (oldCuOpt.isPresent() && newCuOpt.isPresent()) {
			CompilationUnit oldCu = oldCuOpt.get();
			CompilationUnit newCu = newCuOpt.get();

			// --- Список helper-методов старый и новый ---
			List<HelperMethodInfo> oldHelpers = findHelperMethods(oldCu);
			List<HelperMethodInfo> newHelpers = findHelperMethods(newCu);

			// --- Собираем изменённые helper-методы по имени и (например) телу ---
			Set<String> changedHelpers = new HashSet<>();
			for (HelperMethodInfo oldHelper : oldHelpers) {
				for (HelperMethodInfo newHelper : newHelpers) {
					if (oldHelper.methodName().equals(newHelper.methodName())
							&& !oldHelper.bodyString().equals(newHelper.bodyString())) {
						changedHelpers.add(oldHelper.methodName());
					}
				}
			}
			// Можно добавить detect новых или удалённых helper-методов, если нужно

			// --- Применяем анализ к каждому тест-методу ---
			for (TestMethodInfo test : newTests) {
				// Находим MethodDeclaration для теста
				Optional<MethodDeclaration> maybeMethodDecl = newCu.findAll(MethodDeclaration.class).stream()
						.filter(m -> m.getNameAsString().equals(test.methodName()))
						.findFirst();
				if (maybeMethodDecl.isPresent()) {
					Set<String> calledMethods = extractCalledMethods(maybeMethodDecl.get());
					// Если тест вызывает изменённый helper — считаем затронутым
					if (!Collections.disjoint(changedHelpers, calledMethods)) {
						affectedTests.add(test);
						log.info(">>> Affected test (HELPER CHANGED): " + test.displayName());
					}
				}
			}
		}
	}

	private boolean isTestAffected(TestMethodInfo test, EditList edits, DiffEntry.ChangeType changeType) {
		// для новых файлов все тесты считаем затронутыми
		if (changeType == DiffEntry.ChangeType.ADD) {
			log.info(format("New file - considering test '%s' as affected\n", test.displayName()));
			return true;
		}

		// для удалённых файлов все тесты считаем затронутыми
		if (changeType == DiffEntry.ChangeType.DELETE) {
			log.info(format("File deleted - considering test '%s' as affected\n", test.displayName()));
			return true;
		}

		// для изменённых файлов анализируем
		for (Edit edit : edits) {
			// для DELETE проверяем, был ли тест в удалённом диапазоне
			if (edit.getType() == Edit.Type.DELETE) {
				int deleteStart = edit.getBeginA() + 1; // Старые строки (из master)
				int deleteEnd = edit.getEndA() + 1;

				log.info(format("Checking DELETE [%d-%d] vs test '%s' [%d-%d]",
						deleteStart, deleteEnd,
						test.displayName(), test.startLine(), test.endLine()));
			}
			// Для INSERT проверяем новые тесты
			else if (edit.getType() == Edit.Type.INSERT) {
				int insertStart = edit.getBeginB() + 1;
				int insertEnd = edit.getEndB();

				if (insertEnd < insertStart && !(insertStart == insertEnd + 1 && edit.getLengthA() == 0)) { // пустая вставка может иметь end < start
					// Если правка реально что-то вставляет/заменяет, то insertEnd >= insertStart
					if (edit.getLengthB() == 0) { // Ничего не добавлено/не заменено в
						log.debug(format("Skipping %s edit as it results in an empty or invalid range in the new file: B_lines [%d-%d]",
								edit.getType(), insertStart, insertEnd));
						continue;
					}
				}

				if (rangesOverlap(insertStart, insertEnd, test.startLine(), test.endLine())) {
					log.info(format(">>> Affected test (MODIFIED/INSERTED): %s (method: %s) " +
									"because its new range [%d-%d] overlaps with %s edit's new file impact [%d-%d]",
							test.displayName(), test.methodName(), test.startLine(), test.endLine(),
							edit.getType(), insertStart, insertEnd));
					return true;
				}
			}
			// Для REPLACE и других изменений
			else {
				int editStart = edit.getBeginB() + 1;
				int editEnd = edit.getEndB();

				if (rangesOverlap(editStart, editEnd, test.startLine(), test.endLine())) {
					log.info(format("Test '%s' overlaps with modification\n", test.displayName()));
					return true;
				}
			}
		}

		return false;
	}

	private boolean isTestMethod(MethodDeclaration method) {
		return method.getAnnotations().stream()
				.anyMatch(a -> a.getNameAsString().equals("ParameterizedTest"));
	}

	private String getPackageName(MethodDeclaration method) {
		return method.findCompilationUnit()
				.flatMap(CompilationUnit::getPackageDeclaration)
				.map(pkg -> pkg.getName().asString())
				.orElse("unknown.package");
	}

	private String getClassName(MethodDeclaration method) {
		Optional<ClassOrInterfaceDeclaration> maybeClass = findAncestorOfType(method, ClassOrInterfaceDeclaration.class);
		return maybeClass.map(ClassOrInterfaceDeclaration::getNameAsString).orElse("unknown class");
	}

	private List<HelperMethodInfo> findHelperMethods(CompilationUnit cu) {
		List<HelperMethodInfo> helpers = new ArrayList<>();
		cu.accept(new VoidVisitorAdapter<Void>() {
			@Override
			public void visit(MethodDeclaration method, Void arg) {
				super.visit(method, arg);
				if (!isTestMethod(method)) {
					method.getRange().ifPresent(range -> helpers.add(new HelperMethodInfo(
							method.getNameAsString(),
							range.begin.line,
							range.end.line,
							method.getBody().map(Object::toString).orElse("")
					)));
				}
			}
		}, null);
		return helpers;
	}

	private Set<String> extractCalledMethods(MethodDeclaration method) {
		Set<String> called = new HashSet<>();
		method.accept(new VoidVisitorAdapter<Void>() {
			@Override
			public void visit(MethodCallExpr callExpr, Void arg) {
				super.visit(callExpr, arg);
				called.add(callExpr.getNameAsString());
			}
		}, null);
		return called;
	}

	private boolean rangesOverlap(int start1, int end1, int start2, int end2) {
		return Math.max(start1, start2) <= Math.min(end1, end2);
	}

	public static <T extends Node> Optional<T> findAncestorOfType(Node node, Class<T> clazz) {
		Node current = node.getParentNode().orElse(null);
		while (current != null) {
			if (clazz.isInstance(current)) {
				return Optional.of(clazz.cast(current));
			}
			current = current.getParentNode().orElse(null);
		}
		return Optional.empty();
	}

	public record TestMethodInfo(
			String packageName,
			String className,
			String displayName,
			String methodName,
			int startLine,
			int endLine,
			List<String> annotations
	) {

	}

	public record HelperMethodInfo(
			String methodName,
			int startLine,
			int endLine,
			String bodyString
	) {

	}

}
