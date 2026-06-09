package com.searchengine.indexer;

import com.searchengine.config.SearchProperties;
import com.searchengine.entity.Page;
import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LuceneIndexerSearcherTest {

    @TempDir
    Path tempDir;

    LuceneIndexer indexer;
    LuceneSearcher searcher;

    @BeforeEach
    void setUp() throws IOException {
        SearchProperties props = new SearchProperties();
        props.setIndexPath(tempDir.toString());
        props.setPageSize(10);
        props.setMaxResults(100);
        props.setSnippetLength(200);

        LuceneAnalyzerFactory factory = new LuceneAnalyzerFactory();
        indexer = new LuceneIndexer(props, factory);
        searcher = new LuceneSearcher(props, factory);
    }

    @AfterEach
    void tearDown() throws IOException {
        indexer.close();
        searcher.close();
    }

    @Test
    void indexAndSearch_findsPageByTitle() throws IOException, ParseException {
        indexer.indexPages(List.of(
            buildPage(1L, "https://example.com/java", "Java programming language", "Java is a programming language.")));

        SearchPage result = searcher.search("java", 0);

        assertThat(result.hits()).isNotEmpty();
        assertThat(result.hits().get(0).url()).isEqualTo("https://example.com/java");
        assertThat(result.hasMore()).isFalse();
    }

    @Test
    void indexAndSearch_findsRussianText() throws IOException, ParseException {
        indexer.indexPages(List.of(
            buildPage(2L, "https://example.com/cats", "Коты и кошки", "Домашние кошки — популярные питомцы.")));

        SearchPage result = searcher.search("кошки", 0);

        assertThat(result.hits()).isNotEmpty();
        assertThat(result.hits().get(0).pageId()).isEqualTo(2L);
    }

    @Test
    void search_returnsEmpty_forUnknownQuery() throws IOException, ParseException {
        indexer.indexPages(List.of(
            buildPage(3L, "https://example.com/foo", "Spring Boot guide", "Learn Spring Boot.")));

        SearchPage result = searcher.search("completelyrandomword12345", 0);

        assertThat(result.hits()).isEmpty();
        assertThat(result.hasMore()).isFalse();
    }

    @Test
    void search_sanitizesMaliciousQuery() throws IOException, ParseException {
        SearchPage result = searcher.search("<script>alert(1)</script>", 0);
        assertThat(result.hits()).isEmpty();
    }

    @Test
    void search_paginates_andReportsHasMore() throws IOException, ParseException {
        List<Page> pages = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            pages.add(buildPage((long) i, "https://example.com/p" + i,
                "Lucene tutorial part " + i, "Content about lucene search engine number " + i));
        }
        indexer.indexPages(pages);

        SearchPage page0 = searcher.search("lucene", 0);
        assertThat(page0.hits()).hasSize(10);
        assertThat(page0.hasMore()).isTrue();

        SearchPage page1 = searcher.search("lucene", 1);
        assertThat(page1.hits()).hasSize(10);
        assertThat(page1.hasMore()).isTrue();

        SearchPage page2 = searcher.search("lucene", 2);
        assertThat(page2.hits()).hasSize(5);
        assertThat(page2.hasMore()).isFalse();
    }

    @Test
    void search_buildsSnippetFromContent() throws IOException, ParseException {
        indexer.indexPages(List.of(
            buildPage(99L, "https://example.com/snippet", "Title",
                "The quick brown fox jumps over the lazy dog near the river bank.")));

        SearchPage result = searcher.search("fox", 0);

        assertThat(result.hits()).isNotEmpty();
        assertThat(result.hits().get(0).snippet()).contains("<mark>");
    }

    @Test
    void search_escapesHtmlInSnippet() throws IOException, ParseException {
        indexer.indexPages(List.of(
            buildPage(100L, "https://evil.com", "Title",
                "Some text with <script>alert('xss')</script> dangerous payload here.")));

        SearchPage result = searcher.search("dangerous", 0);

        assertThat(result.hits()).isNotEmpty();
        String snippet = result.hits().get(0).snippet();
        assertThat(snippet).doesNotContain("<script>");
        assertThat(snippet).contains("&lt;script&gt;");
    }

    @Test
    void countDocs_returnsCorrectCount() throws IOException {
        indexer.indexPages(List.of(
            buildPage(4L, "https://a.com", "Title A", "Content A"),
            buildPage(5L, "https://b.com", "Title B", "Content B")
        ));

        assertThat(searcher.countDocs()).isEqualTo(2);
    }

    private Page buildPage(Long id, String url, String title, String content) {
        return Page.builder()
            .id(id).url(url).urlHash("hash" + id)
            .title(title).content(content)
            .status(Page.PageStatus.FETCHED)
            .build();
    }
}
