package org.api.dto;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record ResultadoDTO(
		String tipo,
		List<ParcelaDTO> parcelas) {
}
