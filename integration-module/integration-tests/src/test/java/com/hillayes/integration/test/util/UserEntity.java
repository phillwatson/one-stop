package com.hillayes.integration.test.util;

import lombok.*;

import java.util.Map;
import java.util.UUID;

@Builder()
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Setter
public class UserEntity {
    private UUID id;
    private String username;
    private String givenName;
    private String email;
    private String password;
    private Map<String,String> authTokens;
}
