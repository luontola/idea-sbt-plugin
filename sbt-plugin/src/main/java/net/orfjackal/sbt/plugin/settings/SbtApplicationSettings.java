// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin.settings;

import net.orfjackal.sbt.plugin.IO;
import org.apache.commons.lang.builder.EqualsBuilder;

import java.io.File;

public class SbtApplicationSettings {
    private static final String DEFAULT_SBT_LAUNCHER = IO.canonicalPathTo(new File(System.getProperty("user.home"), "bin/sbt-launch.jar"));
    private static final String DEFAULT_SBT_VM_PARAMETERS = "-Xmx512M -XX:MaxPermSize=256M";

    private String sbtLauncherJarPath = DEFAULT_SBT_LAUNCHER;
    private String sbtLauncherVmParameters = DEFAULT_SBT_VM_PARAMETERS;
    private boolean useCustomJdk = false;
    private String jdkHome = null;

    public String getSbtLauncherJarPath() {
        return sbtLauncherJarPath;
    }

    public void setSbtLauncherJarPath(String sbtLauncherJarPath) {
        this.sbtLauncherJarPath = IO.canonicalPathTo(new File(sbtLauncherJarPath));
    }

    public String getSbtLauncherVmParameters() {
        return sbtLauncherVmParameters;
    }

    public void setSbtLauncherVmParameters(String sbtLauncherVmParameters) {
        this.sbtLauncherVmParameters = sbtLauncherVmParameters;
    }

    public boolean isUseCustomJdk() {
        return useCustomJdk;
    }

    public void setUseCustomJdk(boolean useCustomJdk) {
        this.useCustomJdk = useCustomJdk;
    }

    public String getJdkHome() {
        return jdkHome;
    }

    public void setJdkHome(String jdkHome) {
        this.jdkHome = jdkHome;
    }

    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}
