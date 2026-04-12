package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.*;
import at.qe.skeleton.mappers.*;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.services.DepartmentService;
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
    private final DepartmentUpdateMapper departmentUpdateMapper;
    private final DepartmentCreateMapper departmentCreateMapper;
    private final UserxMapper userxMapper;
    private final RoomMapper roomMapper;

    public DepartmentController(
            DepartmentService departmentService,
            DepartmentMapper departmentMapper,
            DepartmentUpdateMapper departmentUpdateMapper,
            DepartmentCreateMapper departmentCreateMapper,
            UserxMapper userxMapper,
            RoomMapper roomMapper) {
        this.departmentService = departmentService;
        this.departmentMapper = departmentMapper;
        this.departmentUpdateMapper = departmentUpdateMapper;
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
        Department department = departmentService.getById(id);
        return ResponseEntity.ok(departmentMapper.mapTo(department));
    }

    @GetMapping("/{id}/rooms")
    public ResponseEntity<List<RoomDTO>> getRooms(@PathVariable Long id) {
        Department department = departmentService.getById(id);
        List<RoomDTO> rooms = department.getRooms().stream()
                .map(roomMapper::mapTo)
                .toList();
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{id}/leader")
    public ResponseEntity<UserxDTO> getDepartmentLeader(@PathVariable Long id) {
        Department department = departmentService.getById(id);
        return ResponseEntity.ok(userxMapper.mapTo(department.getDepartmentLeader()));
    }

    @PostMapping
    public ResponseEntity<DepartmentDTO> create(@RequestBody DepartmentCreateDTO dto) {

        Department department = departmentService.create(departmentCreateMapper.mapFrom(dto));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(department.getId())
                .toUri();

        return ResponseEntity.created(location).body(departmentMapper.mapTo(department));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DepartmentDTO> update(
            @PathVariable Long id,
            @RequestBody DepartmentUpdateDTO dto) {

        departmentService.update(id, departmentUpdateMapper.mapFrom(dto));

        Department updated = departmentService.getById(id);

        return ResponseEntity.ok(departmentMapper.mapTo(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
