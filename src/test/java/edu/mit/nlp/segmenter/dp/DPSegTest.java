package edu.mit.nlp.segmenter.dp;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import in.aesh.segment.Main;
import in.aesh.segment.Segmentation;

public class DPSegTest {
    
    private static <K> Map<K,Integer> map(K key, Integer value) {
        return ImmutableMap.of(key, value);
    }
    
    @Test
    public void testSegmentTexts() throws IOException {
        Map<String,List<List<String>>> texts = new Main(new String[]{
            "-stop", "src/test/data/STOPWORD.list",
            "-stem",
            "src/test/data/050.ref" }).loadAndPrepareTexts();
        String textID = texts.keySet().toArray(new String[]{})[0];

        DPSeg dpseg;
        Map<String,Segmentation> segmentations;

        dpseg = new DPSeg(texts, map(textID, 1));
        segmentations = dpseg.segment(0.2);
        assertThat(segmentations.get(textID).toList(), contains(212));

        dpseg = new DPSeg(texts, map(textID, 3));
        segmentations = dpseg.segment(0.2);
        assertThat(segmentations.get(textID).toList(), contains(77,50,85));

        dpseg = new DPSeg(texts, map(textID, 5));
        segmentations = dpseg.segment(0.2);
        assertThat(segmentations.get(textID).toList(), contains(41,36,25,49,61));

        dpseg = new DPSeg(texts, map(textID, 7));
        segmentations = dpseg.segment(0.2);
        assertThat(segmentations.get(textID).toList(), contains(41,11,25,25,25,25,60));
    }
    
    @Test
    public void testLearnPrior() throws IOException {
        Map<String,List<List<String>>> texts = new Main(new String[]{
            "-stop", "src/test/data/STOPWORD.list",
            "-stem",
            "src/test/data/050.ref" }).loadAndPrepareTexts();
        String textID = texts.keySet().toArray(new String[]{})[0];

        DPSeg dpseg = new DPSeg(texts, map(textID, 4));
        
        double estimate = dpseg.estimateConcentrationParameter(0.2);
        assertThat(estimate, closeTo(0.5857, 0.0002));
    }
    
}

