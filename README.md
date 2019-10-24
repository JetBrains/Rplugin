R Language support for Intellij IDEA
====================================

With the plugin for the [R language](https://www.r-project.org) you
can perform various statistical computing and enjoy your favorite
features of the integrated development environment:

- Coding assistance
    - Error and syntax highlighting
    - Code completion
    - Intention actions and quick fixes

-  Smart editing and auto-saving changes in your R files. Supported formats:
    -   R Script
    -   R Markdown

-   Previewing data in the graphic and tabular forms:
    -   R Graphics viewer
    -   Table View
    -   R HTML viewer
    -   R Markdown preview

-   Running and debugging R scripts with the live variables view.
-   Managing R packages; ability to create your own R packages.

For more details, see [PyCharm web help](https://www.jetbrains.com/help/pycharm/2019.3/r-plugin-support.html)

This plugin comes with ABSOLUTELY NO WARRANTY.

This is free software, and you are welcome to redistribute it under certain conditions.


Developer Info
--------------

For building the plugin clone [Rkernel-proto](https://github.com/JetBrains/Rkernel-proto) into `protos` directory in the project root.

Use gradle wrapper for:

- running the plugin: `./gradlew :runIde`
- testing the plugin: `./gradlew :test`
- building the plugin: `./gradlew :buildPlugin`

The plugin can interact with [Rkernel](https://github.com/JetBrains/Rkernel).

To test/run/build the plugin with a specific version of Rkernel put its `rwrapper` and `R files` into `rwrapper` directory in the project root.

Acknowledgements
----------------

This project is based on [R4Intellij](http://holgerbrandl.github.io/r4intellij/). 
The files containing `Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova` share the source code with R4Intellij.
