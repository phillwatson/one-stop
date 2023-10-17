package com.hillayes.executors.scheduler;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.kagkarlsson.scheduler.serializer.Serializer;
import com.hillayes.commons.json.MapperFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TaskDataSerializer implements Serializer {
    private final ObjectReader objectReader;
    private final ObjectWriter objectWriter;

    public TaskDataSerializer() {
        this.objectReader = MapperFactory.readerFor(JobbingTaskData.class);
        this.objectWriter = MapperFactory.writerFor(JobbingTaskData.class);
    }

    @Override
    public byte[] serialize(Object data) {
        if (data == null) {
            return null;
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            objectWriter.writeValue(output, data);
            return output.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T deserialize(Class<T> aClass, byte[] data) {
        if (data == null) {
            return null;
        }

        try (InputStream is = new ByteArrayInputStream(data)) {
            return objectReader.readValue(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
