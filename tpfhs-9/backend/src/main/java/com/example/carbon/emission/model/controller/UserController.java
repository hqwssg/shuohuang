package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.entity.User;
import com.example.carbon.emission.model.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 用户控制器
 * 
 * 负责处理用户相关的HTTP请求，主要提供用户登录功能。
 * 登录成功后返回用户基本信息，供前端页面展示和传递使用。
 * 
 * @author 系统生成
 * @version 3.0
 */
@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    /**
     * 用户登录接口
     * 
     * 验证用户名是否存在，存在则返回用户信息。
     * 返回的用户信息包含：userId、userName、nickName、deptId，
     * 供前端首页显示和后续页面传递使用。
     * 
     * @param loginRequest 登录请求体，包含：
     *        - username (String): 用户名，不能为空
     *        - password (String): 密码，不能为空
     * @return ResponseEntity: 
     *         - 成功：返回用户信息Map，包含userId、userName、nickName、deptId
     *         - 失败：返回错误信息（用户名和密码不能为空 / 用户名不存在）
     */
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