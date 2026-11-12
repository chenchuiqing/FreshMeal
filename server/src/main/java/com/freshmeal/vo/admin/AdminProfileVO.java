package com.freshmeal.vo.admin;

public record AdminProfileVO(
        String username,
        String nickname,
        String avatar,
        String phone,
        String role
) {
}
