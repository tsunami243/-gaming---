package org.example.bigevent.pojo.dto;

import java.util.List;

/** 公开游戏（即前端「分类」）响应：编码、中英文名、别名标签与是否启用。 */
public record GameResponse(String code, String nameZh, String nameOriginal, List<String> aliases, boolean enabled) {}
