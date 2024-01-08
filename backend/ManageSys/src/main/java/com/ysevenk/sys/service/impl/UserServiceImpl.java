package com.ysevenk.sys.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ysevenk.common.vo.Result;
import com.ysevenk.sys.entity.User;
import com.ysevenk.sys.mapper.UserMapper;
import com.ysevenk.sys.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author ysevenk
 * @since 2024-01-08
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Map<String, Object> login(User user) {
        // 根据用户名查询
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        User loginUser = this.baseMapper.selectOne(wrapper);

        // 结果不为空，则生成token
        if (loginUser != null && passwordEncoder.matches(user.getPassword(), loginUser.getPassword())) {
            // 暂时用UUID
            String key = "user:" + UUID.randomUUID();

            // 存入redis
            loginUser.setPassword(null);
            redisTemplate.opsForValue().set(key, loginUser, 30, TimeUnit.MINUTES);

            // 返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("token", key);
            return data;
        }
        return null;
    }

    /**
     * @Override public Map<String, Object> login(User user) {
     * // 根据用户名和密码查询
     * LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
     * wrapper.eq(User::getUsername, user.getUsername());
     * wrapper.eq(User::getPassword, user.getPassword());
     * User loginUser = this.baseMapper.selectOne(wrapper);
     * <p>
     * // 结果不为空，则生成token
     * if (loginUser != null) {
     * // 暂时用UUID
     * String key = "user:" + UUID.randomUUID();
     * <p>
     * // 存入redis
     * loginUser.setPassword(null);
     * redisTemplate.opsForValue().set(key, loginUser, 30, TimeUnit.MINUTES);
     * <p>
     * // 返回数据
     * Map<String, Object> data = new HashMap<>();
     * data.put("token", key);
     * return data;
     * }
     * <p>
     * return null;
     * }
     */

    @Override
    public Map<String, Object> getUserInfo(String token) {

        // 根据token获取用户信息
        Object obj = redisTemplate.opsForValue().get(token);
        if (obj != null) {
            User loginUser = JSON.parseObject(JSON.toJSONString(obj), User.class);
            Map<String, Object> data = new HashMap<>();
            data.put("name", loginUser.getUsername());
            data.put("avatar", loginUser.getAvatar());

            // 角色
            List<String> roleList = this.baseMapper.getRoleNameByUserId(loginUser.getId());
            data.put("roles", roleList);
            return data;
        }
        return null;
    }

    @Override
    public void logout(String token) {
        redisTemplate.delete(token);
    }

}
