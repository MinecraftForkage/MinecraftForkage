MCVERSION=1.7.10
MCFVERSION=1.0

VERSION_NAME_IN_LAUNCHER=MCForkage-$(MCVERSION)-$(MCFVERSION)

FML_PATCHES=$(shell find FML/patches -type f)
FORGE_PATCHES=$(shell find MinecraftForge/patches -type f)

FERNFLOWER_JAR=build/download/fernflower.jar
FFTEMP=build/fftemp

FMLAT=FML/src/main/resources/fml_at.cfg
FORGEAT=MinecraftForge/src/main/resources/forge_at.cfg
EXCEPTOR_JSON=FML/conf/exceptor.json
EXC=FML/conf/joined.exc
SRG=FML/conf/joined.srg
ASTYLE_CFG=FML/conf/astyle.cfg
LIBRARY_JSON=FML/jsons/$(MCVERSION)-dev.json
MCP_MERGE_CFG=FML/mcp_merge.cfg

LIBRARIES_DIR=libraries
NATIVES_DIR=libraries/natives

TEMPDIR=build/temp

noop=
space=$(noop) $(noop)

SOURCE_DIRS=FML/src/main/java FML/src/main/resources MinecraftForge/src/main/java MinecraftForge/src/main/resources FML/vanilla-src
DEOBF_DATA=FML/src/main/resources/deobfuscation_data-missing.lzma

BUILDTOOLS_BIN_DIR=eclipse/BuildTools/bin
BUILDTOOLS=java -cp $(BUILDTOOLS_BIN_DIR)

LIBRARIES := $(shell $(BUILDTOOLS) GetLibsFromJson "$(LIBRARY_JSON)" "$(LIBRARIES_DIR)" "$(NATIVES_DIR)")

ifeq ($(OSTYPE),cygwin)
	CLASSPATH_SEPARATOR := \;
else
	CLASSPATH_SEPARATOR := :
endif

CLASSPATH=$(subst $(space),$(CLASSPATH_SEPARATOR),$(LIBRARIES))

all: build/deobf_source.zip $(DEOBF_DATA)
.PHONY: all

extractsrc: build/deobf_source.zip $(DEOBF_DATA)
	[ -d FML/vanilla-src ] && rm -r FML/vanilla-src; exit 0
	unzip "build/deobf_source.zip" -d FML/vanilla-src
.PHONY: extractsrc

.DELETE_ON_ERROR:

$(shell mkdir -p build build/download)

