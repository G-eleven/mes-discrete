package com.tws.mes.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 系统用户（学习版简化 RBAC：单角色字段） */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    /** BCrypt 密文 */
    private String password;
    private String nickName;
    /** admin / planner / quality / operator */
    private String roleCode;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
