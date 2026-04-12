package at.qe.skeleton.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddressCreateDTO(

        @NotNull
        @NotBlank
        String name,

        @NotNull
        @NotBlank
        String country,

        @NotNull
        @NotBlank
        String zipCode,

        @NotNull
        @NotBlank
        String city,

        @NotNull
        @NotBlank
        String street,

        @NotNull
        @NotBlank
        String houseNumber,

        String extra

) {}
