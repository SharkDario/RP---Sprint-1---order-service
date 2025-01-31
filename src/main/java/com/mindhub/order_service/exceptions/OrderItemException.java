package com.mindhub.order_service.exceptions;

import org.springframework.http.HttpStatus;

public class OrderItemException extends Exception{
    private HttpStatus  httpStatus;
    public OrderItemException(String message) {
        super(message);
    }

    public OrderItemException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}