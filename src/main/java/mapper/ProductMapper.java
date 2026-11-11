package mapper;

import model.Product;
import request.CreateProductRequest;

public class ProductMapper {

    public Product toProduct(CreateProductRequest request) {
        return new Product(
                request.getSku().trim(),
                request.getName().trim(),
                request.getPrice(),
                request.getQuantity());
    }
}