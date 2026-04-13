package at.qe.skeleton.repositories;


import at.qe.skeleton.model.Building;

public interface BuildingRepository extends AbstractRepository<Building, Long> {

    void deleteById(Long id);

    boolean existsBuildingByAddressId(Long addressId);
}
