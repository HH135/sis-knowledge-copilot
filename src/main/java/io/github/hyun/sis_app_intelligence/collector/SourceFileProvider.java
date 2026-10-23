package io.github.hyun.sis_app_intelligence.collector;

public interface SourceFileProvider {

    SourceFileBatch scan();
}
