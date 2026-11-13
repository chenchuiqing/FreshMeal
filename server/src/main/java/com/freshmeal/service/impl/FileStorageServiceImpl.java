package com.freshmeal.service.impl;

import com.freshmeal.common.BusinessException;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.service.FileStorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private final Path uploadRoot;

    public FileStorageServiceImpl(@Value("${freshmeal.upload-dir:./uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String storeProductImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.UPLOAD_EMPTY.getCode(), ErrorCode.UPLOAD_EMPTY.getMessage());
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.UPLOAD_SIZE_EXCEEDED.getCode(), ErrorCode.UPLOAD_SIZE_EXCEEDED.getMessage());
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "读取上传文件失败");
        }

        String extension = detectExtension(bytes);
        if (extension == null) {
            throw new BusinessException(ErrorCode.UPLOAD_TYPE_UNSUPPORTED.getCode(), ErrorCode.UPLOAD_TYPE_UNSUPPORTED.getMessage());
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path directory = uploadRoot.resolve("products");
        try {
            Files.createDirectories(directory);
            Files.write(directory.resolve(filename), bytes);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage());
        }
        return "/api/files/products/" + filename;
    }

    /**
     * 依据魔数识别图片类型，不信任客户端文件名与 Content-Type。
     *
     * @return 规范化扩展名；未匹配到支持的类型时返回 {@code null}
     */
    private String detectExtension(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                && (bytes[4] & 0xFF) == 0x0D && (bytes[5] & 0xFF) == 0x0A
                && (bytes[6] & 0xFF) == 0x1A && (bytes[7] & 0xFF) == 0x0A) {
            return "png";
        }
        if (bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "webp";
        }
        return null;
    }

    /** 供日志或诊断使用的辅助方法：规范化原始文件名扩展名（小写、去点）。 */
    static String normalizeOriginalExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
