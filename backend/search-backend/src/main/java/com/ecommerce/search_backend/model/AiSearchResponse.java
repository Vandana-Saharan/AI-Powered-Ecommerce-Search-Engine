package com.ecommerce.search_backend.model;

import java.util.List;
import java.util.Map;

public class AiSearchResponse {

    private List<Product> products;
    private String query;
    private String reasoning;
    private Map<String, Object> extractedIntent;
    private String generatedDsl;
    private boolean fallbackUsed;
    private int totalHits;

    public AiSearchResponse() {
    }

    public AiSearchResponse(List<Product> products, String query, String reasoning,
                            Map<String, Object> extractedIntent, String generatedDsl,
                            boolean fallbackUsed, int totalHits) {
        this.products = products;
        this.query = query;
        this.reasoning = reasoning;
        this.extractedIntent = extractedIntent;
        this.generatedDsl = generatedDsl;
        this.fallbackUsed = fallbackUsed;
        this.totalHits = totalHits;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public Map<String, Object> getExtractedIntent() {
        return extractedIntent;
    }

    public void setExtractedIntent(Map<String, Object> extractedIntent) {
        this.extractedIntent = extractedIntent;
    }

    public String getGeneratedDsl() {
        return generatedDsl;
    }

    public void setGeneratedDsl(String generatedDsl) {
        this.generatedDsl = generatedDsl;
    }

    public boolean isFallbackUsed() {
        return fallbackUsed;
    }

    public void setFallbackUsed(boolean fallbackUsed) {
        this.fallbackUsed = fallbackUsed;
    }

    public int getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(int totalHits) {
        this.totalHits = totalHits;
    }
}
