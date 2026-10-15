package io.github.hyun.sis_app_intelligence.metadata;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MetadataSyncServiceTest {

    @Test
    void replacesRepositoryAndReturnsSnapshotCounts() {
        MetadataSourcePort source = mock(MetadataSourcePort.class);
        MetadataRepository repository = mock(MetadataRepository.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<MetadataSourcePort> sourceProvider = mock(ObjectProvider.class);
        MetadataSnapshot snapshot = new MetadataSnapshot(
                "ACME_SVC",
                List.of(
                        new TableMeta("dbo", "TB_ONE", "첫 테이블",
                                List.of(new ColumnMeta("ID", 1, "int", false, "식별자"),
                                        new ColumnMeta("NAME", 2, "varchar(20)", true, "이름"))),
                        new TableMeta("dbo", "TB_TWO", "둘째 테이블",
                                List.of(new ColumnMeta("CODE", 1, "varchar(10)", false, "코드")))),
                List.of(new ProcedureMeta("dbo", "USP_SAMPLE", "샘플 프로시저", "CREATE PROCEDURE ...",
                        "portfolio-data/procedures/dbo.USP_SAMPLE.sql")));

        when(sourceProvider.getIfAvailable()).thenReturn(source);
        when(source.load()).thenReturn(snapshot);
        MetadataSyncService service = new MetadataSyncService(sourceProvider, repository);

        SyncResult result = service.sync();

        assertEquals(new SyncResult("ACME_SVC", 2, 3, 1), result);
        verify(repository).replace(snapshot);
    }

    @Test
    void failsExplicitlyWhenNoMetadataSourceIsConfigured() {
        MetadataRepository repository = mock(MetadataRepository.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<MetadataSourcePort> sourceProvider = mock(ObjectProvider.class);
        when(sourceProvider.getIfAvailable()).thenReturn(null);
        MetadataSyncService service = new MetadataSyncService(sourceProvider, repository);

        assertThrows(MetadataSourceNotConfiguredException.class, service::sync);
    }
}
