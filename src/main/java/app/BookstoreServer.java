package app;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import controller.ProductController;
import dao.FileProductDao;
import dao.ProductDao;
import model.Product;
import request.CreateProductRequest;
import service.ProductService;
import service.ProductServiceImpl;

public class BookstoreServer {

    public static void main(String[] args) throws IOException {
        ProductDao dao = new FileProductDao("data/inventory.txt");
        ProductService service = new ProductServiceImpl(dao);
        ProductController controller = new ProductController(service);

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", exchange -> handle(exchange, controller, service));
        server.start();
        System.out.println("Open http://localhost:8080");
    }

    private static void handle(HttpExchange exchange, ProductController controller, ProductService service) throws IOException {
        String message = "";
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            message = perform(controller, parseForm(body));
            String location = "/?message=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Location", location);
            exchange.sendResponseHeaders(303, -1);
            exchange.close();
            return;
        }

        String query = exchange.getRequestURI().getRawQuery();
        if (query != null && query.startsWith("message=")) {
            message = URLDecoder.decode(query.substring("message=".length()), StandardCharsets.UTF_8);
        }
        byte[] page = page(service.list(), message).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, page.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(page);
        }
    }

    private static String perform(ProductController controller, Map<String, String> form) {
        try {
            String action = form.getOrDefault("action", "");
            String sku = form.getOrDefault("sku", "");
            return switch (action) {
                case "create" -> controller.handleCreate(new CreateProductRequest(
                        sku,
                        form.get("name"),
                        Double.valueOf(form.get("price")),
                        Integer.parseInt(form.get("quantity").trim())));
                case "get" -> controller.handleGet(sku);
                case "restock" -> controller.handleRestock(sku, Integer.parseInt(form.get("quantity").trim()));
                case "purchase" -> controller.handlePurchase(sku, Integer.parseInt(form.get("quantity").trim()));
                case "delete" -> controller.handleDelete(sku);
                default -> "Unknown action";
            };
        } catch (RuntimeException e) {
            return "Enter a valid price and whole-number quantity";
        }
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> values = new HashMap<>();
        if (body.isBlank()) {
            return values;
        }
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            values.put(key, value);
        }
        return values;
    }

    private static String page(List<Product> products, String message) {
        StringBuilder rows = new StringBuilder();
        if (products.isEmpty()) {
            rows.append("<tr><td colspan=\"4\">No products in inventory</td></tr>");
        }
        for (Product product : products) {
            rows.append("<tr><td>").append(escape(product.getSku()))
                    .append("</td><td>").append(escape(product.getName()))
                    .append("</td><td>").append(product.getPrice())
                    .append("</td><td>").append(product.getQuantity())
                    .append("</td></tr>");
        }
        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8">
                  <title>Campus Bookstore</title>
                </head>
                <body>
                  <h1>Campus Bookstore Inventory</h1>
                  <p><strong>%s</strong></p>
                  <h2>Inventory</h2>
                  <table border="1" cellpadding="6">
                    <tr><th>SKU</th><th>Name</th><th>Price</th><th>Quantity</th></tr>
                    %s
                  </table>
                  <h2>Add product</h2>
                  <form method="post">
                    <input type="hidden" name="action" value="create">
                    SKU <input name="sku">
                    Name <input name="name">
                    Price <input name="price">
                    Quantity <input name="quantity">
                    <button>Add</button>
                  </form>
                  <h2>Look up</h2>
                  <form method="post">
                    <input type="hidden" name="action" value="get">
                    SKU <input name="sku">
                    <button>Find</button>
                  </form>
                  <h2>Restock</h2>
                  <form method="post">
                    <input type="hidden" name="action" value="restock">
                    SKU <input name="sku">
                    Quantity <input name="quantity">
                    <button>Restock</button>
                  </form>
                  <h2>Purchase</h2>
                  <form method="post">
                    <input type="hidden" name="action" value="purchase">
                    SKU <input name="sku">
                    Quantity <input name="quantity">
                    <button>Purchase</button>
                  </form>
                  <h2>Delete</h2>
                  <form method="post">
                    <input type="hidden" name="action" value="delete">
                    SKU <input name="sku">
                    <button>Delete</button>
                  </form>
                </body>
                </html>
                """.formatted(escape(message), rows);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}