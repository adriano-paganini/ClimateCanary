package at.qe.skeleton.department.controller;

import at.qe.skeleton.department.dto.DepartmentCreateDTO;
import at.qe.skeleton.department.dto.DepartmentDTO;
import at.qe.skeleton.department.dto.DepartmentUpdateDTO;
import at.qe.skeleton.department.mapper.DepartmentCreateMapper;
import at.qe.skeleton.department.mapper.DepartmentMapper;
import at.qe.skeleton.department.model.Department;
import at.qe.skeleton.department.service.DepartmentService;
import at.qe.skeleton.room.dto.RoomDTO;
import at.qe.skeleton.room.mappers.RoomMapper;
import at.qe.skeleton.userx.dto.UserxDTO;
import at.qe.skeleton.userx.mapper.UserxMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/department")
public class DepartmentController {

    private final DepartmentService departmentService;
    private final DepartmentMapper departmentMapper;
    private final DepartmentCreateMapper departmentCreateMapper;
    private final UserxMapper userxMapper;
    private final RoomMapper roomMapper;

    public DepartmentController(
            DepartmentService departmentService,
            DepartmentMapper departmentMapper,
            DepartmentCreateMapper departmentCreateMapper,
            UserxMapper userxMapper,
            RoomMapper roomMapper) {
        this.departmentService = departmentService;
        this.departmentMapper = departmentMapper;
        this.departmentCreateMapper = departmentCreateMapper;
        this.userxMapper = userxMapper;
        this.roomMapper = roomMapper;
    }

    @GetMapping
    public ResponseEntity<List<DepartmentDTO>> getAll() {
        List<DepartmentDTO> departments = departmentService.getAll().stream()
                .map(departmentMapper::mapTo)
                .toList();
        return ResponseEntity.ok(departments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentMapper.mapTo(departmentService.getDepartmentById(id)));
    }

    @GetMapping("/{id}/rooms")
    public ResponseEntity<List<RoomDTO>> getRooms(@PathVariable Long id) {
        List<RoomDTO> rooms = departmentService.getDepartmentById(id).getRooms().stream()
                .map(roomMapper::mapTo)
                .toList();
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{id}/leader")
    public ResponseEntity<UserxDTO> getDepartmentLeader(@PathVariable Long id) {
        Department department = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(userxMapper.mapTo(department.getDepartmentLeader()));
    }

    @PostMapping
    public ResponseEntity<DepartmentDTO> create(@Valid @RequestBody DepartmentCreateDTO dto) {
        Department department = departmentService.create(departmentCreateMapper.mapFrom(dto));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(department.getId())
                .toUri();

        return ResponseEntity.created(location).body(departmentMapper.mapTo(department));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DepartmentDTO> update(@PathVariable Long id,
                                                @Valid @RequestBody DepartmentUpdateDTO dto) {
        Department updated = departmentService.update(id, dto);
        return ResponseEntity.ok(departmentMapper.mapTo(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
