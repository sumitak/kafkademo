package com.codedecode.kafkademo.controller;

import com.codedecode.kafkademo.model.CreateProductRestModel;
import com.codedecode.kafkademo.model.exception.ErrorMessage;
import com.codedecode.kafkademo.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/products")
public class ProductController {
    private final Logger LOGGER= LoggerFactory.getLogger("ProductController.class");

    ProductService productService;

    public ProductController(ProductService productService){
        this.productService = productService;
    }

    @PostMapping("/createProduct")
    public ResponseEntity<Object> createProducts(@RequestBody CreateProductRestModel product){
        String productId  = null;
        try {
            productId = productService.createProduct(product);
        } catch (Exception e) {
            LOGGER.error("**** Error in product creation **********");
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                    .body(new ErrorMessage(new Date(), e.getMessage(), "product has not been created successfully."));
        }
        LOGGER.info("**** Product creation is successful**********");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productId);
    }
}
