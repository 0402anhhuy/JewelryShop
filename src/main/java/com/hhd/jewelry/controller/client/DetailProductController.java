package com.hhd.jewelry.controller.client;

import com.hhd.jewelry.entity.Product;
import com.hhd.jewelry.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/products")
public class DetailProductController {
    private final ProductService productService;
  
    @GetMapping("/detail/{serialNumber}")
    public String productDetail(@PathVariable String serialNumber, Model model) {
        Product product = productService.getProductBySerialNumber(serialNumber);

        model.addAttribute("product", product);
        return "client_1/product/detail";
    }
}
