package org.example.bigevent.service;

import org.example.bigevent.pojo.dto.IngestSourcePolicyResponse;

/** 来源启用策略：采集器运行前查询该来源是否可运行。 */
public interface IngestSourcePolicyService {

    /**
     * 查询某个来源当前的运行策略。
     *
     * <p>「可运行」由验证状态派生（仅 SHADOW / AUTO_ENABLED 放行），
     * 来源不存在时按不可运行处理（fail-closed）。</p>
     *
     * @param code 来源编码
     * @return 策略响应，含是否可运行与验证状态
     */
    IngestSourcePolicyResponse policy(String code);
}
