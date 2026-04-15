package at.qe.skeleton.address.dto;

public record AddressDTO(

        Long id,
        String country,
        String zipCode,
        String city,
        String street,
        String houseNumber,
        String extra

) {}
