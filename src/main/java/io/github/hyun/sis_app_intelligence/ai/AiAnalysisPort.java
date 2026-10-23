package io.github.hyun.sis_app_intelligence.ai;

public interface AiAnalysisPort {

    AnalysisResult analyze(AnalysisRequest request);

    String providerName();

    boolean sendsDataExternally();
}
