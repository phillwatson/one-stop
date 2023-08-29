package com.hillayes.integration.test.util;

import lombok.*;

import java.util.Map;

@Builder()
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Setter
public class UserEntity {
    private String username;
    private String givenName;
    private String emailAddress;
    private String password;
    private Map<String,String> authTokens;
}
