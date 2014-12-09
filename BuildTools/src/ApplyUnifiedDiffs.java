import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import patcher.PatchFile;
import bytecode.BaseStreamingZipProcessor;



public class ApplyUnifiedDiffs extends BaseStreamingZipProcessor {
	public static void main(String[] args) {
		new ApplyUnifiedDiffs().go(args);
	}
	
	@Override
	protected boolean hasConfig() {
		return true;
	}
	
	private PatchFile ipatch;
	@Override
	protected void loadConfig(File file) throws Exception {
		try (BufferedReader fr = new BufferedReader(new FileReader(file))) {
			ipatch = PatchFile.load(fr);
		}
	}
	
	private List<String> seenPaths = new ArrayList<>();
	
	@Override
	protected void done() throws Exception {
		Set<String> notApplied = ipatch.hunks.keySet();
		notApplied.removeAll(seenPaths);
		if(notApplied.size() != 0)
			throw new RuntimeException("Didn't apply patches: "+notApplied);
	}
	
	@Override
	protected byte[] process(byte[] in, String name) throws Exception {
		seenPaths.add(name);
		try {
			return ipatch.applyPatches(in, name);
		} catch(RuntimeException e) {
			throw new RuntimeException("Failed patching "+name, e);
		}
	}
	
	@Override
	protected boolean shouldProcess(String name) {
		return name.endsWith(".java");
	}
}
