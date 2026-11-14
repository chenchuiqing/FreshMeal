package com.freshmeal.security;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;

public final class ShopLoginContext {

    private static final StpLogic USER_LOGIN =
            new StpLogicJwtForSimple().setLoginType(StpInterfaceImpl.USER_LOGIN_TYPE);

    private ShopLoginContext() {
    }

    public static long currentUserId() {
        return USER_LOGIN.getLoginIdAsLong();
    }
}
