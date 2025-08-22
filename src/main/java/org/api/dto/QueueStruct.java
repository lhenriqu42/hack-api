package org.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record QueueStruct(
		long simulacaoId,
		Integer codigoProduto,
		String nomeProduto,
		BigDecimal taxaJurosMensal,
		BigDecimal valorDesejado,
		Integer prazo,
		LocalDate dataReferencia,
		BigDecimal valorTotalParcelas) {
}