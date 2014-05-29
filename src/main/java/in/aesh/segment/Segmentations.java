package in.aesh.segment;

import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

public class Segmentations {
    
    private String id;
    @SerializedName("segmentation_type")
    private String segmentationType;
    private Map<String,Map<String,int[]>> items;
    
    private Segmentations() {} // required for GSON

    public String getId() {
        return this.id;
    }

    public String getSegmentationType() {
        return this.segmentationType;
    }

    public Map<String,Map<String,Segmentation>> getItems() {
        return this.items.entrySet().stream().map(itemEntry -> {
            String itemId = itemEntry.getKey();
            Map<String,Segmentation> codings = itemEntry.getValue().entrySet()
                    .stream().map(codingEntry -> {
                        return Maps.immutableEntry(
                                codingEntry.getKey(), 
                                new Segmentation(codingEntry.getValue()));
                    }).collect(Collectors.toMap(
                            e -> e.getKey(), 
                            e -> e.getValue()));
            return Maps.immutableEntry(itemId, codings);
        }).collect(Utils.toImmutableMap());
    }
    
    public Map<String,Integer> getSegmentCounts(String coder) {
        return this.items.entrySet().stream().map(e -> {
            String itemId = e.getKey();
            int[] segmentation = this.items.get(itemId).get(coder);
            return Maps.immutableEntry(itemId, segmentation.length);
        }).collect(Utils.toImmutableMap());
    }
    
    public Map<String,Integer> getMeanSegmentCounts() {
        return this.items.entrySet().stream().map(e -> {
            String itemId = e.getKey();
            double mean = this.items.get(itemId).values().stream()
                    .mapToInt(segmentation -> segmentation.length).average()
                    .orElseThrow(() -> new IllegalStateException(
                            itemId + " does not have any segmentations"));
            return Maps.immutableEntry(itemId, Math.round((float) mean));
        }).collect(Utils.toImmutableMap());
    }
}
