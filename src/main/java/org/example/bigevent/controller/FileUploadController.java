package org.example.bigevent.controller;

import org.example.bigevent.pojo.Result;
import org.example.bigevent.utils.AliOssUtil;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

/**
 * 图片上传（legacy 接口，上传至阿里云 OSS）。
 *
 * <p>基础路径 {@code /upload}，需登录且具备 CONTENT_ADMIN / SUPER_ADMIN 角色
 * （角色限制见 SecurityConfig，写操作带成本故不放给任意登录用户）。</p>
 *
 * <p>限制：单文件 ≤5MB（同时由 {@code spring.servlet.multipart.max-file-size} 在进入控制器前拦截）；
 * 扩展名仅 jpg/jpeg/png/gif/webp/bmp，<b>且文件头魔数必须与扩展名对应</b>
 * （扩展名来自客户端，可随意伪造，仅看后缀等于没校验）。文件名由服务端随机生成。</p>
 */
@RestController
public class FileUploadController {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"
    );
    /** 读取的魔数长度：WEBP 需要看第 9~12 字节，故取 12。 */
    private static final int MAGIC_BYTES_LENGTH = 12;

    /**
     * 上传图片并返回可访问 URL。
     *
     * <p>{@code POST /upload}（multipart/form-data，字段名 {@code file}）</p>
     *
     * <p>{@link AliOssUtil#uploadFile(String, InputStream)} 自身会吞掉 OSS 异常并以空串表示失败，
     * 因此这里必须检查返回值，否则会把"上传失败"包装成 {@code success("")} 返回给前端。</p>
     *
     * @param file 上传文件，必填
     * @return 上传后的 URL；空文件/超限/非法扩展名/魔数不符/存储失败时返回业务错误信息
     * @throws Exception 读取上传流或存储客户端初始化失败时抛出
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
        if (!magicMatchesExtension(ext, peekMagicBytes(file))) {
            return Result.error("文件内容与扩展名不符");
        }
        String url = AliOssUtil.uploadFile(UUID.randomUUID() + ext, file.getInputStream());
        if (!StringUtils.hasLength(url)) {
            return Result.error("上传失败，请稍后重试");
        }
        return Result.success(url);
    }

    /**
     * 读取文件头部若干字节用于魔数判定。
     *
     * <p>注意这里与后续上传各取一次流：{@link MultipartFile#getInputStream()} 的通用约定是
     * 每次调用返回<b>新</b>的可读流，标准实现（内存或临时文件）都满足；若将来换成自定义实现需复核该假设。</p>
     *
     * @param file 上传文件
     * @return 最多 {@link #MAGIC_BYTES_LENGTH} 个字节；文件过短时返回实际长度
     * @throws IOException 读取失败时抛出
     */
    private static byte[] peekMagicBytes(MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(MAGIC_BYTES_LENGTH);
        }
    }

    /**
     * 判断文件头是否与该扩展名声明的图片格式一致。
     *
     * <p>只验"是不是某张图"是不够的——那样 {@code a.png} 里塞 JPEG 也能通过，
     * 与"内容与扩展名不符"的报错语义不符。这里按扩展名逐一比对对应魔数。</p>
     *
     * @param ext  已归一化的小写扩展名（含点）
     * @param head 文件头字节
     * @return 一致返回 {@code true}
     */
    private static boolean magicMatchesExtension(String ext, byte[] head) {
        return switch (ext) {
            case ".jpg", ".jpeg" -> isJpeg(head);
            case ".png" -> isPng(head);
            case ".gif" -> isGif(head);
            case ".bmp" -> isBmp(head);
            case ".webp" -> isWebp(head);
            default -> false;
        };
    }

    /** @return 是否为 JPEG（FF D8） */
    private static boolean isJpeg(byte[] head) {
        return head.length >= 2 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8;
    }

    /** @return 是否为 PNG（89 50 4E 47） */
    private static boolean isPng(byte[] head) {
        return head.length >= 4 && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G';
    }

    /** @return 是否为 GIF（47 49 46 38，即 "GIF8"） */
    private static boolean isGif(byte[] head) {
        return head.length >= 4 && head[0] == 'G' && head[1] == 'I' && head[2] == 'F' && head[3] == '8';
    }

    /** @return 是否为 BMP（42 4D，即 "BM"） */
    private static boolean isBmp(byte[] head) {
        return head.length >= 2 && head[0] == 'B' && head[1] == 'M';
    }

    /** @return 是否为 WEBP（"RIFF" + 4 字节长度 + "WEBP"） */
    private static boolean isWebp(byte[] head) {
        return head.length >= 12
                && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
    }
}
