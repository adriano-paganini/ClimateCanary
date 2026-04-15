package at.qe.skeleton.address.dto;

import jakarta.validation.constraints.Size;

public record AddressUpdateDTO(

        @Size(min = 1, message = "Country must not be blank if provided")
        String country,

        @Size(min = 1, message = "Zip code must not be blank if provided")
        String zipCode,

        @Size(min = 1, message = "City must not be blank if provided")
        String city,

        @Size(min = 1, message = "Street must not be blank if provided")
        String street,

        @Size(min = 1, message = "House number must not be blank if provided")
        String houseNumber,

        String extra
) {
}
