package com.example.todo.dto;

import com.example.todo.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Data the client sends when creating or updating a todo.
 * A record is an immutable DTO; validation annotations are checked by @Valid in the controller.
 */
public record TodoRequest(
        @NotBlank(message = "title must not be blank")
        @Size(max = 150, message = "title must be at most 150 characters")
        String title,

        @Size(max = 1000, message = "description must be at most 1000 characters")
        String description,

        Priority priority,

        LocalDate dueDate,

        Boolean completed
) {
}
