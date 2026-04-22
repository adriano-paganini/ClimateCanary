package at.qe.skeleton.repositories;


import at.qe.skeleton.models.Building;
import at.qe.skeleton.common.AbstractRepository;

public interface BuildingRepository extends AbstractRepository<Building, Long> {

    void deleteById(Long id);

    boolean existsBuildingByAddressId(Long addressId);
}
