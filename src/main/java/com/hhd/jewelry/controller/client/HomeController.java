package com.hhd.jewelry.controller.client;

import com.hhd.jewelry.entity.Collection;
import com.hhd.jewelry.entity.Product;
import com.hhd.jewelry.service.CollectionService;
import com.hhd.jewelry.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final CollectionService collectionService;

    @GetMapping("/")
    public String home(Model model) {
        List<Product> products = productService.getAllProducts();
        List<Collection> collections = collectionService.getAllCollections();

        model.addAttribute("products", products);
        model.addAttribute("collections", collections);

        return "client_1/homepage/home";
    }
}