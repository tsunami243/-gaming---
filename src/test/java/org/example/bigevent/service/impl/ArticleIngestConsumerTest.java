package org.example.bigevent.service.impl;

import org.example.bigevent.pojo.Article;
import org.example.bigevent.service.ArticleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ArticleIngestConsumer 单元测试。
 *
 * 教学模式：本文件展示两种最常用的单元测试写法——
 *   A. 纯逻辑方法测试（无 Mock，直接调方法、断言返回值）
 *   B. Mock 依赖测试（用 Mockito 替换 Redis/Service，验证交互行为）
 *
 * 每个外层 @Test 就是一个独立测试用例，彼此隔离。
 */
@ExtendWith(MockitoExtension.class)   // 启用 Mockito：自动初始化 @Mock 和 @InjectMocks
@DisplayName("ArticleIngestConsumer")
class ArticleIngestConsumerTest {

    // ================================================================
    //  A. 纯逻辑测试 → toArticle()
    //
    //  被测方法 toArticle(Map) 是一个无外部依赖的纯数据转换方法。
    //  不需要 Mock 任何东西 — 构造一个 Map 传入，断言 Article 字段正确。
    //  这种测试最快、最稳定，应尽可能多地写。
    // ================================================================

    @Nested   // @Nested 只是将相关测试用例分组展示，无功能影响
    @DisplayName("toArticle — 数据转换")
    class ToArticleTests {

        private ArticleIngestConsumer consumer;

        @BeforeEach
        void setUp() {
            // 直接 new — 因为 toArticle 不依赖任何注入字段
            consumer = new ArticleIngestConsumer();
        }

        @Test
        @DisplayName("正常 JSON 数据 → Article 字段正确映射")
        void shouldMapAllFields() {
            // ── Given ──
            Map<String, Object> map = Map.of(
                    "title", "GTA6 发布",
                    "content", "<p>GTA6 将于 2025 年秋季发售，支持 PS5 和 Xbox Series X 平台，预购现已开启。</p>",
                    "category_id", 5,
                    "cover_img", "https://oss.example.com/cover.png",
                    "tags", "[\"GTA6\",\"Rockstar\"]"
            );

            // ── When ──
            Article article = consumer.toArticle(map);

            // ── Then ──
            assertEquals("GTA6 发布", article.getTitle());
            assertEquals("<p>GTA6 将于 2025 年秋季发售，支持 PS5 和 Xbox Series X 平台，预购现已开启。</p>",
                    article.getContent());
            assertEquals(5, article.getCategoryId());
            assertEquals("https://oss.example.com/cover.png", article.getCoverImg());
            assertEquals("[\"GTA6\",\"Rockstar\"]", article.getTags());
            assertEquals("草稿", article.getState());
        }

        @Test
        @DisplayName("category_id=0 → Article.categoryId=0")
        void shouldAcceptZeroCategoryId() {
            Map<String, Object> map = Map.of(
                    "title", "测试", "content", "a".repeat(50),
                    "category_id", 0, "cover_img", "", "tags", "[]"
            );

            Article article = consumer.toArticle(map);

            assertEquals(0, article.getCategoryId());
        }

        @Test
        @DisplayName("缺少字段 → 默认空字符串")
        void shouldDefaultMissingFieldsToEmpty() {
            Map<String, Object> map = Map.of(
                    "title", "最小数据",
                    "content", "x".repeat(50)
                    // 不含 category_id, cover_img, tags
            );

            Article article = consumer.toArticle(map);

            assertEquals("最小数据", article.getTitle());
            assertEquals(0, article.getCategoryId());   // Number 缺失 → 0
            assertEquals("", article.getCoverImg());     // String 缺失 → ""
            assertEquals("", article.getTags());
        }

        @Test
        @DisplayName("空标题 → 抛出 IllegalArgumentException")
        void shouldRejectBlankTitle() {
            Map<String, Object> map = Map.of(
                    "title", "   ",          // 全空格
                    "content", "x".repeat(50)
            );

            Exception e = assertThrows(IllegalArgumentException.class,
                    () -> consumer.toArticle(map));
            assertTrue(e.getMessage().contains("title"));
        }

