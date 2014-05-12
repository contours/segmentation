package segmentation.wrappers;

import edu.mit.nlp.ling.LexMap;
import edu.mit.nlp.ling.Sentence;
import edu.mit.nlp.ling.Text;
import edu.mit.nlp.util.StrIntMap;
import edu.mit.nlp.util.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import mt.Matrix;
import smt.FlexCompColMatrix;

public class TextWrapper {

    private static List<String> stem(List<String> words) {
        return words.stream().map(
                word -> Utils.stemWord(word)).collect(Collectors.toList());
    }

    private final LexMap lexMap_;
    private final List<Integer> sentenceBreakList_;
    private final boolean doStemming_;
    private final Text text_;

    public TextWrapper(List<String> lines, List<String> stopwords, boolean doStemming) {
        this.text_ = new Text();
        this.lexMap_ = new LexMap();
        this.sentenceBreakList_ = new ArrayList<>();
        this.doStemming_ = doStemming;

        int sentenceCount = 0;
        int wordCount = 0;
        for (String line : lines) {
            Sentence sentence = new Sentence(20);
            String cleanline = Utils.removeSpecialSymbols(line, false, "_");
            StringTokenizer tokenizer = new StringTokenizer(cleanline, " \t");
            while (tokenizer.hasMoreTokens()) {
                String word = tokenizer.nextToken().trim();
                sentence.add(this.addWord(word));
                wordCount++;
            }
            this.text_.add(sentence);
            sentenceCount++;
        }
        this.sentenceBreakList_.add(sentenceCount);
        this.removeStopWords(stopwords);
        System.out.println("word count: " + wordCount);
    }

    public List<Integer> getReferenceSeg() {
        return this.sentenceBreakList_;
        //return Arrays.asList(new Integer[]{6, 40, 95, 212});
    }

    public double[][] createWordOccurrenceTable() {
        Matrix localMatrix = this.createWordOccurrenceMatrix();
        int[] arrayOfInt1 = new int[localMatrix.numRows()];
        for (int i = 0; i < arrayOfInt1.length; i++) {
            arrayOfInt1[i] = i;
        }
        int[] arrayOfInt2 = new int[localMatrix.numColumns()];
        for (int j = 0; j < arrayOfInt2.length; j++) {
            arrayOfInt2[j] = j;
        }
        double[][] arrayOfDouble = new double[localMatrix.numRows()][localMatrix.numColumns()];
        localMatrix.get(arrayOfInt1, arrayOfInt2, arrayOfDouble);
        return arrayOfDouble;
    }

    public void printSentences() {
        this.text_.stream().forEach(sentence -> {
            System.out.println(sentence.stream()
                    .map(wordId -> this.getTokenMap().get(wordId))
                    .collect(Collectors.joining(" ")));
        });
    }

    private Matrix createWordOccurrenceMatrix() {
        int numWords = this.getLexiconSize();
        FlexCompColMatrix wordOccurrenceMatrix = new FlexCompColMatrix(numWords, this.text_.size());
        int sentenceIndex = 0;
        for (Sentence sentence : this.text_) {
            for (Integer id : sentence) {
                wordOccurrenceMatrix.add(id, sentenceIndex, 1.0D);
            }
            sentenceIndex++;
        }
        return wordOccurrenceMatrix;
    }

    private void removeStopWords(List<String> stopwords) {
        if (stopwords.size() > 0) {
            List<Integer> stopwordIDs = stopwords.stream()
                    .mapToInt(this::getTokenId)
                    .filter(id -> (id != -1))
                    .boxed()
                    .collect(Collectors.toList());
            this.text_.stream().forEach((sentence) -> {
                sentence.removeAll(stopwordIDs);
            });
        }
    }

    private int getLexiconSize() {
        return this.doStemming_
                ? this.lexMap_.getStemLexiconSize()
                : this.lexMap_.getWordLexiconSize();
    }

    private int getTokenId(String token) {
        return this.doStemming_
                ? this.lexMap_.getStemId(Utils.stemWord(token))
                : this.lexMap_.getWordId(token);
    }

    private StrIntMap getTokenMap() {
        return this.doStemming_
                ? this.lexMap_.getStemMap()
                : this.lexMap_.getWordMap();
    }

    private int addWord(String word) {
        return this.doStemming_
                ? this.lexMap_.addStem(Utils.stemWord(word))
                : this.lexMap_.addWord(word);
    }

}
