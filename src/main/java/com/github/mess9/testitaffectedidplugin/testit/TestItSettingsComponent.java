package com.github.mess9.testitaffectedidplugin.testit;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;

import javax.swing.*;

public class TestItSettingsComponent {

	private final JPanel myMainPanel;
	private final JBTextField urlField = new JBTextField();
	private final JBTextField projectIdField = new JBTextField();
	private final JBPasswordField tokenField = new JBPasswordField();

	public TestItSettingsComponent() {
		myMainPanel = FormBuilder.createFormBuilder()
				.addLabeledComponent(new JBLabel("TestIT URL:"), urlField, 1, false)
				.addLabeledComponent(new JBLabel("Project ID:"), projectIdField, 1, false)
				.addLabeledComponent(new JBLabel("API token:"), tokenField, 1, false)
				.addComponentFillVertically(new JPanel(), 0)
				.getPanel();
		//todo добавить кнопочку тест
	}

	public JPanel getPanel() {
		return myMainPanel;
	}

	public JComponent getPreferredFocusedComponent() {
		return urlField;
	}

	public String getUrl() {
		return urlField.getText();
	}

	public void setUrl(String url) {
		urlField.setText(url);
	}

	public String getProjectId() {
		return projectIdField.getText();
	}

	public void setProjectId(String projectId) {
		projectIdField.setText(projectId);
	}

	public String getToken() {
		return new String(tokenField.getPassword());
	}

	public void setToken(String token) {
		tokenField.setText(token);
	}
}

