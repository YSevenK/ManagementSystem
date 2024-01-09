package com.ysevenk.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ysevenk.common.utils.JwtUtil;
import com.ysevenk.sys.entity.Menu;
import com.ysevenk.sys.entity.User;
import com.ysevenk.sys.entity.UserRole;
import com.ysevenk.sys.mapper.UserMapper;
import com.ysevenk.sys.mapper.UserRoleMapper;
import com.ysevenk.sys.service.IMenuService;
import com.ysevenk.sys.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private IMenuService menuService;

    @Override
    public Map<String, Object> login(User user) {
        // 根据用户名查询
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        User loginUser = this.baseMapper.selectOne(wrapper);

        // 结果不为空，则生成token
        if (loginUser != null && passwordEncoder.matches(user.getPassword(), loginUser.getPassword())) {
            // 暂时用UUID
            // String key = "user:" + UUID.randomUUID();

            // 存入redis
            loginUser.setPassword(null);
            // redisTemplate.opsForValue().set(key, loginUser, 30, TimeUnit.MINUTES);

            // 创建jwt
            String token = jwtUtil.createToken(loginUser);

            // 返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
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
        User loginUser = null;
        // 根据token获取用户信息
        // Object obj = redisTemplate.opsForValue().get(token);
        try {
            loginUser = jwtUtil.parseToken(token, User.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (loginUser != null) {
            // User loginUser = JSON.parseObject(JSON.toJSONString(obj), User.class);
            Map<String, Object> data = new HashMap<>();
            data.put("name", loginUser.getUsername());
            data.put("avatar", loginUser.getAvatar());

            // 角色
            List<String> roleList = this.baseMapper.getRoleNameByUserId(loginUser.getId());
            data.put("roles", roleList);

            // 权限列表
            List<Menu> menuList = menuService.getMenuListByUserId(loginUser.getId());
            data.put("menuList", menuList);

            return data;
        }
        return null;
    }

    @Override
    public void logout(String token) {
        // redisTemplate.delete(token);
    }

    @Override
    @Transactional
    public void addUser(User user) {
        // 写入用户表
        this.baseMapper.insert(user);
        // 写入用户角色表
        List<Integer> roleIdList = user.getRoleIdList();
        if (roleIdList != null) {
            for (Integer roleId : roleIdList) {
                userRoleMapper.insert(new UserRole(null, user.getId(), roleId));
            }
        }
    }

    @Override
    public User getUserById(Integer id) {
        User user = this.baseMapper.selectById(id);
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getUserId, id);
        List<UserRole> userRoleList = userRoleMapper.selectList(wrapper);

        List<Integer> roleIdList = userRoleList.stream()
                .map(userRole -> {
                    return userRole.getRoleId();
                })
                .collect(Collectors.toList());
        user.setRoleIdList(roleIdList);
        return user;
    }

    @Override
    @Transactional
    public void updateUser(User user) {
        // 更新用户表
        this.baseMapper.updateById(user);

        // 清除原有角色
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getUserId, user.getId());
        userRoleMapper.delete(wrapper);

        // 设置新角色
        List<Integer> roleIdList = user.getRoleIdList();
        if (roleIdList != null) {
            for (Integer roleId : roleIdList) {
                userRoleMapper.insert(new UserRole(null, user.getId(), roleId));
            }
        }
    }

    @Override
    public void deleteUserById(Integer id) {
        this.baseMapper.deleteById(id);
        // 清除原有角色
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getUserId, id);
        userRoleMapper.delete(wrapper);
    }
}
