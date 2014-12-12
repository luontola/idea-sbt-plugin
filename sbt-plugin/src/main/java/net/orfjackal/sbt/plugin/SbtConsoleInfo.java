// Copyright © 2010-2014, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.execution.console.LanguageConsoleImpl;
import com.intellij.execution.process.ConsoleHistoryModel;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.WeakHashMap;

import java.util.ArrayList;
import java.util.List;

/**
 * @author pfnguyen
 */
public class SbtConsoleInfo {
    private static WeakHashMap<Project,List<ConsoleInfo>> allConsoles =
            new WeakHashMap<Project,List<ConsoleInfo>>();

    public static void addConsole(LanguageConsoleImpl console, ConsoleHistoryModel model, ProcessHandler handler) {
        Project p = console.getProject();
        List<ConsoleInfo> consoles = allConsoles.get(p);
        if (consoles == null) {
            consoles = new ArrayList<ConsoleInfo>();
            allConsoles.put(p, consoles);
        }
        consoles.add(new ConsoleInfo(console, model, handler));
    }

    public static void disposeConsole(LanguageConsoleImpl console) {
        Project p = console.getProject();
        List<ConsoleInfo> consoles = allConsoles.get(p);
        if (consoles != null) {
            List<ConsoleInfo> copy = new ArrayList<ConsoleInfo>(consoles.size());
            for (ConsoleInfo info : consoles) {
                if (info.console != console)
                    copy.add(info);
            }
            allConsoles.put(p, copy);
        }
    }

    public static LanguageConsoleImpl getConsole(Editor e) {
        Project p = e.getProject();
        List<ConsoleInfo> consoles = allConsoles.get(p);
        if (consoles != null) {
            for (ConsoleInfo info : consoles) {
                if (info.console.getConsoleEditor() == e)
                    return info.console;
            }
        }
        return null;
    }
    public static ProcessHandler getHandler(Editor e) {
        Project p = e.getProject();
        List<ConsoleInfo> consoles = allConsoles.get(p);
        if (consoles != null) {
            for (ConsoleInfo info : consoles) {
                if (info.console.getConsoleEditor() == e)
                    return info.handler;
            }
        }
        return null;
    }

    public static ConsoleHistoryModel getModel(Editor e) {
        Project p = e.getProject();
        List<ConsoleInfo> consoles = allConsoles.get(p);
        if (consoles != null) {
            for (ConsoleInfo info : consoles) {
                if (info.console.getConsoleEditor() == e)
                    return info.model;
            }
        }
        return null;
    }

    private static class ConsoleInfo {
        public final LanguageConsoleImpl console;
        public final ProcessHandler handler;
        public final ConsoleHistoryModel model;

        public ConsoleInfo(LanguageConsoleImpl console, ConsoleHistoryModel model, ProcessHandler handler) {
            this.console = console;
            this.handler = handler;
            this.model = model;
        }
    }
}
