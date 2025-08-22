package org.api.dto;

import java.math.BigDecimal;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record ParcelaDTO(
		int numero,
		BigDecimal valorAmortizacao,
		BigDecimal valorJuros,
		BigDecimal valorPrestacao) {
}
