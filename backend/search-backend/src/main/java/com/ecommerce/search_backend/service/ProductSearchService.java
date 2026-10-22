
package com.ecommerce.search_backend.service;

import com.ecommerce.search_backend.model.Product;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import com.ecommerce.search_backend.repository.ProductRepository;
import java.util.List;
import java.util.Optional;
@Service
public class ProductSearchService {

    private final ElasticsearchOperations operations;
    private final ProductRepository productRepository;




    public ProductSearchService(
            ElasticsearchOperations operations,
            ProductRepository productRepository) {

        this.operations = operations;
        this.productRepository = productRepository;
    }


    public List<Product> searchProducts(String text) {

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.multiMatch(m -> m
                        .query(text)
                        .fields(
                                "title",
                                "description",
                                "brand",
                                "category",
                                "tags"
                        )
                ))
                .withMaxResults(20)
                .build();

        SearchHits<Product> results =
                operations.search(query, Product.class);

        return results.getSearchHits()
                .stream()
                .map(hit -> hit.getContent())
                .toList();
    }



    public Optional<Product> getProductById(Integer id) {
        return productRepository.findById(id);
    }

}
