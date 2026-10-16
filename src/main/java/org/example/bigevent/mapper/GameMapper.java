package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.bigevent.pojo.dto.ArticleGameRow;
import org.example.bigevent.pojo.dto.GameRow;

import java.util.List;

/**
 * 游戏字典与「文章-游戏」关联查询。
 */
@Mapper
public interface GameMapper {

    /**
     * 查询所有已启用的游戏（即公开的「分类」列表）。
     *
     * @return 游戏行列表，按 ID 排序
     */
    @Select("select id, code, name_zh as nameZh, name_original as nameOriginal, aliases, enabled from game where enabled=1 order by id")
    List<GameRow> findEnabled();

    /**
     * 批量查询文章关联的游戏编码。
     *
     * <p>SQL 内硬编码四游戏白名单——与公开接口的 {@code ALLOWED_GAMES} 保持一致，
     * 新增游戏需同步修改两处。</p>
     *
     * @param articleIds 文章 ID 列表
     * @return 文章 ID → 游戏编码 的行列表（主游戏排在前面）
     */
    @Select("<script>select ag.article_id as articleId,g.code from article_game ag join game g on g.id=ag.game_id and g.enabled=1 where g.code in ('delta-force','valorant','cs2','apex-legends') and ag.article_id in <foreach collection='articleIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> order by ag.article_id,ag.is_primary desc,g.code</script>")
    List<ArticleGameRow> findArticleGames(@Param("articleIds") List<Integer> articleIds);
}
