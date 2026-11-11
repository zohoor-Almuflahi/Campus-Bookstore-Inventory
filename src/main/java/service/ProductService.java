package service;

import java.util.List;
import model.Product;

public interface ProductService {
    Product create(Product product);
    Product getRequired(String sku);
    List<Product> list();
    Product restock(String sku, int quantityToAdd);
    Product purchase(String sku, int quantityToBuy);
    boolean delete(String sku);
}