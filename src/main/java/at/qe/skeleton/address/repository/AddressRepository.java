package at.qe.skeleton.address.repository;

import at.qe.skeleton.address.model.Address;
import at.qe.skeleton.common.AbstractRepository;

public interface AddressRepository extends AbstractRepository<Address, Long> {

    void deleteById(Long id);
}
