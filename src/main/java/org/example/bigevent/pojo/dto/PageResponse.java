package org.example.bigevent.pojo.dto;

import java.util.List;

/** 通用分页响应封套：当前页数据 + 总数 + 页码信息。 */
public record PageResponse<T>(List<T> items, long total, int pageNum, int pageSize) {}
