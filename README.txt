Go to the "Before launch" options of a Run Configuration, uncheck
"Make" and choose "Run SBT Action / test-compile" to compile the
project with SBT. Or then use the SBT Console tool window to enter SBT
commands directly.

This plugin does not generate or synchronize your IDEA project
structure from the SBT build configuration. The sbt-idea-plugin
(http://github.com/mpeltonen/sbt-idea-plugin), a plugin for SBT,
rather than for IDEA, serves this purpose. These two projects are
complementary, and do not conflict with each other.

Alternatively you can rely on IDEA's Maven integration, so that both
SBT and IDEA find the dependencies from Maven's POM files. But beware
that SBT/Ivy does not support the relativePath element for parent POMs
(https://issues.apache.org/jira/browse/IVY-1173), which can cause
issues with multi-module projects.
