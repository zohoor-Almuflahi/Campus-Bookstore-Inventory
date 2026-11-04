package model;

public class Product {
    private final String sku;
    private final String name;
    private final Double price;
    private final int quantity;

    public Product (String sku, String name, Double price, int quantity){
        this.sku =sku;
        this.name = name;
        this.price = price;
        this.quantity  = quantity;
    }

    public Product(Product original) {
    this(original.sku, original.name, original.price, original.quantity);
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
    public Product withQuantity(int newQuantity) {
    return new Product(sku, name, price, newQuantity);
    }
    @Override
    public String toString() {
        return "SKU: " + sku
                + ", Name: " + name
                + ", Price: " + price
                + ", Quantity: " + quantity;
    }
}