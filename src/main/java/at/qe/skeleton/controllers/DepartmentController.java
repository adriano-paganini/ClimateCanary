package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.DepartmentCreateDTO;
import at.qe.skeleton.dtos.DepartmentDTO;
import at.qe.skeleton.dtos.DepartmentUpdateDTO;
import at.qe.skeleton.mappers.DepartmentCreateMapper;
import at.qe.skeleton.mappers.DepartmentMapper;
import at.qe.skeleton.mappers.DepartmentUpdateMapper;
import at.qe.skeleton.model.Department;
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

    public DepartmentController(
            DepartmentService departmentService,
            DepartmentMapper departmentMapper,
            DepartmentUpdateMapper departmentUpdateMapper,
            DepartmentCreateMapper departmentCreateMapper
    ) {
        this.departmentService = departmentService;
        this.departmentMapper = departmentMapper;
        this.departmentUpdateMapper = departmentUpdateMapper;
        this.departmentCreateMapper = departmentCreateMapper;
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
