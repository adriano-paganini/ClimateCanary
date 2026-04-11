package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.RoomCreateDTO;
import at.qe.skeleton.dtos.RoomDTO;
import at.qe.skeleton.dtos.RoomUpdateDTO;
import at.qe.skeleton.mappers.RoomCreateMapper;
import at.qe.skeleton.mappers.RoomMapper;
import at.qe.skeleton.mappers.RoomUpdateMapper;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.services.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/room")
public class RoomController {

    private final RoomService roomService;
    private final RoomMapper roomMapper;
    private final RoomCreateMapper createMapper;
    private final RoomUpdateMapper updateMapper;

    public RoomController(
            RoomService roomService,
            RoomMapper roomMapper,
            RoomCreateMapper createMapper,
            RoomUpdateMapper updateMapper
    ) {
        this.roomService = roomService;
        this.roomMapper = roomMapper;
        this.createMapper = createMapper;
        this.updateMapper = updateMapper;
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

        Room room = roomService.getById(id);

        return ResponseEntity.ok(roomMapper.mapTo(room));
    }

    @PostMapping
    public ResponseEntity<RoomDTO> create(@RequestBody RoomCreateDTO dto) {

        Room room = roomService.create(createMapper.mapFrom(dto));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(room.getId())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(roomMapper.mapTo(room));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RoomDTO> update(
            @PathVariable Long id,
            @RequestBody RoomUpdateDTO dto) {

        roomService.update(id, updateMapper.mapFrom(dto));

        Room updated = roomService.getById(id);

        return ResponseEntity.ok(roomMapper.mapTo(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomService.delete(id);
        return ResponseEntity.noContent().build();
    }
}