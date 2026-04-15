package at.qe.skeleton.building.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BuildingCreateDTO(

        @NotBlank
        String name,

        @NotNull(message = "Please provide a valid addressId")
        Long addressId
) {}
