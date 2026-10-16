package org.example.bigevent.service;

import org.example.bigevent.pojo.dto.ArticleDetailResponse;
import org.example.bigevent.pojo.dto.ArticleSummaryResponse;
import org.example.bigevent.pojo.dto.GameResponse;
import org.example.bigevent.pojo.dto.PageResponse;

import java.util.List;

/**
 * 公开端资讯读取（v1，只读）。
 *
 * <p>集中承载公开读取的业务规则：参数校验、四游戏与内容类型白名单、DTO 组装、
 * 游戏标签与来源名的批量查询、以及详情正文的读取与文本化。
 * 控制器只负责接收请求与包装响应封套。
 */
public interface PublicArticleReadService {

    /** 启用中的四游戏字典，别名已解析为列表。 */
    List<GameResponse> listGames();

    /**
     * 已发布文章摘要分页。
     *
     * @param gameCode    可选游戏代码，须在白名单内
     * @param contentType 可选内容类型，须在六类内
     * @throws org.springframework.web.server.ResponseStatusException 参数非法时 422
     */
    PageResponse<ArticleSummaryResponse> listArticles(int pageNum, int pageSize, String gameCode, String contentType);

    /**
     * 已发布文章详情，包含可读正文。
     *
     * @throws org.springframework.web.server.ResponseStatusException 未发布或不存在时 404
     */
    ArticleDetailResponse articleDetail(int id);
}
