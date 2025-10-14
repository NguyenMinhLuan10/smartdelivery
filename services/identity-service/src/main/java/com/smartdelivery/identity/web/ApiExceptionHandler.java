package com.smartdelivery.identity.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> badReq(MethodArgumentNotValidException ex){
        var first = ex.getBindingResult().getFieldErrors().stream().findFirst();
        return ResponseEntity.badRequest().body(Map.of(
                "error","validation_error",
                "message", first.map(f->f.getField()+": "+f.getDefaultMessage()).orElse("Invalid")
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> badReq(IllegalArgumentException ex){
        return ResponseEntity.badRequest().body(Map.of("error","bad_request","message",ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> conflict(IllegalStateException ex){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error","conflict","message",ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> server(Exception ex){
        return ResponseEntity.status(500).body(Map.of("error","server_error","message",ex.getMessage()));
    }
}
