package net.minecraftforkage.instsetup.depsort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DependencySorter {
	private DependencySorter() {throw new RuntimeException();}

	
	

	public static <T extends DependencySortedObject> List<T> sort(List<T> inList) throws InvalidInputException, UnfulfiledRequirementException, DependencyCycleException {
		
		class ObjectWithInfo {
			final T object;
			final String id;
			final String depString;
			Collection<ObjectWithInfo> shouldBeAfter = new ArrayList<>();
			boolean isSortedYet;
			int starIndex;
			
			ObjectWithInfo(T object) throws InvalidInputException {
				this.object = object;
				this.id = this.object.getID();
				this.depString = this.object.getDependencies();
				if(id == null || id.equals(""))
					throw new InvalidInputException("IDs cannot be null or blank.");
			}
			
			boolean canGoNow(List<ObjectWithInfo> unsortedObjects) {
				for(ObjectWithInfo other : shouldBeAfter)
					if(!other.isSortedYet)
						return false;
				for(ObjectWithInfo other : unsortedObjects)
					if(other.starIndex < starIndex)
						return false;
				return true;
			}
			
			@Override
			public String toString() {
				return id + "(" + depString + ")";
			}
		}
		
		List<ObjectWithInfo> unsortedObjects = new ArrayList<>();
		Map<String, ObjectWithInfo> byID = new HashMap<>();
		for(T inObject : inList)
			unsortedObjects.add(new ObjectWithInfo(inObject));
		
		// don't allow mods to rely on accidental ordering
		Collections.shuffle(unsortedObjects);
		
		for(ObjectWithInfo i : unsortedObjects)
			if(byID.put(i.id, i) != null)
				throw new InvalidInputException("Duplicate object ID");
		
		// parse dependency strings
		for(ObjectWithInfo i : unsortedObjects) {
			if(i.depString.equals(""))
				continue;
			for(String dep : i.depString.split(";")) {
				String[] parts = dep.split(":");
				if(parts.length != 2 || parts[1].equals(""))
					throw new InvalidInputException(i.id+" has invalid dependency string: "+i.depString);
				
				if(parts[0].equals("requires")) {
					if(!byID.containsKey(parts[1]))
						throw new UnfulfiledRequirementException(i.id+" requires "+parts[1]+" which is not installed");
					
				} else if(parts[0].equals("before")) {
					int stars = countStars(parts[1]);
					if(stars != -1) {
						if(i.starIndex != 0)
							throw new InvalidInputException(i.id+" has invalid dependency string: "+i.depString);
						i.starIndex = -stars;
					
					} else {
						ObjectWithInfo other = byID.get(parts[1]);
						if(other != null)
							other.shouldBeAfter.add(i);
					}
					
				} else if(parts[0].equals("after")) {
					int stars = countStars(parts[1]);
					if(stars != -1) {
						if(i.starIndex != 0)
							throw new InvalidInputException(i.id+" has invalid dependency string: "+i.depString);
						i.starIndex = stars;
					
					} else {
						ObjectWithInfo other = byID.get(parts[1]);
						if(other != null)
							i.shouldBeAfter.add(other);
					}
					
				} else
					throw new InvalidInputException(i.id+" has invalid dependency string: "+i.depString);
			}
		}
		
		
		List<T> sortedObjects = new ArrayList<>();
		
		// simple brute-force method; worst case O(N^2 E) if N is the number of objects and E is the number of dependencies
		// (where after:* counts as a dependency on everything, for example)
		while(!unsortedObjects.isEmpty()) {
			Iterator<ObjectWithInfo> it = unsortedObjects.iterator();
			boolean movedAny = false;
			while(it.hasNext()) {
				ObjectWithInfo item = it.next();
				if(item.canGoNow(unsortedObjects)) {
					it.remove();
					sortedObjects.add(item.object);
					item.isSortedYet = true;
					movedAny = true;
				}
			}
			
			if(!movedAny)
				throw new DependencyCycleException("At least one dependency cycle exists. Unsorted objects: "+unsortedObjects);
		}
		
		
		return sortedObjects;
	}




	private static int countStars(String string) {
		if(string.length() == 0)
			return -1;
		for(int k = 0; k < string.length(); k++)
			if(string.charAt(k) != '*')
				return -1;
		return string.length();
	}
}
