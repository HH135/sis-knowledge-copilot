package io.github.hyun.sis_app_intelligence.metadata;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetadataSyncService {

    private final ObjectProvider<MetadataSourcePort> sourceProvider;
    private final MetadataRepository metadataRepository;

    public MetadataSyncService(ObjectProvider<MetadataSourcePort> sourceProvider,
                               MetadataRepository metadataRepository) {
        this.sourceProvider = sourceProvider;
        this.metadataRepository = metadataRepository;
    }

    @Transactional
    public SyncResult sync() {
        MetadataSourcePort source = sourceProvider.getIfAvailable();
        if (source == null) {
            throw new MetadataSourceNotConfiguredException();
        }

        MetadataSnapshot snapshot = source.load();
        metadataRepository.replace(snapshot);

        int columnCount = snapshot.tables().stream()
                .mapToInt(table -> table.columns().size())
                .sum();
        return new SyncResult(
                snapshot.databaseName(),
                snapshot.tables().size(),
                columnCount,
                snapshot.procedures().size());
    }
}
