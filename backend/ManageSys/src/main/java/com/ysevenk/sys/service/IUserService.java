package com.ysevenk.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ysevenk.sys.entity.User;

import java.util.Map;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author ysevenk
 * @since 2024-01-08
 */
public interface IUserService extends IService<User> {

    Map<String, Object> login(User user);

    Map<String, Object> getUserInfo(String token);

    void logout(String token);
}
