package net.mcforkage.ant.compression;

import java.util.HashMap;
import java.util.Map;

public class FrequencyTable<K> {
	public Map<K, Integer> counts = new HashMap<>();
	public int total = 0;
	public void add(K k) {
		if(!counts.containsKey(k)) counts.put(k, 1);
		else counts.put(k, counts.get(k) + 1);
		total++;
	}
}