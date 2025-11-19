package com.eaglebank.api.helper;

import com.eaglebank.api.beans.AccountType;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Custom Jackson Deserializer to handle case-insensitive mapping of input
 * strings to the AccountType enum by converting the input to uppercase.
 */
public class CaseInsensitiveAccountTypeDeserializer extends JsonDeserializer<AccountType> {

    @Override
    public AccountType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value != null) {
            try {
                // Convert the input string to uppercase before attempting to match the enum name
                return AccountType.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Throw a clear exception if the type is invalid
                String validValues = getValidEnumValues();
                throw ctxt.weirdStringException(value, AccountType.class,
                        "Invalid account type. Valid types are: " + validValues);
            }
        }
        return null;
    }

    /**
     * Helper method to list all valid enum names for the error message.
     */
    private String getValidEnumValues() {
        StringBuilder sb = new StringBuilder();
        for (AccountType type : AccountType.values()) {
            sb.append(type.name()).append(", ");
        }
        // Remove trailing ", "
        return sb.substring(0, sb.length() - 2);
    }
}