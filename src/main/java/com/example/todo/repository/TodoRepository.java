package com.example.todo.repository;

import com.example.todo.model.Priority;
import com.example.todo.model.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    // Optional filters: a null parameter means "do not filter on this"
    @Query("""
            SELECT t FROM Todo t
            WHERE (:completed IS NULL OR t.completed = :completed)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Todo> findWithFilters(@Param("completed") Boolean completed,
                               @Param("priority") Priority priority,
                               @Param("search") String search,
                               Pageable pageable);
}
