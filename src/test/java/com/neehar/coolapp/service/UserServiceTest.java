package com.neehar.coolapp.service;

import com.neehar.coolapp.exception.DuplicateEmailException;
import com.neehar.coolapp.exception.UserNotFoundException;
import com.neehar.coolapp.model.User;
import com.neehar.coolapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;   // fake repository

    @InjectMocks
    private UserService userService;         // real service, fake repo injected

    // ---------- CREATE ----------
    @Test
    void createUser_savesWhenEmailIsNew() {
        User input = new User("Neehar", "n@x.com");
        User saved = new User("Neehar", "n@x.com");
        saved.setId(1L);

        when(userRepository.existsByEmail("n@x.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        User result = userService.createUser(input);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Neehar");
        verify(userRepository).save(input);
    }

    @Test
    void createUser_throwsWhenEmailExists() {
        User input = new User("Neehar", "n@x.com");
        when(userRepository.existsByEmail("n@x.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> userService.createUser(input));
        verify(userRepository, never()).save(any());   // must NOT save
    }

    // ---------- READ ----------
    @Test
    void getAllUsers_returnsList() {
        when(userRepository.findAll()).thenReturn(List.of(
                new User("A", "a@x.com"), new User("B", "b@x.com")));

        assertThat(userService.getAllUsers()).hasSize(2);
    }

    @Test
    void getUserById_returnsUserWhenFound() {
        User user = new User("Neehar", "n@x.com");
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(userService.getUserById(1L).getEmail()).isEqualTo("n@x.com");
    }

    @Test
    void getUserById_throwsWhenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(99L));
    }

    // ---------- UPDATE ----------
    @Test
    void updateUser_changesFields() {
        User existing = new User("Neehar", "old@x.com");
        existing.setId(1L);
        User changes = new User("Neehar S", "new@x.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("new@x.com")).thenReturn(false);
        when(userRepository.save(existing)).thenReturn(existing);

        User result = userService.updateUser(1L, changes);

        assertThat(result.getName()).isEqualTo("Neehar S");
        assertThat(result.getEmail()).isEqualTo("new@x.com");
    }

    @Test
    void updateUser_throwsWhenNewEmailTakenByAnotherUser() {
        User existing = new User("Neehar", "old@x.com");
        existing.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("taken@x.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class,
                () -> userService.updateUser(1L, new User("Neehar", "taken@x.com")));
        verify(userRepository, never()).save(any());
    }

    // ---------- DELETE ----------
    @Test
    void deleteUser_deletesWhenFound() {
        User existing = new User("Neehar", "n@x.com");
        existing.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        userService.deleteUser(1L);

        verify(userRepository).delete(existing);
    }

    @Test
    void deleteUser_throwsWhenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(99L));
        verify(userRepository, never()).delete(any());
    }
}