clean:
	rm -f build/fernflower.log
	rm -f build/*.zip
	rm -f build/*.patch
	rm -f build/*.jar
	rm -f build/*.txt
	rm -f build/*.srg
	rm -f build/*.xz
	rm -f build/new-classes.pack
	rm -f build/install.json
	rm -f build/install.properties
	rm -f build/install-data.zip.lzma
.PHONY: clean
cleanlibs:
	rm -rf $(LIBRARIES_DIR)/*.jar
	rm -rf $(NATIVES_DIR)/*.dll
	rm -rf $(NATIVES_DIR)/*.so
	rm -rf $(NATIVES_DIR)/*.dylib
	rm -rf $(NATIVES_DIR)/*.jnilib
.PHONY: cleanlibs

############################### DOWNLOADS ###################################

build/download/minecraft.jar:
	wget -O "$@" "http://s3.amazonaws.com/Minecraft.Download/versions/$(MCVERSION)/$(MCVERSION).jar"

build/download/minecraft_server.jar:
	wget -O "$@" "http://s3.amazonaws.com/Minecraft.Download/versions/$(MCVERSION)/minecraft_server.$(MCVERSION).jar"

build/download/vanilla.json:
	wget -O "$@" "http://s3.amazonaws.com/Minecraft.Download/versions/$(MCVERSION)/$(MCVERSION).json"

####################### DECOMP BYTECODE PROCESSING ##########################

build/minecraft_merged.jar: build/download/minecraft.jar build/download/minecraft_server.jar
	$(BUILDTOOLS) bytecode.JarMerger "build/download/minecraft.jar" "build/download/minecraft_server.jar" "$@" $(MCP_MERGE_CFG)

build/minecraft_merged_remapped.jar: build/minecraft_merged.jar FML/conf/joined.srg
	$(BUILDTOOLS) bytecode.ApplySRG FML/conf/joined.srg "$<" "$@"

build/processed_binary.jar: build/minecraft_merged_remapped.jar $(FMLAT) $(FORGEAT) $(EXCEPTOR_JSON) $(EXC)
	cat "$<" | \
		$(BUILDTOOLS) bytecode.ApplyExceptorJson "$(EXCEPTOR_JSON)" | \
		$(BUILDTOOLS) bytecode.ApplyAT "$(FMLAT)" | \
		$(BUILDTOOLS) bytecode.ApplyAT "$(FORGEAT)" | \
		$(BUILDTOOLS) bytecode.ApplyExceptions "$(EXC)" | \
		$(BUILDTOOLS) bytecode.ApplyParamNames "$(EXC)" | \
		$(BUILDTOOLS) bytecode.AddOBFID "$(EXC)" | \
		$(BUILDTOOLS) bytecode.RemoveGenericMethods | \
	cat > "$@"

build/processed_binary_trimmed.jar: build/processed_binary.jar
	$(BUILDTOOLS) bytecode.TrimBytecode < "$<" > "$@"

build/processed_binary_trimmed_sorted.jar: build/processed_binary_trimmed.jar
	$(BUILDTOOLS) bytecode.SortZipEntries "$<" > "$@"

build/bytecode-orig.txt: build/processed_binary_trimmed_sorted.jar
	$(BUILDTOOLS) bytecode.Bytecode2Text < "$<" > "$@"

build/raw_source.zip: build/processed_binary.jar
	[ -d "$(FFTEMP)" ] && rm -r "$(FFTEMP)"; exit 0
	mkdir -p "$(FFTEMP)"
	java -Xmx512M -cp eclipse/fernflower/bin org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler -din=1 -rbr=0 -dgs=1 -asc=1 -log=ALL "$<" "$(FFTEMP)" #| tee "build/fernflower.log"
	mv "$(FFTEMP)/$(shell basename "$<")" "$@"
	rmdir "$(FFTEMP)"

######################## DECOMP SOURCE PROCESSING ###########################

build/cleaned_source.zip: build/raw_source.zip $(ASTYLE_CFG)
	$(BUILDTOOLS) MCPCleanup "$(ASTYLE_CFG)" < "$<" > "$@"
build/patched_source_fml.zip: build/cleaned_source.zip build/fml.patch
	$(BUILDTOOLS) ApplyUnifiedDiffs build/fml.patch < "$<" > "$@"
build/patched_source_forge.zip: build/patched_source_fml.zip build/forge.patch
	$(BUILDTOOLS) ApplyUnifiedDiffs build/forge.patch < "$<" > "$@"
build/processed_source.zip: build/patched_source_forge.zip
	cp "$<" "$@"

build/deobf_source.zip: build/processed_source.zip
	$(BUILDTOOLS) RemapSources FML/conf/methods.csv FML/conf/fields.csv FML/conf/params.csv true < "$<" > "$@"

build/fml.patch: $(FML_PATCHES)
	cat $(FML_PATCHES) > "$@"
build/forge.patch: $(FORGE_PATCHES)
	cat $(FORGE_PATCHES) > "$@"

$(DEOBF_DATA): FML/conf/joined.srg
	lzma < "$<" > "$@"


################### RECOMPILATION OF EDITED SOURCE ##########################

build/edited_source.zip: $(find $(SOURCE_DIRS) -type f)
	rm -f "$@"
	for srcdir in $(SOURCE_DIRS); do cd $$srcdir; zip -r "$$OLDPWD/$@" .; cd "$$OLDPWD"; done

build/recomp.jar: build/edited_source.zip $(LIBRARIES)
	$(BUILDTOOLS) CompileZip "$<" $(LIBRARIES_DIR) > "$@"

build/reobf.jar: build/recomp.jar build/mcp-to-srg.srg
	$(BUILDTOOLS) bytecode.ApplySRG build/mcp-to-srg.srg "$<" "$@"

build/srg-to-mcp.srg: $(SRG) FML/conf/methods.csv FML/conf/fields.csv
	$(BUILDTOOLS) CSV2SRG FML/conf/methods.csv FML/conf/fields.csv < "$<" > "$@"

build/mcp-to-srg.srg: build/srg-to-mcp.srg
	$(BUILDTOOLS) InvertSRG < "$<" > "$@"

################### INSTALL DATA FILE CREATION ##########################

build/reobf_trimmed.jar: build/reobf.jar
	$(BUILDTOOLS) bytecode.TrimBytecode < "$<" > "$@"

build/reobf_trimmed_sorted_filtered.jar: build/reobf_trimmed.jar build/processed_binary.jar
	$(BUILDTOOLS) bytecode.SortZipEntries "$<" build/processed_binary.jar > "$@"

build/bytecode-recomp.txt: build/reobf_trimmed_sorted_filtered.jar
	$(BUILDTOOLS) bytecode.Bytecode2Text < "$<" > "$@"

build/bytecode.patch: build/bytecode-orig.txt build/bytecode-recomp.txt
	diff -U0 build/bytecode-orig.txt build/bytecode-recomp.txt > "$@"; [ $$? -lt 2 ]

build/new-classes.zip: build/reobf.jar build/processed_binary.jar
	$(BUILDTOOLS) bytecode.SortZipEntries "$<" !build/processed_binary.jar > "$@"

build/new-classes.pack: build/new-classes.zip
	pack200 --no-gzip "$@" "$<"

build/install.properties:
	rm -f "$@"
	echo mcver=$(MCVERSION) >> "$@"
	echo mcfver=$(MCFVERSION) >> "$@"
	echo launcherVersionName=$(VERSION_NAME_IN_LAUNCHER) >> "$@"

build/install.json: build/download/vanilla.json $(LIBRARY_JSON)
	$(BUILDTOOLS) MakeInstallJSON build/download/vanilla.json $(LIBRARY_JSON) $(VERSION_NAME_IN_LAUNCHER) > "$@"

build/install-data.zip: build/new-classes.pack build/bytecode.patch $(SRG) $(EXCEPTOR_JSON) $(EXC) $(FMLAT) $(FORGEAT) build/install.properties $(MCP_MERGE_CFG) build/install.json
	rm -f "$@"
	zip -j0 "$@" $^

build/install-data.zip.lzma: build/install-data.zip
	lzma -9e < "$<" > "$@"

build/BuildTools.jar: $(find $(BUILDTOOLS_BIN_DIR) -type f)
	rm -f "$@"
	cd $(BUILDTOOLS_BIN_DIR); zip -r "$$OLDPWD/$@" .

build/installer.jar: build/BuildTools.jar build/install-data.zip.lzma
	cp "$<" "$@"
	zip "$@" build/install-data.zip.lzma
	cd $(BUILDTOOLS_BIN_DIR)/installer; zip "$$OLDPWD/$@" META-INF/MANIFEST.MF
