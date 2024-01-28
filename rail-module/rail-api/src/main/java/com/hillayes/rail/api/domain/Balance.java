package com.hillayes.rail.api.domain;

import com.hillayes.commons.MonetaryAmount;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class Balance {
    private LocalDate dateTime;
    private MonetaryAmount amount;
    private String type;
}
