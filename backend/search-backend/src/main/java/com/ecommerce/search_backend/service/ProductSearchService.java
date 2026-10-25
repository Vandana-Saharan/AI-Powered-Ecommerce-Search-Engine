package com.ecommerce.search_backend.service;

import com.ecommerce.search_backend.model.AiQueryTranslationResult;
import com.ecommerce.search_backend.model.AiSearchResponse;
import com.ecommerce.search_backend.model.Product;
import com.ecommerce.search_backend.repository.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ProductSearchService {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchService.class);

    private final ElasticsearchOperations operations;
    private final ProductRepository productRepository;
    private final GeminiQueryTranslatorService queryTranslatorService;
    private final ObjectMapper objectMapper;

    public ProductSearchService(
            ElasticsearchOperations operations,
            ProductRepository productRepository,
            GeminiQueryTranslatorService queryTranslatorService,
            ObjectMapper objectMapper) {

        this.operations = operations;
        this.productRepository = productRepository;
        this.queryTranslatorService = queryTranslatorService;
        this.objectMapper = objectMapper;
    }

    /**
     * AI-Powered Natural Language Search:
     * 1. Translates raw user query into Elasticsearch Query DSL via Gemini / LLM.
     * 2. Executes the Query DSL directly against Elasticsearch.
     * 3. If zero hits occur, falls back to a relaxed query while strictly preserving product-type and gender constraints.
     */
    public AiSearchResponse searchWithAi(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new AiSearchResponse(List.of(), text, "Empty query", null, "{}", false, 0);
        }

        String rawQuery = text.trim();
        AiQueryTranslationResult translation = queryTranslatorService.translateQuery(rawQuery);
        String dslToExecute = prepareQueryDsl(translation.getDslQuery());

        log.info("Executing AI Search for '{}' with DSL: {}", rawQuery, dslToExecute);

        List<Product> products = List.of();
        boolean fallbackUsed = false;

        try {
            StringQuery stringQuery = new StringQuery(dslToExecute, PageRequest.of(0, 20));
            SearchHits<Product> results = operations.search(stringQuery, Product.class);

            products = results.getSearchHits()
                    .stream()
                    .map(hit -> hit.getContent())
                    .toList();

            log.info("AI DSL Search returned {} results", products.size());

            // If 0 results returned, trigger resilient fallback that preserves essential product constraints
            if (products.isEmpty()) {
                log.info("Zero hits from AI DSL, triggering resilient fallback search preserving product constraints");
                products = searchWithRelaxedConstraints(rawQuery, translation);
                fallbackUsed = true;
            }
        } catch (Exception ex) {
            log.error("Error executing AI DSL query: {}. Fallback to resilient search.", ex.getMessage(), ex);
            products = searchWithRelaxedConstraints(rawQuery, translation);
            fallbackUsed = true;
        }

        return new AiSearchResponse(
                products,
                rawQuery,
                translation.getReasoning(),
                translation.getExtractedIntent(),
                translation.getDslQuery(),
                fallbackUsed,
                products.size()
        );
    }

    /**
     * Standard Search:
     * Translates the query using deterministic local semantic extraction to ensure strict
     * category, brand, and gender constraints, avoiding irrelevant product category pollution.
     */
    public List<Product> searchProducts(String text) {
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }

        String rawQuery = text.trim();
        AiQueryTranslationResult translation = queryTranslatorService.translateLocally(rawQuery);
        String dslToExecute = prepareQueryDsl(translation.getDslQuery());

        try {
            StringQuery stringQuery = new StringQuery(dslToExecute, PageRequest.of(0, 20));
            SearchHits<Product> results = operations.search(stringQuery, Product.class);

            List<Product> products = results.getSearchHits()
                    .stream()
                    .map(hit -> hit.getContent())
                    .toList();

            if (!products.isEmpty()) {
                return products;
            }

            // Fallback while preserving product-type and gender constraints
            return searchWithRelaxedConstraints(rawQuery, translation);
        } catch (Exception ex) {
            log.error("Error executing search for '{}': {}", rawQuery, ex.getMessage(), ex);
            return searchWithRelaxedConstraints(rawQuery, translation);
        }
    }

    /**
     * Resilient Fallback Search:
     * Never removes essential product-type (category) or gender constraints.
     * Relaxes strict price or exact title match restrictions to provide relevant suggestions
     * within the user's intended product domain.
     */
    @SuppressWarnings("unchecked")
    public List<Product> searchWithRelaxedConstraints(String rawQuery, AiQueryTranslationResult translation) {
        try {
            Map<String, Object> intent = translation != null ? translation.getExtractedIntent() : Map.of();
            List<String> categories = intent != null && intent.get("categories") instanceof List<?>
                    ? (List<String>) intent.get("categories")
                    : List.of();
            String gender = intent != null && intent.get("gender") instanceof String
                    ? (String) intent.get("gender")
                    : null;

            Map<String, Object> bool = new LinkedHashMap<>();
            List<Map<String, Object>> filterList = new ArrayList<>();
            List<Map<String, Object>> shouldList = new ArrayList<>();

            // Strictly preserve category constraint if present
            if (!categories.isEmpty()) {
                filterList.add(Map.of("terms", Map.of("category", categories)));
                shouldList.add(Map.of("terms", Map.of("category", categories, "boost", 1.0)));
            }

            // Strictly preserve gender constraint if present
            if (gender != null) {
                filterList.add(Map.of("terms", Map.of("gender", List.of(gender, "unisex"))));
            }

            // Broad keyword matching across relevant textual fields
            shouldList.add(Map.of(
                    "multi_match", Map.of(
                            "query", rawQuery,
                            "fields", List.of("title^3", "tags^2", "brand^2", "description^1"),
                            "fuzziness", "AUTO",
                            "prefix_length", 2,
                            "boost", 2.0
                    )
            ));

            if (!filterList.isEmpty()) {
                bool.put("filter", filterList);
            }
            bool.put("should", shouldList);
            bool.put("minimum_should_match", 1);

            Map<String, Object> esQuery = Map.of("bool", bool);
            String dsl = objectMapper.writeValueAsString(esQuery);

            StringQuery fallbackQuery = new StringQuery(dsl, PageRequest.of(0, 20));
            SearchHits<Product> results = operations.search(fallbackQuery, Product.class);

            return results.getSearchHits()
                    .stream()
                    .map(hit -> hit.getContent())
                    .toList();
        } catch (Exception e) {
            log.error("Failed to execute relaxed fallback search: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Ensures the JSON string passed to StringQuery contains the query part.
     */
    private String prepareQueryDsl(String rawDsl) {
        try {
            JsonNode root = objectMapper.readTree(rawDsl);
            if (root.has("query")) {
                return objectMapper.writeValueAsString(root.get("query"));
            }
            return rawDsl;
        } catch (Exception e) {
            log.warn("Could not parse DSL JSON: {}. Using as-is.", rawDsl);
            return rawDsl;
        }
    }

    public Optional<Product> getProductById(Integer id) {
        return productRepository.findById(id);
    }
}
