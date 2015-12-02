## idea-sbt-plugin

Integrates Intellij IDEA with [SBT](scala-sbt.org) to build Scala projects easily and quickly.

### Documentation

  [User Guide](https://github.com/orfjackal/idea-sbt-plugin/wiki)

### Obtaining

The plugin is distributed through the Plugin Manager in IntelliJ.

### Problem?

Please reports issues to the GitHub issue tracker

### Related Projects

 - [sbt-idea](https://github.com/mpeltonen/sbt-idea) An SBT plugin for generating IntelliJ projects from your SBT build
 - [intellij-structure](https://github.com/JetBrains/sbt-structure) Developed by JetBrains, a plugin that might eventually take over from sbt-idea AND idea-sbt-plugin.

### Building

To build the project:

    % mvn -Didea.home="/Applications/IntelliJ Idea 15.app/Contents" install
    % ls -la sbt-dist/target/idea-sbt-plugin-*.zip  # File, Settings, Plugins, Install from Disk

You can also open this project in IntelliJ. Point the Project SDK to an IntelliJ Plugin SDK,
which you can setup easily by pointing at your IntelliJ installation.

You can the use the Run Configuration "plugin" to spawn a child IntelliJ process
with your modified version of the plugin.
