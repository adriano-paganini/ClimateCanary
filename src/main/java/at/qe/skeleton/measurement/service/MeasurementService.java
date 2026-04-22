package at.qe.skeleton.measurement.service;

import at.qe.skeleton.climatehint.model.Metric;
import at.qe.skeleton.common.exceptions.MeasurementNotFoundException;
import at.qe.skeleton.measurement.model.Measurement;
import at.qe.skeleton.measurement.repository.MeasurementRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public List<Measurement> getFiltered(Long roomId,
                                         Metric metric,
                                         LocalDateTime from,
                                         LocalDateTime to) {
        boolean hasRoom   = roomId != null;
        boolean hasMetric = metric != null;
        boolean hasRange  = from != null && to != null;

        if (hasRoom && hasMetric && hasRange)
            return repository.findByRoomIdAndMetricAndTimestampBetween(roomId, metric, from, to);
        if (hasRoom && hasMetric)
            return repository.findByRoomIdAndMetric(roomId, metric);
        if (hasRoom && hasRange)
            return repository.findByRoomIdAndTimestampBetween(roomId, from, to);
        if (hasRoom)
            return repository.findByRoomId(roomId);
        if (hasRange)
            return repository.findByTimestampBetween(from, to);

        return repository.findAll();
    }

    public Map<Metric, Measurement> getLatestPerMetric(Long roomId) {
        return Arrays.stream(Metric.values())
                .map(metric -> repository.findLatestByRoomIdAndMetric(roomId, metric))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Measurement::getMetric, m -> m));
    }
}
