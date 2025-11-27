package com.codedecode.kafkademo.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/response")
public class MockServiceController {
    private final Logger LOGGER= LoggerFactory.getLogger("MockServiceController.class");

   @GetMapping("/200")
    public ResponseEntity<String> response200String(){

        return ResponseEntity.status(HttpStatus.OK).body("200");

    }

    @GetMapping("/500")
    public ResponseEntity<String> response500String(){

        return ResponseEntity.internalServerError().build();

    }
}
