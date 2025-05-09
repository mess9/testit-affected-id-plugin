package com.github.mess9.testitaffectedidplugin.startup;

import com.github.mess9.testitaffectedidplugin.testit.TestItSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TestItProjectStartupActivity implements ProjectActivity {

	@Nullable
	@Override
	public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
		// Выполняем чтение файла в фоновом потоке, чтобы не блокировать UI
		ApplicationManager.getApplication().executeOnPooledThread(() -> {
			TestItSettings settings = project.getService(TestItSettings.class);
			if (settings != null) {
				settings.initProjectContext(project);
				settings.loadFromPropertiesFile();
			}
		});
		return null;
	}
}
