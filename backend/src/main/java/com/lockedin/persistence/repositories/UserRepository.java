package com.lockedin.persistence.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lockedin.models.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** Emails are stored trimmed and lower-cased; pass the normalised form. */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
