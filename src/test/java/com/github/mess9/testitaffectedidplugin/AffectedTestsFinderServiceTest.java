package com.github.mess9.testitaffectedidplugin;

import com.github.mess9.testitaffectedidplugin.service.AffectedTestsFinderService;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class AffectedTestsFinderServiceTest {

	final AffectedTestsFinderService service = new AffectedTestsFinderService();

	@Test
	void testAffected_whenNewFile_shouldBeAffected() {
		var test = new AffectedTestsFinderService.TestMethodInfo("class", "Display name", "method", 10, 20, List.of("Anno1", "Anno2"));
		var result = callIsTestAffected(test, List.of(), DiffEntry.ChangeType.ADD);
		assertTrue(result, "New file — тест должен считаться затронутым");
	}

	private boolean callIsTestAffected(AffectedTestsFinderService.TestMethodInfo test,
	                                   List<Edit> edits,
	                                   DiffEntry.ChangeType changeType) {
		EditList editList = new EditList();
		editList.addAll(edits);

		// Доступ к isTestAffected через Reflection (если оставить private)
		try {
			var method = AffectedTestsFinderService.class.getDeclaredMethod(
					"isTestAffected", AffectedTestsFinderService.TestMethodInfo.class,
					EditList.class, DiffEntry.ChangeType.class);
			method.setAccessible(true);
			return (boolean) method.invoke(service, test, editList, changeType);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void testAffected_whenFileDeleted_shouldBeAffected() {
		var test = new AffectedTestsFinderService.TestMethodInfo("class", "Display name", "method", 10, 20, List.of("Anno1", "Anno2"));
		var result = callIsTestAffected(test, List.of(), DiffEntry.ChangeType.DELETE);
		assertTrue(result, "Удалённый файл — тест должен считаться затронутым");
	}

	@Test
	void testAffected_whenTestInEditRange_shouldBeAffected() {
		var edit = new Edit(5, 25, 5, 25); // REPLACE 5-25
		var edits = new EditList();
		edits.add(edit);

		var test = new AffectedTestsFinderService.TestMethodInfo("class", "Display name", "method", 15, 17, List.of("Anno1", "Anno2"));
		var result = callIsTestAffected(test, edits, DiffEntry.ChangeType.MODIFY);
		assertTrue(result, "Изменения затрагивают строки теста — должен быть затронут");
	}

	@Test
	void testAffected_whenTestOutsideEditRange_shouldNotBeAffected() {
		var edit = new Edit(30, 35, 30, 35); // REPLACE 30-35
		var edits = new EditList();
		edits.add(edit);

		var test = new AffectedTestsFinderService.TestMethodInfo("class", "Display name", "method", 10, 20, List.of("Anno1", "Anno2"));
		var result = callIsTestAffected(test, edits, DiffEntry.ChangeType.MODIFY);
		assertFalse(result, "Изменения не затрагивают строки теста — не должен быть затронут");
	}

	@Test
	void testAffected_whenAnnotationLineAffected_shouldBeDetected() {
		// Диапазон изменения охватывает только строки над методом
		var edit = new Edit(8, 10, 8, 10); // DELETE/INSERT строк 9-10
		var edits = new EditList();
		edits.add(edit);

		// метод начинается с 10 строки, но аннотация — с 9
		var test = new AffectedTestsFinderService.TestMethodInfo("class", "Display name", "method", 9, 15, List.of("Anno1", "Anno2"));
		var result = callIsTestAffected(test, edits, DiffEntry.ChangeType.MODIFY);
		assertTrue(result, "Изменение аннотации должно засчитать тест как затронутый");
	}

	@Test
	void testAffected_whenTestFullyInInsertedBlock_shouldBeAffected() {
		var edit = new Edit(0, 0, 20, 25); // INSERT в новые строки 21-25
		var edits = new EditList();
		edits.add(edit);

		var test = new AffectedTestsFinderService.TestMethodInfo("class", "Display name", "method", 21, 24, List.of("Anno1", "Anno2"));
		var result = callIsTestAffected(test, edits, DiffEntry.ChangeType.MODIFY);
		assertTrue(result, "Полностью новый тест должен быть отмечен как затронутый");
	}
}
