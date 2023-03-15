package com.cmbc.infras.health.exception;

import lombok.Data;

@Data
public class DataErrorException extends RuntimeException{

    private int code;
    private String message;

    public DataErrorException() { }

    public DataErrorException(String message) {
        this.message = message;
    }

    public DataErrorException(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
