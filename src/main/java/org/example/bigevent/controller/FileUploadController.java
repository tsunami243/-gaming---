package org.example.bigevent.controller;

import org.example.bigevent.pojo.Result;
import org.example.bigevent.utils.AliOssUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
public class FileUploadController {
    /**
     * 文件上传接口
     * 接收用户上传的文件，并将其保存到服务器指定目录
     *
     * @param file 用户上传的文件
     * @return Result<String> 包含操作结果的响应对象
     * @throws Exception 文件操作异常
     */
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) throws Exception {
        // 检查是否有文件被上传
        if (file.isEmpty()) {
            return Result.error("未选择文件");
        }
        // 获取原始文件名
        String fileName = file.getOriginalFilename();
        // 生成唯一文件名，防止文件名冲突，格式为：UUID.原扩展名
        String finalName = UUID.randomUUID().toString() + fileName.substring(fileName.lastIndexOf("."));
        // 上传文件到OSS
        String url = AliOssUtil.uploadFile(finalName, file.getInputStream());
        return Result.success(url);
    }
}