        @Test
        @DisplayName("内容过短 (＜50 chars) → 抛出 IllegalArgumentException")
        void shouldRejectShortContent() {
            Map<String, Object> map = Map.of(
                    "title", "正常标题",
                    "content", "太短了"     // 只有 3 个字符
            );

            Exception e = assertThrows(IllegalArgumentException.class,
                    () -> consumer.toArticle(map));
            assertTrue(e.getMessage().contains("too short"));
        }
    }

    // ================================================================
    //  B. Mock 依赖测试 → consume()
    //
    //  consume() 依赖 StringRedisTemplate（Redis）和 ArticleService（DB）。
    //  单元测试不启动 Redis，而是用 Mockito 创建假对象：
    //    @Mock  → 假的依赖对象，行为和返回值由测试控制
    //    @InjectMocks → 把 @Mock 对象自动注入到被测对象中
    //
    //  口诀：
    //    when(mock.method()).thenReturn(value) → 预设返回值
    //    verify(mock).method(args)             → 验证是否被调用
    // ================================================================

    @Nested
    @DisplayName("consume — Redis 拉取 + 入库")
    class ConsumeTests {

        @Mock
        private StringRedisTemplate redisTemplate;

        @Mock
        private ListOperations<String, String> listOps;

        @Mock
        private ArticleService articleService;

        @InjectMocks
        private ArticleIngestConsumer consumer;

        @BeforeEach
        void setUp() {
            // 让 redisTemplate.opsForList() 返回我们的假 ListOperations
            when(redisTemplate.opsForList()).thenReturn(listOps);
        }

        @Test
        @DisplayName("队列为空 → 什么都不做")
        void shouldDoNothingWhenQueueEmpty() {
            // ── Given ──: LPOP 返回 null（队列为空）
            when(listOps.leftPop("ingest:article_queue")).thenReturn(null);

            // ── When ──
            consumer.consume();

            // ── Then ──: 没有调用任何 addArticle
            verify(articleService, never()).addArticle(any(), anyInt());
        }

        @Test
        @DisplayName("队列有有效数据 → 入库成功")
        void shouldIngestValidArticle() {
            // ── Given ──
            String json = """
                    {
                        "title": "GTA6 最新预告",
                        "content": "{%s}",
                        "category_id": 5,
                        "cover_img": "https://oss.example.com/img.png",
                        "tags": "[]",
                        "source": "游民星空"
                    }""".formatted("a".repeat(50));

            when(listOps.leftPop("ingest:article_queue")).thenReturn(json);

            // ── When ──
            consumer.consume();

            // ── Then ──: addArticle 被调用了 1 次
            // ArgumentCaptor 捕获传给 addArticle 的 Article 参数
            ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
            verify(articleService, times(1))
                    .addArticle(captor.capture(), eq(1));   // SYSTEM_USER_ID = 1

            Article captured = captor.getValue();
            assertEquals("GTA6 最新预告", captured.getTitle());
            assertEquals(5, captured.getCategoryId());
            assertEquals("草稿", captured.getState());
        }

        @Test
        @DisplayName("数据校验失败 (空标题) → 推回队列，不入库")
        void shouldPushBackOnInvalidData() {
            // ── Given ──: title 为空 → toArticle 抛异常
            String json = """
                    {
                        "title": "",
                        "content": "%s",
                        "category_id": 0,
                        "cover_img": "",
                        "tags": "[]"
                    }""".formatted("a".repeat(50));

            when(listOps.leftPop("ingest:article_queue")).thenReturn(json);

            // ── When ──
            consumer.consume();

            // ── Then ──
            // 1. 没有入库
            verify(articleService, never()).addArticle(any(), anyInt());
            // 2. 消息被推回队列
            verify(listOps, times(1))
                    .rightPush(eq("ingest:article_queue"), eq(json));
        }
    }
}
