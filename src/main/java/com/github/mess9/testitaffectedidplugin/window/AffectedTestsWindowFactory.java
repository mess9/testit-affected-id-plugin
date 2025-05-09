package com.github.mess9.testitaffectedidplugin.window;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AffectedTestsWindowFactory implements ToolWindowFactory {

	private static final Map<Project, AffectedTestsWindow> PROJECT_TO_WINDOW_MAP = new ConcurrentHashMap<>();

	public static AffectedTestsWindow getInstance(Project project) {
		if (project == null) {
			return null;
		}
		return PROJECT_TO_WINDOW_MAP.get(project);
	}

	@Override
	public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
		AffectedTestsWindow affectedTestsWindow = new AffectedTestsWindow(project);
		PROJECT_TO_WINDOW_MAP.put(project, affectedTestsWindow);

		Content content = ContentFactory.getInstance().createContent(
				affectedTestsWindow.getContent(),
				"Affected Tests", // информативный заголовок
				false);
		toolWindow.getContentManager().addContent(content);

	}
}
