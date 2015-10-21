MCVERSION=1.7.10
MCFVERSION=1.1

VERSION_NAME_IN_LAUNCHER=MCForkage-$(MCVERSION)-$(MCFVERSION)

FML_PATCHES=$(shell find patches/fml -type f)
FORGE_PATCHES=$(shell find patches/forge -type f)

FERNFLOWER_JAR=build/download/fernflower.jar
FFTEMP=build/fftemp

FMLAT=new-src/fml_at.cfg
FORGEAT=new-src/forge_at.cfg
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

SOURCE_DIRS=new-src vanilla-src
DEOBF_DATA=new-src/deobfuscation_data-missing.lzma

BUILDTOOLS_SRC_DIRS=BuildTools/src BuildTools/BON
BUILDTOOLS_BIN_DIR=build/BuildTools
BUILDTOOLS=java -cp $(BUILDTOOLS_BIN_DIR)
BUILDTOOLS_DEP=build/BuildTools.jar

LIBRARIES := $(shell $(BUILDTOOLS) decompsource.GetLibsFromJson "$(LIBRARY_JSON)" "$(LIBRARIES_DIR)" "$(NATIVES_DIR)" | tr '\r\n' '  ')

ifeq ($(OSTYPE),cygwin)
	CLASSPATH_SEPARATOR := \;
else
	CLASSPATH_SEPARATOR := :
endif

CLASSPATH=$(subst $(space),$(CLASSPATH_SEPARATOR),$(LIBRARIES))

all: build/deobf_source.zip $(DEOBF_DATA)
.PHONY: all

extractsrc: build/deobf_source.zip $(DEOBF_DATA)
	[ -d vanilla-src ] && rm -r vanilla-src; exit 0
	unzip "build/deobf_source.zip" -d vanilla-src
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
	rm -rf build/BuildTools
	rm -rf $(TEMPDIR)
