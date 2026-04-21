package at.qe.skeleton.building.controller;

import at.qe.skeleton.building.dto.BuildingCreateDTO;
import at.qe.skeleton.building.dto.BuildingDTO;
import at.qe.skeleton.building.dto.BuildingUpdateDTO;
import at.qe.skeleton.room.dto.RoomDTO;
import at.qe.skeleton.building.mapper.BuildingCreateMapper;
import at.qe.skeleton.building.mapper.BuildingMapper;
import at.qe.skeleton.room.mappers.RoomMapper;
import at.qe.skeleton.building.model.Building;
import at.qe.skeleton.building.service.BuildingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/building")
public class BuildingController {

    private final BuildingService buildingService;
    private final BuildingMapper buildingMapper;
    private final BuildingCreateMapper buildingCreateMapper;
    private final RoomMapper roomMapper;

    public BuildingController(BuildingService buildingService,
                              BuildingMapper buildingMapper,
                              BuildingCreateMapper buildingCreateMapper,
                              RoomMapper roomMapper) {
        this.buildingService = buildingService;
        this.buildingMapper = buildingMapper;
        this.buildingCreateMapper = buildingCreateMapper;
        this.roomMapper = roomMapper;
    }

    @GetMapping
    public ResponseEntity<List<BuildingDTO>> getAll() {
        List<BuildingDTO> buildings = buildingService.getAllBuildings().stream()
                .map(buildingMapper::mapTo)
                .toList();
        return ResponseEntity.ok(buildings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BuildingDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(buildingMapper.mapTo(buildingService.getBuildingById(id)));
    }

    @PostMapping
    public ResponseEntity<BuildingDTO> create(@Valid @RequestBody BuildingCreateDTO dto) {
        Building building = buildingService.create(buildingCreateMapper.mapFrom(dto));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(building.getId())
                .toUri();

        return ResponseEntity.created(location).body(buildingMapper.mapTo(building));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BuildingDTO> update(@PathVariable Long id,
                                              @Valid @RequestBody BuildingUpdateDTO dto) {
        Building building = buildingService.update(id, dto);
        return ResponseEntity.ok(buildingMapper.mapTo(building));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        buildingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/rooms")
    public ResponseEntity<List<RoomDTO>> getRooms(@PathVariable Long id) {
        List<RoomDTO> buildingRooms = buildingService.getBuildingById(id).getRooms()
                .stream()
                .map(roomMapper::mapTo)
                .toList();
        return ResponseEntity.ok(buildingRooms);
    }

}
