package net.minecraftforkage.instsetup.depsort;

@SuppressWarnings("serial")
public class UnfulfilledRequirementException extends DependencySortingException {
	public UnfulfilledRequirementException(String message) {
		super(message);
	}
}
