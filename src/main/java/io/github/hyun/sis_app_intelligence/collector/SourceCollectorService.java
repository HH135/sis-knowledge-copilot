package io.github.hyun.sis_app_intelligence.collector;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.hyun.sis_app_intelligence.source.SourceFile;
import io.github.hyun.sis_app_intelligence.source.SourceFileRepository;

@Service
public class SourceCollectorService {

    private final ObjectProvider<SourceFileProvider> provider;
    private final SourceFileRepository repository;

    public SourceCollectorService(ObjectProvider<SourceFileProvider> provider, SourceFileRepository repository) {
        this.provider = provider;
        this.repository = repository;
    }

    @Transactional
    public ScanResult scan() {
        SourceFileProvider sourceProvider = provider.getIfAvailable();
        if (sourceProvider == null) {
            throw new SourceRootNotConfiguredException();
        }

        SourceFileBatch batch = sourceProvider.scan();
        Map<String, String> existing = new HashMap<>(repository.findHashesByOrigin(batch.origin()));
        int inserted = 0;
        int updated = 0;
        int unchanged = 0;
        for (SourceFile file : batch.files()) {
            String oldHash = existing.remove(file.relativePath());
            if (oldHash == null) {
                repository.insert(file);
                inserted++;
            }
            else if (oldHash.equals(file.sha256())) {
                unchanged++;
            }
            else {
                repository.update(file);
                updated++;
            }
        }

        List<String> currentPaths = batch.files().stream()
                .map(SourceFile::relativePath)
                .collect(Collectors.toList());
        int deleted = repository.deleteMissing(batch.origin(), currentPaths);
        int skipped = batch.skippedReasons().values().stream().mapToInt(Integer::intValue).sum();
        return new ScanResult(batch.origin(), batch.files().size(), inserted, updated, unchanged, deleted,
                skipped, batch.skippedReasons(), batch.byExtension());
    }
}
