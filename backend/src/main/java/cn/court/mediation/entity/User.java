package cn.court.mediation.entity;

import cn.court.mediation.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 系统用户：窗口人员、调解员、当事人、法官助理、执行联络、管理员 */
@Getter
@Setter
@Entity
@Table(name = "sys_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 登录账号 */
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(length = 50)
    private String realName;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Role role;

    /** 调解员专长（案由 code，逗号分隔），仅调解员使用 */
    @Column(length = 200)
    private String expertise;

    /** 调解员所属调解室 */
    @Column(length = 100)
    private String mediationRoom;

    /** 办案上限（同时在办案件数） */
    private Integer capacity = 20;

    private LocalDateTime createdAt = LocalDateTime.now();
}
