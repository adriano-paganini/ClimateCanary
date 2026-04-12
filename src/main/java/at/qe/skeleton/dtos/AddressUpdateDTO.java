package at.qe.skeleton.dtos;

public record AddressUpdateDTO(
        String country,
        String zipCode,
        String city,
        String street,
        String houseNumber,
        String extra
) {
}
