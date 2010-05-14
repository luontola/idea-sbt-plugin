// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;

import javax.swing.*;
import java.awt.*;

public class SelectSbtActionDialog extends DialogWrapper {

    private String action = "test-compile"; // TODO: make configurable

    public SelectSbtActionDialog(Project project) {
        super(project, false);
    }

    protected JComponent createCenterPanel() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(new JLabel("SelectSbtActionDialog"), BorderLayout.CENTER);
        return root;
    }

    public String getAction() {
        return action;
    }
}
