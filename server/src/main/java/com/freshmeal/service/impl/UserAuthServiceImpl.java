package com.freshmeal.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.freshmeal.common.BusinessException;
import com.freshmeal.dto.user.UserAuthRequest;
import com.freshmeal.entity.User;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.mapper.UserMapper;
import com.freshmeal.service.UserAuthService;
import com.freshmeal.vo.user.UserLoginVO;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {

    private static final String USER_ROLE = "user";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final StpLogic userStpLogic = createLoginLogic();

    @Override
    public UserLoginVO register(UserAuthRequest request) {
        if (userMapper.exists(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, request.username()))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "用户名已存在");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname("");
        user.setStatus(1);
        user.setDeleted(0);

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "用户名已存在");
        }

        return login(user);
    }

    @Override
    public UserLoginVO login(UserAuthRequest request) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, request.username()));

        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "账号已禁用");
        }

        return login(user);
    }

    @Override
    public void logout() {
        userStpLogic.checkLogin();
        userStpLogic.logout();
    }

    private UserLoginVO login(User user) {
        userStpLogic.login(user.getId());
        String accessToken = userStpLogic.getTokenValue();
        SaTokenConfig tokenConfig = SaManager.getConfig();
        Instant expires = Instant.now().plusSeconds(tokenConfig.getTimeout());
        String nickname = user.getNickname() == null ? "" : user.getNickname();

        return new UserLoginVO(
                user.getUsername(),
                nickname,
                accessToken,
                expires,
                List.of(USER_ROLE)
        );
    }

    private static StpLogic createLoginLogic() {
        return new StpLogicJwtForSimple().setLoginType("user");
    }
}
