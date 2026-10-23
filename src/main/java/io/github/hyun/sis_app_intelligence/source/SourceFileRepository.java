package io.github.hyun.sis_app_intelligence.source;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SourceFileRepository {

    private final JdbcTemplate jdbcTemplate;

    public SourceFileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, String> findHashesByOrigin(String origin) {
        return jdbcTemplate.query(
                "SELECT relative_path, sha256 FROM source_file WHERE origin = ?",
                resultSet -> {
                    Map<String, String> hashes = new HashMap<>();
                    while (resultSet.next()) {
                        hashes.put(resultSet.getString("relative_path"), resultSet.getString("sha256").trim());
                    }
                    return hashes;
                },
                origin);
    }

    public void insert(SourceFile file) {
        jdbcTemplate.update(
                """
                INSERT INTO source_file
                    (origin, relative_path, file_type, charset_name, content, sha256, size_bytes)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                file.origin(), file.relativePath(), file.fileType(), file.charsetName(),
                file.content(), file.sha256(), file.sizeBytes());
    }

    public void update(SourceFile file) {
        jdbcTemplate.update(
                """
                UPDATE source_file
                SET file_type = ?, charset_name = ?, content = ?, sha256 = ?, size_bytes = ?, collected_at = now()
                WHERE origin = ? AND relative_path = ?
                """,
                file.fileType(), file.charsetName(), file.content(), file.sha256(), file.sizeBytes(),
                file.origin(), file.relativePath());
    }

    public int deleteMissing(String origin, Collection<String> currentPaths) {
        if (currentPaths.isEmpty()) {
            return jdbcTemplate.update("DELETE FROM source_file WHERE origin = ?", origin);
        }

        StringJoiner placeholders = new StringJoiner(", ");
        currentPaths.forEach(path -> placeholders.add("?"));
        Object[] parameters = new Object[currentPaths.size() + 1];
        parameters[0] = origin;
        int index = 1;
        for (String path : currentPaths) {
            parameters[index++] = path;
        }
        return jdbcTemplate.update(
                "DELETE FROM source_file WHERE origin = ? AND relative_path NOT IN (" + placeholders + ")",
                parameters);
    }
}
