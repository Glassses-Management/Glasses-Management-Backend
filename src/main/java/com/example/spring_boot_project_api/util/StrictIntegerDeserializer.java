package com.example.spring_boot_project_api.util;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * Reads a JSON number into an Integer without rounding it away.
 *
 * Jackson accepts a fractional JSON number for an Integer field by default and
 * truncates it, so a request carrying {"quantity": 2.5} was quietly stored as 2.
 * A count of items cannot be fractional, so truncating it hides a mistake in the
 * request instead of reporting it. This deserializer rejects the value so the
 * caller gets a validation error naming the field and can correct it.
 *
 * Written against the Jackson 3 API (tools.jackson). This project runs Spring
 * Boot 4, whose JSON layer is Jackson 3; the older com.fasterxml.jackson
 * databind annotations sit on the classpath as well and are silently ignored by
 * Jackson 3, so an annotation from that package would appear to compile and then
 * do nothing.
 *
 * Applied per field with @JsonDeserialize rather than by turning
 * ACCEPT_FLOAT_AS_INT off globally, so no other endpoint changes behaviour.
 */
public class StrictIntegerDeserializer extends ValueDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        JsonToken token = parser.currentToken();

        // A JSON string such as "5" is still accepted: form fields arrive as
        // strings often enough that rejecting them would be needlessly strict.
        if (token == JsonToken.VALUE_STRING) {
            String text = parser.getString().trim();
            if (text.isEmpty()) {
                return null;
            }
            try {
                return Integer.valueOf(text);
            }
            catch (NumberFormatException ex) {
                throw invalid(parser.currentName(), text, parser);
            }
        }

        if (token == JsonToken.VALUE_NUMBER_INT) {
            return parser.getIntValue();
        }

        if (token == JsonToken.VALUE_NUMBER_FLOAT) {
            throw invalid(parser.currentName(), parser.getString(), parser);
        }

        if (token == JsonToken.VALUE_NULL) {
            return null;
        }

        return (Integer) context.handleUnexpectedToken(Integer.class, parser);
    }

    private static InvalidFormatException invalid(String field, String value, JsonParser parser) {
        return InvalidFormatException.from(parser,
                "\"" + field + "\" must be a whole number, got: " + value,
                value, Integer.class);
    }
}
