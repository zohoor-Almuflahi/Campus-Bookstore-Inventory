package service;

import java.util.List;
import java.util.NoSuchElementException;

import dao.ProductDao;
import model.Product;

public class ProductServiceImpl implements ProductService {

    private final ProductDao productDao;

    public ProductServiceImpl(ProductDao productDao) {
        this.productDao = productDao;
    }

    @Override
    public Product create(Product product) {
        validateNewProduct(product);
        if (productDao.findBySku(product.getSku()).isPresent()) {
            throw new IllegalArgumentException("A product with SKU " + product.getSku() + " already exists");
        }
        return productDao.save(product);
    }

    @Override
    public Product getRequired(String sku) {
        requireSku(sku);
        return productDao.findBySku(sku)
                .orElseThrow(() -> new NoSuchElementException("No product found with SKU " + sku));
    }

    @Override
    public List<Product> list() {
        return productDao.findAll();
    }

    @Override
    public Product restock(String sku, int quantityToAdd) {
        if (quantityToAdd <= 0) {
            throw new IllegalArgumentException("Restock quantity must be greater than zero");
        }
        Product current = getRequired(sku);
        Product updated = current.withQuantity(current.getQuantity() + quantityToAdd);
        return productDao.save(updated);
    }

    @Override
    public Product purchase(String sku, int quantityToBuy) {
        if (quantityToBuy <= 0) {
            throw new IllegalArgumentException("Purchase quantity must be greater than zero");
        }
        Product current = getRequired(sku);
        if (quantityToBuy > current.getQuantity()) {
            throw new IllegalArgumentException("Not enough stock to purchase " + quantityToBuy);
        }
        Product updated = current.withQuantity(current.getQuantity() - quantityToBuy);
        return productDao.save(updated);
    }

    @Override
    public boolean delete(String sku) {
        requireSku(sku);
        return productDao.deleteBySku(sku);
    }

    private void validateNewProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product must not be null");
        }
        requireSku(product.getSku());
        if (product.getName() == null || product.getName().isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }
        if (product.getPrice() == null || product.getPrice() <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }
        if (product.getQuantity() < 0) {
            throw new IllegalArgumentException("Quantity must be zero or greater");
        }
    }

    private void requireSku(String sku) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU must not be blank");
        }
    }
}