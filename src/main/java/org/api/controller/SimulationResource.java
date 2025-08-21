package org.api.controller;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.api.database.postgres.model.Simulacao;
import org.api.database.postgres.repository.SimulacaoRepository;
import org.api.dto.SimulationRequest;
import org.api.dto.SimulationResponse;
import org.api.service.SimulacaoService;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/simulacoes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SimulationResource {

	@Inject
	SimulacaoService service;

	@Inject
	SimulacaoRepository simulacaoRepository;

	@POST
	public Response simular(@Valid SimulationRequest req) {
		Instant start = Instant.now();
		try {
			SimulationResponse res = service.simular(req);
			long elapsed = Duration.between(start, Instant.now()).toMillis();
			// métricas simples via headers
			return Response.ok(res)
					.header("X-Elapsed-ms", elapsed)
					.build();
		} catch (IllegalArgumentException ex) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(Map.of("erro", ex.getMessage()))
					.build();
		}
	}

	@GET
	public List<Simulacao> listar(@QueryParam("page") @DefaultValue("0") int page,
			@QueryParam("size") @DefaultValue("50") int size) {
		return simulacaoRepository.findAll().page(page, size).list();
	}
}
