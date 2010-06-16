Go to the "Before launch" options of a Run Configuration, uncheck
"Make" and choose "Run SBT Action / test-compile" to compile the
project with SBT.

This plugin does not have integration with SBT's dependency management
(nor is it planned), because it's possible to use Maven for that. If
you declare the dependencies using Maven, then both IDEA and SBT will
find them.
