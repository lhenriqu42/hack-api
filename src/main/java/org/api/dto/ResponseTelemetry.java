package org.api.dto;

import java.time.LocalDate;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record ResponseTelemetry(
		LocalDate dataReferencia,
		List<MetricsSnapshot> listaEndpoints) {
}