package com.github.mess9.testitaffectedidplugin.testit;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class TestItSettingsConfigurable implements Configurable {

	private final Project project;
	private TestItSettingsComponent mySettingsComponent;

	public TestItSettingsConfigurable(Project project) {
		this.project = project;
	}

	@Nls(capitalization = Nls.Capitalization.Title)
	@Override
	public String getDisplayName() {
		return "TestIT Settings";
	}

	@Override
	public JComponent getPreferredFocusedComponent() {
		return mySettingsComponent.getPreferredFocusedComponent();
	}

	@Nullable
	@Override
	public JComponent createComponent() {
		mySettingsComponent = new TestItSettingsComponent();
		return mySettingsComponent.getPanel();
	}

	@Override
	public boolean isModified() {
		TestItSettings settings = project.getService(TestItSettings.class);
		boolean modified = !mySettingsComponent.getUrl().equals(settings.getUrl());
		modified |= !mySettingsComponent.getProjectId().equals(settings.getProjectId());
		modified |= !mySettingsComponent.getToken().equals(settings.getPrivateToken());
		return modified;
	}

	@Override
	public void apply() {
		TestItSettings settings = project.getService(TestItSettings.class);
		settings.setUrl(mySettingsComponent.getUrl());
		settings.setProjectId(mySettingsComponent.getProjectId());
		settings.setPrivateToken(mySettingsComponent.getToken());
	}

	@Override
	public void reset() {
		TestItSettings settings = project.getService(TestItSettings.class);
		mySettingsComponent.setUrl(settings.getUrl());
		mySettingsComponent.setProjectId(settings.getProjectId());
		mySettingsComponent.setToken(settings.getPrivateToken());
	}

	@Override
	public void disposeUIResources() {
		mySettingsComponent = null;
	}
}
