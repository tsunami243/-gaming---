package org.example.bigevent.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 标签 JSON 编解码工具：把字符串标签列表序列化为数据库中存储的 JSON 数组文本。
 */
public final class JsonTags {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 工具类，禁止实例化。 */
    private JsonTags() { }

    /**
     * 把标签列表序列化为 JSON 数组文本。
     *
     * @param tags 标签列表，为 {@code null} 时按空数组处理
     * @return JSON 数组文本，例如 {@code ["补丁","赛事"]}
     * @throws IllegalArgumentException 序列化失败时抛出
     */
    public static String encode(List<String> tags) {
        try {
            return OBJECT_MAPPER.writeValueAsString(tags == null ? List.of() : tags);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("tags cannot be serialized", exception);
        }
    }
}
