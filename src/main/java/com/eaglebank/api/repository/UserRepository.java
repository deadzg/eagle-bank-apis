package com.eaglebank.api.repository;

import com.eaglebank.api.beans.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for the User entity.
 * Provides standard CRUD (Create, Read, Update, Delete) operations
 * for the User entity with Long as the ID type.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Finds a User entity by its username. This is crucial for Spring Security's
     * UserDetailsService implementation.
     * @param email The username to search for.
     * @return An Optional containing the User if found.
     */
    Optional<User> findByEmail(String email);

    /**
     * Deletes a User entity by its email.
     * @param email The email of the user to delete.
     */
    void deleteByEmail(String email);
}