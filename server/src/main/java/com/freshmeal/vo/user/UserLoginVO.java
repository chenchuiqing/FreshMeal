package com.freshmeal.vo.user;

import java.time.Instant;
import java.util.List;

public record UserLoginVO(
        String username,
        String nickname,
        String accessToken,
        Instant expires,
        List<String> roles
) {
}
