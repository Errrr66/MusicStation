package com.example.music.service.impl;

import com.example.music.constant.JwtClaimsConstant;
import com.example.music.constant.MessageConstant;
import com.example.music.enumeration.RoleEnum;
import com.example.music.mapper.AdminMapper;
import com.example.music.model.dto.AdminDTO;
import com.example.music.model.entity.Admin;
import com.example.music.result.Result;
import com.example.music.service.IAdminService;
import com.example.music.util.JwtUtil;
import com.example.music.util.PasswordUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
  
 */
@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements IAdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 管理员注册
     *
     * @param adminDTO 管理员信息
     * @return 结果
     */
    @Override
    @Transactional
    public Result register(AdminDTO adminDTO) {
        Admin admin = adminMapper.selectOne(new QueryWrapper<Admin>().eq("username", adminDTO.getUsername()));
        if (admin != null) {
            return Result.error(MessageConstant.USERNAME + MessageConstant.ALREADY_EXISTS);
        }

        String passwordEncoded = PasswordUtils.encode(adminDTO.getPassword());
        Admin adminRegister = new Admin();
        adminRegister.setUsername(adminDTO.getUsername()).setPassword(passwordEncoded);

        if (adminMapper.insert(adminRegister) == 0) {
            return Result.error(MessageConstant.REGISTER + MessageConstant.FAILED);
        }
        return Result.success(MessageConstant.REGISTER + MessageConstant.SUCCESS);
    }

    /**
     * 管理员登录
     *
     * @param adminDTO 管理员信息
     * @return 结果
     */
    @Override
    public Result login(AdminDTO adminDTO) {
        log.info("Admin login attempt, username={}", adminDTO.getUsername());
        Admin admin = adminMapper.selectOne(new QueryWrapper<Admin>().eq("username", adminDTO.getUsername()));
        if (admin == null) {
            log.warn("Admin login failed, username not found: {}", adminDTO.getUsername());
            return Result.error(MessageConstant.USERNAME + MessageConstant.ERROR);
        }

        boolean passwordMatched = PasswordUtils.matches(adminDTO.getPassword(), admin.getPassword());
        log.debug("Admin password match result for username={}, matched={}, storedPasswordLength={}",
                adminDTO.getUsername(), passwordMatched,
                admin.getPassword() == null ? 0 : admin.getPassword().length());
        if (passwordMatched) {
            // 兼容旧 MD5 密码：登录成功后迁移到 BCrypt
            if (PasswordUtils.isLegacyMd5(admin.getPassword())) {
                log.info("Migrating legacy MD5 password to BCrypt for username={}", adminDTO.getUsername());
                adminMapper.update(new Admin().setPassword(PasswordUtils.encode(adminDTO.getPassword())),
                        new QueryWrapper<Admin>().eq("id", admin.getAdminId()));
            }
            // 登录成功
            Map<String, Object> claims = new HashMap<>();
            claims.put(JwtClaimsConstant.ROLE, RoleEnum.ADMIN.getRole());
            claims.put(JwtClaimsConstant.ADMIN_ID, admin.getAdminId());
            claims.put(JwtClaimsConstant.USERNAME, admin.getUsername());
            String token = JwtUtil.generateToken(claims);

            // 将token存入redis
            stringRedisTemplate.opsForValue().set(token, token, 6, TimeUnit.HOURS);

            log.info("Admin login success, username={}", adminDTO.getUsername());
            return Result.success(MessageConstant.LOGIN + MessageConstant.SUCCESS, token);
        }

        log.warn("Admin login failed, password mismatch for username={}", adminDTO.getUsername());
        return Result.error(MessageConstant.PASSWORD + MessageConstant.ERROR);
    }

    /**
     * 登出
     *
     * @param token 认证token
     * @return 结果
     */
    @Override
    public Result logout(String token) {
        if (token == null || token.isEmpty()) {
            return Result.success(MessageConstant.LOGOUT + MessageConstant.SUCCESS);
        }
        // 注销token
        Boolean result = stringRedisTemplate.delete(token);
        if (result != null && result) {
            return Result.success(MessageConstant.LOGOUT + MessageConstant.SUCCESS);
        } else {
            return Result.error(MessageConstant.LOGOUT + MessageConstant.FAILED);
        }
    }
}
