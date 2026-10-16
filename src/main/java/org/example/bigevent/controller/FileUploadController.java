package org.example.bigevent.controller;

import org.example.bigevent.pojo.Result;
import org.example.bigevent.utils.AliOssUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

/**
 * 图片上传（legacy 接口，上传至阿里云 OSS）。
 *
 * <p>基础路径 {@code /upload}，需登录（当前仅要求 authenticated，未限管理员角色）。
 * 限制：单文件 ≤5MB，扩展名仅 jpg/jpeg/png/gif/webp/bmp；文件名服务端随机生成。
 */
@RestController
public class FileUploadController {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"
    );

    /**
     * 上传图片并返回可访问 URL。
     *
     * <p>{@code POST /upload}（multipart/form-data，字段名 {@code file}）
     *
     * @param file 上传文件，必填
     * @return 上传后的 URL；空文件/超限/非法扩展名返回业务错误信息
     */
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            return Result.error("未选择文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error("文件大小不能超过5MB");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.contains(".")) {
            return Result.error("无效的文件名");
        }
        String ext = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            return Result.error("不支持的文件类型，仅允许: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
        String finalName = UUID.randomUUID().toString() + ext;
        String url = AliOssUtil.uploadFile(finalName, file.getInputStream());
        return Result.success(url);
    }
}
