
package com.ecommerce.search_backend.controllers;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ElasticsearchTestController {

    private final ElasticsearchOperations operations;

    public ElasticsearchTestController(
            ElasticsearchOperations operations) {
        this.operations = operations;
    }

    @GetMapping("/api/test-elasticsearch")
    public String testConnection() {

        boolean exists = operations
                .indexOps(IndexCoordinates.of("products"))
                .exists();

        return exists
                ? "Elasticsearch connected! Products index exists."
                : "Elasticsearch connected, but products index not found.";
    }
}
