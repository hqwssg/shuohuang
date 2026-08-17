package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.entity.User;
import com.example.carbon.emission.model.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body("用户名和密码不能为空");
        }

        Optional<User> userOpt = userRepository.findByUserName(username);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getUserId());
            userInfo.put("userName", user.getUserName());
            userInfo.put("nickName", user.getNickName());
            userInfo.put("deptId", user.getDeptId());
            return ResponseEntity.ok(userInfo);
        } else {
            return ResponseEntity.badRequest().body("用户名不存在");
        }
    }
}