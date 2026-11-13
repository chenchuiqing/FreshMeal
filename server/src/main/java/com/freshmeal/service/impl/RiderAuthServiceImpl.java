package com.freshmeal.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.freshmeal.common.BusinessException;
import com.freshmeal.dto.rider.RiderAuthRequest;
import com.freshmeal.entity.DeliveryMan;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.mapper.DeliveryManMapper;
import com.freshmeal.service.RiderAuthService;
import com.freshmeal.vo.rider.RiderLoginVO;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RiderAuthServiceImpl implements RiderAuthService {

    private static final String RIDER_ROLE = "rider";

    private final DeliveryManMapper deliveryManMapper;
    private final PasswordEncoder passwordEncoder;
    private final StpLogic riderStpLogic = createLoginLogic();

    @Override
    public RiderLoginVO login(RiderAuthRequest request) {
        DeliveryMan deliveryMan = deliveryManMapper.selectOne(
                Wrappers.<DeliveryMan>lambdaQuery()
                        .eq(DeliveryMan::getUsername, request.username()));

        if (deliveryMan == null
                || !passwordEncoder.matches(request.password(), deliveryMan.getPassword())) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST.getCode(),
                    "用户名或密码错误"
            );
        }
        if (deliveryMan.getStatus() == null || deliveryMan.getStatus() != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "账号已禁用");
        }

        riderStpLogic.login(deliveryMan.getId());
        SaTokenConfig tokenConfig = SaManager.getConfig();
        Instant expires = Instant.now().plusSeconds(tokenConfig.getTimeout());

        return new RiderLoginVO(
                deliveryMan.getUsername(),
                deliveryMan.getRealName(),
                deliveryMan.getPhone(),
                deliveryMan.getAvatar() == null ? "" : deliveryMan.getAvatar(),
                List.of(RIDER_ROLE),
                riderStpLogic.getTokenValue(),
                expires
        );
    }

    @Override
    public void logout() {
        riderStpLogic.checkLogin();
        riderStpLogic.logout();
    }

    private static StpLogic createLoginLogic() {
        return new StpLogicJwtForSimple().setLoginType("rider");
    }
}
