package com.freshmeal.security;

import cn.dev33.satoken.stp.StpInterface;
import com.freshmeal.entity.Admin;
import com.freshmeal.mapper.AdminMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 向 Sa-Token 提供当前登录账号的角色与权限，鉴权判断统一走 StpInterface。
 *
 * <p>三端登录类型：用户端 {@code user}、配送员端 {@code rider}、管理端使用默认登录类型。
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    public static final String USER_LOGIN_TYPE = "user";
    public static final String RIDER_LOGIN_TYPE = "rider";

    public static final String ROLE_USER = "user";
    public static final String ROLE_RIDER = "rider";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_SUPER_ADMIN = "super_admin";

    private static final int ADMIN_STATUS_ENABLED = 1;

    private final AdminMapper adminMapper;

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (USER_LOGIN_TYPE.equals(loginType)) {
            return List.of(ROLE_USER);
        }
        if (RIDER_LOGIN_TYPE.equals(loginType)) {
            return List.of(ROLE_RIDER);
        }
        Admin admin = findEnabledAdmin(loginId);
        if (admin == null) {
            return List.of();
        }
        return ROLE_SUPER_ADMIN.equals(admin.getRole())
                ? List.of(ROLE_ADMIN, ROLE_SUPER_ADMIN)
                : List.of(ROLE_ADMIN);
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Admin admin = findEnabledAdmin(loginId);
        if (admin != null && ROLE_SUPER_ADMIN.equals(admin.getRole())) {
            return List.of("*");
        }
        return List.of();
    }

    private Admin findEnabledAdmin(Object loginId) {
        Long adminId = parseAdminId(loginId);
        if (adminId == null) {
            return null;
        }
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null || admin.getStatus() == null
                || admin.getStatus() != ADMIN_STATUS_ENABLED) {
            return null;
        }
        return admin;
    }

    private Long parseAdminId(Object loginId) {
        if (loginId instanceof Number number) {
            return number.longValue();
        }
        if (loginId instanceof String value && !value.isBlank()) {
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return null;
    }
}
