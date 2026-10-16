package org.example.bigevent.pojo;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.example.bigevent.anno.State;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

/**
 * 文章实体（legacy 主模型）：既承载管理员/作者原创文章，也承载采集入库的文章。
 *
 * <p>采集链路会额外填充来源相关字段（sourceId / originalUrl / canonicalUrlHash /
 * originalTitle / sourceLanguage / originalPublishedAt 等）；管理员原创文章这些字段为空。</p>
 *
 * <p>{@code state} 是 legacy 的发布状态（已发布/草稿），新模型另用 {@code workflowStatus}
 * 表达审核生命周期，二者暂时并存以兼容旧接口。</p>
 */
public class Article {
    private Integer id;//主键ID（新建时自动生成，更新时必传）
    @NotEmpty(message = "文章标题不能为空")
    @Pattern(regexp = "^[\\S ]{1,50}$",message = "文章标题只能包含字母、数字、下划线、中文和空格")
    private String title;//文章标题
    private String summary;//首期摘要，摘要阅读模式下公开展示
    @NotEmpty(message = "文章内容不能为空")
    private String content;//文章内容
    @URL(message = "封面图像格式错误")
    private String coverImg;//封面图像
    @State
    private String state;//发布状态 已发布|草稿
    @NotNull(message = "文章分类不能为空")
    private Integer categoryId;//文章分类id
    private Long sourceId;//内容来源ID，管理员原创文章为null
    private String originalUrl;//原文链接
    private String canonicalUrlHash;//规范化原文链接SHA-256
    private String originalTitle;//原始标题
    private String sourceLanguage;//原文语言，如zh-CN/en
    private java.time.LocalDate originalPublishedDate;
    private LocalDateTime originalPublishedAt;//来源发布时间
    private LocalDateTime ingestedAt;//采集入站时间
    private LocalDateTime publishedAt;//站内发布时间
    private String contentType;//公告、更新、活动、内容、攻略、赛事的稳定代码
    private String readingMode = ReadingMode.FULL_TEXT.name();//站内直接阅读已入库正文
    private String workflowStatus = WorkflowStatus.DRAFT.name();//审核生命周期
    private String workflowReasonCode;//拒绝/审核原因码
    private Integer revision = 0;//管理员更新的乐观锁版本
    @JsonIgnore
    private Integer createUser;//创建人ID
    private LocalDateTime createTime;//创建时间
    private LocalDateTime updateTime;//更新时间
    private Integer views = 0;//浏览量
    private Integer uniqueViews = 0;//独立浏览量
    private Integer favorites = 0;//收藏数
    private String tags = "";//标签，JSON数组格式存储
    private Integer isCarousel = 0;//是否轮播图文章，0否，1是
    private Integer heat = 0;//热度（ZSet 热度榜，定时回写）

    public Article() {
    }

    public Article(Integer id, String title, String content, String coverImg, String state, Integer categoryId, Integer createUser, LocalDateTime createTime, LocalDateTime updateTime, Integer views, Integer uniqueViews, Integer favorites, String tags, Integer isCarousel) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.coverImg = coverImg;
        this.state = state;
        this.categoryId = categoryId;
        this.createUser = createUser;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.views = views;
        this.uniqueViews = uniqueViews;
        this.favorites = favorites;
        this.tags = tags;
        this.isCarousel = isCarousel;
    }

    /**
     * 获取
     * @return id
     */
    public Integer getId() {
        return id;
    }

    /**
     * 设置
     * @param id
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * 获取
     * @return title
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置
     * @param title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 获取
     * @return content
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置
     * @param content
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 获取
     * @return coverImg
     */
    public String getCoverImg() {
        return coverImg;
    }

    /**
     * 设置
     * @param coverImg
     */
    public void setCoverImg(String coverImg) {
        this.coverImg = coverImg;
    }

    /**
     * 获取
     * @return state
     */
    public String getState() {
        return state;
    }

    /**
     * 设置
     * @param state
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * 获取
     * @return categoryId
     */
    public Integer getCategoryId() {
        return categoryId;
    }

    /**
     * 设置
     * @param categoryId
     */
    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    /**
     * 获取
     * @return createUser
     */
    public Integer getCreateUser() {
        return createUser;
    }

    /**
     * 设置
     * @param createUser
     */
    public void setCreateUser(Integer createUser) {
        this.createUser = createUser;
    }

    /**
     * 获取
     * @return createTime
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 设置
     * @param createTime
     */
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 获取
     * @return updateTime
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * 设置
     * @param updateTime
     */
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getViews() {
        return views;
    }

    public void setViews(Integer views) {
        this.views = views;
    }

    public Integer getUniqueViews() {
        return uniqueViews;
    }

    public void setUniqueViews(Integer uniqueViews) {
        this.uniqueViews = uniqueViews;
    }

    public Integer getFavorites() {
        return favorites;
    }

    public void setFavorites(Integer favorites) {
        this.favorites = favorites;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Integer getIsCarousel() {
        return isCarousel;
    }

    public void setIsCarousel(Integer isCarousel) {
        this.isCarousel = isCarousel;
    }

    public Integer getHeat() {
        return heat;
    }

    public void setHeat(Integer heat) {
        this.heat = heat;
    }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }
    public String getCanonicalUrlHash() { return canonicalUrlHash; }
    public void setCanonicalUrlHash(String canonicalUrlHash) { this.canonicalUrlHash = canonicalUrlHash; }
    public String getOriginalTitle() { return originalTitle; }
    public void setOriginalTitle(String originalTitle) { this.originalTitle = originalTitle; }
    public String getSourceLanguage() { return sourceLanguage; }
    public void setSourceLanguage(String sourceLanguage) { this.sourceLanguage = sourceLanguage; }
    public java.time.LocalDate getOriginalPublishedDate() { return originalPublishedDate; }
    public void setOriginalPublishedDate(java.time.LocalDate value) { originalPublishedDate = value; }
    public LocalDateTime getOriginalPublishedAt() { return originalPublishedAt; }
    public void setOriginalPublishedAt(LocalDateTime originalPublishedAt) { this.originalPublishedAt = originalPublishedAt; }
    public LocalDateTime getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(LocalDateTime ingestedAt) { this.ingestedAt = ingestedAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getReadingMode() { return readingMode; }
    public void setReadingMode(String readingMode) { this.readingMode = readingMode; }
    public String getWorkflowStatus() { return workflowStatus; }
    public void setWorkflowStatus(String workflowStatus) { this.workflowStatus = workflowStatus; }
    public String getWorkflowReasonCode() { return workflowReasonCode; }
    public void setWorkflowReasonCode(String workflowReasonCode) { this.workflowReasonCode = workflowReasonCode; }
    public Integer getRevision() { return revision; }
    public void setRevision(Integer revision) { this.revision = revision; }

    public String toString() {
        return "Article{id = " + id + ", title = " + title + ", content = " + content + ", coverImg = " + coverImg + ", state = " + state + ", categoryId = " + categoryId + ", createUser = " + createUser + ", createTime = " + createTime + ", updateTime = " + updateTime + "}";
    }
}
