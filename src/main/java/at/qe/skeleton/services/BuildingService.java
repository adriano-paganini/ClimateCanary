package at.qe.skeleton.services;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.BuildingUpdateDTO;
import at.qe.skeleton.models.Building;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.repositories.BuildingRepository;
import at.qe.skeleton.repositories.RoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing buildings.
 * Handles building lifecycle operations including creation, updates,
 * and safe deletion with relationship cleanup for associated rooms.
 */
@Slf4j
@Service
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final AddressService addressService;
    private final RoomRepository roomRepository;

    public BuildingService(BuildingRepository buildingRepository, AddressService addressService, RoomRepository roomRepository) {
        this.buildingRepository = buildingRepository;
        this.addressService = addressService;
        this.roomRepository = roomRepository;
    }

    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'BUILDING_ADMIN')")
    public List<Building> getAllBuildings() {
        return buildingRepository.findAll();
    }

    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'BUILDING_ADMIN')")
    public Building getBuildingById(long id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Building with id " + id + " not found"));
    }

    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'BUILDING_ADMIN')")
    public Building create(Building building) {
        Building savedBuilding = buildingRepository.save(building);

        log.info("Created building with id={}", savedBuilding.getId());
        log.debug("Created building details: id={}, name={}, addressId={}",
                savedBuilding.getId(),
                savedBuilding.getName(),
                savedBuilding.getAddress() != null ? savedBuilding.getAddress().getId() : null);

        return savedBuilding;
    }

    /**
     * Updates an existing building using partial update semantics.
     * Only non-null fields in the DTO are applied.
     * Supports changing the building's address via addressId lookup.
     */
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'BUILDING_ADMIN')")
    public Building update(Long id, BuildingUpdateDTO dto) {
        Building building = getBuildingById(id);

        StringBuilder debugInfo = new StringBuilder("Updated building details:")
                .append(" id=").append(id);

        if (dto.name() != null) {
            building.setName(dto.name());
            debugInfo.append(", name=").append(dto.name());
        }

        if (dto.addressId() != null) {
            building.setAddress(addressService.getById(dto.addressId()));
            debugInfo.append(", addressId=").append(dto.addressId());
        }

        Building updatedBuilding = buildingRepository.save(building);

        log.info("Updated building with id={}", id);
        log.debug(debugInfo.toString());

        return updatedBuilding;
    }

    /**
     * Deletes a building and safely detaches all associated rooms.
     * <p>
     * Before deletion:
     * - All rooms referencing this building are detached
     * - Room-building relationships are cleared to prevent foreign key violations
     * <p>
     * This operation is transactional to ensure consistency.
     */
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'BUILDING_ADMIN')")
    @Transactional
    public void delete(Long id) {
        Building building = getBuildingById(id);

        for (Room room : building.getRooms()) {
            room.setBuilding(null);
            roomRepository.save(room);
        }
        building.getRooms().clear();

        buildingRepository.deleteById(id);
        log.info("Deleted building with id={}", id);
        log.debug("Removed building associations for rooms before deleting building id={}", id);
    }

}