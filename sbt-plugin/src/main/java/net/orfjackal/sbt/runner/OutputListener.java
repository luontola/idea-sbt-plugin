// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.runner;

public interface OutputListener {

    OutputListener NULL_LISTENER = new OutputListener() {
        public void append(char c) {
        }
    };

    void append(char c);
}
