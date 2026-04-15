package at.qe.skeleton.common.exceptions;

public class EmployeeProfileNotFoundException extends RuntimeException {
    public EmployeeProfileNotFoundException(String message) {
        super(message);
    }
}
