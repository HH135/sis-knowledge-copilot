package io.github.hyun.sis_app_intelligence.ai;

import java.util.List;

public interface EmbeddingPort {

    List<float[]> embedDocuments(List<String> texts);

    float[] embedQuery(String text);

    String modelName();

    int dimensions();

    boolean sendsDataExternally();
}
