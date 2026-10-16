package org.example.bigevent.service;

import org.example.bigevent.pojo.dto.AdminIngestRunRow;
import org.example.bigevent.pojo.dto.AdminSourceRow;
import org.example.bigevent.pojo.dto.SourcePauseRequest;

import java.util.List;

/** 运营后台：来源状态查询与暂停/恢复、最近采集批次。 */
public interface AdminOperationsService {

    /**
     * 查询全部内容来源及其验证状态、暂停原因与最后成功时间。
     *
     * @return 来源列表
     */
    List<AdminSourceRow> sources();

    /**
     * 查询最近的采集运行批次。
     *
     * @param limit 条数上限
     * @return 采集批次列表，按时间倒序
     */
    List<AdminIngestRunRow> latestRuns(int limit);

    /**
     * 暂停某个来源：采集器随后会被策略接口拒绝运行。
     *
     * @param code    来源编码
     * @param request 暂停原因
     */
    void pause(String code, SourcePauseRequest request);

    /**
     * 恢复某个来源：把暂停状态回退到暂停前的验证状态。
     *
     * @param code 来源编码
     */
    void resume(String code);
}
