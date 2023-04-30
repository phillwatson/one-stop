package com.hillayes.events.consumer;

import com.hillayes.events.domain.EventPacket;

public interface EventConsumer<T, E extends Exception> {
    public void consume(T eventPacket) throws E;
}
