package com.freshmeal.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * 保存商品图片并返回可访问地址。
     *
     * @param file 上传的图片
     * @return 以 {@code /api/files/} 开头的访问地址
     */
    String storeProductImage(MultipartFile file);
}
