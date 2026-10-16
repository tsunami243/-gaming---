package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.bigevent.pojo.dto.ArticleSourceRow;

import java.util.List;

/**
 * 文章来源名批量查询：避免列表/详情逐条查来源造成的 N+1。
 */
@Mapper
public interface ArticleSourceMapper {

    /**
     * 批量查询文章对应的来源展示名。
     *
     * @param ids 文章 ID 列表（需非空，空列表会生成非法 SQL 的 IN 子句）
     * @return 文章 ID → 来源名 的行列表
     */
    @Select("<script>select a.id as articleId,s.name as sourceName from article a join content_source s on s.id=a.source_id where a.id in <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<ArticleSourceRow> findNames(@Param("ids") List<Integer> ids);
}
