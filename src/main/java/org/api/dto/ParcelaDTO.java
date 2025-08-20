package org.api.dto;

import java.math.BigDecimal;

public record ParcelaDTO(
		int numero,
		BigDecimal valorAmortizacao,
		BigDecimal valorJuros,
		BigDecimal valorPrestacao) {
}
