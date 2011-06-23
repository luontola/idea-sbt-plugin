// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin.settings;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.event.*;
import java.io.File;

public class SbtSettingsForm {
    // org.jetbrains.idea.maven.project.MavenImportingSettingsForm

    private final JPanel root;
    private final JTextField sbtLauncherJarPath;
    private final JTextField vmParameters;

    public SbtSettingsForm() {
        sbtLauncherJarPath = new JTextField();
        vmParameters = new JTextField();

        JPanel ideSettings = new JPanel(new MigLayout("", "[grow]", "[nogrid]"));
        ideSettings.setBorder(BorderFactory.createTitledBorder("IDE Settings"));
        {
            JLabel label = new JLabel("SBT launcher JAR file (sbt-launch.jar)");
            label.setDisplayedMnemonic('L');
            label.setLabelFor(sbtLauncherJarPath);

            JButton browse = new JButton("...");
            browse.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    browseForSbtLauncherJar();
                }
            });

            ideSettings.add(label, "wrap");
            ideSettings.add(sbtLauncherJarPath, "growx");
            ideSettings.add(browse, "wrap");
        }
        {
            JLabel label = new JLabel("VM parameters");
            label.setDisplayedMnemonic('V');
            label.setLabelFor(vmParameters);
            ideSettings.add(label, "wrap");
            ideSettings.add(vmParameters, "growx");
        }

        root = new JPanel(new MigLayout("wrap 1", "[grow]"));
        root.add(ideSettings, "grow");
    }

    private void browseForSbtLauncherJar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory() ||
                        file.getName().toLowerCase().endsWith(".jar");
            }

            public String getDescription() {
                return "JAR files (*.jar)";
            }
        });

        File oldValue = new File(sbtLauncherJarPath.getText());
        chooser.setCurrentDirectory(oldValue);
        chooser.setSelectedFile(oldValue);

        int result = chooser.showOpenDialog(root);
        if (result == JFileChooser.APPROVE_OPTION) {
            sbtLauncherJarPath.setText(chooser.getSelectedFile().getAbsolutePath());
        }
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
        applicationSettings.setSbtLauncherJarPath(sbtLauncherJarPath.getText());
        applicationSettings.setSbtLauncherVmParameters(vmParameters.getText());
    }

    public void copyFrom(SbtProjectSettings projectSettings, SbtApplicationSettings applicationSettings) {
        sbtLauncherJarPath.setText(new File(applicationSettings.getSbtLauncherJarPath()).getAbsolutePath());
        vmParameters.setText(applicationSettings.getSbtLauncherVmParameters());
    }

    public static void main(String[] args) {
        SbtSettingsForm form = new SbtSettingsForm();
        form.copyFrom(new SbtProjectSettings(), new SbtApplicationSettings());

        JFrame frame = new JFrame("Test: SbtSettingsForm");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(form.createComponent());
        frame.setSize(600, 600);
        frame.setLocation(500, 300);
        frame.setVisible(true);
    }
}
