package at.qe.skeleton.room.controller;

import at.qe.skeleton.room.dto.RoomCreateDTO;
import at.qe.skeleton.room.dto.RoomDTO;
import at.qe.skeleton.room.dto.RoomUpdateDTO;
import at.qe.skeleton.room.mappers.RoomCreateMapper;
import at.qe.skeleton.room.mappers.RoomMapper;
import at.qe.skeleton.room.model.Room;
import at.qe.skeleton.room.service.RoomService;
import jakarta.validation.Valid;
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