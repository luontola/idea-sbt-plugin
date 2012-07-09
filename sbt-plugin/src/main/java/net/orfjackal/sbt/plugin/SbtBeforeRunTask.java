// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.execution.BeforeRunTask;
import com.intellij.openapi.util.Key;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class SbtBeforeRunTask extends BeforeRunTask<SbtBeforeRunTask> {

    public SbtBeforeRunTask(@NotNull Key<SbtBeforeRunTask> providerId) {
        super(providerId);
        runInCurrentModule = true;
        action = "test:products";
    }

    private String action;
    private boolean runInCurrentModule;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public boolean isRunInCurrentModule() {
        return runInCurrentModule;
    }

    public void setRunInCurrentModule(boolean runInCurrentModule) {
        this.runInCurrentModule = runInCurrentModule;
    }

    @Override
    public void writeExternal(Element element) {
        super.writeExternal(element);
        if (action != null) {
            element.setAttribute("action", action);
        }
        element.setAttribute("runInCurrentModule", Boolean.toString(runInCurrentModule));
    }

    @Override
    public void readExternal(Element element) {
        super.readExternal(element);
        action = element.getAttributeValue("action");
        String runInCurrentModuleText = element.getAttributeValue("runInCurrentModule");
        try {
            runInCurrentModule = Boolean.parseBoolean(runInCurrentModuleText);
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SbtBeforeRunTask that = (SbtBeforeRunTask) o;

        if (runInCurrentModule != that.runInCurrentModule) return false;
        if (action != null ? !action.equals(that.action) : that.action != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (action != null ? action.hashCode() : 0);
        result = 31 * result + (runInCurrentModule ? 1 : 0);
        return result;
    }
}
