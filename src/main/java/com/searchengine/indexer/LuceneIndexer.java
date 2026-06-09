package com.searchengine.indexer;

import com.searchengine.config.SearchProperties;
import com.searchengine.entity.Page;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

@Component
@Slf4j
public class LuceneIndexer {

    static final String FIELD_ID      = "id";
    static final String FIELD_URL     = "url";
    static final String FIELD_TITLE   = "title";
    static final String FIELD_CONTENT = "content";

    private final IndexWriter writer;

    public LuceneIndexer(SearchProperties props, LuceneAnalyzerFactory analyzerFactory) throws IOException {
        Path indexPath = Path.of(props.getIndexPath());
        Files.createDirectories(indexPath);

        Analyzer analyzer = analyzerFactory.create();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        config.setRAMBufferSizeMB(64);

        this.writer = new IndexWriter(FSDirectory.open(indexPath), config);
    }

    public void indexPage(Page page) throws IOException {
        Document doc = toDocument(page);
        writer.updateDocument(new Term(FIELD_ID, String.valueOf(page.getId())), doc);
    }

    public void indexPages(Collection<Page> pages) throws IOException {
        for (Page page : pages) {
            indexPage(page);
        }
        writer.commit();
        log.debug("Indexed {} pages, commit done", pages.size());
    }

    public void deletePage(Long pageId) throws IOException {
        writer.deleteDocuments(new Term(FIELD_ID, String.valueOf(pageId)));
        writer.commit();
    }

    public void commit() throws IOException {
        writer.commit();
    }

    @PreDestroy
    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            log.error("Error closing Lucene IndexWriter", e);
        }
    }

    private Document toDocument(Page page) {
        Document doc = new Document();
        doc.add(new StoredField(FIELD_ID,    String.valueOf(page.getId())));
        doc.add(new StoredField(FIELD_URL,   page.getUrl()));
        doc.add(new TextField(FIELD_TITLE,   nullSafe(page.getTitle()),   Field.Store.YES));
        doc.add(new TextField(FIELD_CONTENT, nullSafe(page.getContent()), Field.Store.YES));
        return doc;
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
