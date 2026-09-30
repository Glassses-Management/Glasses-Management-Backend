package com.example.spring_boot_project_api.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Opening stock is a count, so a fractional value has to be reported back to the
 * caller rather than quietly rounded. Jackson converts a JSON float into an
 * Integer field by default, which is why the quantity field carries a deserializer
 * of its own. These tests pin that behaviour down.
 *
 * The mapper here is deliberately Jackson 3 (tools.jackson), the same JSON stack
 * Spring Boot 4 uses at runtime. An earlier version of this test built a Jackson 2
 * mapper instead; it passed while the real endpoint still truncated 2.5 to 2,
 * because the annotation being verified belonged to Jackson 2 and Jackson 3
 * ignores it. A test of this kind has to exercise the stack the server runs on.
 */
class ProductRequestQuantityTest {

    private final ObjectMapper mapper = JsonMapper.builder().build();

    private static final String BASE = """
            {"sku":"S-1","model":"M","brand":"B","category":"C",
             "cost_price":1,"sale_price":2%s}
            """;

    private static String body(String quantityJson) {
        return String.format(BASE, quantityJson);
    }

    // Jackson 3 dropped the readValue(String, Class) overload, so a String is read
    // through an ObjectReader instead.
    private ProductRequest read(String quantityJson) {
        return mapper.readerFor(ProductRequest.class).readValue(body(quantityJson));
    }

    @Test
    @DisplayName("a whole number is read as the quantity")
    void readsWholeNumber() {
        assertThat(read(",\"quantity\":7").getQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("a numeric string is still accepted, because form fields arrive as strings")
    void readsNumericString() {
        assertThat(read(",\"quantity\":\"7\"").getQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("an absent quantity stays null so the product is created without stock")
    void absentQuantityIsNull() {
        assertThat(read("").getQuantity()).isNull();
    }

    @Test
    @DisplayName("an explicit null quantity stays null")
    void nullQuantityIsNull() {
        assertThat(read(",\"quantity\":null").getQuantity()).isNull();
    }

    @Test
    @DisplayName("a fractional number is rejected instead of being truncated to a whole one")
    void rejectsFractionalNumber() {
        // Without the field-level deserializer Jackson would store this as 2.
        assertThatThrownBy(() -> read(",\"quantity\":2.5"))
                .hasMessageContaining("whole number");
    }

    @Test
    @DisplayName("a fractional number in a string is rejected too")
    void rejectsFractionalString() {
        assertThatThrownBy(() -> read(",\"quantity\":\"2.5\""))
                .hasMessageContaining("whole number");
    }

    @Test
    @DisplayName("a non-numeric string is rejected")
    void rejectsNonNumericString() {
        assertThatThrownBy(() -> read(",\"quantity\":\"many\""))
                .hasMessageContaining("whole number");
    }

    @Test
    @DisplayName("a whole number written with a decimal point is rejected too")
    void rejectsIntegralFloat() {
        // 7.0 holds no fraction, but accepting it would mean deciding how many
        // decimal places count as "whole". Rejecting every float keeps the rule
        // easy to state: send a number with no decimal point.
        assertThatThrownBy(() -> read(",\"quantity\":7.0"))
                .hasMessageContaining("whole number");
    }
}
