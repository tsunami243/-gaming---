package org.example.bigevent.pojo.dto;
import org.example.bigevent.pojo.Result;

/** Result of an idempotent internal delivery. */
public record IngestAcceptance(IngestReceipt receipt, boolean created) { }
