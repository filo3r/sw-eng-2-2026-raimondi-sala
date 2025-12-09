package it.polimi.se.bbp.repository;

import it.polimi.se.bbp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository interface for User entity.
 * Provides database access methods for users.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds user by email.
     * @param email email to search for
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if username exists.
     * @param username username to check
     * @return true if username exists, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Checks if email exists.
     * @param email email to check
     * @return true if email exists, false otherwise
     */
    boolean existsByEmail(String email);

}