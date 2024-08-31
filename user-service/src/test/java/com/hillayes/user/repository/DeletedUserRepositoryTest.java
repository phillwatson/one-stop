package com.hillayes.user.repository;

import com.hillayes.user.domain.DeletedUser;
import com.hillayes.user.domain.User;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static com.hillayes.user.utils.TestData.mockUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@TestTransaction
public class DeletedUserRepositoryTest {
    @Inject
    UserRepository userRepository;

    @Inject
    DeletedUserRepository fixture;

    @Test
    public void testDeleteUser() {
        // given: an existing user
        User existingUser = userRepository.save(mockUser());

        // and: the data is flushed and the cache is cleared
        fixture.flush();
        fixture.clearCache();

        // and: the user is located
        DeletedUser result = userRepository.findByIdOptional(existingUser.getId())
            .map(user -> {
                // when the user is copied to the deleted user table
                DeletedUser deletedUser = fixture.save(DeletedUser.fromUser(user));
                userRepository.delete(user);

                return deletedUser;
            }).orElse(null);

        // then: the deleted copy matches the original
        assertNotNull(result);
        assertEquals(existingUser.getId(), result.getId());
        assertEquals(existingUser.getVersion(), result.getVersion());
    }
}
