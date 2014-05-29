package edu.mit.nlp.segmenter.dp;

import com.google.common.base.Splitter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.apache.commons.math3.special.Gamma;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import segmentation.PorterStemmer;
import segmentation.Segment;
import segmentation.Stemmer;
import segmentation.Utils;


public class DPDocumentTest {
    
    private static final List<List<String>> SENTENCES = Arrays.asList(
            /*0*/Arrays.asList("gimme", "ax", "lived", "house"),
            /*1*/Arrays.asList("chimney", "sits", "house", "smoke", "gimme", "ax"),
            /*2*/Arrays.asList("doorknobs", "open", "doors"),
            /*3*/Arrays.asList("windows", "open", "shut"),
            /*4*/Arrays.asList("upstairs", "downstairs", "house"),
            /*5*/Arrays.asList(),
            /*6*/Arrays.asList("decided", "let", "children"));    

    private static final Function<Double,Double> logGamma = Utils.memoize(Gamma::logGamma);
    
    @Test
    public void testConstruct() {
        DPDocument doc = new DPDocument.Builder().addAll(SENTENCES).build(logGamma);
        assertThat(doc.sentenceCount, equalTo(7));
        assertThat(doc.vocabularySize, equalTo(17));
        assertThat(doc.vocabulary, contains("gimme", "ax", "lived", "house", "chimney", 
                "sits", "smoke", "doorknobs", "open", "doors", "windows", "shut", 
                "upstairs", "downstairs", "decided", "let", "children"));
        assertThat(doc.cumulativeTokenCounts, contains(4, 10, 13, 16, 19, 19, 22));
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
        DPDocument doc = new DPDocument.Builder().addAll(sentences).build(logGamma);
        assertThat(doc.vocabularySize, equalTo(940));
    }

    @Test
    public void testCountWordInSegment() {
        DPDocument doc = new DPDocument.Builder().addAll(SENTENCES).build(logGamma);
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
        DPDocument doc = new DPDocument.Builder().addAll(SENTENCES).build(logGamma);
        doc.countWordInSegment("house", new Segment(1,0));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testCountWordInSegmentRejectsNegativeLength() {
        DPDocument doc = new DPDocument.Builder().addAll(SENTENCES).build(logGamma);
        doc.countWordInSegment("house", new Segment(1,-1));
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testCountWordInSegmentRejectsStartOutOfBounds() {
        DPDocument doc = new DPDocument.Builder().addAll(SENTENCES).build(logGamma);
        doc.countWordInSegment("house", new Segment(7,1));
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testCountWordInSegmentRejectsLengthOutOfBounds() {
        DPDocument doc = new DPDocument.Builder().addAll(SENTENCES).build(logGamma);
        doc.countWordInSegment("house", new Segment(6,2));
    }

    @Test
    public void testCountWordsInSegment() {
        DPDocument doc = new DPDocument.Builder().addAll(SENTENCES).build(logGamma);
        assertThat(doc.countWordsInSegment(new Segment(0,1)), equalTo(4));
        assertThat(doc.countWordsInSegment(new Segment(0,2)), equalTo(10));
        assertThat(doc.countWordsInSegment(new Segment(0,3)), equalTo(13));
        assertThat(doc.countWordsInSegment(new Segment(0,4)), equalTo(16));
        assertThat(doc.countWordsInSegment(new Segment(1,4)), equalTo(15));
        assertThat(doc.countWordsInSegment(new Segment(5,2)), equalTo(3));
        assertThat(doc.countWordsInSegment(new Segment(6,1)), equalTo(3));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testCountWordsInSegmentRejectsZeroLength() {
        DPDocument doc = new DPDocument.Builder().addAll(SENTENCES).build(logGamma);
        doc.countWordsInSegment(new Segment(1,0));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testCountWordsInSegmentRejectsNegativeLength() {
        DPDocument doc = new DPDocument.Builder().addAll(SENTENCES).build(logGamma);
        doc.countWordsInSegment(new Segment(1,-1));
    }
    
    @Test(expected=IndexOutOfBoundsException.class)
    public void testCountWordsInSegmentRejectsStartOutOfBounds() {
        DPDocument doc = new DPDocument.Builder().addAll(SENTENCES).build(logGamma);
        doc.countWordsInSegment(new Segment(7,1));
    }
    
    @Test(expected=IndexOutOfBoundsException.class)
    public void testCountWordsInSegmentRejectsLengthOutOfBounds() {
        DPDocument doc = new DPDocument.Builder().addAll(SENTENCES).build(logGamma);
        doc.countWordsInSegment(new Segment(6,2));
    }
    
    @Test
    public void testDigamma() {
        assertThat(Gamma.digamma(188.60000000000002), closeTo(5.236974913937414, 1e-10));
        assertThat(Gamma.digamma(1027.6), closeTo(6.934494615668682, 1e-10));
    }
}