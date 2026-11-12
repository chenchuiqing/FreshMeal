package com.freshmeal.vo.admin;

import java.time.Instant;
import java.util.List;

public record AdminLoginVO(
        String username,
        String nickname,
        String avatar,
        List<String> roles,
        List<String> permissions,
        String accessToken,
        Instant expires
) {
}
