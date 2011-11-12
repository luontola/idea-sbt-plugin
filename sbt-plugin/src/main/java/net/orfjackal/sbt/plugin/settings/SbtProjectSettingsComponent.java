// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin.settings;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;

import java.io.File;

@State(name = "SbtSettings", storages = {
        @Storage(id = "default", file = "$PROJECT_FILE$", scheme = StorageScheme.DEFAULT),
        @Storage(id = "dir", file = "$PROJECT_CONFIG_DIR$/compiler.xml", scheme = StorageScheme.DIRECTORY_BASED)})
public class SbtProjectSettingsComponent extends AbstractProjectComponent implements PersistentStateComponent<SbtProjectSettings> {

    private SbtProjectSettings projectSettings = new SbtProjectSettings();

    public SbtProjectSettingsComponent(Project project) {
        super(project);
    }

    public SbtProjectSettings getState() {
        return projectSettings;
    }

    public void loadState(SbtProjectSettings state) {
        this.projectSettings = state;
    }

    public String effectiveSbtLauncherVmParameters(SbtApplicationSettingsComponent applicationSettings) {
        if (projectSettings.isUseApplicationSettings()) {
            return applicationSettings.getState().getSbtLauncherVmParameters();
        } else {
            return getState().getSbtLauncherVmParameters();
        }
    }

    public String effectiveSbtLauncherJarPath(SbtApplicationSettingsComponent applicationSettings) {
        if (projectSettings.isUseApplicationSettings()) {
            return applicationSettings.getState().getSbtLauncherJarPath();
        } else {
            return getState().getSbtLauncherJarPath();
        }
    }

    public String getJavaCommand(SbtApplicationSettingsComponent applicationSettings) {
        if (applicationSettings.getState().isUseCustomJdk()) {
            String systemDependentJdkHome = FileUtil.toSystemDependentName(applicationSettings.getState().getJdkHome());
            return systemDependentJdkHome + File.separator + "bin" + File.separator + "java";
        } else {
            return "java";
        }
    }
}
