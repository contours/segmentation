package edu.mit.nlp.segmenter.dp;

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.apache.commons.math3.special.Gamma;
import segmentation.Utils;

public class DPDocumentMap {

    private final Map<String,DPDocument> documents;

    public DPDocumentMap(Map<String,List<List<String>>> texts) {
        Map<String,DPDocument.Builder> builders = new HashMap<>();
        texts.keySet().stream().forEach(key -> {
            DPDocument.Builder builder = new DPDocument.Builder();
            builder.addAll(texts.get(key));
            builders.put(key, builder);
        });

        // Share a memoized logGamma function among all documents. 
        int initialCacheCapacity = 2 * (
                builders.values().stream().mapToInt(b -> b.getWordCount()).sum());
        float cacheLoadFactor = 0.6f;
        Function<Double,Double> logGamma = Utils.memoize(Gamma::logGamma, 
                initialCacheCapacity, cacheLoadFactor);

        this.documents = builders.entrySet().stream()
                .map(e -> Maps.immutableEntry(
                        e.getKey(), e.getValue().build(logGamma)))
                .collect(Utils.toImmutableMap());
    }
    
    public int size() {
        return this.documents.size();
    }
    
    public DPDocument get(String key) {
        return this.documents.get(key);
    }
    
    public Set<String> keySet() {
        return this.documents.keySet();
    }
    
    public Collection<DPDocument> values() {
        return this.documents.values();
    }
    
    public Set<Map.Entry<String,DPDocument>> entrySet() {
        return this.documents.entrySet();
    }
}
