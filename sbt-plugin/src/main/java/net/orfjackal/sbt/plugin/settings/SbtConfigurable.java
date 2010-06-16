// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin.settings;

import com.intellij.openapi.options.*;
import net.orfjackal.sbt.plugin.MessageBundle;

import javax.swing.*;

public class SbtConfigurable implements Configurable {
    // org.jetbrains.idea.maven.utils.MavenSettings
    // org.jetbrains.idea.maven.project.MavenImportingConfigurable

    private final SbtProjectSettingsComponent projectSettings;
    private final SbtApplicationSettingsComponent applicationSettings;
    private final SbtSettingsForm settingsForm = new SbtSettingsForm();

    public SbtConfigurable(SbtProjectSettingsComponent projectSettings, SbtApplicationSettingsComponent applicationSettings) {
        this.projectSettings = projectSettings;
        this.applicationSettings = applicationSettings;
    }

    public String getDisplayName() {
        return MessageBundle.message("sbt.config.title");
    }

    public Icon getIcon() {
        return null;
    }

    public String getHelpTopic() {
        return null;
    }

    public JComponent createComponent() {
        return settingsForm.createComponent();
    }

    public boolean isModified() {
        return settingsForm.isModified(projectSettings.getState(), applicationSettings.getState());
    }

    public void apply() throws ConfigurationException {
        settingsForm.copyTo(projectSettings.getState(), applicationSettings.getState());
    }

    public void reset() {
        settingsForm.copyFrom(projectSettings.getState(), applicationSettings.getState());
    }

    public void disposeUIResources() {
    }
}
