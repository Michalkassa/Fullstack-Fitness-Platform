package com.lockedin.services;

import static com.lockedin.services.StatusAssertions.assertStatus;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.lockedin.JpaModelTest;
import com.lockedin.api.dto.LoginRequest;
import com.lockedin.api.dto.RegisterRequest;
import com.lockedin.api.dto.UserResponse;
import com.lockedin.api.dto.UserStatsResponse;
import com.lockedin.models.User;

@JpaModelTest
@Import({UserService.class, BCryptPasswordEncoder.class})
class UserServiceTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    UserService service;

    @Autowired
    PasswordEncoder encoder;

    @Test
    void registerStoresTheEmailTrimmedAndLowerCasedAndHashesThePassword() {
        UserResponse user = service.register(new RegisterRequest("  Lifter@Example.COM ", "correct-horse"));

        assertThat(user.id()).isNotNull();
        assertThat(user.email()).isEqualTo("lifter@example.com");
        assertThat(user.name()).isNull();
        assertThat(user.image()).isNull();
        assertThat(user.xp()).isZero();
        String hash = em.find(User.class, user.id()).getPasswordHash();
        assertThat(hash).isNotEqualTo("correct-horse");
        assertThat(encoder.matches("correct-horse", hash)).isTrue();
    }

    @Test
    void registerRejectsAnEmailThatIsAlreadyTakenWhateverTheCase() {
        service.register(new RegisterRequest("lifter@example.com", "correct-horse"));

        assertStatus(HttpStatus.CONFLICT, "An account with this email already exists",
            () -> service.register(new RegisterRequest(" LIFTER@example.com", "another-pass")));
    }

    @Test
    void authenticateReturnsTheUserForTheRightPassword() {
        UserResponse registered = service.register(new RegisterRequest("lifter@example.com", "correct-horse"));

        UserResponse user = service.authenticate(new LoginRequest(" Lifter@Example.com ", "correct-horse"));

        assertThat(user).isEqualTo(registered);
    }

    @Test
    void authenticateGivesTheSameErrorForAWrongPasswordAndAnUnknownEmail() {
        service.register(new RegisterRequest("lifter@example.com", "correct-horse"));

        assertStatus(HttpStatus.UNAUTHORIZED, "Email or password is incorrect",
            () -> service.authenticate(new LoginRequest("lifter@example.com", "wrong-horse")));
        assertStatus(HttpStatus.UNAUTHORIZED, "Email or password is incorrect",
            () -> service.authenticate(new LoginRequest("nobody@example.com", "correct-horse")));
    }

    @Test
    void getReturnsTheUser() {
        User user = new User("me@example.com", "hash");
        user.setName("Me");
        user.addXp(40);
        em.persistAndFlush(user);

        assertThat(service.get(user.getId()))
            .isEqualTo(new UserResponse(user.getId(), "me@example.com", "Me", null, 40));
    }

    @Test
    void getForAUserThatNoLongerExistsIsUnauthorized() {
        assertStatus(HttpStatus.UNAUTHORIZED, () -> service.get(UUID.randomUUID()));
    }

    @Test
    void statsComputesTheLevelFromXp() {
        User user = new User("stats@example.com", "hash");
        user.addXp(150);
        em.persistAndFlush(user);

        assertThat(service.stats(user.getId())).isEqualTo(new UserStatsResponse(150, 2, 50, 200, 25));
    }
}
