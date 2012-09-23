// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class SelectSbtActionDialog extends DialogWrapper {

    private static final String[] SBT_10_ACTIONS = new String[]{
            "compile",
            "test:compile",
            "products",
            "test:products"
    };

    private String selectedAction;
    private JComboBox actionField;
    private boolean runInCurrentModule;
    private JCheckBox runInCurrentModuleField;

    public SelectSbtActionDialog(Project project, String selectedAction, boolean runInCurrentModule) {
        super(project, false);
        this.selectedAction = selectedAction;
        this.runInCurrentModule = runInCurrentModule;

        setTitle(MessageBundle.message("sbt.tasks.select.action.title"));
        init();
    }

    public boolean isRunInCurrentModule() {
        return runInCurrentModule;
    }

    public String getSelectedAction() {
        return selectedAction;
    }

    protected JComponent createCenterPanel() {
        runInCurrentModuleField = new JCheckBox();
        runInCurrentModuleField.setSelected(runInCurrentModule);
        JLabel runInCurrentModuleLabel = new JLabel(MessageBundle.message("sbt.tasks.select.action.run.current"));
        runInCurrentModuleLabel.setToolTipText(MessageBundle.message("sbt.tasks.select.action.run.current.tooltip"));

        actionField = new JComboBox(SBT_10_ACTIONS);
        actionField.setEditable(true);
        actionField.setSelectedItem(selectedAction);

        JPanel root = new JPanel(new MigLayout());
        root.add(runInCurrentModuleField, "");
        root.add(runInCurrentModuleLabel, "wrap");
        root.add(actionField, "width 200::, spanx 2");
        return root;
    }

    protected void doOKAction() {
        super.doOKAction();
        this.selectedAction = nullIfEmpty((String) actionField.getSelectedItem());
        this.runInCurrentModule = runInCurrentModuleField.isSelected();
    }

    private static String nullIfEmpty(String s) {
        s = s.trim();
        return s.equals("") ? null : s;
    }
}
