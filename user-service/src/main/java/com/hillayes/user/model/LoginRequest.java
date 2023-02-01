package com.hillayes.user.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Setter(AccessLevel.PROTECTED)
@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class LoginRequest {
    private String username;

    private String password;
}
