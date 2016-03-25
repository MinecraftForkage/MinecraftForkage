Components
==========

Minecraft Forkage consists of several components:

* The **core** is a modified version of Minecraft.
    * The core has a complicated build process in order to work around not being allowed to distribute Minecraft;
        several miscellaneous **build tools** are required to build it.
    * We can't distribute the source code for the core, so instead we distribute a deterministic decompiler, and **patches**
        to its output.
    * We also can't distribute the binary for the core, so instead the core's build process generates an **installer**
        which re-generates it at runtime, based on a bytecode patch. 
* The **packer** takes the core (either one you built from this repository, or one re-generated by the installer)
    and a list of mods, and outputs a **modpack** JAR.
* Modpack JARs are modified versions of the game, which *should* (barring bugs) be completely standalone.
    * They cannot be distributed either. Instead, modpack creators are recommended to distribute packer configuration files.

Core build tools
================

This component resides in the `core-BuildTools/` directory.

It can be built by running `ant` in that directory, which will yield the file `core-BuildTools/build/BuildTools.jar`.

As a convenience for editing, the directory `core-BuildTools/` may be imported as a project into Eclipse. It should require no
further setup.

Note: the Eclipse project references a bundled copy of Ant 1.9.4, but the Ant build script will use your installed version of Ant.



Core
====

This component resides in the `core/` directory. This directory may be imported into Eclipse as a project.

The build process is complex by necessity, since we are not allowed to distribute the original or modified Minecraft code --
only patches to it. This means we may not distribute the full source code for the core (although it can be generated) nor the
full binary.

To generate the source code, run `ant extractsrc` in the `core/` directory.
You need an Internet connection to run this step for the first time, as it downloads Minecraft and several libraries.

To generate the binary, run `ant recompile` in the `core/` directory.

To generate the installer, run `ant make-installer` in the `core/` directory. The installer contains only bytecode patches to
Minecraft, as well as new code, and can be distributed. End-users can run the installer to re-generate the binary.

Note: the binary produced by the installer is not expected to be byte-wise identical to the binary produced by "ant recompile".

Note: the core is compiled as Java 1.6.

Note: any modifications you make to files in the vanilla-src *will be lost* next time they are regenerated from patches (with
`ant extractsrc`)




Packer
======

This component resides in the `packer/` directory. This directory may be imported into Eclipse as a project.

The packer relies on some of the libraries downloaded by running `ant extractsrc` in `core/`. This may be fixed in the future.
For now, you need to do that if you want to be able to compile the packer.

To build the packer, run `ant` in the `packer/` directory, or use your favourite IDE.



Libraries
=========

External libraries for both Minecraft and Forge will be downloaded to `core/libraries/` when you run `ant extractsrc`.
The folder structure is not preserved - all the library JARs are simply dumped into that directory.
Natives will be extracted to `core/libraries/natives/`.


User-friendliness layer
=======================

Users want Forkage modpacks to just work. Telling users to mess around with command lines or directory structures is a good way to not have any users :)

Users can either generate a *standalone modpack JAR*, or use the vanilla launcher.

## Standalone JARs

Supplying the --standalone, --libraryDir and --nativesDir options to the command-line packer will cause it to emit a
standalone JAR file. Note that many mods rely on the current working directory being the instance directory.

Launching the standalone JAR is platform-specific; on Windows, it must be placed in the instance directory and then
double-clicked, or a shortcut can be made to it.

Note that standalone JARs contain Mojang code, and may contain open-source code in a manner not compliant with its license (as the licenses have not been reviewed for this purpose); therefore, they should not be 

TODO: improve the login interface, and make it save sessions/profiles in a way compatible with the vanilla launcher.

For now, it does not download assets, so the vanilla launcher must have been used at least once for this Minecraft version.

For now, it does not download libraries. This is a problem - the packer must have the libraries all placed in one directory, and natives extracted, and it requires those two directories to be specified on the command line. None of this is user-friendly.

## Vanilla launcher

When using the vanilla launcher, the user should be able to set up a profile by selecting the Forkage version as the Minecraft
version, and then selecting the instance directory - exactly the same as for Forge instances.

To accomplish this, the *packer* is registered with the launcher. When the user launches Minecraft, the installer will
quickly check if any mods have changed. If so, it will re-generate the modpack JAR. Then, it will launch the modpack JAR.
The installer will be registered with all of the required libraries and assets, so that the launcher will handle downloading them.

