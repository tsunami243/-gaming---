package org.example.bigevent.pojo;

import java.util.List;

/** 游戏字典项：由接口契约与采集契约共享的稳定游戏定义。 */
public record Game(
        Integer id,
        String code,
        String nameZh,
        String nameOriginal,
        List<String> aliases,
        boolean enabled
) {}
