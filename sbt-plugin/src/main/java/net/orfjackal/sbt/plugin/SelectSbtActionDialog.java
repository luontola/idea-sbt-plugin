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

    private static final String[] SBT_07_ACTIONS = new String[]{
            "compile",
            "test-compile",
            ";compile;copy-resources",
            ";test-compile;copy-test-resources",
    };
    private static final String[] SBT_10_ACTIONS = new String[]{
            "compile",
            "test:compile",
            "products",
            "test:products"
    };
    public static final String[] SBT_ACTIONS_WITH_SEPARATOR = combineActions();
    private static final String SBT_07_HEADER = " SBT 0.7.x";
    private static final String SBT_10_HEADER = " SBT 0.10.x";
    private static final String SEPARATOR = "---";

    private static String[] combineActions() {
        List<String> buffer = new ArrayList<String>();
        buffer.addAll(Arrays.asList(SBT_10_ACTIONS));
        return buffer.toArray(new String[buffer.size()]);
    }

    public static final List<String> NON_SELECTABLE_ITEMS = Arrays.asList(SBT_07_HEADER, SBT_10_HEADER, SEPARATOR);

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

        actionField = new JComboBox(SBT_ACTIONS_WITH_SEPARATOR);
        actionField.setEditable(true);
        actionField.setSelectedItem(selectedAction);
        actionField.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList jList, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value.equals(SEPARATOR)) {
                    return new JSeparator(JSeparator.HORIZONTAL);
                }
                if (value.equals(SBT_07_HEADER) || value.equals(SBT_10_HEADER)) {
                    setEnabled(false);
                    setText("<html><b>" + value + "</b></html>");
                    return this;
                }
                return super.getListCellRendererComponent(jList, value, index, isSelected, cellHasFocus);
            }
        });
        // TODO Make the headings and separator non-selectable.

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
