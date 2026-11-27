package com.lockedin.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.lockedin.JpaModelTest;

@JpaModelTest
class UserTest {

    @Autowired
    TestEntityManager em;

    @Test
    void persistsAndReadsBackAllFields() {
        User user = new User("ada@example.com", "hash");
        user.setName("Ada");
        user.setImage("https://example.com/ada.png");

        User saved = em.persistFlushFind(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("ada@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hash");
        assertThat(saved.getName()).isEqualTo("Ada");
        assertThat(saved.getImage()).isEqualTo("https://example.com/ada.png");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void nameAndImageAreOptional() {
        User saved = em.persistFlushFind(new User("bob@example.com", "hash"));

        assertThat(saved.getName()).isNull();
        assertThat(saved.getImage()).isNull();
    }

    @Test
    void startsWithZeroXp() {
        User saved = em.persistFlushFind(new User("new@example.com", "hash"));

        assertThat(saved.getXp()).isZero();
    }

    @Test
    void addXpAccumulates() {
        User user = new User("xp@example.com", "hash");

        user.addXp(10);
        user.addXp(25);

        assertThat(user.getXp()).isEqualTo(35);
    }

    @Test
    void addXpRejectsNegativeAmounts() {
        User user = new User("xp@example.com", "hash");

        assertThatThrownBy(() -> user.addXp(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emailIsUnique() {
        em.persistAndFlush(new User("dup@example.com", "hash"));

        assertThatThrownBy(() -> em.persistAndFlush(new User("dup@example.com", "other")))
            .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void requiresEmailAndPasswordHash() {
        assertThatThrownBy(() -> new User(null, "hash")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new User("a@example.com", null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void updatedAtMovesForwardOnUpdate() throws InterruptedException {
        User user = em.persistFlushFind(new User("upd@example.com", "hash"));
        var createdAt = user.getCreatedAt();
        var before = user.getUpdatedAt();

        Thread.sleep(5);
        user.setName("Renamed");
        em.flush();
        em.clear();

        User reloaded = em.find(User.class, user.getId());
        assertThat(reloaded.getName()).isEqualTo("Renamed");
        assertThat(reloaded.getUpdatedAt()).isAfter(before);
        assertThat(reloaded.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void systemUserIsSeeded() {
        User system = em.getEntityManager()
            .createQuery("select u from User u where u.email = :email", User.class)
            .setParameter("email", "system@lockedin.internal")
            .getSingleResult();

        assertThat(system.getName()).isEqualTo("LockedIn");
    }
}
