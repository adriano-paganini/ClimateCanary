package at.qe.skeleton.repositories;

import at.qe.skeleton.model.Address;

public interface AddressRepository extends AbstractRepository<Address, Long> {

    void deleteById(Long id);
}
