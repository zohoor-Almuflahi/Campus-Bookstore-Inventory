package dao;

import java.util.List;
import java.util.Optional;

import model.Product;

public interface ProductDao{

    // Insert a new product or replace the product with the same SKU.
    Product save(Product product);


    // Return Optional.empty() when the SKU does not exist.
    Optional<Product> findBySku(String sku);


    // Return all products without exposing the backing collection.
    List<Product> findAll();


    // Return true if a product was removed; otherwise return false.
    boolean deleteBySku(String sku);
}