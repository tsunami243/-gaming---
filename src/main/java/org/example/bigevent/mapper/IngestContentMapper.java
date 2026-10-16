package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.bigevent.pojo.Article;

/**
 * 采集入库写入：来源/分类/游戏的种子查询，以及文章、正文副本与游戏关联的插入。
 *
 * <p>本接口只服务内部采集链路（{@code InternalIngestController}），
 * 其调用方必须已完成服务令牌校验。</p>
 */
@Mapper
public interface IngestContentMapper {

    /**
     * 按来源编码查询来源 ID。
     *
     * @param sourceCode 来源编码
     * @return 来源 ID，不存在时返回 {@code null}
     */
    @Select("select id from content_source where code=#{sourceCode}")
    Long findSourceId(@Param("sourceCode") String sourceCode);

    /**
     * 兜底创建来源（仅在来源不存在时插入）。
     *
     * <p>新来源一律以 {@code CANDIDATE} 状态种入——即"能抓但不能真实运行"，
     * 必须人工提升验证状态后才会被采集器放行。{@code insert ignore} 保证幂等，
     * 已存在的来源（含人工调整过的名称与域名）不会被覆盖。</p>
     *
     * @param sourceCode     来源编码
     * @param sourceName     来源展示名
     * @param baseUrl        来源站点根
     * @param sourceLanguage 内容语言
     */
    @Insert("insert ignore into content_source(code,name,source_type,base_url,allowed_domains,game_scope,language,adapter_key,schedule_hours,candidate_limit,auto_publish_enabled,verification_status) values(#{sourceCode},#{sourceName},'UNKNOWN',#{baseUrl},JSON_ARRAY(),JSON_ARRAY(),#{sourceLanguage},#{sourceCode},24,20,0,'CANDIDATE')")
    void ensureSource(@Param("sourceCode") String sourceCode, @Param("sourceName") String sourceName, @Param("baseUrl") String baseUrl, @Param("sourceLanguage") String sourceLanguage);

    /**
     * 按游戏编码查询已启用游戏的 ID。
     *
     * @param gameCode 游戏编码
     * @return 游戏 ID，未启用或不存在时返回 {@code null}
     */
    @Select("select id from game where code=#{gameCode} and enabled=1")
    Integer findEnabledGameId(@Param("gameCode") String gameCode);

    /**
     * 把游戏编码映射为 legacy 分类 ID。
     *
     * @param gameCode 游戏编码（与 category_alias 对应）
     * @return 分类 ID，未映射时返回 {@code null}
     */
    @Select("select id from category where category_alias=#{gameCode}")
    Integer findCategoryId(@Param("gameCode") String gameCode);

    /**
     * 查询采集系统用户 ID（作为入库文章的 create_user）。
     *
     * @return 用户 ID，种子缺失时返回 {@code null}
     */
    @Select("select id from `user` where username='ingest-system'")
    Integer findIngestSystemUserId();

    /**
     * 按规范化 URL 指纹查重，避免同一篇公告重复入库。
     *
     * @param canonicalUrlHash 原文 URL 的 SHA-256
     * @return 已存在的文章 ID，未命中时返回 {@code null}
     */
    @Select("select id from article where canonical_url_hash=#{canonicalUrlHash}")
    Integer findArticleIdByCanonicalUrlHash(@Param("canonicalUrlHash") String canonicalUrlHash);

    /**
     * 插入采集文章，并回填自增主键到 {@code article.id}。
     *
     * <p>落库状态固定为「草稿」，审核状态由上层按 decision 决定
     * （一律进入待审核区，不会自动发布）。</p>
     *
     * @param article 文章实体
     */
    @Insert("insert into article(title,summary,content,cover_img,state,category_id,source_id,original_url,canonical_url_hash,original_title,source_language,original_published_at,original_published_date,ingested_at,content_type,reading_mode,workflow_status,workflow_reason_code,create_user,tags,is_carousel) values(#{title},#{summary},#{content},'',#{state},#{categoryId},#{sourceId},#{originalUrl},#{canonicalUrlHash},#{originalTitle},#{sourceLanguage},#{originalPublishedAt},#{originalPublishedDate},#{ingestedAt},#{contentType},#{readingMode},#{workflowStatus},#{workflowReasonCode},#{createUser},CAST(#{tags} AS JSON),0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertArticle(Article article);

    /**
     * 写入正文副本（原始清洗 HTML + 内容哈希），用于留痕与比对。
     *
     * @param articleId   文章 ID
     * @param content     清洗后的正文 HTML
     * @param contentHash 正文的 SHA-256
     */
    @Insert("insert into article_content(article_id,sanitized_html,content_hash,extractor_version) values(#{articleId},#{content},#{contentHash},'ingest-v1')")
    void insertArticleContent(@Param("articleId") Integer articleId, @Param("content") String content, @Param("contentHash") String contentHash);

    /**
     * 建立文章与游戏的关联。
     *
     * @param articleId 文章 ID
     * @param gameId    游戏 ID
     * @param primary   是否为主游戏（影响前端归类展示）
     */
    @Insert("insert into article_game(article_id,game_id,is_primary) values(#{articleId},#{gameId},#{primary})")
    void insertArticleGame(@Param("articleId") Integer articleId, @Param("gameId") Integer gameId, @Param("primary") boolean primary);

    /**
     * 把落库文章回填到采集回执上（回执先插入以获得幂等锁，随后挂文章）。
     *
     * @param eventId   事件 ID
     * @param articleId 文章 ID
     */
    @Update("update ingest_receipt set article_id=#{articleId} where event_id=#{eventId}")
    void attachArticle(@Param("eventId") String eventId, @Param("articleId") Integer articleId);
}
