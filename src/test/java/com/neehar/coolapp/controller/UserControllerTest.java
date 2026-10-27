package com.neehar.coolapp.controller;

import com.neehar.coolapp.exception.DuplicateEmailException;
import com.neehar.coolapp.exception.UserNotFoundException;
import com.neehar.coolapp.model.User;
import com.neehar.coolapp.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;           // sends fake HTTP requests

    @MockitoBean
    private UserService userService;   // fake service in the Spring context

    private User user(long id, String name, String email) {
        User u = new User(name, email);
        u.setId(id);
        return u;
    }

    // ---------- CREATE ----------
    @Test
    void create_returns201() throws Exception {
        when(userService.createUser(any(User.class)))
                .thenReturn(user(1, "Neehar", "n@x.com"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Neehar","email":"n@x.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Neehar"));
    }

    @Test
    void create_returns400WhenInvalid() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"bad"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists());

        verify(userService, never()).createUser(any());   // validation stopped it
    }

    @Test
    void create_returns409WhenDuplicateEmail() throws Exception {
        when(userService.createUser(any(User.class)))
                .thenThrow(new DuplicateEmailException("n@x.com"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Neehar","email":"n@x.com"}
                                """))
                .andExpect(status().isConflict());
    }

    // ---------- READ ----------
    @Test
    void getAll_returns200AndList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(
                user(1, "A", "a@x.com"), user(2, "B", "b@x.com")));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getById_returns404WhenMissing() throws Exception {
        when(userService.getUserById(99L)).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(get("/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id 99"));
    }

    // ---------- UPDATE ----------
    @Test
    void update_returns200() throws Exception {
        when(userService.updateUser(eq(1L), any(User.class)))
                .thenReturn(user(1, "Neehar", "new@x.com"));

        mockMvc.perform(put("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Neehar","email":"new@x.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@x.com"));
    }

    // ---------- DELETE ----------
    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }

    @Test
    void delete_returns404WhenMissing() throws Exception {
        doThrow(new UserNotFoundException(99L)).when(userService).deleteUser(99L);

        mockMvc.perform(delete("/users/99"))
                .andExpect(status().isNotFound());
    }
}