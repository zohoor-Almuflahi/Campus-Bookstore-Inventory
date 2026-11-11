package validation;

import request.CreateProductRequest;

public class CreateProductRequestValidator {

    public String validate(CreateProductRequest request) {
        if (request == null) {
            return "Request must not be null";
        }
        if (request.getSku() == null || request.getSku().isBlank()) {
            return "SKU must not be blank";
        }
        if (request.getName() == null || request.getName().isBlank()) {
            return "Name must not be blank";
        }
        if (request.getPrice() == null || request.getPrice() <= 0) {
            return "Price must be greater than zero";
        }
        if (request.getQuantity() < 0) {
            return "Quantity must be zero or greater";
        }
        return null;
    }
}