package com.school.classboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class NewsSyncServiceTest {

    @Test
    void parseListExtractsDatedArticlesAndResolvesLinks() {
        String html = """
                <div class="list">
                  <a href="/20261019/abc123/c.html">新华网教育新闻标题示例一</a>
                  <a href="/20261018/def456/c.html">新华网教育新闻标题示例二</a>
                  <a href="/about.html">关于我们</a>
                  <a href="javascript:void(0)">更多新闻</a>
                </div>
                """;

        List<NewsSyncService.Draft> drafts = NewsSyncService.parseList(
                html, "http://education.news.cn/", "新华网", "教育", "xinhua-edu");

        assertEquals(2, drafts.size());
        NewsSyncService.Draft draft = drafts.get(0);
        assertEquals("新华网教育新闻标题示例一", draft.title());
        assertEquals("新华网", draft.source());
        assertEquals("教育", draft.category());
        assertTrue(draft.url().startsWith("http://education.news.cn/20261019/"));
        assertTrue(draft.key().startsWith("xinhua-edu:"));
        assertEquals(LocalDate.of(2026, 10, 19), draft.publishedAt().toLocalDate());
    }

    @Test
    void parseListUnderstandsPeopleDailyAndEducationDailyLinks() {
        String html = """
                <a href="/n1/2026/1018/c1006-40816819.html">人民网教育频道新闻标题示例</a>
                <a href="/rmtzgjyb/202610/t20261019_2111531733.html">中国教育报新闻标题示例文本</a>
                """;

        List<NewsSyncService.Draft> drafts = NewsSyncService.parseList(
                html, "http://edu.people.com.cn/", "人民网", "教育", "people-edu");

        assertEquals(2, drafts.size());
        assertEquals(LocalDate.of(2026, 10, 18), drafts.get(0).publishedAt().toLocalDate());
        assertEquals(LocalDate.of(2026, 10, 19), drafts.get(1).publishedAt().toLocalDate());
    }

    @Test
    void parseListSkipsOldArticles() {
        String html = """
                <a href="/20200101/old/c.html">很久以前的教育新闻示例文本</a>
                <a href="/20261019/new/c.html">最近的教育新闻示例文本内容</a>
                """;

        List<NewsSyncService.Draft> drafts = NewsSyncService.parseList(
                html, "http://education.news.cn/", "新华网", "教育", "xinhua-edu");

        assertEquals(1, drafts.size());
        assertEquals("最近的教育新闻示例文本内容", drafts.get(0).title());
    }
}
