package com.lockedin.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.lockedin.JpaModelTest;
import com.lockedin.models.User;

@JpaModelTest
class UserRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    UserRepository users;

    @Test
    void findsAUserByEmail() {
        User user = em.persistAndFlush(new User("lifter@example.com", "hash"));

        assertThat(users.findByEmail("lifter@example.com")).hasValueSatisfying(
            found -> assertThat(found.getId()).isEqualTo(user.getId()));
        assertThat(users.findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void tellsWhetherAnEmailIsTaken() {
        em.persistAndFlush(new User("taken@example.com", "hash"));

        assertThat(users.existsByEmail("taken@example.com")).isTrue();
        assertThat(users.existsByEmail("free@example.com")).isFalse();
    }
}
