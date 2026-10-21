package com.example.todo.model;

/**
 * Priority of a todo. Stored in MySQL as text (EnumType.STRING),
 * so reordering the constants later never corrupts existing data.
 */
public enum Priority {
    LOW, MEDIUM, HIGH
}
