package at.qe.skeleton.raspberrypi.repository;

import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.raspberrypi.model.RaspberryPi;

public interface RaspberryPiRepository extends AbstractRepository<RaspberryPi, Long> {
    void deleteById(Long id);
}