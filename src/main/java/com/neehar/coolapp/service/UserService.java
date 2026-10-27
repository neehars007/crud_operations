package com.neehar.coolapp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.neehar.coolapp.exception.DuplicateEmailException;
import com.neehar.coolapp.exception.UserNotFoundException;
import com.neehar.coolapp.model.User;
import com.neehar.coolapp.repository.UserRepository;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    // Constructor injection: Spring passes the repository in automatically
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // CREATE
    @Transactional
    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateEmailException(user.getEmail());
        }
        user.setId(null);                 // ignore any id the client sent
        return userRepository.save(user); // no id → INSERT
    }

    // READ ALL
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // READ ONE
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    // UPDATE
    @Transactional
    public User updateUser(Long id, User updated) {
        User existing = getUserById(id);  // 404 if missing

        // Only block the new email if a DIFFERENT user already has it
        if (!existing.getEmail().equals(updated.getEmail())
                && userRepository.existsByEmail(updated.getEmail())) {
            throw new DuplicateEmailException(updated.getEmail());
        }

        existing.setName(updated.getName());
        existing.setEmail(updated.getEmail());
        return userRepository.save(existing); // id exists → UPDATE
    }

    // DELETE
    @Transactional
    public void deleteUser(Long id) {
        User existing = getUserById(id);  // 404 if missing
        userRepository.delete(existing);
    }
}