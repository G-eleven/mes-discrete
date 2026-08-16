package com.tws.mes.base.config;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tws.mes.base.entity.SysUser;
import com.tws.mes.base.mapper.SysUserMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限数据源：告诉框架"当前登录人是什么角色"。
 * 学习版一个用户一个角色（sys_user.role_code），将来扩展多角色改这里即可。
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private final SysUserMapper sysUserMapper;

    public StpInterfaceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 学习版用角色粗粒度控制，不细分到按钮权限
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getId, Long.valueOf(loginId.toString())));
        return user == null ? Collections.emptyList() : Collections.singletonList(user.getRoleCode());
    }
}
