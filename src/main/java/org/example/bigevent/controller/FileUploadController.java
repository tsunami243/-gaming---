package org.example.bigevent.controller;

import org.example.bigevent.pojo.Result;
import org.example.bigevent.utils.AliOssUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

@RestController
public class FileUploadController {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"
    );

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
