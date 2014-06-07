package in.aesh.segment;

import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Segmentations {
    
    private String id;
    @SerializedName("segmentation_type")
    private String segmentationType;
    private Map<String,Map<String,int[]>> items;
    
    private Segmentations() {} // required for GSON

    private Segmentations(String id, Builder builder) {
        this.id = id;
        this.segmentationType = "linear";
        this.items = ImmutableMap.copyOf(builder.items);
    }

    public static Segmentations empty(Set<String> itemIDs) {
        Map<String,Map<String,int[]>> items = itemIDs.stream()
                .map(itemID -> {
                    Map<String,int[]> nocodings = ImmutableMap.of();
                    return Maps.immutableEntry(itemID, nocodings);
                })
                .collect(Utils.toImmutableMap());
        return new Builder().add(items).build("");
    }

    public static class Builder {
        private final Map<String,Map<String,int[]>> items;

        public Builder() {
            this.items = new HashMap<>();
        }

        private Builder add(Map<String,Map<String,int[]>> items) {
            this.items.putAll(items);
            return this;
        }

        public Builder add(String coder, Map<String,Segmentation> segmentations) {
            segmentations.entrySet().stream().forEach(e -> {
                this.items.computeIfAbsent(e.getKey(), key -> new HashMap<>())
                        .put(coder, e.getValue().toArray());
            });
            return this;
        }

        public Segmentations build(String id) {
            return new Segmentations(id, this);
        }
    }

    public Segmentations merge(Segmentations segmentations) {
        checkArgument(segmentations.getItemIDs().equals(this.getItemIDs()),
                "cannot merge segmentations of different item sets:\n%s\n%s",
                Arrays.toString(this.getItemIDs().toArray()),
                Arrays.toString(segmentations.getItemIDs().toArray()));
        Builder builder = new Builder();
        builder.add(this.items);
        builder.add(segmentations.items);
        String mergedID;
        if (this.id.length() == 0) {
            mergedID = segmentations.id;
        } else if (segmentations.id.length() == 0) {
            mergedID = this.id;
        } else {
            mergedID = this.id + "+" + segmentations.id;
        }
        return builder.build(mergedID);
    }

    public String getID() {
        return this.id;
    }

    public String getSegmentationType() {
        return this.segmentationType;
    }

    public Set<String> getItemIDs() {
        return ImmutableSet.copyOf(this.items.keySet());
    }

    public Set<String> getCoders() {
        ImmutableSet.Builder<String> builder = new ImmutableSet.Builder<>();
        this.items.values().stream()
                .forEach(codings -> builder.addAll(codings.keySet()));
        return builder.build();
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
