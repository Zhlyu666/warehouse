package com.zh.controller;


import com.zh.domain.dto.Result;
import com.zh.domain.dto.UserDTO;
import com.zh.service.IUserService;
import com.zh.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author zhlyu
 * @since 2026-07-31
 */
@RestController
@RequestMapping("/auth")
@Slf4j
public class UserController {
    @Autowired
    private IUserService iUserService;


    @PostMapping("/login")
    public Result login(@RequestBody UserDTO user) {
        return iUserService.login(user);
    }

    @PostMapping("/logout")
    public void logout() {
        //TODO 请求头携带 token，服务端删除 Redis 会话。
        Long userId = UserHolder.getUser().getId();
        log.info("用户{}退出登录", userId);
    }
}
