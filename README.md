# Processing Mode Template
This is an example setup to create a new mode in [Processing 3](https://processing.org/). It extends Processing's default Java mode, though you can easily remove the Java mode dependencies to create a mode for a different language.

This template has been tested with Processing 4.0a6, 3.5.4, 3.0, and 3.0a11. 3.0a10 and previous versions will not load the template.

## Credits
The mode templates for previous versions created by [Martin Leopold](https://github.com/martinleopold/TemplateMode) and [Joel Moniz](https://github.com/joelmoniz/TemplateMode/tree/3.0-compatibility) provided a guide for this template.

This repository is a GitHub template and has some unnecessary files removed. It also works on newer Processing versions.

## Setup Instructions
### Prerequisites
* Clone the [3.x Processing source repository](https://github.com/processing/processing) and **build it at least once**.
* Install Apache Ant if your IDE does not already include it.

### Setting the Build Properties
There are a number of build properties you can set in [build.properties](build.properties), though you likely do not need to change most of them:
* `lib.name` - the name of your mode. **This must end in "Mode" and match the name of your mode class.**
* `release` - the "pretty version" of your mode
* `java.target.version` - the target Java version. 1.8 is Java 8.
* `epoch.now` - the current epoch time in milliseconds. This is overwritten by the build script with the actual time.
* `processing.source` - location of the top-level folder of the Processing source
* `processing.core` - location of core .jar files in the Processing source
* `processing.app` - location of PDE .jar files in the Processing source
* `processing.executable` - location of your Processing executable used by the `run` build task
* `processing.modes` - installation location for Processing modes used by the `install` build task

Descriptions of Java mode-specific properties are in the [Java mode build.properties](#buildproperties) section.

### Creating a Mode Class
The name of your mode class must match `lib.name` in the build properties. You can either extend an existing mode or extend the abstract `processing.app.Mode` class.

### Adding Mode Details
The [mode.properties](resources/mode.properties) file contains metadata about your mode, some of which is visible in the mode installer:
* `name` - display name for your mode
* `authors` - a comma-separated list of authors in Markdown link format, such as `[soir20](https://github.com/soir20)`
* `url` - URL leading to a website for your mode, probably its GitHub repository
* `sentence` - a short description of your mode
* `paragraph` - a long description of your mode
* `version` - an integer representing the current version of your mode. This template populates this field automatically.
* `prettyVersion` - the display version of your mode. This template replaces this field with the value of `release` in build.properties automatically.
* `lastUpdated` - the epoch time in milliseconds when you last updated your mode. This template populates this field automatically.
* `minRevision` - minimum Processing [revision](https://raw.githubusercontent.com/processing/processing/master/build/shared/revisions.txt) that this mode will work on. `0238` is [Processing 3.0a11](https://github.com/processing/processing/releases/tag/processing-0238-3.0a11).
* `maxRevision` - maximum Processing [revision](https://raw.githubusercontent.com/processing/processing/master/build/shared/revisions.txt) that this mode will work on. Unless you know of a specific version that breaks this mode, set this to `0` to indicate no upper bound.
* `imports` - libraries or other modes to import

A [list of the approved Processing modes, tools, and libraries](http://download.processing.org/contribs) contains more properties. Some of these do not need to be put in your mode's source code. (For example, if your mode isn't approved, the `download` property does nothing.)

The Processing website has [an outdated list (for Processing 2)](https://processing.org/books/gswp_2e.txt) of these properties. It also has a [script that generates the build listing](https://processing.org/contrib_generate/build_listing.py).

### Running and Building Your Mode
There are several Ant tasks for building and running your mode:
* `build` - builds your mode and puts the output in the "dist" folder
* `install` - builds your mode and places the output in your modes directory 
* `run` - builds and installs your mode and then starts Processing
* `clean` - deletes the "build" and "dist" folders to clear all build output

### IntelliJ Setup
IntelliJ requires some additional setup to recognize the imports from the Processing source code correctly in its editor.

You need to add the `app` and `core` folders in the Processing source as modules for your mode project. If your mode is a Java mode, you also need to add the `java` folder as a module.
* File > New > **Module** from Existing Sources
* Select the folder you want to add from the Processing source (`app`, `core`, or `java`).
* Select "Create module from existing sources."
* Keep pressing "Next" while IntelliJ lists the libraries that will be imported.
* "Finish."
* Follow these steps again for the other folders.

If you already have the Processing source code set up for IntelliJ with modules, you can use the existing ones instead of overwriting them.

Finally, add the new modules as dependencies to your mode's module. You can do this by hovering over code marked as an error in IntelliJ and selecting "Add module as dependency." You can also add the dependencies manually:
* File > Project Structure > Modules
* Select your mode's module.
* Press the plus sign button (alt + insert) to add modules from Processing as dependencies.

You also need to add build.xml as an Ant build file:
* View > Tool Windows > Ant
* "Add Ant build file" in the tool window that opens.
* Choose "[build.xml](build.xml)" in the root of this repository.

## Converting This Mode to a Non-Java Mode
The template mode in this repository extends Java mode. In other words, it functions identically to Java mode, but you could add more features to Java mode if you wanted.

If you don't want to extend Java mode, there are only a few changes that you need to make. Java mode-specific snippets have a comment above them starting with `JAVA MODE:`.

### Your Mode Class ([ProcessingMode.java](src/io/github/soir20/mode/processingmode/ProcessingMode.java))
To make a general mode, this class should extend `processing.app.Mode` and implement its methods instead of extending `processing.mode.java.JavaMode`. You also don't need to use Java's class loader. Unless you need it, you can remove the `getClassLoader()` method entirely.

### [build.xml](build.xml)
There are two snippets marked with `JAVA MODE:` in this file.

The first includes Java mode's dependencies in the classpath.

The second copies resource files (such as icons and keywords) into your build. Processing won't fall back to Java mode's theme, so it won't be able to find those files if you don't copy them.

Remove both of these snippets if you don't want to extend Java mode.

### [build.properties](build.properties)
You can define build properties directly in build.xml if you wish, but this file separates them to make them clearer and easier to find.

There are several properties in this file that you can remove if you are not using Java mode:
* `processing.java` - the location of Java mode in the Processing source code
* `processing.java.mode` - the location of the Java mode .jar files in the Processing source code
* `processing.java.theme` - the location of the theme icons for Java mode. Ant copies this directory into your mode when it is built.
* `processing.java.keywords` - the location of the keywords.txt file for Java mode. Ant copies this file into your mode when it is built.
* `processing.java.suggestions` - the location of the suggestions.txt file for Java mode. Ant copies this file into your mode when it is built.

### [mode.properties](resources/mode.properties)
The `imports=` line is necessary to make this mode depend on Java mode. Remove this if you don't want to extend Java mode.