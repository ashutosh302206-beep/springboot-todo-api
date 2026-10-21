package com.example.todo.dto;

import com.example.todo.model.Priority;
import com.example.todo.model.Todo;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data the API returns. We never expose the JPA entity directly.
 */
public record TodoResponse(
        Long id,
        String title,
        String description,
        boolean completed,
        Priority priority,
        LocalDate dueDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TodoResponse from(Todo todo) {
        return new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getDescription(),
                todo.isCompleted(),
                todo.getPriority(),
                todo.getDueDate(),
                todo.getCreatedAt(),
                todo.getUpdatedAt()
        );
    }
}
