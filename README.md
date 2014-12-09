== Workspace setup ==

To set up a workspace for developing Minecraft Forkage:

0. If you're on Windows, install Cygwin.

1. Clone this repository.

2. Run `git submodule update --init --recursive`.

  If the above command fails with an error similar to:

	fatal: Not a git repository: ../../../..//c/mcf-test/.git/modules/BuildTools/modules/BON
	Failed to recurse into submodule path 'BuildTools'

  then update Git (or Git For Windows), delete everything, and
  start again from step 1.

3. Unzip `eclipse-workspace.zip`. This should create `eclipse/` in the  repository root.
   If you unzipped to a subdirectory by mistake, then move the `eclipse` directory to the
   repository root.

4. Open the Eclipse workspace you just unzipped (in `eclipse/`). Open the "BuildTools"
   and "fernflower" projects, if necessary, so that they build. (TODO: make them buildable
   by makefile)

5. Run `make extractsrc` and wait. You need an Internet connection for this step.
   If you are on Windows, you should run this in Cygwin.
   MSYS may also work, but has not been tested by the Minecraft Forkage team.

6. If patching fails, run `make clean` and try again. This is probably related to
   nondeterminism in FernFlower. You might need to try a few times.

7. Open the MinecraftForkage project in Eclipse.

8. Once Eclipse finishes building the workspace, you are now set up.
   There are predefined "MCF Client" and "MCF Server" run configurations. Note that they
   will not be in the drop-down menu - you will need to select them in the run configurations
   dialog the first time you use them.

== Libraries ==

External libraries for both Minecraft and Forge will be downloaded to `libraries/` when you run
any makefile target (TODO: make it an actual target, instead of running automatically). The folder
structure is not preserved - all the library JARs are simply dumped into that directory.
Natives will be extracted to `libraries/natives/`.
