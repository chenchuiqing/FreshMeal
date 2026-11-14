package com.freshmeal.security;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;

public final class RiderLoginContext {

    private static final StpLogic RIDER_LOGIN =
            new StpLogicJwtForSimple().setLoginType(StpInterfaceImpl.RIDER_LOGIN_TYPE);

    private RiderLoginContext() {
    }

    public static long currentRiderId() {
        return RIDER_LOGIN.getLoginIdAsLong();
    }
}
