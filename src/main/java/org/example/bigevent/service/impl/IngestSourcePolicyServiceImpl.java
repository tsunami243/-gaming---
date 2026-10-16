package org.example.bigevent.service.impl;

import org.example.bigevent.mapper.IngestSourcePolicyMapper;
import org.example.bigevent.pojo.dto.IngestSourcePolicyResponse;
import org.example.bigevent.service.IngestSourcePolicyService;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 采集来源发布门禁的业务实现。
 *
 * <p>采集器在真正运行前会调用本服务，用来源的验证状态决定是否允许开跑。
 * 只有进入影子运行（{@code SHADOW}）或已允许自动发布（{@code AUTO_ENABLED}）
 * 的来源才算「可运行」；其余状态一律 fail-closed，采集器直接跳过。
 *
 * <p>{@code enabled} 不是数据库里的独立列，而是由 {@code verification_status} 派生，
 * 避免状态与开关两处不一致。
 */
@Service
public class IngestSourcePolicyServiceImpl implements IngestSourcePolicyService {

    /** 允许真实运行的验证状态；不在此集合内的一律视为不可运行。 */
    private static final Set<String> RUNNABLE = Set.of("SHADOW", "AUTO_ENABLED");

    private final IngestSourcePolicyMapper mapper;

    /**
     * 构造来源策略服务。
     *
     * @param mapper 来源验证状态查询
     */
    public IngestSourcePolicyServiceImpl(IngestSourcePolicyMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 查询某个来源当前是否允许运行。
     *
     * @param code 来源代码，如 {@code apex-official}
     * @return 是否可运行，以及该来源的验证状态；来源不存在时状态为 {@code MISSING} 且不可运行
     */
    @Override
    public IngestSourcePolicyResponse policy(String code) {
        String status = mapper.verificationStatus(code);
        boolean enabled = status != null && RUNNABLE.contains(status);
        return new IngestSourcePolicyResponse(enabled, status == null ? "MISSING" : status);
    }
}
