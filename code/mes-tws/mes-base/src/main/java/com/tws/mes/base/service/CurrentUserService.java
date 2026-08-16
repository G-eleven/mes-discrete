package com.tws.mes.base.service;

import cn.dev33.satoken.stp.StpUtil;
import com.tws.mes.base.entity.SysUser;
import com.tws.mes.base.mapper.SysUserMapper;
import com.tws.mes.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 当前登录人信息（流水/单据的 operator 字段用） */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final SysUserMapper sysUserMapper;

    /** 取当前登录人的用户名（而非用户ID），保证流水里存的是人看得懂的标识 */
    public String currentUsername() {
        if (!StpUtil.isLogin()) {
            throw new BizException(401, "未登录");
        }
        SysUser user = sysUserMapper.selectById(StpUtil.getLoginIdAsLong());
        return user == null ? "unknown" : user.getUsername();
    }
}
