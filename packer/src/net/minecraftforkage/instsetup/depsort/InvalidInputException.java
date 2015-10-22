package net.minecraftforkage.instsetup.depsort;

@SuppressWarnings("serial")
public class InvalidInputException extends DependencySortingException {
	public InvalidInputException(String message) {
		super(message);
	}
}
