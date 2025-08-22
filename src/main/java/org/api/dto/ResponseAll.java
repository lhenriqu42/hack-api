package org.api.dto;

import java.math.BigDecimal;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record ResponseAll(
		Integer pagina,
		Long qtdRegistros,
		Integer qtdRegistrosPagina,
		List<Registro> registros) {
	@RegisterForReflection
	public record Registro(
			Long idSimulacao,
			BigDecimal valorDesejado,
			Integer prazo,
			BigDecimal valorTotalParcelas) {
	}
}
