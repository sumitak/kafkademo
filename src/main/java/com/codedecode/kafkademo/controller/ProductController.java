package com.codedecode.kafkademo.controller;

import com.codedecode.kafkademo.model.CreateProductRestModel;
import com.codedecode.kafkademo.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductController {


    ProductService productService;

    public ProductController(ProductService productService){
        this.productService = productService;
    }

    @PostMapping("/createProduct")
    public ResponseEntity<String> createProducts(@RequestBody CreateProductRestModel product){
       String productId  = productService.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productId);
    }
}
