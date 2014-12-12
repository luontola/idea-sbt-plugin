// Copyright © 2010-2014, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.openapi.actionSystem.ActionPromoter;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author pfnguyen
 */
public class SbtActionPromoter implements ActionPromoter {
    public List<AnAction> promote(List<AnAction> actions, DataContext context) {
        for (AnAction action : actions) {

            if (action instanceof SbtConsoleExecuteAction) {
                List<AnAction> promoted = new ArrayList<AnAction>(1);
                promoted.add(action);
                return promoted;
            }
        }

        return Collections.emptyList();
    }
}
