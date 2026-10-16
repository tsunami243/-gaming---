package org.example.bigevent.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 暂停来源请求体：原因说明用于留痕。 */
public record SourcePauseRequest(@NotBlank @Size(max = 500) String reason) { }
