package controller;

import java.util.List;
import java.util.NoSuchElementException;

import mapper.ProductMapper;
import model.Product;
import request.CreateProductRequest;
import service.ProductService;
import validation.CreateProductRequestValidator;

public class ProductController {

    private final ProductService productService;
    private final CreateProductRequestValidator validator;
    private final ProductMapper mapper;

    public ProductController(ProductService productService) {
        this.productService = productService;
        this.validator = new CreateProductRequestValidator();
        this.mapper = new ProductMapper();
    }

    public String handleCreate(CreateProductRequest request) {
        String error = validator.validate(request);
        if (error != null) {
            return error;
        }
        try {
            Product product = mapper.toProduct(request);
            Product created = productService.create(product);
            return "Created product: " + created;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    public String handleGet(String sku) {
        try {
            return productService.getRequired(sku).toString();
        } catch (IllegalArgumentException | NoSuchElementException e) {
            return e.getMessage();
        }
    }

    public String handleList() {
        List<Product> products = productService.list();
        if (products.isEmpty()) {
            return "No products in inventory";
        }
        StringBuilder result = new StringBuilder();
        for (Product product : products) {
            result.append(product).append(System.lineSeparator());
        }
        return result.toString().trim();
    }

    public String handleRestock(String sku, int quantityToAdd) {
        try {
            Product updated = productService.restock(sku, quantityToAdd);
            return "Restocked " + sku + ". New quantity: " + updated.getQuantity();
        } catch (IllegalArgumentException | NoSuchElementException e) {
            return e.getMessage();
        }
    }

    public String handlePurchase(String sku, int quantityToBuy) {
        try {
            Product updated = productService.purchase(sku, quantityToBuy);
            return "Purchased " + quantityToBuy + " of " + sku
                    + ". Remaining quantity: " + updated.getQuantity();
        } catch (IllegalArgumentException | NoSuchElementException e) {
            return e.getMessage();
        }
    }

    public String handleDelete(String sku) {
        try {
            boolean removed = productService.delete(sku);
            if (removed) {
                return "Removed product with SKU " + sku;
            }
            return "No product found with SKU " + sku;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }
}