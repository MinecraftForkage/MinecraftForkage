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

6. If patching fails, run `make clean` and retry. (This is probably related to
   nondeterminism in FernFlower.)

7. Refresh the MinecraftForkage project in Eclipse.


External libraries for both Minecraft and Forge will be downloaded to `libraries/` when you run
any makefile target (TODO: make it an actual target, instead of running automatically). The folder
structure is not preserved - all the library JARs are simply dumped into that directory.
Natives will be extracted to `libraries/natives/`.

TODO: Write something here about how the decompilation/patching/install process works internally.
(Or not here, but somewhere)

As long as this project is taking upstream patches from Minecraft Forge, we need to use their
source-patching process. If Minecraft Forkage ever becomes independent from Forge, then it may
be preferable to switch to a bytecode-patching process for simplicity, as source-patching is
heavily dependent on the decompilation process.

It would also be convenient to distribute the server and client a single JARs, if possible.
Downloading stuff at runtime is a bad idea. (Downloading stuff at install time is a bit better)