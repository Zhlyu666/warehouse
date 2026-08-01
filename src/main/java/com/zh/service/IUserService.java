package com.zh.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zh.domain.dto.Result;
import com.zh.domain.dto.UserDTO;
import com.zh.domain.po.User;


/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author zhlyu
 * @since 2026-07-31
 */
public interface IUserService extends IService<User> {

    Result login(UserDTO user);
}
