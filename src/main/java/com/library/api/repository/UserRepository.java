package com.library.api.repository;

import com.library.api.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// Spring Data JPA generates SQL queries automatically based on method names
// No need to write SQL manually — Spring reads the method name and builds the query
public interface UserRepository extends JpaRepository<AppUser, Long> {

    // Equivalent to: SELECT * FROM users WHERE username = ?
    Optional<AppUser> findByUsername(String username);

    // Used to check if a username is already taken during registration
    boolean existsByUsername(String username);
}
