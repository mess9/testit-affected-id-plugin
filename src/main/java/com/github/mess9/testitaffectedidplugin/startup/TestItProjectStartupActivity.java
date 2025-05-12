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
		// Получаем сервис в главном потоке — это важно!
		TestItSettings settings = project.getService(TestItSettings.class);

		// Предотвращаем дальнейшие ошибки, если сервис не инициализирован
		if (settings == null) {
			return null;
		}

		// Выполняем только файловые операции/тяжёлую логику в фоне
		ApplicationManager.getApplication().executeOnPooledThread(() -> {
			settings.initProjectContext(project);
			settings.loadFromPropertiesFile();
		});
		return null;
	}

}
