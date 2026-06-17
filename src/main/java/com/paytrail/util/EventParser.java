package com.paytrail.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;

public final class EventParser {

    public record ParsedEvent(String event, String reference, String merchantId, Document data) { }

    private final ObjectMapper mapper;

    public EventParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public ParsedEvent parse(String rawJson) {
        try {
            JsonNode root = mapper.readTree(rawJson);
            String event = text(root, "event");
            JsonNode data = root.path("data");
            String reference = text(data, "reference");
            String merchantId = data.path("metadata").path("merchant_id").asText("UNKNOWN");
            if (merchantId == null || merchantId.isBlank()) merchantId = "UNKNOWN";
            Document doc = data.isMissingNode() ? new Document() : Document.parse(mapper.writeValueAsString(data));
            return new ParsedEvent(event, reference, merchantId, doc);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed webhook payload", e);
        }
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }
}
