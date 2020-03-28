package ca.vanzyl.concord.plugins;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ImmutablesYamlMapper
{

    private final ObjectMapper mapper;

    public ImmutablesYamlMapper() {
        mapper = new ObjectMapper(new YAMLFactory());
        mapper.registerModule(new GuavaModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    public <T> T read(File input, Class<T> clazz) throws IOException {
        return mapper.readValue(input, clazz);
    }

    public <T> T read(InputStream input, Class<T> clazz) throws IOException {
        return mapper.readValue(input, clazz);
    }

    public <T> T read(String input, Class<T> clazz) throws IOException {
        return mapper.readValue(input, clazz);
    }

    public <T> T read(byte[] input, Class<T> clazz) throws IOException {
        return mapper.readValue(input, clazz);
    }

    public <T> String write(T instance) throws IOException {
        return mapper.writeValueAsString(instance);
    }
}
