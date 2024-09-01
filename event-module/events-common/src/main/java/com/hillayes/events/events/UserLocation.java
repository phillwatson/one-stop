package com.hillayes.events.events;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.*;

/**
 * A collection of data, taken from the user's browser, regarding the user's
 * location. This can only be a best-guess and should not be assumed to be
 * accurate.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@ToString
@RegisterForReflection
public class UserLocation {
    private String ip;
    private String city;
    private String country;
    private String latitude;
    private String longitude;

    /**
     * A factory method to extract the user's location from the HTTP request headers.
     *
     * @param headers the HTTP request headers.
     * @return the extracted UserLocation instance.
     */
    public static UserLocation fromHeaders(MultivaluedMap<String, String> headers) {
        return UserLocation.builder()
            .ip(headers.getFirst("X-Location-IP"))
            .city(headers.getFirst("X-Location-City"))
            .country(headers.getFirst("X-Location-Country"))
            .latitude(headers.getFirst("X-Location-Lat"))
            .longitude(headers.getFirst("X-Location-Long"))
            .build();
    }
}
