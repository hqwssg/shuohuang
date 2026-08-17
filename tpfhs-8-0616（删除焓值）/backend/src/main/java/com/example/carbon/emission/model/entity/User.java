package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sys_user")
@Data
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "user_name", nullable = false, unique = true, length = 30)
    private String userName;
    
    @Column(name = "nick_name", length = 30)
    private String nickName;
    
    @Column(name = "dept_id")
    private Long deptId;
}
