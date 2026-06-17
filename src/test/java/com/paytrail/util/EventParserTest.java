package com.paytrail.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EventParserTest {
    private final EventParser parser = new EventParser(new ObjectMapper());

    @Test
    void extractsEventReferenceAndMerchant() {
        String json = "{\"event\":\"charge.success\",\"data\":{\"reference\":\"ref_1\",\"metadata\":{\"merchant_id\":\"m1\"}}}";
        EventParser.ParsedEvent p = parser.parse(json);
        assertEquals("charge.success", p.event());
        assertEquals("ref_1", p.reference());
        assertEquals("m1", p.merchantId());
    }

    @Test
    void defaultsMerchantToUnknown() {
        String json = "{\"event\":\"charge.success\",\"data\":{\"reference\":\"ref_2\"}}";
        assertEquals("UNKNOWN", parser.parse(json).merchantId());
    }
}
