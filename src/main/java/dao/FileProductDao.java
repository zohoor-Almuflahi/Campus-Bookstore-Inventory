package dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import model.Product;

public class FileProductDao implements ProductDao {

    private final Path inventoryFile;

    public FileProductDao(String filePath) {
        this.inventoryFile = Path.of(filePath);
    }

    @Override
    public Product save(Product product) {
        Map<String, Product> products = readAll();
        Product copy = new Product(product);
        products.put(copy.getSku(), copy);
        writeAll(products);
        return new Product(copy);
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        Product stored = readAll().get(sku);
        if (stored == null) {
            return Optional.empty();
        }
        return Optional.of(new Product(stored));
    }

    @Override
    public List<Product> findAll() {
        List<Product> copies = new ArrayList<>();
        for (Product stored : readAll().values()) {
            copies.add(new Product(stored));
        }
        return copies;
    }

    @Override
    public boolean deleteBySku(String sku) {
        Map<String, Product> products = readAll();
        if (products.remove(sku) == null) {
            return false;
        }
        writeAll(products);
        return true;
    }

    private Map<String, Product> readAll() {
        if (Files.notExists(inventoryFile)) {
            return new HashMap<>();
        }
        try {
            Map<String, Product> products = new HashMap<>();
            for (String line : Files.readAllLines(inventoryFile)) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("\\|");
                Product product = new Product(
                        parts[0],
                        parts[1],
                        Double.valueOf(parts[2]),
                        Integer.parseInt(parts[3]));
                products.put(product.getSku(), product);
            }
            return products;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read inventory file", e);
        }
    }

    private void writeAll(Map<String, Product> products) {
        List<String> lines = new ArrayList<>();
        for (Product product : products.values()) {
            lines.add(product.getSku() + "|"
                    + product.getName() + "|"
                    + product.getPrice() + "|"
                    + product.getQuantity());
        }
        try {
            Files.write(inventoryFile, lines);
        } catch (IOException e) {
            throw new IllegalStateException("Could not write inventory file", e);
        }
    }
}