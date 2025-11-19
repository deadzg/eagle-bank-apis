package com.eaglebank.api.helper;

import com.eaglebank.api.beans.AccountType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CaseInsensitiveAccountTypeDeserializer.
 *
 * These tests verify that AccountType values are parsed case-insensitively,
 * that unknown values cause a Jackson mapping error, and that explicit nulls
 * deserialize to null.
 */
public class CaseInsensitiveAccountTypeDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    // Small wrapper bean used only for testing deserialization of the AccountType field.
    static class Wrapper {

        @JsonDeserialize(using = CaseInsensitiveAccountTypeDeserializer.class) private AccountType type;

        public AccountType getType() {
            return type;
        }

        public void setType(AccountType type) {
            this.type = type;
        }
    }

    @Test void deserialize_lowercase_matchesEnum() throws Exception {
        String json = "{\"type\":\"savings\"}";

        Wrapper w = mapper.readValue(json, Wrapper.class);

        assertNotNull(w);
        assertEquals(AccountType.SAVINGS, w.getType());
    }

    @Test void deserialize_mixedcase_matchesEnum() throws Exception {
        String json = "{\"type\":\"ChEckInG\"}";

        Wrapper w = mapper.readValue(json, Wrapper.class);

        assertNotNull(w);
        assertEquals(AccountType.CHECKING, w.getType());
    }

    @Test void deserialize_uppercase_matchesEnum() throws Exception {
        String json = "{\"type\":\"SAVINGS\"}";

        Wrapper w = mapper.readValue(json, Wrapper.class);

        assertNotNull(w);
        assertEquals(AccountType.SAVINGS, w.getType());
    }

    @Test void deserialize_null_returnsNull() throws Exception {
        String json = "{\"type\":null}";

        Wrapper w = mapper.readValue(json, Wrapper.class);

        assertNotNull(w);
        assertNull(w.getType());
    }

    @Test void deserialize_unknown_throwsJsonMappingException() {
        String json = "{\"type\":\"unknown-type\"}";

        assertThrows(JsonMappingException.class, () -> mapper.readValue(json, Wrapper.class));
    }
}
