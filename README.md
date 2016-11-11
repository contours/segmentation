This is a heavily modified fork of [jacobeisenstein/bayes-seg](https://github.com/jacobeisenstein/bayes-seg).

Edit `build.gradle` to specify:

* `num-segments` The desired number of segments. This only makes sense to use if you are segmenting a single text, otherwise you should specify a reference segmentation.
* `reference` A reference segmentation for determinining the desired number of segments for each text.
* `coder` If specified, use this coder's reference segmentations to determinine the desired number of segments for each text, otherwise use the mean length of all coder's segmentations.
* `stem` Stem terms before segmenting.
* `stopwords` Path to a file with stopwords to remove before segmenting.
* `files` Paths to the files to be segmented.

Then:

`./gradlew run`
