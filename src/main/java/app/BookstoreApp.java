package app;

import controller.ProductController;
import dao.FileProductDao;
import dao.ProductDao;
import request.CreateProductRequest;
import service.ProductService;
import service.ProductServiceImpl;

public class BookstoreApp {

    public static void main(String[] args) {
        ProductDao dao = new FileProductDao("data/inventory.txt");
        ProductService service = new ProductServiceImpl(dao);
        ProductController controller = new ProductController(service);

        System.out.println(controller.handleCreate(
                new CreateProductRequest("NB-1", "Notebook", 4.50, 10)));
        System.out.println(controller.handleCreate(
                new CreateProductRequest("NB-1", "Notebook", 4.50, 10)));
        System.out.println(controller.handleGet("NB-1"));
        System.out.println(controller.handleList());
        System.out.println(controller.handlePurchase("NB-1", 3));
        System.out.println(controller.handlePurchase("NB-1", 8));
        System.out.println(controller.handleGet("NB-1"));
        System.out.println(controller.handleRestock("NB-1", 5));
        System.out.println(controller.handleDelete("NB-1"));
        System.out.println(controller.handleDelete("NB-1"));
        System.out.println(controller.handleList());
    }
}