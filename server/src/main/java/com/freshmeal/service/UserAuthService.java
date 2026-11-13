package com.freshmeal.service;

import com.freshmeal.dto.user.UserAuthRequest;
import com.freshmeal.vo.user.UserLoginVO;

public interface UserAuthService {

    UserLoginVO register(UserAuthRequest request);

    UserLoginVO login(UserAuthRequest request);

    void logout();
}
