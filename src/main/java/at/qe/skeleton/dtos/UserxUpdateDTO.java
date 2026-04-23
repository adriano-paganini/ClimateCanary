package at.qe.skeleton.dtos;

import at.qe.skeleton.models.UserxRole;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

public record UserxUpdateDTO(
        String email,
        @ArraySchema(schema = @Schema(implementation = UserxRole.class))
        Set<UserxRole> roles,
        String firstName,
        String lastName,
        String phone,
        Boolean enabled
) {}
