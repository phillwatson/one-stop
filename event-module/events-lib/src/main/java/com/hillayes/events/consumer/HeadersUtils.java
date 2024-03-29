package com.hillayes.events.consumer;

import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class HeadersUtils {
    public final static String REASON_HEADER = "dead-letter-reason";
    public final static String CAUSE_HEADER = "dead-letter-cause";
    public final static String CONSUMER_HEADER = "dead-letter-consumer";
    public final static String SCHEDULE_HEADER = "schedule-for";

    public static Optional<String> getHeader(org.apache.kafka.common.header.Headers headers, String key) {
        Header header = headers.lastHeader(key);
        return ((header == null) || (header.value() == null))
            ? Optional.empty()
            : Optional.of(new String(header.value(), StandardCharsets.UTF_8));
    }
}
