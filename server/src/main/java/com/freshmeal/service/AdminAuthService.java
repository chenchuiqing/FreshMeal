package com.freshmeal.service;

import com.freshmeal.dto.admin.AdminLoginRequest;
import com.freshmeal.vo.admin.AdminLoginVO;
import com.freshmeal.vo.admin.AdminProfileVO;

public interface AdminAuthService {

    AdminLoginVO login(AdminLoginRequest request);

    AdminProfileVO getProfile();

    void logout();
}
