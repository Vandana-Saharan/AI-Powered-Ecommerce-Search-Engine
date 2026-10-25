package com.ecommerce.search_backend.model;

import java.util.Map;

public class AiQueryTranslationResult {

    private String reasoning;
    private Map<String, Object> extractedIntent;
    private String dslQuery;
    private boolean simulated;

    public AiQueryTranslationResult() {
    }

    public AiQueryTranslationResult(String reasoning, Map<String, Object> extractedIntent, String dslQuery, boolean simulated) {
        this.reasoning = reasoning;
        this.extractedIntent = extractedIntent;
        this.dslQuery = dslQuery;
        this.simulated = simulated;
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

    public String getDslQuery() {
        return dslQuery;
    }

    public void setDslQuery(String dslQuery) {
        this.dslQuery = dslQuery;
    }

    public boolean isSimulated() {
        return simulated;
    }

    public void setSimulated(boolean simulated) {
        this.simulated = simulated;
    }
}
