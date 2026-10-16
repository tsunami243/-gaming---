package org.example.bigevent.service.impl;

import org.example.bigevent.mapper.AdminOperationsMapper;
import org.example.bigevent.pojo.dto.AdminIngestRunRow;
import org.example.bigevent.pojo.dto.AdminSourceRow;
import org.example.bigevent.pojo.dto.SourcePauseRequest;
import org.example.bigevent.service.AdminOperationsService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 管理端「来源与采集运行」只读查询及暂停/恢复的业务实现。
 *
 * <p>暂停是发布门禁的一部分：暂停会把来源状态改为 {@code PAUSED} 并关闭自动发布，
 * 采集器的策略查询随即失败，从而停止该来源的采集。恢复则把来源退回 {@code FETCH_VERIFIED}，
 * 仍需再次评估后才允许真实运行。
 */
@Service
public class AdminOperationsServiceImpl implements AdminOperationsService {

    private final AdminOperationsMapper mapper;

    /**
     * 构造运维服务。
     *
     * @param mapper 来源与采集运行记录的读写
     */
    public AdminOperationsServiceImpl(AdminOperationsMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 查询全部内容来源（含验证状态、自动发布开关、暂停原因与最后成功时间）。
     *
     * @return 按验证状态与代码排序的来源列表
     */
    @Override
    public List<AdminSourceRow> sources() {
        return mapper.listSources();
    }

    /**
     * 查询最近的采集运行记录。
     *
     * @param limit 返回条数，必须在 1–100 之间
     * @return 最近运行记录；{@code limit} 越界时返回 422
     */
    @Override
    public List<AdminIngestRunRow> latestRuns(int limit) {
        if (limit < 1 || limit > 100) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "limit must be between 1 and 100");
        }
        return mapper.latestRuns(limit);
    }

    /**
     * 暂停某个来源的采集。
     *
     * @param code    来源代码
     * @param request 暂停请求，理由必填
     * @throws ResponseStatusException 来源不存在或已被暂停时返回 409
     */
    @Override
    @Transactional
    public void pause(String code, SourcePauseRequest request) {
        if (mapper.pauseSource(code, request.reason()) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "来源不存在或已暂停");
        }
    }

    /**
     * 恢复某个已暂停来源的采集。
     *
     * @param code 来源代码
     * @throws ResponseStatusException 来源不存在或当前未暂停时返回 409
     */
    @Override
    @Transactional
    public void resume(String code) {
        if (mapper.resumeSource(code) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "来源不存在或当前未暂停");
        }
    }
}
