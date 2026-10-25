package com.ecommerce.search_backend.service;

import com.ecommerce.search_backend.model.AiQueryTranslationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GeminiQueryTranslatorServiceTest {

    private GeminiQueryTranslatorService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        service = new GeminiQueryTranslatorService(objectMapper);
    }

    @Test
    public void testPartyWearForMenTranslation() throws Exception {
        AiQueryTranslationResult result = service.translateQuery("party wear for men");

        assertNotNull(result);
        assertNotNull(result.getExtractedIntent());
        assertEquals("male", result.getExtractedIntent().get("gender"));

        @SuppressWarnings("unchecked")
        List<String> categories = (List<String>) result.getExtractedIntent().get("categories");
        assertTrue(categories.contains("mens-shirts"));

        String dsl = result.getDslQuery();
        assertNotNull(dsl);

        JsonNode root = objectMapper.readTree(dsl);
        assertTrue(root.has("bool"));
        JsonNode boolNode = root.get("bool");

        assertTrue(boolNode.has("filter"));
        assertTrue(boolNode.has("should"));
        assertEquals(1, boolNode.get("minimum_should_match").asInt());

        // Ensure fuzziness AUTO is included in should clauses
        String boolString = boolNode.toString();
        assertTrue(boolString.contains("AUTO"));
    }

    @Test
    public void testShoesForMenStrictFiltering() throws Exception {
        AiQueryTranslationResult result = service.translateQuery("shoes for men");

        assertNotNull(result);
        assertEquals("male", result.getExtractedIntent().get("gender"));

        @SuppressWarnings("unchecked")
        List<String> categories = (List<String>) result.getExtractedIntent().get("categories");
        assertEquals(List.of("mens-shoes"), categories);

        String dsl = result.getDslQuery();
        JsonNode root = objectMapper.readTree(dsl);
        JsonNode boolNode = root.get("bool");

        assertTrue(boolNode.has("filter"));
        JsonNode filter = boolNode.get("filter");
        String filterString = filter.toString();
        assertTrue(filterString.contains("mens-shoes"));
        assertTrue(filterString.contains("male"));
    }

    @Test
    public void testRedRunningShoesCategories() throws Exception {
        AiQueryTranslationResult result = service.translateQuery("red running shoes");

        assertNotNull(result);
        @SuppressWarnings("unchecked")
        List<String> categories = (List<String>) result.getExtractedIntent().get("categories");
        assertTrue(categories.contains("mens-shoes"));
        assertTrue(categories.contains("womens-shoes"));

        String dsl = result.getDslQuery();
        JsonNode root = objectMapper.readTree(dsl);
        JsonNode boolNode = root.get("bool");

        assertTrue(boolNode.has("filter"));
        String filterString = boolNode.get("filter").toString();
        assertTrue(filterString.contains("mens-shoes"));
        assertTrue(filterString.contains("womens-shoes"));

        String shouldString = boolNode.get("should").toString();
        assertTrue(shouldString.contains("red running"));
    }

    @Test
    public void testApplePhonesUnder2000() throws Exception {
        AiQueryTranslationResult result = service.translateQuery("Apple phones under 2000");

        assertNotNull(result);
        assertEquals("Apple", result.getExtractedIntent().get("brand"));
        assertEquals(2000.0, (Double) result.getExtractedIntent().get("max_price"), 0.001);

        @SuppressWarnings("unchecked")
        List<String> categories = (List<String>) result.getExtractedIntent().get("categories");
        assertEquals(List.of("smartphones"), categories);

        String dsl = result.getDslQuery();
        JsonNode root = objectMapper.readTree(dsl);
        JsonNode boolNode = root.get("bool");

        assertTrue(boolNode.has("filter"));
        String filterString = boolNode.get("filter").toString();
        assertTrue(filterString.contains("smartphones"));
        assertTrue(filterString.contains("Apple"));
        assertTrue(filterString.contains("2000"));
    }

    @Test
    public void testWomensDresses() throws Exception {
        AiQueryTranslationResult result = service.translateQuery("women's dresses");

        assertNotNull(result);
        assertEquals("female", result.getExtractedIntent().get("gender"));

        @SuppressWarnings("unchecked")
        List<String> categories = (List<String>) result.getExtractedIntent().get("categories");
        assertTrue(categories.contains("womens-dresses"));
        assertTrue(categories.contains("tops"));

        String dsl = result.getDslQuery();
        JsonNode root = objectMapper.readTree(dsl);
        JsonNode boolNode = root.get("bool");

        assertTrue(boolNode.has("filter"));
        String filterString = boolNode.get("filter").toString();
        assertTrue(filterString.contains("womens-dresses"));
        assertTrue(filterString.contains("female"));
    }

    @Test
    public void testLuxuryWatches() throws Exception {
        AiQueryTranslationResult result = service.translateQuery("luxury watches");

        assertNotNull(result);
        @SuppressWarnings("unchecked")
        List<String> categories = (List<String>) result.getExtractedIntent().get("categories");
        assertTrue(categories.contains("mens-watches"));
        assertTrue(categories.contains("womens-watches"));

        String dsl = result.getDslQuery();
        JsonNode root = objectMapper.readTree(dsl);
        JsonNode boolNode = root.get("bool");

        assertTrue(boolNode.has("filter"));
        String filterString = boolNode.get("filter").toString();
        assertTrue(filterString.contains("mens-watches"));
        assertTrue(filterString.contains("womens-watches"));
    }

    @Test
    public void testPriceAndBrandTranslation() throws Exception {
        AiQueryTranslationResult result = service.translateQuery("Nike shoes under 100");

        assertNotNull(result);
        assertEquals("Nike", result.getExtractedIntent().get("brand"));
        assertEquals(100.0, (Double) result.getExtractedIntent().get("max_price"), 0.001);

        String dsl = result.getDslQuery();
        JsonNode root = objectMapper.readTree(dsl);
        JsonNode boolNode = root.get("bool");

        assertTrue(boolNode.has("filter"));
        assertTrue(boolNode.get("filter").toString().contains("100.0") || boolNode.get("filter").toString().contains("100"));
    }

    @Test
    public void testParseModelResponseWithMarkdown() {
        String mockLlmJson = """
                ```json
                {
                  "reasoning": "Identified formal male occasion wear",
                  "extracted_intent": {
                    "gender": "male",
                    "categories": ["mens-shirts"],
                    "brand": null,
                    "max_price": null
                  },
                  "es_query": {
                    "bool": {
                      "filter": [
                        {"terms": {"gender": ["male", "unisex"]}},
                        {"terms": {"category": ["mens-shirts"]}}
                      ],
                      "should": [
                        {"terms": {"category": ["mens-shirts"], "boost": 3.0}},
                        {"multi_match": {"query": "party wear", "fields": ["title^3", "description"], "fuzziness": "AUTO"}}
                      ],
                      "minimum_should_match": 1
                    }
                  }
                }
                ```
                """;

        AiQueryTranslationResult parsed = service.parseModelResponse(mockLlmJson);
        assertNotNull(parsed);
        assertEquals("Identified formal male occasion wear", parsed.getReasoning());
        assertEquals("male", parsed.getExtractedIntent().get("gender"));
        assertTrue(parsed.getDslQuery().contains("mens-shirts"));
        assertTrue(parsed.getDslQuery().contains("AUTO"));
    }
}
