package com.court.mediation.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 平台用户：窗口人员、调解员、当事人、法官助理、执行联络人员均使用同一用户表，按 role 区分。
 */
@Data
@Entity
@Table(name = "sys_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(name = "real_name", length = 50)
    private String realName;

    /** WINDOW-立案窗口 MEDIATOR-调解员 PARTY-当事人 JUDGE_ASSISTANT-法官助理 EXECUTION-执行联络 ADMIN-管理员 */
    @Column(nullable = false, length = 30)
    private String role;

    @Column(length = 20)
    private String phone;

    /** 职称/所属部门 */
    @Column(length = 100)
    private String title;

    /** 调解员专长案由，逗号分隔，如 民间借贷,交通事故 */
    @Column(length = 200)
    private String specialties;

    /** 是否外地（仅调解员排班参考，可空） */
    @Column(name = "is_remote")
    private Boolean remote = false;

    @Column(nullable = false)
    private Boolean enabled = true;
}
