package org.example.bigevent.pojo;

/** 首期内容类型枚举，以稳定的小写编码持久化。 */
public enum ContentType {
    ANNOUNCEMENT("announcement"),
    PATCH_NOTES("patch_notes"),
    EVENT("event"),
    NEW_CONTENT("new_content"),
    GUIDE("guide"),
    ESPORTS("esports");

    private final String code;

    ContentType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
