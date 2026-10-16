package org.example.bigevent.pojo.dto;
/** 来源运行策略响应：该来源当前是否允许真实运行及其验证状态。 */
public record IngestSourcePolicyResponse(boolean enabled, String verificationStatus) { }
