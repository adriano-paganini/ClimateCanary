package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.RoomCreateDTO;
import at.qe.skeleton.dtos.RoomDTO;
import at.qe.skeleton.dtos.RoomUpdateDTO;
import at.qe.skeleton.mappers.RoomCreateMapper;
import at.qe.skeleton.mappers.RoomMapper;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.services.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST controller for managing rooms.
 * Provides CRUD operations for room entities.
 */
@RestController
@RequestMapping("/api/room")
public class RoomController {

    private final RoomService roomService;
    private final RoomMapper roomMapper;
    private final RoomCreateMapper createMapper;

    public RoomController(
            RoomService roomService,
            RoomMapper roomMapper,
            RoomCreateMapper createMapper
    ) {
        this.roomService = roomService;
        this.roomMapper = roomMapper;
        this.createMapper = createMapper;
    }

    @GetMapping
    public ResponseEntity<List<RoomDTO>> getAll() {
        List<RoomDTO> rooms = roomService.getAll().stream()
                .map(roomMapper::mapTo)
                .toList();
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(roomMapper.mapTo(roomService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<RoomDTO> create(@Valid @RequestBody RoomCreateDTO dto) {
        Room room = roomService.create(createMapper.mapFrom(dto));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(room.getId())
                .toUri();

        return ResponseEntity.created(location).body(roomMapper.mapTo(room));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RoomDTO> update(@PathVariable Long id,
                                          @Valid @RequestBody RoomUpdateDTO dto) {
        Room updated = roomService.update(id, dto);
        return ResponseEntity.ok(roomMapper.mapTo(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomService.delete(id);
        return ResponseEntity.noContent().build();
    }
}