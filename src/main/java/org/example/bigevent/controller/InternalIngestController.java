package org.example.bigevent.controller;

import jakarta.validation.Valid;
import org.example.bigevent.pojo.dto.IngestAcceptance;
import org.example.bigevent.pojo.dto.IngestEvent;
import org.example.bigevent.pojo.dto.IngestReceipt;
import org.example.bigevent.pojo.dto.IngestSourcePolicyResponse;
import org.example.bigevent.service.IngestReceiptService;
import org.example.bigevent.service.IngestSourcePolicyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;

/**
 * 内部 · 采集交付接口（非公网）。
 *
 * <p>基础路径 {@code /internal/v1/ingest}，仅采集服务身份可调用，凭 {@code X-Ingest-Service-Token} 头鉴权。
 * 该路径不在 nginx 公网转发范围内（部署侧对 {@code /internal/} 直接返回 404）。
 * 入库采用 eventId + payloadHash 幂等：同 eventId 同载荷返回原结果，载荷不同返回 409。
 */
@RestController
@RequestMapping("/internal/v1/ingest")
public class InternalIngestController {

    private static final Set<String> ALLOWED_GAMES = Set.of("delta-force", "valorant", "cs2", "apex-legends");

    private final IngestReceiptService service;
    private final IngestSourcePolicyService sourcePolicyService;
    private final String token;

    public InternalIngestController(IngestReceiptService service, IngestSourcePolicyService sourcePolicyService,
                                    @Value("${ingest.service-token:}") String token) {
        this.service = service;
        this.sourcePolicyService = sourcePolicyService;
        this.token = token;
    }

    /**
     * 查询某来源是否允许运行（采集器运行前调用）。
     *
     * <p>{@code GET /internal/v1/ingest/sources/{code}/policy}
     *
     * @param supplied 采集服务令牌
     * @param code     来源代码
     * @return 该来源的启用状态与验证状态；令牌无效返回 403
     */
    @GetMapping("/sources/{code}/policy")
    public IngestSourcePolicyResponse sourcePolicy(@RequestHeader("X-Ingest-Service-Token") String supplied,
                                                   @PathVariable String code) {
        requireServiceToken(supplied);
        return sourcePolicyService.policy(code);
    }

    /**
     * 接收一条采集事件（幂等入库）。
     *
     * <p>{@code POST /internal/v1/ingest/events}
     *
     * @param supplied 采集服务令牌
     * @param event    采集事件；游戏须在四游戏白名单内、去重、时间与 URL 合法
     * @return 201 首次接收 / 200 幂等重放，body 为 receipt；令牌无效 403；载荷非法或同 eventId 不同载荷 400/409
     */
    @PostMapping("/events")
    public ResponseEntity<IngestReceipt> accept(@RequestHeader("X-Ingest-Service-Token") String supplied,
                                                @RequestBody @Valid IngestEvent event) {
        requireServiceToken(supplied);
        if (!event.gameCodes().stream().allMatch(ALLOWED_GAMES::contains)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported first-release game");
        }
        if (new HashSet<>(event.gameCodes()).size() != event.gameCodes().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duplicate game codes");
        }
        try {
            if (event.originalPublishedDate() != null) {
                LocalDate.parse(event.originalPublishedDate());
                if (event.originalPublishedAt() != null) throw new IllegalArgumentException("ambiguous date precision");
            }
            OffsetDateTime.parse(event.fetchedAt());
            if (event.originalPublishedAt() != null) OffsetDateTime.parse(event.originalPublishedAt());
            URI url = URI.create(event.originalUrl());
            if (url.getHost() == null || url.getUserInfo() != null) throw new IllegalArgumentException();
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid source URL or offset timestamp");
        }
        IngestAcceptance acceptance = service.accept(event, service.payloadHash(event));
        return ResponseEntity.status(acceptance.created() ? HttpStatus.CREATED : HttpStatus.OK).body(acceptance.receipt());
    }

    /** 校验采集服务令牌（常量时间比较）；token 未配置或为空时一律拒绝。 */
    private void requireServiceToken(String supplied) {
        if (token.isBlank() || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid ingest service token");
        }
    }
}
