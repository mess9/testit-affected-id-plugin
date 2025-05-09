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
import com.intellij.util.xmlb.XmlSerializerUtil;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Setter
@Getter
@Service(Service.Level.PROJECT)
@State(
		name = "TestItSettings",
		storages = {@Storage("testItSettings.xml")}
)
public final class TestItSettings implements PersistentStateComponent<TestItSettings> {

	private static final Logger LOG = Logger.getInstance(TestItSettings.class);

	private static final String CREDENTIAL_ATTRIBUTE_USER = "TestIt API Token";
	private String url = "";
	private String projectId = "";
	private String projectUrlId = "";

	private transient Project project;

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
	public @NotNull TestItSettings getState() {
		return this;
	}

	@Override
	public void loadState(@NotNull TestItSettings state) {
		XmlSerializerUtil.copyBean(state, this);
		LOG.info("TestItSettings loaded from XML: URL='" + this.url + "', ProjectID='" + this.projectId + "'");
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
				this.url = fileUrl;
			}
			if (fileProjectId != null) {
				this.projectId = fileProjectId;
			}
			if (filePrivateToken != null) {
				setPrivateToken(filePrivateToken);
			}
			LOG.info("TestItSettings loaded/overridden from testit.properties: URL='" + this.url + "', ProjectID='" + this.projectId + "'");

		} catch (IOException e) {
			LOG.warn("Could not load testit.properties file: " + e.getMessage() + ". Using settings from XML or defaults.");
		}
	}


}