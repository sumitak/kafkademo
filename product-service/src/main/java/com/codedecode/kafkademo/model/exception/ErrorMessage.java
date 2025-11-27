package com.codedecode.kafkademo.model.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorMessage {
    private Date timeStamp;
    private String message;
    private String details;
}
