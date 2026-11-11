package request;

public class CreateProductRequest {

    private final String sku;
    private final String name;
    private final Double price;
    private final int quantity;

    public CreateProductRequest(String sku, String name, Double price, int quantity) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public Double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }
}