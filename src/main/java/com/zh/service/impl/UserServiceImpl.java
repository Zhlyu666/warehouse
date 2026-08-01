package com.zh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zh.domain.dto.UserDTO;
import com.zh.domain.dto.Result;
import com.zh.domain.po.User;
import com.zh.mapper.UserMapper;
import com.zh.service.IUserService;
import com.zh.utils.JwtTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author zhlyu
 * @since 2026-07-31
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    private final JwtTool jwtTool;

    public UserServiceImpl(JwtTool jwtTool) {
        this.jwtTool = jwtTool;
    }


    @Override
    public Result login(UserDTO user) {
        // 1.校验参数
        if (StringUtils.isBlank(user.getUsername()) || StringUtils.isBlank(user.getPassword())) {
            return Result.fail("账号密码不能为空");
        }

        // 2.查用户
        User dbUser = this.getOne(new QueryWrapper<User>().eq("username", user.getUsername()));
        if (dbUser == null) {
            return Result.fail("用户不存在");
        }
        if (!dbUser.getPassword().equals(user.getPassword())) {
            return Result.fail("密码错误");
        }
        // 5.登录成功：下发JWT
        String token = jwtTool.createToken(dbUser.getId(), Duration.ofDays(7));
        log.info("用户登录成功：{}", token);
        return Result.ok("登录成功", token);
    }
}