package at.qe.skeleton.repositories;

import at.qe.skeleton.models.Address;
import at.qe.skeleton.common.AbstractRepository;

public interface AddressRepository extends AbstractRepository<Address, Long> {

    void deleteById(Long id);
}
