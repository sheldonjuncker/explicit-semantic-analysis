package com.dreamcloud.esa.tfidf;

public class MemoryScoreReader implements DocumentScoreReader {
    @Override
    public TfIdfScore[] getTfIdfScores(String term) {
        return new TfIdfScore[0];
    }

    @Override
    public TfIdfScore[] getTfIdfScores(String[] term) {
        return new TfIdfScore[0];
    }
}
