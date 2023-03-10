package com.hillayes.user.repository;

import com.hillayes.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public interface UserRepository extends JpaRepository<User, UUID> {
    /**
     * Returns the user with the given username. Usernames are unique, even across
     * deleted users.
     *
     * @param username the username to search for.
     * @return the user record found with the given username, if any.
     */
    public Optional<User> findByUsername(String username);

    /**
     * Returns all users holding the given email address. Although only one user
     * can be "active" for a given email address, users marked as deleted may share
     * duplicate email addresses.
     *
     * @param email the email address to be searched.
     * @return the collection of users holding the given email address.
     */
    public List<User> findByEmail(String email);
}
