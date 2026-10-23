package io.github.hyun.sis_app_intelligence.collector;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import io.github.hyun.sis_app_intelligence.source.SourceFile;
import io.github.hyun.sis_app_intelligence.source.SourceFileRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SourceCollectorServiceTest {

    @Test
    void aggregatesInsertUpdateUnchangedAndDeletedCountsForOnlyCurrentOrigin() {
        SourceFileProvider provider = mock(SourceFileProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<SourceFileProvider> providerObject = mock(ObjectProvider.class);
        SourceFileRepository repository = mock(SourceFileRepository.class);
        when(providerObject.getIfAvailable()).thenReturn(provider);
        when(provider.scan()).thenReturn(new SourceFileBatch("portfolio", List.of(
                source("new.java", "new-hash"),
                source("changed.java", "new-changed-hash"),
                source("same.java", "same-hash")),
                Map.of("TOO_LARGE", 1), Map.of("java", 3)));
        when(repository.findHashesByOrigin("portfolio")).thenReturn(Map.of(
                "changed.java", "old-hash",
                "same.java", "same-hash",
                "removed.java", "old-hash"));
        when(repository.deleteMissing(eq("portfolio"), any())).thenReturn(1);

        ScanResult result = new SourceCollectorService(providerObject, repository).scan();

        assertEquals(3, result.scanned());
        assertEquals(1, result.inserted());
        assertEquals(1, result.updated());
        assertEquals(1, result.unchanged());
        assertEquals(1, result.deleted());
        assertEquals(1, result.skipped());
        verify(repository).findHashesByOrigin("portfolio");
        verify(repository).deleteMissing(eq("portfolio"), any());
        verify(repository).insert(source("new.java", "new-hash"));
        verify(repository).update(source("changed.java", "new-changed-hash"));
    }

    private SourceFile source(String path, String hash) {
        return new SourceFile("portfolio", path, "java", "UTF-8", "content", hash, 7);
    }
}
