package at.qe.skeleton.mappers;

import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.dtos.EmployeeProfileDTO;
import at.qe.skeleton.models.EmployeeProfile;
import org.springframework.stereotype.Service;

@Service
public class EmployeeProfileMapper implements DTOMapper<EmployeeProfile, EmployeeProfileDTO> {

    @Override
    public EmployeeProfileDTO mapTo(EmployeeProfile entity) {

        return new EmployeeProfileDTO(
                entity.getId(),
                entity.getUser().getId(),
                entity.getUser().getFirstName(),
                entity.getUser().getLastName(),
                entity.getDepartment() == null ? null : entity.getDepartment().getId(),
                entity.getRoom() == null ? null : entity.getRoom().getId()
        );
    }

    @Override
    public EmployeeProfile mapFrom(EmployeeProfileDTO dto) {
        throw new UnsupportedOperationException();
    }
}
