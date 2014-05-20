package edu.mit.nlp.segmenter.dp;

import com.google.common.collect.ImmutableList;
import edu.mit.util.stats.FastGamma;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import segmentation.StreamUtils;

public class DPDocumentList {

    private final ImmutableList<DPDocument> documents;

    public DPDocumentList(List<List<List<String>>> texts) {
        List<DPDocument.Builder> builders = new ArrayList<>();
        texts.stream().forEach(text -> {
            DPDocument.Builder builder = new DPDocument.Builder();
            builder.addAll(text);
            builders.add(builder);
        });

        int initialCacheCapacity = 2 * (
                builders.stream().mapToInt(b -> b.getWordCount()).sum());
        float cacheLoadFactor = 0.6f;
        FastGamma gamma = new FastGamma(initialCacheCapacity, cacheLoadFactor);
        this.documents = builders.stream()
                .map(builder -> builder.build(gamma))
                .collect(StreamUtils.toImmutableList());
    }
    
    public int size() {
        return this.documents.size();
    }
    
    public DPDocument get(int index) {
        return this.documents.get(index);
    }
    
    public Stream<DPDocument> stream() {
        return this.documents.stream();
    }
}
