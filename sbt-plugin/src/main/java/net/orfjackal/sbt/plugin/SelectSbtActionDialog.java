// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

public class SelectSbtActionDialog extends DialogWrapper {

    private static final String[] SBT_ACTIONS = new String[]{
            "clean",
            "clean-cache",
            "clean-lib",
            "compile",
            "doc",
            "doc-test",
            "doc-all",
            "graph-src",
            "graph-pkg",
            "jetty-run",
            "jetty-stop",
            "package",
            "package-test",
            "package-docs",
            "package-all",
            "package-project",
            "package-src",
            "package-test-src",
            "run <argument>*",
            "reload",
            "test",
            "test-failed <test>*",
            "test-quick <test>*",
            "test-only <test>*",
            "test-compile",
            "test-run <argument>*",
            "update",
    };

    private String selectedAction;
    private JComboBox actionField;

    public SelectSbtActionDialog(Project project, String selectedAction) {
        super(project, false);
        this.selectedAction = selectedAction;

        setTitle(MessageBundle.message("sbt.tasks.select.action.title"));
        init();
    }

    public String getSelectedAction() {
        return selectedAction;
    }

    protected JComponent createCenterPanel() {
        actionField = new JComboBox(SBT_ACTIONS);
        actionField.setEditable(true);
        actionField.setSelectedItem(selectedAction);

        JPanel root = new JPanel(new MigLayout());
        root.add(actionField, "width 200::");
        return root;
    }

    protected void doOKAction() {
        super.doOKAction();
        this.selectedAction = nullIfEmpty((String) actionField.getSelectedItem());
    }

    private static String nullIfEmpty(String s) {
        s = s.trim();
        return s.equals("") ? null : s;
    }
}
