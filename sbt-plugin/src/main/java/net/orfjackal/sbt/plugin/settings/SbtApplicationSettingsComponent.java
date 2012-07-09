// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin.settings;

import com.intellij.openapi.components.*;
import org.jetbrains.annotations.NotNull;

@State(name = "SbtSettings", storages = {@Storage(id = "default", file = "$APP_CONFIG$/other.xml")})
public class SbtApplicationSettingsComponent implements PersistentStateComponent<SbtApplicationSettings>, ApplicationComponent {

    private SbtApplicationSettings applicationSettings = new SbtApplicationSettings();

    public SbtApplicationSettings getState() {
        return applicationSettings;
    }

    public void loadState(SbtApplicationSettings state) {
        applicationSettings = state;
    }

    public void initComponent() {
    }

    public void disposeComponent() {
    }

    @NotNull
    public String getComponentName() {
        return "SbtSettings";
    }
}
