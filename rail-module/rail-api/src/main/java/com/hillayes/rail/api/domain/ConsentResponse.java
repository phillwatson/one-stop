package com.hillayes.rail.api.domain;

import com.hillayes.commons.Strings;
import lombok.*;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConsentResponse {
    @EqualsAndHashCode.Include
    private String consentReference;
    private String errorCode;
    private String errorDescription;

    /**
     * Tests whether this response is an error.
     * @return true if this response is an error.
     */
    public boolean isError() {
        return Strings.isNotBlank(errorCode);
    }
}
