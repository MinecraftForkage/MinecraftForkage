To set up a workspace for developing Minecraft Forkage:

* Clone this repository.
* `git submodule init`
* `git submodule update`
* Open the included Eclipse workspace (`eclipse/`). Clean and refresh the "BuildTools"
  and "fernflower" projects, if necessary, so that they build. (TODO: make them buildable
  by makefile)
* `make extractsrc`
* If patching fails, run `make clean` and retry. This is probably related to
  nondeterminism in FernFlower.
* Refresh the MinecraftForkage project in Eclipse.

If you are on Windows, we recommend running the shell scripts under Cygwin.
MSYS may also work, but has not been tested by the Minecraft Forkage team.
If you find any compatibility issues with shell scripts, we will gladly accept pull requests.

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