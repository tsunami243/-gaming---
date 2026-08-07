package org.example.bigevent.push;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PushTaskMapper {

    void insert(PushTask task);

    PushTask findByMessageId(@Param("messageId") String messageId);

    /**
     * 乐观锁式状态流转：只有当前状态匹配时才更新（对应状态机非法流转防护）。
     *
     * @return 更新行数（0 表示状态已被其他线程改过）
     */
    int transition(@Param("messageId") String messageId,
                   @Param("fromStatus") String fromStatus,
                   @Param("toStatus") String toStatus,
                   @Param("error") String error);

    int bumpRetry(@Param("messageId") String messageId);

    int updateError(@Param("messageId") String messageId, @Param("error") String error);

    List<PushTask> listByStatus(@Param("status") String status);

    long countByStatus(@Param("status") String status);
}

