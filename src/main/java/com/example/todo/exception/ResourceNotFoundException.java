package com.example.todo.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(Long id) {
        super("Todo not found with id " + id);
    }
}
