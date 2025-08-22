package org.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record ResponseDia(
		LocalDate dataReferencia,
		List<SimuProduct> simulacoes) {
	@RegisterForReflection
	public record SimuProduct(
			Integer codigoProduto,
			String descricaoProduto,
			BigDecimal taxaMediaJuro,
			BigDecimal valorMedioPrestacao,
			BigDecimal valorTotalDesejado,
			BigDecimal valorTotalCredito) {
	}
}