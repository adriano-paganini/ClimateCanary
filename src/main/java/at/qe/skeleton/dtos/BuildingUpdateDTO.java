package at.qe.skeleton.dtos;

import jakarta.validation.constraints.Size;

public record BuildingUpdateDTO(

        @Size(min = 1, message = "Name must not be blank if provided")
        String name,

        Long addressId
) {}
