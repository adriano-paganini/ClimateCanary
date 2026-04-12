package at.qe.skeleton.dtos;

import jakarta.validation.constraints.NotBlank;

public record AddressCreateDTO(

        @NotBlank
        String country,

        @NotBlank
        String zipCode,

        @NotBlank
        String city,

        @NotBlank
        String street,

        @NotBlank
        String houseNumber,

        String extra
) {}
