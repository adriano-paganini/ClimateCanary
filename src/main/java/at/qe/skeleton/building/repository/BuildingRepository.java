package at.qe.skeleton.building.repository;


import at.qe.skeleton.building.model.Building;
import at.qe.skeleton.common.AbstractRepository;

public interface BuildingRepository extends AbstractRepository<Building, Long> {

    void deleteById(Long id);

    boolean existsBuildingByAddressId(Long addressId);
}
