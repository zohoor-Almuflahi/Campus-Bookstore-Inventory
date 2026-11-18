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
        int copies = 0;
        double inventoryValue = 0;
        StringBuilder rows = new StringBuilder();
        for (Product product : products) {
            copies += product.getQuantity();
            inventoryValue += product.getPrice() * product.getQuantity();
            rows.append("<tr><td>").append(escape(product.getSku()))
                    .append("</td><td>").append(escape(product.getName()))
                    .append("</td><td>$").append(String.format("%.2f", product.getPrice()))
                    .append("</td><td>").append(product.getQuantity())
                    .append("</td></tr>");
        }

        String empty = products.isEmpty()
                ? """
                <div class="empty-state">
                  <strong>No products in inventory</strong>
                  <span>Add your first book using the form on the right.</span>
                  <small>Every great collection begins with one title.</small>
                </div>
                """
                : "";

        String notice = "";
        if (message != null && !message.isBlank()) {
            String kind = message.startsWith("SKU:") ? " info" : isSuccess(message) ? "" : " error";
            String label = message.startsWith("SKU:") ? "PRODUCT" : isSuccess(message) ? "SUCCESS" : "CHECK";
            notice = "<div class=\"notice" + kind + "\" role=\"status\"><span class=\"notice-label\">"
                    + label + "</span><span>" + escape(message) + "</span><a href=\"/\">Dismiss</a></div>";
        }

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Campus Bookstore</title>
                  <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Baloo+2&display=swap">
                  <style>
                {{CSS}}
                  .notice a { color: inherit; font-size: 12px; text-decoration: none; }
                  </style>
                </head>
                <body>
                  <div class="app-shell">
                    <header class="site-header">
                      <div class="content header-content">
                        <div class="brand-group">
                          <a class="brand" href="#top">books.</a>
                          <span class="brand-divider"></span>
                          <span class="brand-subtitle">CAMPUS BOOKSTORE</span>
                        </div>
                        <nav aria-label="Main navigation">
                          <a class="active" href="#inventory">Inventory</a>
                          <a href="#actions">Product actions</a>
                        </nav>
                        <span class="staff-label">Staff workspace</span>
                      </div>
                    </header>
                    <main id="top">
                      <section class="overview">
                        <div class="content overview-content">
                          <div class="overview-copy">
                            <p class="eyebrow">A LITTLE ORDER. A LOT OF POSSIBILITY.</p>
                            <h1>Campus Bookstore Inventory</h1>
                            <p class="lead">More time for books. Less time keeping track.</p>
                            <p>Add titles, manage stock, and keep your campus reading.</p>
                          </div>
                          <div class="metrics" aria-label="Inventory summary">
                            <div><strong>{{COUNT}}</strong><span>Titles in inventory</span></div>
                            <div><strong>{{COPIES}}</strong><span>Copies in stock</span></div>
                            <div><strong>${{VALUE}}</strong><span>Inventory value</span></div>
                          </div>
                        </div>
                      </section>
                      <section class="workspace" id="inventory">
                        <div class="content">
                          {{NOTICE}}
                          <div class="workspace-grid">
                            <section class="inventory-card">
                              <div class="card-intro">
                                <div class="title-row">
                                  <h2>Inventory</h2>
                                  <span class="count-pill">{{COUNT}} {{TITLE_WORD}}</span>
                                </div>
                                <p>Your complete book catalog, at a glance.</p>
                              </div>
                              <div class="table-wrap">
                                <table>
                                  <thead>
                                    <tr><th>SKU</th><th>Name</th><th>Price</th><th>Quantity</th></tr>
                                  </thead>
                                  <tbody>{{ROWS}}</tbody>
                                </table>
                                {{EMPTY}}
                              </div>
                              <div class="table-footer">
                                <span>Showing {{COUNT}} products</span>
                                <span>All prices in USD</span>
                              </div>
                            </section>
                            <form class="add-card" method="post">
                              <input type="hidden" name="action" value="create">
                              <div class="card-intro">
                                <h2>Add product</h2>
                                <p>Make room for the next great read.</p>
                              </div>
                              <label>SKU <input name="sku" placeholder="e.g. 10001" required></label>
                              <label>Name <input name="name" placeholder="Enter book title" required></label>
                              <div class="add-split">
                                <label>Price
                                  <span class="price-input"><span>$</span>
                                    <input name="price" min="0" placeholder="0.00" required step="0.01" type="number">
                                  </span>
                                </label>
                                <label>Quantity <input name="quantity" min="0" required type="number" value="0"></label>
                              </div>
                              <button class="add-button" type="submit">Add product</button>
                            </form>
                          </div>
                        </div>
                      </section>
                      <section class="actions-section" id="actions">
                        <div class="content">
                          <div class="section-heading">
                            <h2>Keep your shelves in order</h2>
                            <p>Everyday actions, all in one place.</p>
                          </div>
                          <div class="actions-grid">
                            <form class="action-card" method="post">
                              <input type="hidden" name="action" value="get">
                              <div class="action-heading"><span class="action-accent" style="background:#67577d"></span><h3>Look up</h3></div>
                              <p class="action-description">Find a book and check its details.</p>
                              <div class="action-fields"><label>SKU <input name="sku" placeholder="Enter SKU" required></label></div>
                              <button class="action-button lookup-button" type="submit">Find</button>
                              <p class="action-footer">Search by the exact SKU.</p>
                            </form>
                            <form class="action-card" method="post">
                              <input type="hidden" name="action" value="restock">
                              <div class="action-heading"><span class="action-accent" style="background:#526a55"></span><h3>Restock</h3></div>
                              <p class="action-description">Add new copies to your shelves.</p>
                              <div class="action-fields split">
                                <label>SKU <input name="sku" placeholder="Enter SKU" required></label>
                                <label class="quantity-field">Quantity <input name="quantity" min="1" required type="number"></label>
                              </div>
                              <button class="action-button restock-button" type="submit">Restock</button>
                              <p class="action-footer">Quantity is added to current stock.</p>
                            </form>
                            <form class="action-card" method="post">
                              <input type="hidden" name="action" value="purchase">
                              <div class="action-heading"><span class="action-accent" style="background:#776447"></span><h3>Purchase</h3></div>
                              <p class="action-description">Record a sale and update stock.</p>
                              <div class="action-fields split">
                                <label>SKU <input name="sku" placeholder="Enter SKU" required></label>
                                <label class="quantity-field">Quantity <input name="quantity" min="1" required type="number"></label>
                              </div>
                              <button class="action-button purchase-button" type="submit">Purchase</button>
                              <p class="action-footer">Quantity is removed from stock.</p>
                            </form>
                            <form class="action-card" method="post">
                              <input type="hidden" name="action" value="delete">
                              <div class="action-heading"><span class="action-accent" style="background:#925d56"></span><h3>Delete</h3></div>
                              <p class="action-description">Remove a book from your inventory.</p>
                              <div class="action-fields"><label>SKU <input name="sku" placeholder="Enter SKU" required></label></div>
                              <button class="action-button delete-button" type="submit">Delete</button>
                              <p class="action-footer">This permanently removes the product.</p>
                            </form>
                          </div>
                        </div>
                      </section>
                    </main>
                    <footer>
                      <div class="content footer-content">
                        <div>
                          <span class="footer-brand">books.</span>
                          <span>A well-stocked campus. A well-read community.</span>
                        </div>
                        <span>CAMPUS BOOKSTORE&nbsp;&nbsp;/&nbsp;&nbsp;INVENTORY</span>
                      </div>
                    </footer>
                  </div>
                </body>
                </html>
                """;

        return html.replace("{{CSS}}", css())
                .replace("{{COUNT}}", String.valueOf(products.size()))
                .replace("{{TITLE_WORD}}", products.size() == 1 ? "title" : "titles")
                .replace("{{COPIES}}", String.valueOf(copies))
                .replace("{{VALUE}}", String.format("%.2f", inventoryValue))
                .replace("{{ROWS}}", rows.toString())
                .replace("{{EMPTY}}", empty)
                .replace("{{NOTICE}}", notice);
    }

    private static boolean isSuccess(String message) {
        return message.startsWith("Created")
                || message.startsWith("Restocked")
                || message.startsWith("Purchased")
                || message.startsWith("Removed");
    }

    private static String css() {
        return """
                :root {
                  color: #2e3035;
                  background: #f8f7f4;
                  font-family: "Baloo 2", sans-serif;
                  font-synthesis: none;
                }

                html { scroll-behavior: smooth; }

                body { min-width: 320px; min-height: 100vh; }

                button, input { font-family: "Baloo 2", sans-serif; }

                button, a { -webkit-tap-highlight-color: transparent; }

                .app-shell {
                  min-height: 100dvh;
                  background: #f8f7f4;
                  font-family: "Baloo 2", sans-serif;
                  line-height: 1.2;
                }

                .content {
                  width: min(1312px, calc(100% - 48px));
                  margin: 0 auto;
                }

                .site-header { height: 96px; background: #faf9f5; }

                .header-content {
                  display: grid;
                  grid-template-columns: 1fr auto 1fr;
                  align-items: center;
                  height: 100%;
                  border-bottom: 1px solid #e5e1df;
                }

                .brand-group, .header-content nav, .footer-content > div {
                  display: flex;
                  align-items: center;
                }

                .brand { color: #2e3035; font-size: 30px; text-decoration: none; }

                .brand-divider { width: 1px; height: 32px; margin: 0 20px; background: #ddd9e3; }

                .brand-subtitle { color: #686674; font-size: 11px; }

                .header-content nav { gap: 8px; }

                .header-content nav a {
                  padding: 14px 24px 13px;
                  border-radius: 6px;
                  color: #686674;
                  font-size: 14px;
                  text-decoration: none;
                }

                .header-content nav a.active { color: #6f6288; background: #ebe7f0; }

                .staff-label { justify-self: end; color: #686674; font-size: 13px; }

                .overview { height: 232px; background: #ebe7f0; }

                .overview-content {
                  display: flex;
                  align-items: flex-start;
                  justify-content: space-between;
                  height: 100%;
                }

                .overview-copy { padding-top: 32px; }

                .eyebrow { margin: 0 0 21px; color: #6f6288; font-size: 11px; }

                .overview h1 { margin: 0 0 14px; color: #2e3035; font-size: 40px; font-weight: 400; }

                .overview-copy > p:not(.eyebrow) { margin: 0; color: #686674; font-size: 14px; }

                .overview-copy .lead { margin-bottom: 10px; font-size: 16px; }

                .metrics { display: grid; grid-template-columns: repeat(3, 148px); margin-top: 65px; }

                .metrics > div {
                  display: flex;
                  min-height: 100px;
                  flex-direction: column;
                  padding-left: 20px;
                  border-left: 1px solid #d2cbde;
                }

                .metrics > div:first-child { border-left: 0; }

                .metrics strong { margin-bottom: 12px; font-size: 30px; font-weight: 400; }

                .metrics span { max-width: 130px; color: #686674; font-size: 12px; }

                .workspace { min-height: 582px; padding: 24px 0 50px; }

                .notice {
                  display: grid;
                  grid-template-columns: 83px 1fr auto;
                  align-items: center;
                  min-height: 44px;
                  padding: 0 16px;
                  border-radius: 6px;
                  color: #405744;
                  background: #e8eee7;
                  font-size: 13px;
                }

                .notice.error { color: #764b45; background: #f2e8e5; }

                .notice.info { color: #67577d; background: #f0edf5; }

                .notice-label { color: #526a55; font-size: 10px; }

                .notice.error .notice-label { color: #925d56; }

                .notice.info .notice-label { color: #67577d; }

                .notice button {
                  border: 0;
                  color: inherit;
                  background: transparent;
                  cursor: pointer;
                  font-size: 12px;
                }

                .workspace-grid {
                  display: grid;
                  grid-template-columns: minmax(0, 824px) minmax(360px, 464px);
                  gap: 24px 32px;
                  margin-top: 24px;
                }

                .inventory-card, .add-card, .action-card {
                  border: 1px solid #e2dfe4;
                  border-radius: 10px;
                  background: #fff;
                }

                .inventory-card, .add-card { height: 440px; overflow: hidden; }

                .inventory-card { display: grid; grid-template-rows: 102px 1fr 50px; }

                .card-intro { padding: 22px 23px 0; }

                .card-intro h2, .section-heading h2 { margin: 0; font-weight: 400; }

                .card-intro h2 { font-size: 21px; }

                .card-intro p { margin: 9px 0 0; color: #706d77; font-size: 13px; }

                .title-row { display: flex; align-items: center; gap: 14px; }

                .count-pill {
                  padding: 7px 15px 6px;
                  border-radius: 14px;
                  color: #71638a;
                  background: #f1eef5;
                  font-size: 11px;
                }

                .table-wrap { position: relative; overflow: auto; }

                table { width: 100%; border-collapse: collapse; table-layout: fixed; font-size: 12px; }

                th {
                  height: 48px;
                  padding: 0 16px;
                  color: #706d77;
                  background: #f7f6f8;
                  font-weight: 400;
                  text-align: left;
                }

                th:first-child { width: 19%; padding-left: 23px; }

                th:nth-child(2) { width: 51%; }

                th:nth-child(3) { width: 13%; }

                td { height: 42px; padding: 0 16px; border-bottom: 1px solid #efedf0; color: #4e4c53; }

                td:first-child { padding-left: 23px; }

                .empty-state {
                  position: absolute;
                  inset: 48px 0 0;
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  flex-direction: column;
                  text-align: center;
                }

                .empty-state strong { margin: 0 0 13px; font-size: 20px; font-weight: 400; }

                .empty-state span { margin-bottom: 11px; color: #706d77; font-size: 14px; }

                .empty-state small { color: #817b89; font-size: 12px; }

                .table-footer {
                  display: flex;
                  align-items: center;
                  justify-content: space-between;
                  padding: 0 23px;
                  border-top: 1px solid #e2dfe4;
                  color: #706d77;
                  font-size: 12px;
                }

                .table-footer span:last-child { font-size: 11px; }

                .add-card { display: flex; flex-direction: column; padding: 0 23px; }

                .add-card .card-intro { padding: 22px 0 0; margin-bottom: 28px; }

                label { display: flex; flex-direction: column; gap: 11px; color: #2e3035; font-size: 12px; }

                input {
                  width: 100%;
                  height: 44px;
                  padding: 0 13px;
                  outline: 0;
                  border: 1px solid #e2dfe4;
                  border-radius: 6px;
                  color: #2e3035;
                  background: #fcfbfd;
                  font-size: 13px;
                  box-sizing: border-box;
                }

                input:focus { border-color: #71638a; box-shadow: 0 0 0 2px rgb(113 99 138 / 12%); }

                input::placeholder { color: #817b89; opacity: 1; }

                .add-card > label + label { margin-top: 14px; }

                .add-split { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin-top: 14px; }

                .price-input { position: relative; display: block; }

                .price-input > span {
                  position: absolute;
                  z-index: 1;
                  top: 14px;
                  left: 13px;
                  color: #817b89;
                  font-size: 13px;
                }

                .price-input input { padding-left: 29px; }

                .add-button {
                  height: 48px;
                  margin-top: 23px;
                  border: 0;
                  border-radius: 6px;
                  color: #fff;
                  background: #71638a;
                  cursor: pointer;
                  font-size: 14px;
                }

                .add-button:hover { background: #67577d; }

                .actions-section { min-height: 364px; }

                .section-heading { display: flex; align-items: baseline; justify-content: space-between; }

                .section-heading h2 { font-size: 22px; }

                .section-heading p { margin: 0; color: #706d77; font-size: 13px; }

                .actions-grid {
                  display: grid;
                  grid-template-columns: repeat(4, 1fr);
                  gap: 24px 32px;
                  margin-top: 31px;
                }

                .action-card { min-width: 0; height: 274px; padding: 20px 19px 0; }

                .action-heading { display: flex; align-items: center; gap: 11px; }

                .action-accent { width: 3px; height: 20px; border-radius: 1px; }

                .action-heading h3 { margin: 0; font-size: 18px; font-weight: 400; }

                .action-description { height: 29px; margin: 18px 0 12px; color: #706d77; font-size: 12px; }

                .action-fields { display: grid; grid-template-columns: 1fr; }

                .action-fields.split { grid-template-columns: minmax(0, 170px) minmax(62px, 88px); gap: 12px; }

                .action-button {
                  width: 100%;
                  height: 42px;
                  margin-top: 16px;
                  border: 0;
                  border-radius: 6px;
                  cursor: pointer;
                  font-size: 13px;
                }

                .lookup-button { color: #67577d; background: #f0edf5; }

                .restock-button { color: #526a55; background: #e8eee7; }

                .purchase-button { color: #776447; background: #efeae1; }

                .delete-button { color: #925d56; background: #f2e8e5; }

                .action-button:hover { filter: brightness(0.97); }

                .action-footer { margin: 15px 0 0; color: #706d77; font-size: 10px; }

                footer { height: 80px; color: #d0cad4; background: #343139; }

                .footer-content {
                  display: flex;
                  align-items: center;
                  justify-content: space-between;
                  height: 100%;
                  font-size: 10px;
                }

                .footer-content > div { gap: 48px; font-size: 12px; }

                .footer-brand { color: #f6f3f7; font-size: 23px; }

                @media (max-width: 1050px) {
                  .content { width: min(100% - 40px, 900px); }
                  .header-content { grid-template-columns: 1fr auto; }
                  .staff-label { display: none; }
                  .overview { height: auto; }
                  .overview-content { flex-direction: column; padding-bottom: 28px; }
                  .metrics { width: 100%; grid-template-columns: repeat(3, 1fr); margin-top: 28px; }
                  .workspace-grid { grid-template-columns: 1fr; }
                  .inventory-card, .add-card { height: 440px; }
                  .actions-section { padding: 0 0 32px; }
                  .actions-grid { grid-template-columns: repeat(2, 1fr); }
                }

                @media (max-width: 680px) {
                  .content { width: calc(100% - 32px); }
                  .site-header { height: auto; }
                  .header-content { display: flex; min-height: 82px; flex-wrap: wrap; padding: 14px 0; }
                  .brand { font-size: 26px; }
                  .brand-divider, .brand-subtitle { display: none; }
                  .header-content nav { margin-left: auto; }
                  .header-content nav a { padding: 11px 12px 10px; font-size: 12px; }
                  .overview-copy { padding-top: 28px; }
                  .overview h1 { margin-top: 17px; font-size: 31px; }
                  .metrics { gap: 0; }
                  .metrics > div { min-height: 82px; padding: 0 10px; }
                  .metrics > div:first-child { padding-left: 0; }
                  .metrics strong { font-size: 24px; }
                  .metrics span { font-size: 10px; }
                  .notice { grid-template-columns: 60px 1fr; gap: 4px; padding: 10px 12px; }
                  .notice a { grid-column: 2; justify-self: start; }
                  .workspace-grid { margin-top: 16px; }
                  .inventory-card { height: 430px; }
                  .table-wrap { overflow-x: auto; }
                  table { min-width: 560px; }
                  .empty-state { position: sticky; left: 0; width: calc(100vw - 34px); }
                  .empty-state span { max-width: 260px; }
                  .add-card { height: auto; padding-bottom: 23px; }
                  .section-heading { align-items: flex-start; flex-direction: column; gap: 8px; }
                  .actions-grid { grid-template-columns: 1fr; gap: 16px; margin-top: 24px; }
                  .action-card { height: 274px; }
                  .action-fields.split { grid-template-columns: minmax(0, 1fr) 100px; }
                  footer { height: auto; }
                  .footer-content { min-height: 104px; align-items: flex-start; flex-direction: column; justify-content: center; gap: 16px; }
                  .footer-content > div { align-items: flex-start; flex-direction: column; gap: 6px; }
                }
                """;
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