package at.qe.skeleton.exceptions;

public class BuildingNotFoundException extends RuntimeException {
    public BuildingNotFoundException(String message) {
        super(message);
    }
}
