package com.example.tmdt.service;


import com.example.tmdt.model.Role;

public interface RoleService {
    Iterable<Role> findAll();

    void save(Role role);

    Role findByName(String name);
}
