package at.qe.skeleton.address.service;

import at.qe.skeleton.address.dto.AddressUpdateDTO;
import at.qe.skeleton.common.exceptions.AddressNotFoundException;
import at.qe.skeleton.address.model.Address;
import at.qe.skeleton.address.repository.AddressRepository;
import at.qe.skeleton.building.repository.BuildingRepository;
import at.qe.skeleton.common.exceptions.EntityInUseException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final BuildingRepository buildingRepository;

    public AddressService(AddressRepository addressRepository, BuildingRepository buildingRepository) {
        this.addressRepository = addressRepository;
        this.buildingRepository = buildingRepository;
    }

    public List<Address> getAll() {
        return addressRepository.findAll();
    }

    public Address getById(Long id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new AddressNotFoundException(String.format("Address with id %d not found", id)));
    }

    public Address create(Address address) {
        return addressRepository.save(address);
    }

    public Address update(Long id, AddressUpdateDTO dto) {
        Address existing = getById(id);

        if (dto.country() != null) {
            existing.setCountry(dto.country());
        }
        if (dto.zipCode() != null) {
            existing.setZipCode(dto.zipCode());
        }
        if (dto.city() != null) {
            existing.setCity(dto.city());
        }
        if (dto.street() != null) {
            existing.setStreet(dto.street());
        }
        if (dto.houseNumber() != null) {
            existing.setHouseNumber(dto.houseNumber());
        }
        if (dto.extra() != null) {
            existing.setExtra(dto.extra());
        }

        return addressRepository.save(existing);
    }

    public void delete(Long id) {
        getById(id);

        if (buildingRepository.existsBuildingByAddressId(id)) {
            throw new EntityInUseException("Address with id " + id + " is still referenced by a building");
        }
        addressRepository.deleteById(id);
    }
}
