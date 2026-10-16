package org.example.bigevent.pojo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;

/** 审核动作请求体：revision 用于乐观锁校验，reasonCode 记录审核原因。 */
public record ModerationRequest(@NotNull @Min(0) @Max(2147483646) Integer revision, @Size(max = 64) String reasonCode, @Size(max = 1000) String reasonDetail) { }
