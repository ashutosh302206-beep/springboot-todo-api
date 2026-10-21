package com.example.todo.service;

import com.example.todo.dto.TodoRequest;
import com.example.todo.dto.TodoResponse;
import com.example.todo.exception.ResourceNotFoundException;
import com.example.todo.model.Priority;
import com.example.todo.model.Todo;
import com.example.todo.repository.TodoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic layer. Controllers call this; this calls the repository.
 */
@Service
@Transactional(readOnly = true) // default for all methods; write methods override below
public class TodoService {

    private final TodoRepository todoRepository;

    // Constructor injection (preferred way of dependency injection in Spring)
    public TodoService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    @Transactional
    public TodoResponse create(TodoRequest request) {
        Todo todo = new Todo();
        apply(todo, request);
        return TodoResponse.from(todoRepository.save(todo));
    }

    public TodoResponse getById(Long id) {
        return TodoResponse.from(findOrThrow(id));
    }

    public Page<TodoResponse> getAll(Boolean completed, Priority priority, String search, Pageable pageable) {
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
        return todoRepository.findWithFilters(completed, priority, normalizedSearch, pageable)
                .map(TodoResponse::from);
    }

    @Transactional
    public TodoResponse update(Long id, TodoRequest request) {
        Todo todo = findOrThrow(id);
        apply(todo, request);
        // No save() needed: the entity is managed, so Hibernate dirty-checking
        // writes the changes when the transaction commits.
        return TodoResponse.from(todo);
    }

    @Transactional
    public TodoResponse toggleCompleted(Long id) {
        Todo todo = findOrThrow(id);
        todo.setCompleted(!todo.isCompleted());
        return TodoResponse.from(todo);
    }

    @Transactional
    public void delete(Long id) {
        todoRepository.delete(findOrThrow(id));
    }

    private Todo findOrThrow(Long id) {
        return todoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    // Copies request values onto the entity, using defaults for optional fields
    private void apply(Todo todo, TodoRequest request) {
        todo.setTitle(request.title().trim());
        todo.setDescription(request.description());
        todo.setPriority(request.priority() != null ? request.priority() : Priority.MEDIUM);
        todo.setDueDate(request.dueDate());
        if (request.completed() != null) {
            todo.setCompleted(request.completed());
        }
    }
}
