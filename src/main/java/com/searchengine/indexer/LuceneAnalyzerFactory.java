package com.searchengine.indexer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.springframework.stereotype.Component;

import java.io.Reader;
import java.util.Map;

@Component
public class LuceneAnalyzerFactory {

    public Analyzer create() {
        Analyzer russian = new RussianAnalyzer();
        Analyzer combined = new CombinedRuEnAnalyzer();
        return new PerFieldAnalyzerWrapper(combined, Map.of(
            LuceneIndexer.FIELD_TITLE,   new CombinedRuEnAnalyzer(),
            LuceneIndexer.FIELD_CONTENT, new CombinedRuEnAnalyzer()
        ));
    }

    public static final class CombinedRuEnAnalyzer extends Analyzer {

        private static final CharArraySet STOP_WORDS;

        static {
            CharArraySet words = new CharArraySet(
                RussianAnalyzer.getDefaultStopSet().size() + 50, true
            );
            words.addAll(RussianAnalyzer.getDefaultStopSet());
            words.addAll(org.apache.lucene.analysis.en.EnglishAnalyzer.getDefaultStopSet());
            STOP_WORDS = CharArraySet.unmodifiableSet(words);
        }

        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
            StandardTokenizer tokenizer = new StandardTokenizer();
            TokenStream stream = new LowerCaseFilter(tokenizer);
            stream = new StopFilter(stream, STOP_WORDS);
            stream = new org.apache.lucene.analysis.ru.RussianLightStemFilter(stream);
            return new TokenStreamComponents(tokenizer, stream);
        }

        @Override
        protected Reader initReader(String fieldName, Reader reader) {
            return reader;
        }
    }
}
