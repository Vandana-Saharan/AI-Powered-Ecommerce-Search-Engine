package com.ecommerce.search_backend.service;

import com.ecommerce.search_backend.model.AiQueryTranslationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeminiQueryTranslatorService {

    private static final Logger log = LoggerFactory.getLogger(GeminiQueryTranslatorService.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${gemini.api.key:${GEMINI_API_KEY:}}")
    private String geminiApiKey;

    @Value("${gemini.api.model:${GEMINI_MODEL:gemini-3.8-flash}}")
    private String geminiModel;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiApiUrl;

    // Allowed catalog categories
    public static final List<String> CATEGORIES = List.of(
            "beauty", "fragrances", "furniture", "groceries", "home-decoration",
            "kitchen-accessories", "laptops", "mens-shirts", "mens-shoes", "mens-watches",
            "mobile-accessories", "motorcycle", "skin-care", "smartphones", "sports-accessories",
            "sunglasses", "tablets", "tops", "vehicle", "womens-bags", "womens-dresses",
            "womens-jewellery", "womens-shoes", "womens-watches"
    );

    // Catalog brands
    public static final List<String> BRANDS = List.of(
            "Amazon", "Annibale Colombo", "Apple", "Asus", "Attitude", "Bath Trends", "Beats",
            "Calvin Klein", "Casual Comfort", "Chanel", "Chic Cosmetics", "Chrysler",
            "Classic Wear", "Comfort Trends", "Dell", "Dior", "Dodge", "Dolce & Gabbana",
            "Elegance Collection", "Essence", "Fashion Co.", "Fashion Diva", "Fashion Express",
            "Fashion Fun", "Fashion Gold", "Fashion Shades", "Fashion Timepieces", "Fashion Trends",
            "Fashionista", "Furniture Co.", "GadgetMaster", "Generic Motors", "Gigabyte",
            "Glamour Beauty", "Gucci", "Heshe", "Huawei", "IWC", "Kawasaki", "Knoll",
            "Lenovo", "Longines", "MotoGP", "Nail Couture", "Nike", "Off White", "Olay",
            "Oppo", "Pampi", "Prada", "ProVision", "Puma", "Realme", "Rolex", "Samsung",
            "ScootMaster", "SnapTech", "SpeedMaster", "TechGear", "Urban Chic", "Vaseline",
            "Velvet Touch", "Vivo"
    );

    private static final String SYSTEM_INSTRUCTION = """
            You are an expert E-Commerce Search Engine Query Translator for an online store.
            Your task is to transform a user's natural language search query into an Elasticsearch 8.x Query DSL JSON.

            CATALOG SCHEMA & VOCABULARY:
            - Index: "products"
            - Allowed categories (EXACT strings):
              [beauty, fragrances, furniture, groceries, home-decoration, kitchen-accessories,
               laptops, mens-shirts, mens-shoes, mens-watches, mobile-accessories, motorcycle,
               skin-care, smartphones, sports-accessories, sunglasses, tablets, tops, vehicle,
               womens-bags, womens-dresses, womens-jewellery, womens-shoes, womens-watches]
            - Known Brands: [Apple, Nike, Puma, Rolex, Prada, Samsung, Oppo, Realme, Asus, Dell, Lenovo,
              Calvin Klein, Chanel, Dior, Dolce & Gabbana, Gucci, Off White, IWC, Longines, Fashion Express,
              Pampi, Elegance Collection, Heshe, Fashion Diva, Urban Chic, Classic Wear, SpeedMaster, TechGear]
            - Genders: ["male", "female", "unisex"]

            STRICT QUERY TRANSLATION RULES:
            1. STRICT CATEGORY FILTERING:
               - When the user query clearly targets a specific product type, you MUST put the matching catalog category/categories into a "filter" clause using {"terms": {"category": [...]}}.
               - Category mappings:
                 * "shoes for men", "men's sneakers", "men shoes", "men cleats" -> category: ["mens-shoes"], gender: ["male"]
                 * "women's shoes", "shoes for women", "heels", "slippers" -> category: ["womens-shoes"], gender: ["female"]
                 * "shoes", "running shoes", "sneakers" (no gender specified) -> category: ["mens-shoes", "womens-shoes"]
                 * "women's dresses", "dresses", "frocks", "gowns" -> category: ["womens-dresses", "tops"], gender: ["female"]
                 * "phones", "smartphones", "iphone", "cell phone" -> category: ["smartphones"]
                 * "watches", "luxury watches" -> category: ["mens-watches", "womens-watches"]
                 * "men's watches" -> category: ["mens-watches"], gender: ["male"]
                 * "men's shirts", "t-shirts", "shirts for men" -> category: ["mens-shirts"], gender: ["male"]
                 * "laptops", "notebook", "macbook" -> category: ["laptops"]
                 * "perfume", "fragrance", "cologne" -> category: ["fragrances"]
                 * "handbag", "purse", "women's bag" -> category: ["womens-bags"]
               - NEVER put category solely in "should" when product intent is clear! Placing it in "filter" guarantees zero irrelevant categories in search results.
            2. GENDER FILTERING:
               - If query targets men ("men", "man", "male", "guy", "boys"): filter by gender: ["male", "unisex"].
               - If query targets women ("women", "woman", "female", "girl", "lady"): filter by gender: ["female", "unisex"].
            3. BRAND & PRICE FILTERING:
               - If a brand is explicitly mentioned (e.g. "Apple phones under 2000", "Nike shoes"): filter by brand: "Apple" or "Nike".
               - If price bounds are specified ("under X", "below X", "less than X"): filter by range: { "price": { "lte": X } }.
               - "above X", "over X": filter by range: { "price": { "gte": X } }.
            4. RELEVANCE RANKING ("should" clauses):
               - In "should", include {"terms": {"category": [...], "boost": 1.0}} so all valid filtered items can match.
               - Boost exact title matches using "match_phrase" with high boost (e.g. 8.0).
               - Score descriptive non-filter keywords (e.g. "red", "running", "luxury", "party wear") on "title^3", "tags^2", and "description^1".
               - Use "fuzziness": "AUTO" with "prefix_length": 2 ONLY on descriptive keywords for typo tolerance. Never fuzzy-match stopwords ("for", "under", "in", "with").
               - Set "minimum_should_match": 1.

            OUTPUT FORMAT:
            You MUST return a single JSON object with exact keys:
            {
              "reasoning": "Brief explanation",
              "extracted_intent": {
                "gender": "male"|"female"|"unisex"|null,
                "categories": ["matching category 1", ...],
                "brand": "matching brand"|null,
                "tags": ["matching tag 1", ...],
                "max_price": number|null,
                "min_price": number|null,
                "keywords": "search terms"
              },
              "es_query": {
                "bool": {
                  "filter": [...],
                  "should": [...],
                  "minimum_should_match": 1
                }
              }
            }
            Do not include markdown ticks or explanations outside the JSON object.
            """;

    public GeminiQueryTranslatorService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public AiQueryTranslationResult translateQuery(String userQuery) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            return buildDefaultMatchAll();
        }

        String trimmed = userQuery.trim();

        // 1. If Gemini API Key is configured, attempt real LLM call
        if (geminiApiKey != null && !geminiApiKey.trim().isEmpty()) {
            try {
                return callGeminiApi(trimmed);
            } catch (Exception ex) {
                log.warn("Gemini API call failed: {}. Falling back to internal semantic translator.", ex.getMessage());
            }
        } else {
            log.info("No GEMINI_API_KEY set. Using internal semantic translator with catalog taxonomy.");
        }

        // 2. Intelligent local semantic translator fallback
        return translateLocally(trimmed);
    }

    private AiQueryTranslationResult callGeminiApi(String userQuery) throws Exception {
        String url = String.format("%s/%s:generateContent?key=%s", geminiApiUrl, geminiModel, geminiApiKey);

        Map<String, Object> payload = Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", SYSTEM_INSTRUCTION))
                ),
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", "Translate query: \"" + userQuery + "\""))
                        )
                ),
                "generationConfig", Map.of(
                        "response_mime_type", "application/json",
                        "temperature", 0.1
                )
        );

        String requestBody = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(8))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode candidate = root.path("candidates").path(0);
            JsonNode parts = candidate.path("content").path("parts");

            String textContent = "";
            if (parts.isArray()) {
                for (JsonNode part : parts) {
                    if (part.has("text") && !part.path("text").asText("").trim().isEmpty()) {
                        textContent = part.path("text").asText();
                        break;
                    }
                }
            } else if (parts.has("text")) {
                textContent = parts.path("text").asText();
            }

            if (!textContent.isEmpty()) {
                return parseModelResponse(textContent);
            }
        }

        throw new RuntimeException("Gemini API returned status " + response.statusCode() + ": " + response.body());
    }

    public AiQueryTranslationResult parseModelResponse(String responseText) {
        try {
            String cleanJson = responseText.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substring(3);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            cleanJson = cleanJson.trim();

            JsonNode node = objectMapper.readTree(cleanJson);
            String reasoning = node.path("reasoning").asText("Translated via Gemini LLM");

            Map<String, Object> intent = new HashMap<>();
            if (node.has("extracted_intent")) {
                intent = objectMapper.convertValue(node.get("extracted_intent"), Map.class);
            }

            JsonNode esQueryNode = node.has("es_query") ? node.get("es_query") : node;
            String dslQuery = objectMapper.writeValueAsString(esQueryNode);

            return new AiQueryTranslationResult(reasoning, intent, dslQuery, false);
        } catch (Exception ex) {
            log.error("Failed to parse Gemini response JSON: {}", responseText, ex);
            throw new RuntimeException("Invalid JSON from LLM: " + ex.getMessage(), ex);
        }
    }

    /**
     * Smart deterministic local semantic translator incorporating the catalog taxonomy,
     * strict category filtering, brand extraction, gender rules, and relevance ranking.
     */
    public AiQueryTranslationResult translateLocally(String rawQuery) {
        String lower = rawQuery.toLowerCase().replace("'", "").replace("’", "");
        Map<String, Object> intent = new LinkedHashMap<>();

        // 1. Gender Detection
        String gender = null;
        if (Pattern.compile("\\b(men|man|mens|male|guy|guys|boy|boys)\\b").matcher(lower).find()) {
            gender = "male";
        } else if (Pattern.compile("\\b(women|woman|womens|female|lady|ladies|girl|girls)\\b").matcher(lower).find()) {
            gender = "female";
        }
        intent.put("gender", gender);

        // 2. Category & Product Type Mapping with Synonyms
        List<String> matchedCategories = new ArrayList<>();

        // Footwear / Shoes
        if (Pattern.compile("\\b(shoe|shoes|sneaker|sneakers|cleat|cleats|trainer|trainers|boot|boots|footwear|heel|heels|slipper|slippers|loafer|loafers)\\b").matcher(lower).find()) {
            if ("male".equals(gender)) {
                matchedCategories.add("mens-shoes");
            } else if ("female".equals(gender)) {
                matchedCategories.add("womens-shoes");
            } else {
                matchedCategories.add("mens-shoes");
                matchedCategories.add("womens-shoes");
            }
        }

        // Dresses / Gowns / Frocks / Skirts
        if (Pattern.compile("\\b(dress|dresses|frock|frocks|gown|gowns|skirt|skirts)\\b").matcher(lower).find()) {
            if (!matchedCategories.contains("womens-dresses")) matchedCategories.add("womens-dresses");
            if (!matchedCategories.contains("tops")) matchedCategories.add("tops");
            if (gender == null) gender = "female";
        }

        // Shirts / Tops / Wear for Men
        if (Pattern.compile("\\b(shirt|shirts|tshirt|tshirts|t-shirt|t-shirts|polo)\\b").matcher(lower).find()) {
            if ("female".equals(gender)) {
                if (!matchedCategories.contains("tops")) matchedCategories.add("tops");
                if (!matchedCategories.contains("womens-dresses")) matchedCategories.add("womens-dresses");
            } else {
                if (!matchedCategories.contains("mens-shirts")) matchedCategories.add("mens-shirts");
            }
        }

        // Occasion wear (e.g., party wear for men)
        if (Pattern.compile("\\b(party wear|formal wear|casual wear|wear)\\b").matcher(lower).find()) {
            if ("male".equals(gender) && matchedCategories.isEmpty()) {
                matchedCategories.add("mens-shirts");
            } else if ("female".equals(gender) && matchedCategories.isEmpty()) {
                matchedCategories.add("womens-dresses");
                matchedCategories.add("tops");
            }
        }

        // Watches
        if (Pattern.compile("\\b(watch|watches|timepiece|timepieces)\\b").matcher(lower).find()) {
            if ("male".equals(gender)) {
                matchedCategories.add("mens-watches");
            } else if ("female".equals(gender)) {
                matchedCategories.add("womens-watches");
            } else {
                matchedCategories.add("mens-watches");
                matchedCategories.add("womens-watches");
            }
        }

        // Smartphones / Phones
        if (Pattern.compile("\\b(phone|phones|smartphone|smartphones|cellphone|cellphones|iphone|iphones|android)\\b").matcher(lower).find()) {
            if (!matchedCategories.contains("smartphones")) matchedCategories.add("smartphones");
        }

        // Laptops / Computers
        if (Pattern.compile("\\b(laptop|laptops|notebook|notebooks|macbook|macbooks|computer|computers)\\b").matcher(lower).find()) {
            if (!matchedCategories.contains("laptops")) matchedCategories.add("laptops");
        }

        // Tablets
        if (Pattern.compile("\\b(tablet|tablets|ipad|ipads)\\b").matcher(lower).find()) {
            if (!matchedCategories.contains("tablets")) matchedCategories.add("tablets");
        }

        // Fragrances / Perfumes
        if (Pattern.compile("\\b(perfume|perfumes|fragrance|fragrances|cologne|colognes|scent)\\b").matcher(lower).find()) {
            if (!matchedCategories.contains("fragrances")) matchedCategories.add("fragrances");
        }

        // Beauty / Makeup
        if (Pattern.compile("\\b(beauty|makeup|mascara|mascaras|lipstick|lipsticks|eyeshadow|eyeshadows|cosmetic|cosmetics)\\b").matcher(lower).find()) {
            if (!matchedCategories.contains("beauty")) matchedCategories.add("beauty");
        }

        // Skin care
        if (Pattern.compile("\\b(skincare|skin-care|skin care|lotion|lotions|serum|serums|moisturizer|moisturizers|face cream)\\b").matcher(lower).find()) {
            if (!matchedCategories.contains("skin-care")) matchedCategories.add("skin-care");
        }

        // Bags / Handbags
        if (Pattern.compile("\\b(bag|bags|handbag|handbags|purse|purses|tote|totes|clutch|clutches)\\b").matcher(lower).find()) {
            if (!matchedCategories.contains("womens-bags")) matchedCategories.add("womens-bags");
            if (gender == null) gender = "female";
        }

        // Jewellery
        if (Pattern.compile("\\b(jewellery|jewelry|earring|earrings|necklace|necklaces|bracelet|bracelets|ring|rings)\\b").matcher(lower).find()) {
            if (!matchedCategories.contains("womens-jewellery")) matchedCategories.add("womens-jewellery");
            if (gender == null) gender = "female";
        }

        // Sunglasses
        if (Pattern.compile("\\b(sunglass|sunglasses|shades|eyewear)\\b").matcher(lower).find()) {
            if (!matchedCategories.contains("sunglasses")) matchedCategories.add("sunglasses");
        }

        // Motorcycle
        if (Pattern.compile("\\b(motorcycle|motorcycles|motorbike|motorbikes|sportbike|sportbikes)\\b").matcher(lower).find()) {
            if (!matchedCategories.contains("motorcycle")) matchedCategories.add("motorcycle");
        }

        // Vehicle / Cars
        if (Pattern.compile("\\b(vehicle|vehicles|car|cars|automotive)\\b").matcher(lower).find()) {
            if (!matchedCategories.contains("vehicle")) matchedCategories.add("vehicle");
        }

        // Furniture
        if (Pattern.compile("\\b(furniture|sofa|sofas|bed|beds|chair|chairs|table|tables|couch|couches)\\b").matcher(lower).find()) {
            if (!matchedCategories.contains("furniture")) matchedCategories.add("furniture");
        }

        // Groceries
        if (Pattern.compile("\\b(grocery|groceries|food|fruit|fruits|vegetable|vegetables|meat|juice|eggs|egg|steak|snack|snacks)\\b").matcher(lower).find()) {
            if (!matchedCategories.contains("groceries")) matchedCategories.add("groceries");
        }

        // Kitchen accessories
        if (Pattern.compile("\\b(kitchen|kitchen-accessories|cooktop|stove|stoves|pan|pans|pot|pots|tray|trays|slicer|chopping board)\\b").matcher(lower).find()) {
            if (!matchedCategories.contains("kitchen-accessories")) matchedCategories.add("kitchen-accessories");
        }

        // Sports accessories
        if (Pattern.compile("\\b(sports|sports-accessories|golf|fitness|gym)\\b").matcher(lower).find()) {
            if (!matchedCategories.contains("sports-accessories")) matchedCategories.add("sports-accessories");
        }

        // Direct catalog category matching as fallback
        for (String cat : CATEGORIES) {
            String catClean = cat.replace("-", " ");
            if (lower.contains(catClean) || lower.contains(cat)) {
                if (!matchedCategories.contains(cat)) matchedCategories.add(cat);
            }
        }
        intent.put("categories", matchedCategories);

        // 3. Brand Detection
        String matchedBrand = null;
        for (String brand : BRANDS) {
            Pattern brandPattern = Pattern.compile("\\b" + Pattern.quote(brand.toLowerCase()) + "\\b");
            if (brandPattern.matcher(lower).find()) {
                // Special check: "Apple" could mean the fruit if query mentions food/groceries
                if ("apple".equalsIgnoreCase(brand) && matchedCategories.contains("groceries") && !lower.contains("phone") && !lower.contains("laptop") && !lower.contains("iphone")) {
                    continue;
                }
                matchedBrand = brand;
                break;
            }
        }
        intent.put("brand", matchedBrand);

        // 4. Price Bounds Extraction
        Double maxPrice = null;
        Double minPrice = null;

        Matcher underMatcher = Pattern.compile("(?:under|below|less than|<=|<)\\s*\\$?([0-9]+(?:\\.[0-9]+)?)").matcher(lower);
        if (underMatcher.find()) {
            try {
                maxPrice = Double.parseDouble(underMatcher.group(1));
            } catch (Exception ignored) {}
        }

        Matcher overMatcher = Pattern.compile("(?:above|over|more than|>=|>)\\s*\\$?([0-9]+(?:\\.[0-9]+)?)").matcher(lower);
        if (overMatcher.find()) {
            try {
                minPrice = Double.parseDouble(overMatcher.group(1));
            } catch (Exception ignored) {}
        }
        intent.put("max_price", maxPrice);
        intent.put("min_price", minPrice);

        // 5. Clean Residual Descriptive Keywords (e.g. "red running" from "red running shoes")
        String remainingText = lower;
        if (matchedBrand != null) {
            remainingText = remainingText.replaceAll("\\b" + Pattern.quote(matchedBrand.toLowerCase()) + "\\b", " ");
        }
        // Remove common stopwords and filter terms
        remainingText = remainingText.replaceAll("\\b(for|under|below|less than|above|over|more than|men|man|mens|male|guy|guys|boy|boys|women|woman|womens|female|lady|ladies|girl|girls|shoes|shoe|sneakers|sneaker|dresses|dress|frock|frocks|shirt|shirts|phones|phone|watches|watch|with|and|the|a|an|in|of|to)\\b", " ");
        remainingText = remainingText.replaceAll("\\$?[0-9]+(?:\\.[0-9]+)?", " ");
        remainingText = remainingText.replaceAll("\\s+", " ").trim();
        intent.put("keywords", remainingText.isEmpty() ? rawQuery : remainingText);

        // 6. Construct Strict Bool Query DSL
        Map<String, Object> bool = new LinkedHashMap<>();
        List<Map<String, Object>> filterList = new ArrayList<>();
        List<Map<String, Object>> shouldList = new ArrayList<>();

        // Strict category filter
        if (!matchedCategories.isEmpty()) {
            filterList.add(Map.of("terms", Map.of("category", matchedCategories)));
        }

        // Strict gender filter
        if (gender != null) {
            filterList.add(Map.of("terms", Map.of("gender", List.of(gender, "unisex"))));
        }

        // Brand filter
        if (matchedBrand != null) {
            filterList.add(Map.of("term", Map.of("brand", matchedBrand)));
        }

        // Price range filter
        if (maxPrice != null || minPrice != null) {
            Map<String, Object> rangeParams = new LinkedHashMap<>();
            if (maxPrice != null) rangeParams.put("lte", maxPrice);
            if (minPrice != null) rangeParams.put("gte", minPrice);
            filterList.add(Map.of("range", Map.of("price", rangeParams)));
        }

        // Should clauses for ranking within the strictly filtered subset
        if (!matchedCategories.isEmpty()) {
            shouldList.add(Map.of("terms", Map.of("category", matchedCategories, "boost", 1.0)));
        }
        if (matchedBrand != null) {
            shouldList.add(Map.of("term", Map.of("brand", Map.of("value", matchedBrand, "boost", 5.0))));
        }

        // Score descriptive keywords (e.g. "red running", "luxury", "party wear")
        if (!remainingText.isEmpty()) {
            shouldList.add(Map.of("match_phrase", Map.of("title", Map.of("query", remainingText, "boost", 10.0))));
            shouldList.add(Map.of("match", Map.of("title", Map.of("query", remainingText, "boost", 5.0, "operator", "or"))));
            shouldList.add(Map.of("terms", Map.of("tags", List.of(remainingText.split("\\s+")), "boost", 4.0)));
            shouldList.add(Map.of("multi_match", Map.of(
                    "query", remainingText,
                    "fields", List.of("title^3", "tags^2", "description^1"),
                    "fuzziness", "AUTO",
                    "prefix_length", 2,
                    "boost", 2.0
            )));
        } else {
            // No extra descriptive words: rank by title matching the raw query
            shouldList.add(Map.of("match_phrase", Map.of("title", Map.of("query", rawQuery, "boost", 5.0))));
            shouldList.add(Map.of("multi_match", Map.of(
                    "query", rawQuery,
                    "fields", List.of("title^3", "tags^2"),
                    "fuzziness", "AUTO",
                    "prefix_length", 2,
                    "boost", 2.0
            )));
        }

        if (!filterList.isEmpty()) {
            bool.put("filter", filterList);
        }
        bool.put("should", shouldList);
        bool.put("minimum_should_match", 1);

        Map<String, Object> esQuery = Map.of("bool", bool);

        String reasoning = String.format("Parsed intent for '%s': gender=%s, categories=%s, brand=%s, maxPrice=%s with strict category constraints",
                rawQuery, gender, matchedCategories, matchedBrand, maxPrice);

        try {
            String dslQuery = objectMapper.writeValueAsString(esQuery);
            return new AiQueryTranslationResult(reasoning, intent, dslQuery, true);
        } catch (Exception e) {
            return buildDefaultMatchAll();
        }
    }

    private AiQueryTranslationResult buildDefaultMatchAll() {
        return new AiQueryTranslationResult(
                "Default match all query",
                Map.of(),
                "{\"match_all\":{}}",
                true
        );
    }
}
