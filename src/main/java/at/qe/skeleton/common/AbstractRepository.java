package at.qe.skeleton.common;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * Common base repository for all other repositories. Provides basic methods for
 * loading, saving, and removing entities.
 * This class is part of the skeleton project provided for students of the
* course "Software Engineering" offered by Innsbruck University.
 *
 * @param <T> The domain type this repository manages.
 * @param <ID> The type of the id of the entity this repository manages.
 */
@NoRepositoryBean
public interface AbstractRepository<T, ID extends Serializable> extends Repository<T, ID> {

    /**
     * Deletes an entity.
     *
     * @param entity The entity to be enabled.
     * @throws IllegalArgumentException If the given entity is (@literal null).
     */
    void delete(T entity);

    /**
     * Returns all instances of the type.
     *
     * @return All entities.
     */
    List<T> findAll();

    /**
     * Returns all entities with the given ids.
     *
     * @param ids The ids to retrieve entities for.
     * @return A list of entities with the given ids.
     * @throws IllegalArgumentException If {@code ids} is {@literal null}.
     */
    List<T> findAllById(Iterable<ID> ids);

    /**
     * Retrieves an entity by its id.
     *
     * @param id must not be {@literal null}.
     * @return An Optional containing the entity with the given id or {@literal null} if none found.
     * @throws IllegalArgumentException If {@code id} is {@literal null}.
     */
    Optional<T> findById(ID id);

    /**
     * Saves a given entity. Use the returned instance for further operations as
     * the save operation might have changed the entity instance completely.
     *
     * @param <S> The actual domain type if the entity.
     * @param entity The entity to be saved or updated.
     * @return The saved entity.
     */
    <S extends T> S save(S entity);
}
