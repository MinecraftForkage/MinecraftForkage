Workspace setup
===============

To set up a workspace for developing Minecraft Forkage:

0. If you're on Windows, install Cygwin.

1. Clone this repository.

2. Run `git submodule update --init --recursive`.

  If the above command fails with an error similar to:

	fatal: Not a git repository: ../../../..//c/mcf-test/.git/modules/BuildTools/modules/BON
	Failed to recurse into submodule path 'BuildTools'

  then update Git (or Git For Windows), delete everything, and
  start again from step 1.

3. Run `ant extractsrc` and wait. You can set up the Eclipse workspace while this completes.
  You need an Internet connection for this step.

4. Unzip `eclipse-workspace.zip`. This should create `eclipse/` in the  repository root.
   If you unzipped to a subdirectory by mistake, then move the `eclipse` directory to the
   repository root.

5. Open the Eclipse workspace you just unzipped (in `eclipse/`).
  Note: If `ant` is still running, and you open the MinecraftForkage project, you might need
  to close and reopen the project in Eclipse after it finishes in order for Eclipse to detect the source files.

6. You are now set up to develop in Eclipse.
   There are predefined "MCF Client" and "MCF Server" run configurations. Note that they
   will not be in the drop-down menu - you will need to select them in the run configurations
   dialog the first time you use them.



Libraries
=========

External libraries for both Minecraft and Forge will be downloaded to `libraries/` when you run
any makefile target (TODO: make it an actual target, instead of running automatically). The folder
structure is not preserved - all the library JARs are simply dumped into that directory.
Natives will be extracted to `libraries/natives/`.


Installer
=========

To generate the installer, run `make build/installer.jar` after letting Eclipse compile the main
Minecraft Forkage project.