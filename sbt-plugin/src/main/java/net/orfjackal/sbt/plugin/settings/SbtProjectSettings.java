// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin.settings;

import org.apache.commons.lang.builder.EqualsBuilder;

public class SbtProjectSettings {
    private String sbtLauncherJarPath = "";
    private String sbtLauncherVmParameters = "";
    private boolean useApplicationSettings = true;

    public boolean isUseApplicationSettings() {
        return useApplicationSettings;
    }

    public void setUseApplicationSettings(boolean useApplicationSettings) {
        this.useApplicationSettings = useApplicationSettings;
    }

    public String getSbtLauncherJarPath() {
        return sbtLauncherJarPath;
    }

    public void setSbtLauncherJarPath(String sbtLauncherJarPath) {
        this.sbtLauncherJarPath = sbtLauncherJarPath;
    }

    public String getSbtLauncherVmParameters() {
        return sbtLauncherVmParameters;
    }

    public void setSbtLauncherVmParameters(String sbtLauncherVmParameters) {
        this.sbtLauncherVmParameters = sbtLauncherVmParameters;
    }

    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}
