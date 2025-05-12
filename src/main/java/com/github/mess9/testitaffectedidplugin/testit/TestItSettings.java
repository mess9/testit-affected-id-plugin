package com.github.mess9.testitaffectedidplugin.testit;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Service(Service.Level.PROJECT)
@State(
		name = "TestItSettings",
		storages = {@Storage("testItSettings.xml")}
)
public final class TestItSettings implements PersistentStateComponent<TestItSettings.State> {

	private static final Logger LOG = Logger.getInstance(TestItSettings.class);

	private static final String CREDENTIAL_ATTRIBUTE_USER = "TestIt API Token";

	private State state = new State();
	// Служебное поле (не сериализуется)
	private transient Project project;

	public TestItSettings() {
	}

	@Override
	public @NotNull State getState() {
		return state;
	}


	// Метод для инициализации сервиса проектом
	public void initProjectContext(Project project) {
		this.project = project;
	}


	public String getPrivateToken() {
		CredentialAttributes attributes = createCredentialAttributes();
		Credentials credentials = PasswordSafe.getInstance().get(attributes);
		return credentials != null ? credentials.getPasswordAsString() : "";
	}
	public void setPrivateToken(String token) {
		CredentialAttributes attributes = createCredentialAttributes();
		Credentials credentials = new Credentials(attributes.getUserName(), token);
		PasswordSafe.getInstance().set(attributes, credentials);
	}
	private CredentialAttributes createCredentialAttributes() {
		return new CredentialAttributes(
				CredentialAttributesKt.generateServiceName("TestItPlugin", CREDENTIAL_ATTRIBUTE_USER)
		);
	}

	@Override
	public void loadState(@NotNull State state) {
		this.state = state;
		LOG.info("TestItSettings loaded from XML: URL='" + this.state.url + "', ProjectID='" + this.state.projectId + "'");
	}

	public void loadFromPropertiesFile() {
		if (this.project == null || this.project.getBasePath() == null) {
			LOG.warn("Cannot load TestIt settings from properties file: Project context not set or project base path is null.");
			return;
		}
		Path propertiesPath = Paths.get(this.project.getBasePath(), "src", "test", "resources", "testit.properties");

		if (!Files.exists(propertiesPath)) {
			LOG.info("testit.properties file not found at " + propertiesPath + ". Using settings from XML or defaults.");
			return;
		}

		Properties properties = new Properties();
		try (FileInputStream input = new FileInputStream(propertiesPath.toFile())) {
			properties.load(input);

			String fileUrl = properties.getProperty("url");
			String fileProjectId = properties.getProperty("projectId");
			String filePrivateToken = properties.getProperty("privateToken");

			if (fileUrl != null) {
				this.state.url = fileUrl;
			}
			if (fileProjectId != null) {
				this.state.projectId = fileProjectId;
			}
			if (filePrivateToken != null) {
				setPrivateToken(filePrivateToken);
			}
			LOG.info("TestItSettings loaded/overridden from testit.properties: URL='" + this.state.url + "', ProjectID='" + this.state.projectId + "'");

		} catch (IOException e) {
			LOG.warn("Could not load testit.properties file: " + e.getMessage() + ". Using settings from XML or defaults.");
		}
	}

	public String getUrl() {
		return state.url;
	}

	public void setUrl(String url) {
		this.state.url = url;
	}

	public String getProjectId() {
		return state.projectId;
	}

	public void setProjectId(String projectId) {
		this.state.projectId = projectId;
	}

	public String getProjectUrlId() {
		return state.projectUrlId;
	}

	public void setProjectUrlId(String projectUrlId) {
		this.state.projectUrlId = projectUrlId;
	}

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	// сериализуемое состояние — во внутреннем классе!
	public static class State {

		public String url = "";
		public String projectId = "";
		public String projectUrlId = "";
	}

}