package net.minecraftforkage.instsetup.depsort;

@SuppressWarnings("serial")
public class DependencyCycleException extends DependencySortingException {
	public DependencyCycleException(String message) {
		super(message);
	}
}
