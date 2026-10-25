package com.ecommerce.search_backend.controllers;

import com.ecommerce.search_backend.model.AiSearchResponse;
import com.ecommerce.search_backend.model.Product;
import com.ecommerce.search_backend.service.ProductSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductController {

    private final ProductSearchService productSearchService;

    public ProductController(ProductSearchService productSearchService) {
        this.productSearchService = productSearchService;
    }

    /**
     * AI-Powered Natural Language Search with Explainability & Intent Metadata.
     * Returns products along with parsed intent, reasoning, and generated Query DSL.
     */
    @GetMapping("/search/ai")
    public ResponseEntity<AiSearchResponse> searchWithAi(
            @RequestParam("q") String query) {
        AiSearchResponse response = productSearchService.searchWithAi(query);
        return ResponseEntity.ok(response);
    }

    /**
     * Standard Search Endpoint (Supports traditional search or AI mode via parameter).
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(
            @RequestParam("q") String query,
            @RequestParam(value = "ai", defaultValue = "false") boolean useAi) {

        if (useAi) {
            return ResponseEntity.ok(productSearchService.searchWithAi(query));
        }
        return ResponseEntity.ok(productSearchService.searchProducts(query));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(
            @PathVariable Integer id) {

        return productSearchService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build());
    }
}
