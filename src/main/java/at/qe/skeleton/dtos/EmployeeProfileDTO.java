package at.qe.skeleton.dtos;

public record EmployeeProfileDTO(
        Long id,
        Long userxId,
        String firstName,
        String lastName,
        Long departmentId,
        Long roomId
) {}
