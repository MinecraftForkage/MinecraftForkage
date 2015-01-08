package net.minecraftforkage.instsetup.depsort;

@SuppressWarnings("serial")
public class UnfulfiledRequirementException extends DependencySortingException {
	public UnfulfiledRequirementException(String message) {
		super(message);
	}
}
