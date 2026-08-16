package com.tws.mes.base.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tws.mes.base.entity.SysUser;
import com.tws.mes.base.mapper.SysUserMapper;
import com.tws.mes.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 登录鉴权：Sa-Token 会话由框架管理（token 存 Redis，重启不丢）。
 * 密码校验用 BCrypt（hutool），库里只存密文。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;

    public LoginVO login(String username, String password) {
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            throw new BizException("用户名或密码不能为空");
        }
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null || !cn.hutool.crypto.digest.BCrypt.checkpw(password, user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException("账号已停用");
        }
        StpUtil.login(user.getId());
        return new LoginVO(StpUtil.getTokenValue(), user.getId(), user.getUsername(),
                user.getNickName(), user.getRoleCode());
    }

    public void logout() {
        StpUtil.logout();
    }

    public LoginVO me() {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }
        return new LoginVO(StpUtil.getTokenValue(), user.getId(), user.getUsername(),
                user.getNickName(), user.getRoleCode());
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class LoginVO {
        private String token;
        private Long userId;
        private String username;
        private String nickName;
        private String roleCode;
    }
}
