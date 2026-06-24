package at.qe.skeleton.services;

import at.qe.skeleton.common.exceptions.ConflictException;
import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.AddressUpdateDTO;
import at.qe.skeleton.models.Address;
import at.qe.skeleton.repositories.AddressRepository;
import at.qe.skeleton.repositories.BuildingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for managing addresses.
 * Provides CRUD operations with security restrictions for system and building administrators.
 * Ensures referential integrity with buildings before deletion.
 */
@Slf4j
@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final BuildingRepository buildingRepository;

    public AddressService(AddressRepository addressRepository, BuildingRepository buildingRepository) {
        this.addressRepository = addressRepository;
        this.buildingRepository = buildingRepository;
    }

    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'BUILDING_ADMIN')")
    public List<Address> getAll() {
        return addressRepository.findAll();
    }

    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'BUILDING_ADMIN')")
    public Address getById(Long id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Address with id %d not found", id)));
    }

    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'BUILDING_ADMIN')")
    public Address create(Address address) {
        Address savedAddress =  addressRepository.save(address);

        log.info("Created address with id: {}", savedAddress.getId());

        log.debug("Created address details: id={}, country={}, zipCode={}, city={}, street={}, houseNumber={}, extra={}",
                savedAddress.getId(),
                savedAddress.getCountry(),
                savedAddress.getZipCode(),
                savedAddress.getCity(),
                savedAddress.getStreet(),
                savedAddress.getHouseNumber(),
                savedAddress.getExtra());

        return savedAddress;
    }

    /**
     * Partially updates an existing address.
     * Only non-null fields from the DTO are applied.
     */
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'BUILDING_ADMIN')")
    public Address update(Long id, AddressUpdateDTO dto) {
        Address existing = getById(id);

        StringBuilder debugInfo = new StringBuilder("Updated address details:")
                .append(" id=").append(id);

        if (dto.country() != null) {
            existing.setCountry(dto.country());
            debugInfo.append(", country=").append(dto.country());
        }
        if (dto.zipCode() != null) {
            existing.setZipCode(dto.zipCode());
            debugInfo.append(", zipCode=").append(dto.zipCode());
        }
        if (dto.city() != null) {
            existing.setCity(dto.city());
            debugInfo.append(", city=").append(dto.city());
        }
        if (dto.street() != null) {
            existing.setStreet(dto.street());
            debugInfo.append(", street=").append(dto.street());
        }
        if (dto.houseNumber() != null) {
            existing.setHouseNumber(dto.houseNumber());
            debugInfo.append(", houseNumber=").append(dto.houseNumber());
        }
        if (dto.extra() != null) {
            existing.setExtra(dto.extra());
            debugInfo.append(", extra=").append(dto.extra());
        }

        Address updatedAddress = addressRepository.save(existing);

        log.info("Updated absence id={}", updatedAddress.getId());
        log.debug(debugInfo.toString());

        return updatedAddress;
    }

    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'BUILDING_ADMIN')")
    public void delete(Long id) {
        getById(id);

        if (buildingRepository.existsBuildingByAddressId(id)) {
            throw new ConflictException("Address with id " + id + " is still referenced by a building");
        }
        addressRepository.deleteById(id);
        log.info("Deleted address with id: {}", id);
    }
}
