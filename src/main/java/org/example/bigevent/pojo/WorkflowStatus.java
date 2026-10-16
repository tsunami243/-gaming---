package org.example.bigevent.pojo;

/** 内部审核生命周期。legacy 的 {@code article.state} 暂时保留以兼容旧接口。 */
public enum WorkflowStatus {
    DRAFT,
    PENDING_REVIEW,
    PUBLISHED,
    REJECTED,
    UNPUBLISHED
}
