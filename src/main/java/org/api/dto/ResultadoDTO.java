package org.api.dto;

import java.util.List;

public record ResultadoDTO(
		String tipo,
		List<ParcelaDTO> parcelas) {
}
