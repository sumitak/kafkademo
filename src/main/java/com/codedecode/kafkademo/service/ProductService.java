package com.codedecode.kafkademo.service;

import com.codedecode.kafkademo.model.CreateProductRestModel;

public interface ProductService {

    String createProduct(CreateProductRestModel productRestModel);
}
