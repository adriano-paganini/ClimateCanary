package at.qe.skeleton.exceptions;

public class EmployeeProfileNotFoundException extends RuntimeException {
    public EmployeeProfileNotFoundException(String message) {
        super(message);
    }
}
