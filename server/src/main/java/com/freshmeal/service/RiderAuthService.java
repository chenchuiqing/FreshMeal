package com.freshmeal.service;

import com.freshmeal.dto.rider.RiderAuthRequest;
import com.freshmeal.vo.rider.RiderLoginVO;

public interface RiderAuthService {

    RiderLoginVO login(RiderAuthRequest request);

    void logout();
}
