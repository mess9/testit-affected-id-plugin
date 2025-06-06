package com.github.mess9.testitaffectedidplugin.action;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static com.github.mess9.testitaffectedidplugin.window.AffectedTestsWindow.CLASS_NAME_COLUMN_INDEX;
import static com.github.mess9.testitaffectedidplugin.window.AffectedTestsWindow.METHOD_NAME_COLUMN_INDEX;
import static com.github.mess9.testitaffectedidplugin.window.AffectedTestsWindow.PACKAGE_COLUMN_INDEX;
import static com.github.mess9.testitaffectedidplugin.window.AffectedTestsWindow.TEST_ID_COLUMN_INDEX;
import static com.github.mess9.testitaffectedidplugin.window.AffectedTestsWindow.TEST_IT_URL_COLUMN_INDEX;
import static com.github.mess9.testitaffectedidplugin.window.AffectedTestsWindow.TEST_NAME_COLUMN_INDEX;
import static com.intellij.openapi.ui.Messages.showWarningDialog;

public class MouseActions {

	public void addMouseListenerToTable(JTable table, Project project) {
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {

					int row = table.rowAtPoint(e.getPoint());
					int col = table.columnAtPoint(e.getPoint());

					if (row == -1 || col == -1) return;

					String packageName = table.getValueAt(row, PACKAGE_COLUMN_INDEX).toString();
					String className = table.getValueAt(row, CLASS_NAME_COLUMN_INDEX).toString();
					String fqn = packageName + "." + className;

					if (col == CLASS_NAME_COLUMN_INDEX || col == PACKAGE_COLUMN_INDEX) {
						// Клик по классу — открыть класс
						openElementInEditor(fqn, null, project);
					} else if ((col == METHOD_NAME_COLUMN_INDEX || col == TEST_NAME_COLUMN_INDEX || col == TEST_ID_COLUMN_INDEX) && table.getValueAt(row, METHOD_NAME_COLUMN_INDEX) != null) {
						// Клик по методу — открыть метод
						String methodName = table.getValueAt(row, METHOD_NAME_COLUMN_INDEX).toString();
						openElementInEditor(fqn, methodName, project);
					} else if (col == TEST_IT_URL_COLUMN_INDEX && table.getValueAt(row, TEST_IT_URL_COLUMN_INDEX) != null && table.getValueAt(row, TEST_ID_COLUMN_INDEX) != null) {
						String url = table.getValueAt(row, TEST_IT_URL_COLUMN_INDEX).toString();
						if (!url.isBlank()) {
							BrowserUtil.browse(url);
						}
					}
				}
			}
		});
	}

	private void openElementInEditor(String fqn, String methodName, Project project) {
		ApplicationManager.getApplication().invokeLater(() -> {
			GlobalSearchScope scope = GlobalSearchScope.allScope(project);
			PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(fqn, scope);

			if (psiClass == null) {
				showWarningDialog(project,
						"Не удалось найти класс: " + fqn,
						"Ошибка");
				return;
			}

			if (methodName == null || methodName.isEmpty()) {
				psiClass.navigate(true);
			} else {
				// Ищем метод по имени
				for (PsiMethod method : psiClass.getMethods()) {
					if (method.getName().equals(methodName)) {
						method.navigate(true);
						return;
					}
				}
				// Если не нашли метод — просто открываем класс
				psiClass.navigate(true);
			}
		});
	}

}
