// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin.settings;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;

public class SbtSettingsForm {
    // org.jetbrains.idea.maven.project.MavenImportingSettingsForm

    private final JPanel root;
    private final JCheckBox useSbtOutputDirs;
    private final JTextField sbtLauncherJarPath;

    public SbtSettingsForm() {
        root = new JPanel(new MigLayout());

        useSbtOutputDirs = new JCheckBox();
        root.add(useSbtOutputDirs);
        root.add(new JLabel("Use SBT output directories"), "wrap");

        sbtLauncherJarPath = new JTextField();
        root.add(new JLabel("Location of SBT launcher JAR file:"), "wrap");
        root.add(sbtLauncherJarPath);
    }

    public JComponent createComponent() {
        return root;
    }

    public boolean isModified(SbtProjectSettings projectSettings, SbtApplicationSettings applicationSettings) {
        SbtProjectSettings currentProj = new SbtProjectSettings();
        SbtApplicationSettings currentApp = new SbtApplicationSettings();
        copyTo(currentProj, currentApp);
        return !currentProj.equals(projectSettings) ||
                !currentApp.equals(applicationSettings);
    }

    public void copyTo(SbtProjectSettings projectSettings, SbtApplicationSettings applicationSettings) {
        projectSettings.setUseSbtOutputDirs(useSbtOutputDirs.isSelected());
        applicationSettings.setSbtLauncherJarPath(sbtLauncherJarPath.getText());
    }

    public void copyFrom(SbtProjectSettings projectSettings, SbtApplicationSettings applicationSettings) {
        useSbtOutputDirs.setSelected(projectSettings.isUseSbtOutputDirs());
        sbtLauncherJarPath.setText(applicationSettings.getSbtLauncherJarPath());
    }
}
