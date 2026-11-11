package dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import model.Product;

public class InMemoryProductDao implements ProductDao {

    private final Map<String, Product> products = new HashMap<>();

    @Override
    public Product save(Product product) {
        Product copy = new Product(product);
        products.put(copy.getSku(), copy);
        return new Product(copy);
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        Product stored = products.get(sku);
        if (stored == null) {
            return Optional.empty();
        }
        return Optional.of(new Product(stored));
    }

    @Override
    public List<Product> findAll() {
        List<Product> copies = new ArrayList<>();
        for (Product stored : products.values()) {
            copies.add(new Product(stored));
        }
        return copies;
    }

    @Override
    public boolean deleteBySku(String sku) {
        return products.remove(sku) != null;
    }
}