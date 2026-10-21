package com.example.todo;

import com.example.todo.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TodoApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TodoRepository todoRepository;

    @BeforeEach
    void cleanDatabase() {
        todoRepository.deleteAll();
    }

    private String createTodo(String json) throws Exception {
        return mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void createTodo_returns201_withDefaults() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Learn Spring Boot\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title").value("Learn Spring Boot"))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.priority").value("MEDIUM"));
    }

    @Test
    void createTodo_withBlankTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void createTodo_withInvalidPriority_returns400() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"priority\":\"URGENT\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/todos/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", containsString("9999")));
    }

    @Test
    void getById_returnsTodo() throws Exception {
        createTodo("{\"title\":\"Read docs\",\"priority\":\"HIGH\",\"dueDate\":\"2026-12-31\"}");
        Long id = todoRepository.findAll().get(0).getId();

        mockMvc.perform(get("/api/todos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Read docs"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.dueDate").value("2026-12-31"));
    }

    @Test
    void updateTodo_changesFields() throws Exception {
        createTodo("{\"title\":\"Old title\"}");
        Long id = todoRepository.findAll().get(0).getId();

        mockMvc.perform(put("/api/todos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New title\",\"priority\":\"LOW\",\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New title"))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    void toggleTodo_flipsCompleted() throws Exception {
        createTodo("{\"title\":\"Toggle me\"}");
        Long id = todoRepository.findAll().get(0).getId();

        mockMvc.perform(patch("/api/todos/" + id + "/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));

        mockMvc.perform(patch("/api/todos/" + id + "/toggle"))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    void deleteTodo_returns204_thenGet404() throws Exception {
        createTodo("{\"title\":\"Delete me\"}");
        Long id = todoRepository.findAll().get(0).getId();

        mockMvc.perform(delete("/api/todos/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/todos/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void listTodos_supportsFilteringSearchAndPagination() throws Exception {
        createTodo("{\"title\":\"Study Java\",\"priority\":\"HIGH\"}");
        createTodo("{\"title\":\"Study SQL\",\"priority\":\"LOW\"}");
        createTodo("{\"title\":\"Buy milk\",\"priority\":\"LOW\",\"completed\":true}");

        // no filters
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        // filter by priority
        mockMvc.perform(get("/api/todos").param("priority", "LOW"))
                .andExpect(jsonPath("$.content", hasSize(2)));

        // filter by completed
        mockMvc.perform(get("/api/todos").param("completed", "true"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Buy milk"));

        // case-insensitive search
        mockMvc.perform(get("/api/todos").param("search", "study"))
                .andExpect(jsonPath("$.content", hasSize(2)));

        // combined filters
        mockMvc.perform(get("/api/todos").param("search", "study").param("priority", "HIGH"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Study Java"));

        // pagination
        mockMvc.perform(get("/api/todos").param("size", "2").param("page", "0"))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalPages").value(2));
    }
}
