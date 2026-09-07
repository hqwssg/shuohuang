package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据访问接口
 * 
 * 基于Spring Data JPA的用户实体Repository，
 * 提供用户表(sys_user)的基本CRUD操作及自定义查询方法。
 * 
 * @see com.example.carbon.emission.model.entity.User
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 根据用户名查询用户
     * 
     * 用于用户登录时验证用户名是否存在。
     * 
     * @param userName 用户名（唯一标识）
     * @return Optional<User> 用户对象包装，若不存在则为空
     */
    Optional<User> findByUserName(String userName);
}
