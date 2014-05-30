package edu.mit.nlp.segmenter.dp;

import com.google.common.base.Splitter;
import in.aesh.segment.PorterStemmer;
import in.aesh.segment.Segment;
import in.aesh.segment.Stemmer;
import in.aesh.segment.Utils;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import org.junit.Test;


public class DPDocumentTest {
    
    private static final List<List<String>> SENTENCES = Arrays.asList(
            /*0*/Arrays.asList("gimme", "ax", "lived", "house"),
            /*1*/Arrays.asList("chimney", "sits", "house", "smoke", "gimme", "ax"),
            /*2*/Arrays.asList("doorknobs", "open", "doors"),
            /*3*/Arrays.asList("windows", "open", "shut"),
            /*4*/Arrays.asList("upstairs", "downstairs", "house"),
            /*5*/Arrays.asList(),
            /*6*/Arrays.asList("decided", "let", "children"));    

    @Test
    public void testConstruct() {
        DPDocument doc = new DPDocument(SENTENCES);
        assertThat(doc.sentenceCount, equalTo(7));
        assertThat(doc.vocabulary, contains("gimme", "ax", "lived", "house", "chimney", 
                "sits", "smoke", "doorknobs", "open", "doors", "windows", "shut", 
                "upstairs", "downstairs", "decided", "let", "children"));
        assertThat(doc.cumulativeWordUsageCounts.get(0).count("ax"), equalTo(1));
        assertThat(doc.cumulativeWordUsageCounts.get(1).count("ax"), equalTo(2));
        assertThat(doc.cumulativeWordUsageCounts.get(6).count("open"), equalTo(2));
        assertThat(doc.cumulativeWordUsageCounts.get(6).count("house"), equalTo(3));
    }
    
    @Test 
    public void testVocabularySize() throws IOException {
        // load 050.ref and get vocab size
        Stemmer stemmer = new PorterStemmer();
        List<List<String>> sentences = Utils.loadText(
                new File("src/test/data/050.ref")).getValue().stream()
                .map(Utils::clean)
                .map(Splitter.on(' ')::splitToList)
                .map(stemmer::stemWords)
                .collect(Utils.toImmutableList());
        DPDocument doc = new DPDocument(sentences);
        assertThat(doc.vocabulary.size(), equalTo(940));
    }

    @Test
    public void testCountWordInSegment() {
        DPDocument doc = new DPDocument(SENTENCES);
        assertThat(doc.countWordInSegment("house", new Segment(0,1)), equalTo(1));
        assertThat(doc.countWordInSegment("house", new Segment(0,2)), equalTo(2));
        assertThat(doc.countWordInSegment("house", new Segment(0,3)), equalTo(2));
        assertThat(doc.countWordInSegment("house", new Segment(0,4)), equalTo(2));
        assertThat(doc.countWordInSegment("house", new Segment(0,5)), equalTo(3));
        assertThat(doc.countWordInSegment("house", new Segment(1,4)), equalTo(2));
        assertThat(doc.countWordInSegment("house", new Segment(5,2)), equalTo(0));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testCountWordInSegmentRejectsZeroLength() {
        DPDocument doc = new DPDocument(SENTENCES);
        doc.countWordInSegment("house", new Segment(1,0));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testCountWordInSegmentRejectsNegativeLength() {
        DPDocument doc = new DPDocument(SENTENCES);
        doc.countWordInSegment("house", new Segment(1,-1));
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testCountWordInSegmentRejectsStartOutOfBounds() {
        DPDocument doc = new DPDocument(SENTENCES);
        doc.countWordInSegment("house", new Segment(7,1));
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testCountWordInSegmentRejectsLengthOutOfBounds() {
        DPDocument doc = new DPDocument(SENTENCES);
        doc.countWordInSegment("house", new Segment(6,2));
    }
}