package com.searchengine.indexer;

import com.searchengine.config.SearchProperties;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLEncoder;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class LuceneSearcher {

    private static final int MAX_HIGHLIGHT_CHARS = 50_000;

    private final SearchProperties props;
    private final Analyzer analyzer;
    private final FSDirectory directory;

    public LuceneSearcher(SearchProperties props, LuceneAnalyzerFactory analyzerFactory) throws IOException {
        this.props = props;
        this.analyzer = analyzerFactory.create();
        Path indexPath = Path.of(props.getIndexPath());
        Files.createDirectories(indexPath);
        this.directory = FSDirectory.open(indexPath);
    }

    public SearchPage search(String queryString, int page) throws IOException, ParseException {
        if (!DirectoryReader.indexExists(directory)) {
            return SearchPage.empty();
        }

        String sanitized = QueryParser.escape(queryString.strip());
        if (sanitized.isBlank()) {
            return SearchPage.empty();
        }

        int pageSize = props.getPageSize();
        int offset = page * pageSize;
        if (offset >= props.getMaxResults()) {
            return SearchPage.empty();
        }

        try (DirectoryReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);

            MultiFieldQueryParser parser = new MultiFieldQueryParser(
                new String[]{LuceneIndexer.FIELD_TITLE, LuceneIndexer.FIELD_CONTENT},
                analyzer
            );
            parser.setDefaultOperator(QueryParser.Operator.AND);

            Query query = parser.parse(sanitized);

            TopDocs topDocs = searcher.search(query, props.getMaxResults());
            long totalHits = Math.min(topDocs.totalHits.value, props.getMaxResults());

            List<SearchHit> hits = collectHits(searcher, query, topDocs, offset, pageSize);
            boolean hasMore = (long) offset + pageSize < totalHits;

            return new SearchPage(hits, hasMore, totalHits);
        }
    }

    private List<SearchHit> collectHits(
        IndexSearcher searcher,
        Query query,
        TopDocs topDocs,
        int offset,
        int pageSize
    ) throws IOException {
        SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<mark>", "</mark>");
        Highlighter highlighter = new Highlighter(formatter, new SimpleHTMLEncoder(), new QueryScorer(query));
        highlighter.setTextFragmenter(new SimpleFragmenter(props.getSnippetLength()));
        highlighter.setMaxDocCharsToAnalyze(MAX_HIGHLIGHT_CHARS);

        List<SearchHit> hits = new ArrayList<>();
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        int limit = Math.min(offset + pageSize, scoreDocs.length);
        org.apache.lucene.index.StoredFields storedFields = searcher.storedFields();

        for (int i = offset; i < limit; i++) {
            Document doc = storedFields.document(scoreDocs[i].doc);
            String id      = doc.get(LuceneIndexer.FIELD_ID);
            String url     = doc.get(LuceneIndexer.FIELD_URL);
            String title   = doc.get(LuceneIndexer.FIELD_TITLE);
            String content = doc.get(LuceneIndexer.FIELD_CONTENT);
            String snippet = buildSnippet(highlighter, content, title);

            hits.add(new SearchHit(
                id != null ? Long.parseLong(id) : -1L,
                url,
                title,
                snippet,
                scoreDocs[i].score
            ));
        }
        return hits;
    }

    private String buildSnippet(Highlighter highlighter, String content, String title) {
        String source = (content != null && !content.isBlank()) ? content : title;
        if (source == null || source.isBlank()) return "";
        String field = (content != null && !content.isBlank())
            ? LuceneIndexer.FIELD_CONTENT
            : LuceneIndexer.FIELD_TITLE;
        try {
            String fragment = highlighter.getBestFragment(analyzer, field, source);
            return fragment != null ? fragment : truncate(source);
        } catch (IOException | InvalidTokenOffsetsException e) {
            log.debug("Snippet generation failed: {}", e.getMessage());
            return truncate(source);
        }
    }

    private String truncate(String text) {
        String escaped = new SimpleHTMLEncoder().encodeText(text);
        return escaped.length() > props.getSnippetLength()
            ? escaped.substring(0, props.getSnippetLength()) + "…"
            : escaped;
    }

    public long countDocs() throws IOException {
        if (!DirectoryReader.indexExists(directory)) return 0;
        try (DirectoryReader reader = DirectoryReader.open(directory)) {
            return reader.numDocs();
        }
    }

    @PreDestroy
    public void close() {
        try {
            analyzer.close();
            directory.close();
        } catch (IOException e) {
            log.error("Error closing Lucene searcher resources", e);
        }
    }
}
