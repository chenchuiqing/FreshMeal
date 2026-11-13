package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.freshmeal.common.Result;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.FileStorageService;
import com.freshmeal.vo.admin.UploadVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 管理端文件上传接口，全部要求管理员角色。 */
@RestController
@RequestMapping("/api/admin/file")
@RequiredArgsConstructor
@SaCheckRole(StpInterfaceImpl.ROLE_ADMIN)
public class AdminFileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public Result<UploadVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(new UploadVO(fileStorageService.storeProductImage(file)));
    }
}
