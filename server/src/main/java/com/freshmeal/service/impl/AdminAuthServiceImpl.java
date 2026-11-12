package com.freshmeal.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.freshmeal.common.BusinessException;
import com.freshmeal.dto.admin.AdminLoginRequest;
import com.freshmeal.entity.Admin;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.mapper.AdminMapper;
import com.freshmeal.service.AdminAuthService;
import com.freshmeal.vo.admin.AdminLoginVO;
import com.freshmeal.vo.admin.AdminProfileVO;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private static final String ADMIN_UI_ROLE = "admin";
    private static final String SUPER_ADMIN_ROLE = "super_admin";

    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AdminLoginVO login(AdminLoginRequest request) {
        Admin admin = adminMapper.selectOne(Wrappers.<Admin>lambdaQuery()
                .eq(Admin::getUsername, request.username()));

        if (admin == null || !passwordEncoder.matches(request.password(), admin.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "用户名或密码错误");
        }
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "账号已禁用");
        }

        adminMapper.update(null, Wrappers.<Admin>lambdaUpdate()
                .eq(Admin::getId, admin.getId())
                .set(Admin::getLastLoginTime, LocalDateTime.now()));

        StpUtil.login(admin.getId());
        String accessToken = StpUtil.getTokenValue();
        SaTokenConfig tokenConfig = SaManager.getConfig();
        Instant expires = Instant.now().plusSeconds(tokenConfig.getTimeout());
        List<String> permissions = SUPER_ADMIN_ROLE.equals(admin.getRole())
                ? List.of("*:*:*")
                : List.of();

        return new AdminLoginVO(
                admin.getUsername(),
                admin.getRealName(),
                normalizeAvatar(admin.getAvatar()),
                List.of(ADMIN_UI_ROLE),
                permissions,
                accessToken,
                expires
        );
    }

    @Override
    public AdminProfileVO getProfile() {
        Long adminId = StpUtil.getLoginIdAsLong();
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMessage());
        }
        return new AdminProfileVO(
                admin.getUsername(),
                admin.getRealName(),
                normalizeAvatar(admin.getAvatar()),
                admin.getPhone(),
                admin.getRole()
        );
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    private String normalizeAvatar(String avatar) {
        return avatar == null ? "" : avatar;
    }
}
