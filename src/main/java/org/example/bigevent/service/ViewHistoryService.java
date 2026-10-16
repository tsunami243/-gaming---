package org.example.bigevent.service;

/** 浏览历史：记录浏览（自动去重）并提供浏览量统计。 */
public interface ViewHistoryService {

    /**
     * 记录一次浏览，24 小时内同一用户或同一 IP 只记一次。
     *
     * <p>登录用户按 {@code userId} 去重，游客按 {@code ipAddress} 去重。</p>
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID，游客传 null
     * @param ipAddress 访客 IP，登录用户可为 null
     */
    void recordView(Integer articleId, Integer userId, String ipAddress);

    /**
     * 获取文章总浏览量。
     *
     * @param articleId 文章 ID
     * @return 浏览次数
     */
    Integer getViewCount(Integer articleId);

    /**
     * 获取文章独立浏览量（按去重后的用户数统计）。
     *
     * @param articleId 文章 ID
     * @return 独立浏览数
     */
    Integer getUniqueViewCount(Integer articleId);
}
