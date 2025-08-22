package org.api.controller;

import java.time.LocalDate;
import java.util.Map;

import org.api.dto.ResponseAll;
import org.api.dto.ResponseDia;
import org.api.dto.SimulationRequest;
import org.api.dto.SimulationResponse;
import org.api.performance.anottations.TrackMetrics;
import org.api.service.SimulacaoService;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
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



	@POST
	@TrackMetrics
	public Response simular(@Valid SimulationRequest req) {

		try {
			SimulationResponse res = service.simular(req);
			return Response.ok(res).build();
		} catch (IllegalArgumentException ex) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(Map.of("erro", ex.getMessage()))
					.build();
		}
	}



	@GET()
	@Path("/all")
	@TrackMetrics
	public ResponseAll getAll(
			@QueryParam("pagina") Integer pagina,
			@QueryParam("qtdRegistrosPagina") Integer qtdRegistrosPagina) {

		if (pagina == null || pagina < 1) {
			pagina = 1;
		}
		if (qtdRegistrosPagina == null || qtdRegistrosPagina < 1) {
			qtdRegistrosPagina = 10;
		}

		ResponseAll response = service.getAllSimulacoes(pagina, qtdRegistrosPagina);
		return response;
	}


	
	@GET
	@TrackMetrics
	public ResponseDia porProdutoDia(@QueryParam("dia") String diaReq) {

		LocalDate dia = (diaReq == null || diaReq.isBlank()) ? LocalDate.now() : LocalDate.parse(diaReq);
		ResponseDia response = service.getSimulacoesPorProduto(dia);
		return response;
	}
}
