package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.EmployeeProfileCreateDTO;
import at.qe.skeleton.dtos.EmployeeProfileDTO;
import at.qe.skeleton.dtos.EmployeeProfileUpdateDTO;
import at.qe.skeleton.mappers.EmployeeProfileCreateMapper;
import at.qe.skeleton.mappers.EmployeeProfileMapper;
import at.qe.skeleton.models.EmployeeProfile;
import at.qe.skeleton.services.EmployeeProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/employeeprofile")
public class EmployeeProfileController {

    private final EmployeeProfileService employeeProfileService;
    private final EmployeeProfileMapper employeeProfileMapper;
    private final EmployeeProfileCreateMapper employeeProfileCreateMapper;

    public EmployeeProfileController(EmployeeProfileService employeeProfileService,
                                     EmployeeProfileMapper employeeProfileMapper,
                                     EmployeeProfileCreateMapper employeeProfileCreateMapper) {
        this.employeeProfileService = employeeProfileService;
        this.employeeProfileMapper = employeeProfileMapper;
        this.employeeProfileCreateMapper = employeeProfileCreateMapper;
    }

    @GetMapping
    public ResponseEntity<List<EmployeeProfileDTO>> getAll(){
        List<EmployeeProfileDTO> profiles = employeeProfileService.getAll().stream()
                .map(employeeProfileMapper::mapTo)
                .toList();
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeProfileDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeProfileMapper.mapTo(employeeProfileService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<EmployeeProfileDTO> create(@Valid @RequestBody EmployeeProfileCreateDTO dto) {
        EmployeeProfile profile = employeeProfileService.create(employeeProfileCreateMapper.mapFrom(dto));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(profile.getId())
                .toUri();

        return ResponseEntity.created(location).body(employeeProfileMapper.mapTo(profile));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<EmployeeProfileDTO> update(@PathVariable Long id,
                                                     @Valid @RequestBody EmployeeProfileUpdateDTO dto) {
        EmployeeProfile updated = employeeProfileService.update(id, dto);
        return ResponseEntity.ok(employeeProfileMapper.mapTo(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeProfileService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
