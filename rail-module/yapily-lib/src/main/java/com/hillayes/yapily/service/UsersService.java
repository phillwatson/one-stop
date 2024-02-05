package com.hillayes.yapily.service;

import com.hillayes.yapily.api.UsersApi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.UUID;

@ApplicationScoped
@Slf4j
public class UsersService extends AbstractRailService {
    @Inject
    @RestClient
    UsersApi usersApi;

    /**
     * Deletes the rail user record with the given ID.
     * @param railUserId the ID of the user to be deleted.
     */
    public void deleteUser(UUID railUserId) {
        log.debug("Deleting user [id: {}]", railUserId);
        try {
            usersApi.deleteUser(railUserId);
        } catch (WebApplicationException e) {
            if (!isNotFound(e)) {
                throw e;
            }
        }
    }
}
