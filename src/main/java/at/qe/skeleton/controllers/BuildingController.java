package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.BuildingCreateDTO;
import at.qe.skeleton.dtos.BuildingDTO;
import at.qe.skeleton.dtos.BuildingUpdateDTO;
import at.qe.skeleton.mappers.BuildingCreateMapper;
import at.qe.skeleton.mappers.BuildingMapper;
import at.qe.skeleton.mappers.BuildingUpdateMapper;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.services.BuildingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/building")
public class BuildingController {

    private final BuildingService buildingService;
    private final BuildingMapper buildingMapper;
    private final BuildingCreateMapper buildingCreateMapper;
    private final BuildingUpdateMapper buildingUpdateMapper;

    public BuildingController(BuildingService buildingService,
                              BuildingMapper buildingMapper,
                              BuildingCreateMapper buildingCreateMapper,
                              BuildingUpdateMapper buildingUpdateMapper) {
        this.buildingService = buildingService;
        this.buildingMapper = buildingMapper;
        this.buildingCreateMapper = buildingCreateMapper;
        this.buildingUpdateMapper = buildingUpdateMapper;
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
        Building building = buildingService.getBuildingById(id);

        return ResponseEntity.ok(buildingMapper.mapTo(building));
    }

    @PostMapping
    public ResponseEntity<BuildingDTO> create(@RequestBody BuildingCreateDTO dto) {
        Building building = buildingService.create(buildingCreateMapper.mapFrom(dto));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(building.getId())
                .toUri();

        return ResponseEntity.created(location).body(buildingMapper.mapTo(building));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BuildingDTO> update(@PathVariable Long id,
                                           @RequestBody BuildingUpdateDTO updated) {

        buildingService.update(id, buildingUpdateMapper.mapFrom(updated));
        Building building = buildingService.getBuildingById(id);

        return ResponseEntity.ok(buildingMapper.mapTo(building));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        buildingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // TODO: update this to include DTO

    @GetMapping("/{id}/rooms")
    public ResponseEntity<List<Room>> getRooms(@PathVariable Long id) {
        return ResponseEntity.ok(buildingService.getBuildingById(id).getRooms());
    }

}
