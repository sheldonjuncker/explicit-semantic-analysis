package com.dreamcloud.esa.tfidf;

import java.io.IOException;

public interface DocumentScoreReader {
    public TfIdfScore[] getTfIdfScores(String term) throws IOException;
    public TfIdfScore[] getTfIdfScores(String[] term) throws IOException;
}
