
package com.ecommerce.search_backend.controllers;

import com.ecommerce.search_backend.model.Product;
import com.ecommerce.search_backend.service.ProductSearchService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/products")

@CrossOrigin(origins = "http://localhost:5173")

public class ProductController {

    private final ProductSearchService productSearchService;

    public ProductController(
            ProductSearchService productSearchService) {
        this.productSearchService = productSearchService;
    }

    @GetMapping("/search")
    public List<Product> searchProducts(
            @RequestParam("q") String query) {

        return productSearchService.searchProducts(query);
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
