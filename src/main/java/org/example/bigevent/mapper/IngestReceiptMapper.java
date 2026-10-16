package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.bigevent.pojo.dto.IngestReceipt;

/**
 * 采集回执数据访问：以 {@code event_id} 唯一键实现投递幂等。
 */
@Mapper
public interface IngestReceiptMapper {

    /**
     * 按事件 ID 查询回执。
     *
     * @param eventId 事件 ID
     * @return 回执，不存在时返回 {@code null}
     */
    @Select("select event_id as eventId,payload_hash as payloadHash,article_id as articleId,final_decision as decision,created_at as createdAt from ingest_receipt where event_id=#{eventId}")
    IngestReceipt find(@Param("eventId") String eventId);

    /**
     * 插入回执（article_id 留空，落库成功后再回填）。
     *
     * <p>并发重复投递时依赖 {@code event_id} 唯一约束抛冲突，由事务回滚保证不会重复建文章。</p>
     *
     * @param eventId      事件 ID
     * @param payloadHash  载荷指纹
     * @param articleId    文章 ID（首次插入时传 null）
     * @param decision     最终决策
     * @param ruleVersion  规则版本
     */
    @Insert("insert into ingest_receipt(event_id,payload_hash,article_id,final_decision,rule_version) values(#{eventId},#{payloadHash},#{articleId},#{decision},#{ruleVersion})")
    void insert(@Param("eventId") String eventId, @Param("payloadHash") String payloadHash, @Param("articleId") Integer articleId, @Param("decision") String decision, @Param("ruleVersion") String ruleVersion);

    /**
     * 把落库文章回填到回执上。
     *
     * @param eventId   事件 ID
     * @param articleId 文章 ID
     */
    @Update("update ingest_receipt set article_id=#{articleId} where event_id=#{eventId}")
    void attachArticle(@Param("eventId") String eventId, @Param("articleId") Integer articleId);
}
