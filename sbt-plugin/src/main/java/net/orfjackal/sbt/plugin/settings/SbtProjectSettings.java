// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin.settings;

import org.apache.commons.lang.builder.EqualsBuilder;

public class SbtProjectSettings {

    private boolean useSbtOutputDirs = true;

    public boolean isUseSbtOutputDirs() {
        return useSbtOutputDirs;
    }

    public void setUseSbtOutputDirs(boolean useSbtOutputDirs) {
        this.useSbtOutputDirs = useSbtOutputDirs;
    }

    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}
