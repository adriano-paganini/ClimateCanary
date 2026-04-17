package at.qe.skeleton.measurement.service;

import at.qe.skeleton.common.exceptions.MeasurementNotFoundException;
import at.qe.skeleton.measurement.model.Measurement;
import at.qe.skeleton.measurement.repository.MeasurementRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MeasurementService {

    private final MeasurementRepository repository;

    public MeasurementService(MeasurementRepository repository) {
        this.repository = repository;
    }

    public List<Measurement> getAll() {
        return repository.findAll();
    }

    public Measurement getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new MeasurementNotFoundException("Measurement with id " + id + " not found"));
    }
}
