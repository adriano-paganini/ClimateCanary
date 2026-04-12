package at.qe.skeleton.dtos;

import jakarta.validation.constraints.NotNull;

public record AddressDTO(

        @NotNull Long id,
        @NotNull String country,
        @NotNull String zipCode,
        @NotNull String city,
        @NotNull String street,
        @NotNull String houseNumber,
        @NotNull String extra

) {}
