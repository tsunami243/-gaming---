package org.example.bigevent.pojo.dto;

/** 游戏字典行投影：直接对应数据库列，别名仍是未解析的原始字符串。 */
public record GameRow(Integer id, String code, String nameZh, String nameOriginal, String aliases, Boolean enabled) {}
