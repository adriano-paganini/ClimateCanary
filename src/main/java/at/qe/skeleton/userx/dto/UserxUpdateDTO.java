package at.qe.skeleton.userx.dto;

import at.qe.skeleton.userx.model.UserxRole;

import java.util.Set;

public record UserxUpdateDTO(
        String email,
        Set<UserxRole> roles,
        String firstName,
        String lastName,
        String phone,
        Boolean deleted
) {}