.PHONY: clean
cleanlibs:
	rm -f $(LIBRARIES_DIR)/*.jar
	rm -f $(NATIVES_DIR)/*.dll
	rm -f $(NATIVES_DIR)/*.so
	rm -f $(NATIVES_DIR)/*.dylib
	rm -f $(NATIVES_DIR)/*.jnilib
.PHONY: cleanlibs

############################### DOWNLOADS ###################################

build/download/minecraft.jar:
	wget -O "$@" "http://s3.amazonaws.com/Minecraft.Download/versions/$(MCVERSION)/$(MCVERSION).jar"

build/download/minecraft_server.jar:
	wget -O "$@" "http://s3.amazonaws.com/Minecraft.Download/versions/$(MCVERSION)/minecraft_server.$(MCVERSION).jar"

build/download/vanilla.json:
	wget -O "$@" "http://s3.amazonaws.com/Minecraft.Download/versions/$(MCVERSION)/$(MCVERSION).json"

####################### DECOMP BYTECODE PROCESSING ##########################

build/minecraft_merged.jar: build/download/minecraft.jar build/download/minecraft_server.jar $(BUILDTOOLS_DEP)
	$(BUILDTOOLS) bytecode.JarMerger "build/download/minecraft.jar" "build/download/minecraft_server.jar" "$@" $(MCP_MERGE_CFG)

build/minecraft_merged_remapped.jar: build/minecraft_merged.jar FML/conf/joined.srg $(BUILDTOOLS_DEP)
	$(BUILDTOOLS) bytecode.ApplySRG FML/conf/joined.srg "$<" "$@"

build/processed_binary.jar: build/minecraft_merged_remapped.jar $(FMLAT) $(FORGEAT) $(EXCEPTOR_JSON) $(EXC) $(BUILDTOOLS_DEP)
	cat "$<" | \
		$(BUILDTOOLS) bytecode.ApplyExceptorJson "$(EXCEPTOR_JSON)" | \
		$(BUILDTOOLS) bytecode.ApplyAT "$(FMLAT)" | \
		$(BUILDTOOLS) bytecode.ApplyAT "$(FORGEAT)" | \
		$(BUILDTOOLS) bytecode.ApplyExceptions "$(EXC)" | \
		$(BUILDTOOLS) bytecode.ApplyParamNames "$(EXC)" | \
		$(BUILDTOOLS) bytecode.AddOBFID "$(EXC)" | \
		$(BUILDTOOLS) bytecode.RemoveGenericMethods | \
	cat > "$@"

build/processed_binary_trimmed.jar: build/processed_binary.jar $(BUILDTOOLS_DEP)
	$(BUILDTOOLS) bytecode.TrimBytecode < "$<" > "$@"

build/processed_binary_trimmed_sorted.jar: build/processed_binary_trimmed.jar $(BUILDTOOLS_DEP)
	$(BUILDTOOLS) bytecode.SortZipEntries "$<" > "$@"

build/bytecode-orig.txt: build/processed_binary_trimmed_sorted.jar $(BUILDTOOLS_DEP)
	$(BUILDTOOLS) bytecode.Bytecode2Text < "$<" > "$@"

build/raw_source.zip: build/processed_binary.jar
	[ -d "$(FFTEMP)" ] && rm -r "$(FFTEMP)"; exit 0
	mkdir -p "$(FFTEMP)"
	java -Xmx512M -cp eclipse/fernflower/bin org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler -din=1 -rbr=0 -dgs=1 -asc=1 -log=ALL "$<" "$(FFTEMP)" #| tee "build/fernflower.log"
	mv "$(FFTEMP)/$(shell basename "$<")" "$@"
	rmdir "$(FFTEMP)"

######################## DECOMP SOURCE PROCESSING ###########################

build/cleaned_source.zip: build/raw_source.zip $(ASTYLE_CFG)
	$(BUILDTOOLS) decompsource.MCPCleanup "$(ASTYLE_CFG)" < "$<" > "$@"
build/patched_source_fml.zip: build/cleaned_source.zip build/fml.patch
	$(BUILDTOOLS) decompsource.ApplyUnifiedDiffs build/fml.patch < "$<" > "$@"
build/patched_source_forge.zip: build/patched_source_fml.zip build/forge.patch
	$(BUILDTOOLS) decompsource.ApplyUnifiedDiffs build/forge.patch < "$<" > "$@"
build/processed_source.zip: build/patched_source_forge.zip
	cp "$<" "$@"

build/deobf_source.zip: build/processed_source.zip
	$(BUILDTOOLS) decompsource.RemapSources FML/conf/methods.csv FML/conf/fields.csv FML/conf/params.csv true < "$<" > "$@"

build/fml.patch: $(FML_PATCHES)
	cat $(FML_PATCHES) > "$@"
build/forge.patch: $(FORGE_PATCHES)
	cat $(FORGE_PATCHES) > "$@"

$(DEOBF_DATA): FML/conf/joined.srg
	lzma < "$<" > "$@"


################### RECOMPILATION OF EDITED SOURCE ##########################

build/edited_source.zip: $(shell find $(SOURCE_DIRS) -type f)
	rm -f "$@"
	for srcdir in $(SOURCE_DIRS); do cd $$srcdir; zip -r "$$OLDPWD/$@" .; cd "$$OLDPWD"; done

build/recomp.jar: build/edited_source.zip $(LIBRARIES)
	$(BUILDTOOLS) decompsource.CompileZip "$<" $(LIBRARIES_DIR) > "$@"

build/reobf.jar: build/recomp.jar build/mcp-to-srg.srg
	$(BUILDTOOLS) bytecode.ApplySRG build/mcp-to-srg.srg "$<" "$@"

build/srg-to-mcp.srg: $(SRG) FML/conf/methods.csv FML/conf/fields.csv
	$(BUILDTOOLS) decompsource.CSV2SRG FML/conf/methods.csv FML/conf/fields.csv < "$<" > "$@"

build/mcp-to-srg.srg: build/srg-to-mcp.srg
	$(BUILDTOOLS) decompsource.InvertSRG < "$<" > "$@"

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
	$(BUILDTOOLS) decompsource.MakeInstallJSON build/download/vanilla.json $(LIBRARY_JSON) $(VERSION_NAME_IN_LAUNCHER) > "$@"

build/install-data.zip: build/new-classes.pack build/bytecode.patch $(SRG) $(EXCEPTOR_JSON) $(EXC) $(FMLAT) $(FORGEAT) build/install.properties $(MCP_MERGE_CFG) build/install.json
	rm -f "$@"
	zip -j0 "$@" $^

build/install-data.zip.lzma: build/install-data.zip
	lzma -9e < "$<" > "$@"








################### INSTALLER CREATION ##########################

build/BuildTools.jar: $(shell find $(BUILDTOOLS_SRC_DIRS) -type f)
	rm -f "$@"
	rm -rf $(BUILDTOOLS_BIN_DIR)
	mkdir $(BUILDTOOLS_BIN_DIR)
	cp -rf $(foreach dir, $(BUILDTOOLS_SRC_DIRS), $(dir)/*) $(BUILDTOOLS_BIN_DIR)
	javac -d $(BUILDTOOLS_BIN_DIR) `find $(BUILDTOOLS_SRC_DIRS) -type f -name '*.java'`
	find $(BUILDTOOLS_BIN_DIR) -name '*.java' -exec rm '{}' \;
	jar -cvf "$@" -C $(BUILDTOOLS_BIN_DIR) .

build/installer-code.jar: build/BuildTools.jar
	rm -rf $(TEMPDIR)
	mkdir -p $(TEMPDIR)
	rm -f "$@"
	cd $(TEMPDIR); unzip "$$OLDPWD/$<"
	mv $(TEMPDIR)/LICENSE.txt $(TEMPDIR)/BON-LICENSE.txt
	cd $(TEMPDIR); zip -r9 "$$OLDPWD/$@" bytecode installer lzma misc org/objectweb/asm immibis/bon JAVA-ASM-LICENSE.txt BON-LICENSE.txt

	# add installer/META-INF/MANIFEST.MF as manifest
	cd $(TEMPDIR)/installer; zip -9 "$$OLDPWD/$@" META-INF/MANIFEST.MF

build/installer.jar: build/installer-code.jar build/install-data.zip.lzma
	cp "$<" "$@"
	cd build; zip -0 "../$@" install-data.zip.lzma







