package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 采集来源验证状态查询（用于发布门禁判定）。
 */
@Mapper
public interface IngestSourcePolicyMapper {

    /**
     * 查询来源的验证状态。
     *
     * @param code 来源编码
     * @return 验证状态（如 SHADOW / FETCH_VERIFIED / PAUSED）；来源不存在时返回 {@code null}
     */
    @Select("select verification_status from content_source where code=#{code}")
    String verificationStatus(@Param("code") String code);
}
