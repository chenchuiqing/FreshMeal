package com.freshmeal.vo.rider;

import java.time.Instant;
import java.util.List;

public record RiderLoginVO(
        String username,
        String realName,
        String phone,
        String avatar,
        List<String> roles,
        String accessToken,
        Instant expires
) {
}
