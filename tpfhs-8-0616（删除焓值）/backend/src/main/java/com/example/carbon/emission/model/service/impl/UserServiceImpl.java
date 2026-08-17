package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.entity.User;
import com.example.carbon.emission.model.repository.UserRepository;
import com.example.carbon.emission.model.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}
