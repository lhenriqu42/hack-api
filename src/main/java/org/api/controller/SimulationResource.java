package org.api.controller;

import java.util.Map;

import org.api.database.sqlserver.model.Produto;
import org.api.dto.SimulationRequest;
import org.api.dto.SimulationResponse;
import org.api.performance.anottations.TrackMetrics;
import org.api.service.SimulacaoService;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/simulacoes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SimulationResource {

	@Inject
	SimulacaoService service;

	@POST
	@TrackMetrics
	public Response simular(@Valid SimulationRequest req) {

		try {
			Produto produto = service.getProduto(req);
			if (produto == null) {
				throw new IllegalArgumentException("Infelizmente não temos nenhum produto que atenda a sua solicitação no momento 😓");
			}

			SimulationResponse res = service.simular(produto, req.valorDesejado(), req.prazo());
			return Response.ok(res)
					.build();
		} catch (IllegalArgumentException ex) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(Map.of("erro", ex.getMessage()))
					.build();
		}
	}
}
