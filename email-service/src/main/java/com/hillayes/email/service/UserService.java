package com.hillayes.email.service;

import com.hillayes.email.domain.User;
import com.hillayes.email.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.util.UUID;

@ApplicationScoped
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    public User createUser(User user) {
        log.info("Creating user [id: {}, username: {}]", user.getId(), user.getUsername());
        return userRepository.save(user);
    }

    public void deleteUser(UUID userId) {
        log.info("Deleting user [id: {}]", userId);
        userRepository.deleteById(userId);
    }

    public User updateUser(User user) {
        log.info("Updating user [id: {}, username: {}]", user.getId(), user.getUsername());
        User update = userRepository.findById(user.getId())
            .orElse(user); // create a new record

        update.setUsername(user.getUsername());
        update.setEmail(user.getEmail());
        update.setGivenName(user.getGivenName());
        update.setFamilyName(user.getFamilyName());
        return userRepository.save(update);
    }
}
