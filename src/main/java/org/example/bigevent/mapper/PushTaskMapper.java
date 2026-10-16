package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.bigevent.pojo.PushTask;

import java.util.List;

/**
 * 推送任务数据访问（SQL 在 {@code PushTaskMapper.xml} 中）。
 */
@Mapper
public interface PushTaskMapper {

    /**
     * 插入一条推送任务（初始状态 PENDING）。
     *
     * @param task 推送任务实体
     */
    void insert(PushTask task);

    /**
     * 按消息 ID 查询任务（消息 ID 即幂等键）。
     *
     * @param messageId 消息 ID
     * @return 推送任务，不存在时返回 {@code null}
     */
    PushTask findByMessageId(@Param("messageId") String messageId);

    /**
     * 乐观锁式状态流转：只有当前状态匹配时才更新（对应状态机非法流转防护）。
     *
     * @param messageId  消息 ID
     * @param fromStatus 期望的当前状态
     * @param toStatus   目标状态
     * @param error      失败原因（成功时可为 null）
     * @return 更新行数（0 表示状态已被其他线程改过）
     */
    int transition(@Param("messageId") String messageId,
                   @Param("fromStatus") String fromStatus,
                   @Param("toStatus") String toStatus,
                   @Param("error") String error);

    /**
     * 重试次数加一。
     *
     * @param messageId 消息 ID
     * @return 更新行数
     */
    int bumpRetry(@Param("messageId") String messageId);

    /**
     * 仅更新失败原因（不改状态）。
     *
     * @param messageId 消息 ID
     * @param error     失败原因
     * @return 更新行数
     */
    int updateError(@Param("messageId") String messageId, @Param("error") String error);

    /**
     * 按状态查询任务列表（死信补偿、运维排查用）。
     *
     * @param status 推送任务状态
     * @return 任务列表
     */
    List<PushTask> listByStatus(@Param("status") String status);

    /**
     * 按状态统计任务数（指标导出用）。
     *
     * @param status 推送任务状态
     * @return 任务数量
     */
    long countByStatus(@Param("status") String status);
}
