package org.example.bigevent.pojo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.util.List;

/** Wire contract shared by the Python outbox and the internal ingest endpoint. */
public record IngestEvent(
        @NotNull @Min(1) @Max(1) Integer schemaVersion,
        @NotBlank @Size(max = 36) String eventId,
        @NotBlank @Size(max = 100) @Pattern(regexp = "[a-z0-9][a-z0-9_-]*") String sourceCode,
        @Size(max = 200) String sourceName,
        @Size(max = 16) String sourceLanguage,
        @NotEmpty @Size(max = 4) List<@NotBlank @Pattern(regexp = "delta-force|valorant|cs2|apex-legends") String> gameCodes,
        @NotBlank @Pattern(regexp = "announcement|patch_notes|event|new_content|guide|esports") String contentType,
        @NotBlank @URL @Pattern(regexp = "https?://.+") @Size(max = 2048) String originalUrl,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 1000) String summary,
        @Size(max = 50) List<@Size(max = 64) String> tags,
        @Size(max = 200000) String content,
        @NotBlank @Pattern(regexp = "ACCEPT|REVIEW|REJECT") String decision,
        @NotBlank @Size(max = 50) String ruleVersion,
        @NotBlank @Size(max = 40) String fetchedAt,
        @Size(max = 40) String originalPublishedAt,
        @Size(max = 10) String originalPublishedDate
) { }

