package com.example.tmdt.controller;



import com.example.tmdt.model.JwtResponse;
import com.example.tmdt.model.Role;
import com.example.tmdt.model.User;
import com.example.tmdt.service.RoleService;
import com.example.tmdt.service.UserService;
import com.example.tmdt.service.impl.JwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin("*")
public class UserController {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @GetMapping("/users")
    public ResponseEntity<Iterable<User>> showAllUser() {

        Iterable<User> users = userService.findAll();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/admin/users")
    public ResponseEntity<Iterable<User>> showAllUserByAdmin() {
        Iterable<User> users = userService.findAll();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<?> createUser(@Valid @RequestBody User user, BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors()) {
            return new ResponseEntity<>(bindingResult.getAllErrors(), HttpStatus.BAD_REQUEST);
        }
        if (!user.getPassword().equals(user.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.confirmPassword", "Confirm password must match the password");
        }
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        }

        Iterable<User> users = userService.findAll();
        for (User currentUser : users) {
            if (currentUser.getUsername().equals(user.getUsername())) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }

        Role role1 = roleService.findByName("ROLE_USER");
        Set<Role> roles1 = new HashSet<>();
        roles1.add(role1);
        user.setRoles(roles1);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setConfirmPassword(passwordEncoder.encode(user.getConfirmPassword()));
        userService.save(user);

        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody User user, BindingResult bindingResult) {
        if (user.getUsername() == null || user.getUsername().equals("")){
            String message = "TÊN ĐĂNG NHẬP HOẶC MẬT KHẨU KHÔNG ĐÚNG";
            return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
        }
        if (user.getPassword() == null || user.getPassword().equals("")){
            String message = "TÊN ĐĂNG NHẬP HOẶC MẬT KHẨU KHÔNG ĐÚNG";
            return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
        }
        User currentUser = userService.findByUsername(user.getUsername());
        if (currentUser == null) {
            String message = "TÊN ĐĂNG NHẬP HOẶC MẬT KHẨU KHÔNG ĐÚNG";
            return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
        }
        if (!passwordEncoder.matches(user.getPassword(), currentUser.getPassword())) {
            String message = "TÊN ĐĂNG NHẬP HOẶC MẬT KHẨU KHÔNG ĐÚNG";
            return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
        }

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtService.generateTokenLogin(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(new JwtResponse(jwt, currentUser.getId(), userDetails.getUsername(), userDetails.getAuthorities()));
    }

    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return new ResponseEntity("Hello World", HttpStatus.OK);
    }

    @GetMapping("/users/{id}")//
    public ResponseEntity<User> getProfile(@PathVariable Long id) {
        Optional<User> userOptional = this.userService.findById(id);
        return userOptional.map(user -> new ResponseEntity<>(user, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUserProfile(@PathVariable Long id, @RequestBody User user, @RequestParam() Map<String, String> parameters) {
        User userOptional = userService.findById(id).get();
        if (userOptional == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        userOptional.setEnabled(user.isEnabled());
        userOptional.setDisplayName(user.getDisplayName());
        userOptional.setAvatar(user.getAvatar());
        userOptional.setPhone(user.getPhone());
        userOptional.setEmail(user.getEmail());
        userService.save(userOptional);
        return new ResponseEntity<>(userOptional, HttpStatus.OK);
    }

    @PutMapping("/users/{id}/change-password")
    public ResponseEntity<?> changePassword(@PathVariable Long id, @RequestBody User user) {
        User userOptional = userService.findById(id).get();
        if (userOptional == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        userOptional.setEnabled(user.isEnabled());
        if (passwordEncoder.matches(user.getPassword(), userOptional.getPassword())) {
            userOptional.setPassword(passwordEncoder.encode(user.getConfirmPassword()));
            userOptional.setConfirmPassword(passwordEncoder.encode(user.getConfirmPassword()));
            userService.save(userOptional);
            return new ResponseEntity<>(userOptional, HttpStatus.OK);
        } else if(!passwordEncoder.matches(user.getPassword(), userOptional.getPassword())){
            String message = "Password hiện tại không nhạp đúng";
            return new ResponseEntity<>(message, HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }




}
