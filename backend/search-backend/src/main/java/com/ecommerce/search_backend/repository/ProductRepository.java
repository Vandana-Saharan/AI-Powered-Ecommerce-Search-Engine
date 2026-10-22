
package com.ecommerce.search_backend.repository;

import com.ecommerce.search_backend.model.Product;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductRepository
        extends ElasticsearchRepository<Product, Integer> {

}
