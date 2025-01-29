package com.mindhub.order_service.exceptions;

import org.springframework.http.HttpStatus;

import java.util.function.Supplier;

public class OrderException extends Exception {
    private HttpStatus httpStatus;
    public OrderException(String message) {
        super(message);
    }
    public OrderException(String message, HttpStatus code) {
        super(message);
        this.httpStatus=code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}