package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.bigevent.pojo.dto.AdminIngestRunRow;
import org.example.bigevent.pojo.dto.AdminSourceRow;

import java.util.List;

/**
 * 运营后台数据访问：来源清单与暂停/恢复、采集批次查询。
 */
@Mapper
public interface AdminOperationsMapper {

    /**
     * 查询全部内容来源（按验证状态、编码排序）。
     *
     * @return 来源行列表
     */
    @Select("select id,code,name,source_type as sourceType,language,adapter_key as adapterKey,schedule_hours as scheduleHours,candidate_limit as candidateLimit,auto_publish_enabled as autoPublishEnabled,verification_status as verificationStatus,last_verified_at as lastVerifiedAt,last_success_at as lastSuccessAt,paused_at as pausedAt,pause_reason as pauseReason,update_time as updateTime from content_source order by verification_status, code")
    List<AdminSourceRow> listSources();

    /**
     * 暂停来源：置为 PAUSED 并记录暂停时间与原因，同时关闭自动发布。
     *
     * <p>带 {@code paused_at is null} 条件，重复暂停不会覆盖首次的暂停时间。</p>
     *
     * @param code   来源编码
     * @param reason 暂停原因
     * @return 受影响行数；0 表示来源已是暂停状态
     */
    @Update("update content_source set paused_at=now(),pause_reason=#{reason},auto_publish_enabled=0,verification_status='PAUSED',update_time=now() where code=#{code} and paused_at is null")
    int pauseSource(@Param("code") String code, @Param("reason") String reason);

    /**
     * 恢复来源：清空暂停信息并回到 SHADOW 状态。
     *
     * @param code 来源编码
     * @return 受影响行数；0 表示来源不在暂停状态
     */
    @Update("update content_source set paused_at=null,pause_reason=null,verification_status='SHADOW',update_time=now() where code=#{code} and verification_status='PAUSED'")
    int resumeSource(@Param("code") String code);

    /**
     * 查询最近的采集运行批次，按开始时间倒序。
     *
     * @param limit 条数上限
     * @return 采集批次行列表
     */
    @Select("select run_id as runId,source_id as sourceId,status,started_at as startedAt,finished_at as finishedAt,discovered_count as discoveredCount,processed_count as processedCount,duplicate_count as duplicateCount,review_count as reviewCount,rejected_count as rejectedCount,failed_count as failedCount,daily_candidate_budget_used as dailyCandidateBudgetUsed,paused_reason as pausedReason from ingest_run order by started_at desc limit #{limit}")
    List<AdminIngestRunRow> latestRuns(@Param("limit") int limit);
}
