package at.qe.skeleton.absence.controller;

import at.qe.skeleton.absence.dto.AbsenceCreateDTO;
import at.qe.skeleton.absence.dto.AbsenceDTO;
import at.qe.skeleton.absence.dto.AbsenceUpdateDTO;
import at.qe.skeleton.absence.mapper.AbsenceCreateMapper;
import at.qe.skeleton.absence.mapper.AbsenceMapper;
import at.qe.skeleton.absence.model.Absence;
import at.qe.skeleton.absence.service.AbsenceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/absence")
public class AbsenceController {

    private final AbsenceService absenceService;
    private final AbsenceMapper absenceMapper;
    private final AbsenceCreateMapper absenceCreateMapper;

    public AbsenceController(AbsenceService absenceService,
                             AbsenceMapper absenceMapper,
                             AbsenceCreateMapper absenceCreateMapper) {
        this.absenceService = absenceService;
        this.absenceMapper = absenceMapper;
        this.absenceCreateMapper = absenceCreateMapper;
    }

    @GetMapping
    public ResponseEntity<List<AbsenceDTO>> getAll() {
        List<AbsenceDTO> absences = absenceService.getAll().stream()
                .map(absenceMapper::mapTo)
                .toList();
        return ResponseEntity.ok(absences);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AbsenceDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(absenceMapper.mapTo(absenceService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<AbsenceDTO> create(@Valid @RequestBody AbsenceCreateDTO dto) {
        Absence absence = absenceService.create(absenceCreateMapper.mapFrom(dto));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(absence.getId())
                .toUri();

        return ResponseEntity.created(location).body(absenceMapper.mapTo(absence));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AbsenceDTO> update(@PathVariable Long id,
                                             @Valid @RequestBody AbsenceUpdateDTO dto) {
        Absence updated = absenceService.update(id, dto);
        return ResponseEntity.ok(absenceMapper.mapTo(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        absenceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
