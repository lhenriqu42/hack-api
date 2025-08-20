package org.api.controller;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.api.model.Simulacao;
import org.api.repository.postgres.SimulacaoRepository;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/simulacoes/dia")
@Produces(MediaType.APPLICATION_JSON)
public class AggregationResource {

	private record SimuProduct(
			Integer codigoProduto,
			String descricaoProduto,
			BigDecimal taxaMediaJuro,
			BigDecimal valorMedioPrestacao,
			BigDecimal valorTotalDesejado,
			BigDecimal valorTotalCredito) {
	}

	private record ResponseDia(
			LocalDate dataReferencia,
			List<SimuProduct> simulacoes) {
	}

	private record Registro(
			Long idSimulacao,
			BigDecimal valorDesejado,
			Integer prazo,
			BigDecimal valorTotalParcelas) {
	}

	private record ResponseAll(
			Integer pagina,
			Long qtdRegistros,
			Integer qtdRegistrosPagina,
			List<Registro> registros) {
	}

	private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

	@Inject
	SimulacaoRepository repo;

	@GET
	public ResponseDia porProdutoDia(@QueryParam("dia") String diaReq) {
		LocalDate dia = (diaReq == null || diaReq.isBlank()) ? LocalDate.now() : LocalDate.parse(diaReq);
		List<Simulacao> sims = repo.find("dataReferencia", dia).list();
		Map<Integer, List<Simulacao>> porProduto = sims.stream().collect(Collectors.groupingBy(s -> s.codigoProduto));
		
		List<SimuProduct> simulacoes = new ArrayList<>();
		for (var entry : porProduto.entrySet()) {
			Integer codigoProduto = entry.getKey();
			List<Simulacao> lista = entry.getValue();
			BigDecimal taxaMediaJuro = lista.stream()
					.map(s -> s.taxaJuros)
					.reduce(BigDecimal.ZERO, BigDecimal::add)
					.divide(BigDecimal.valueOf(lista.size()), MC);
			BigDecimal valorMedioPrestacao = lista.stream()
					.map(s -> s.valorTotalParcelas)
					.reduce(BigDecimal.ZERO, BigDecimal::add)
					.divide(BigDecimal.valueOf(lista.size()), MC);
			BigDecimal valorTotalDesejado = lista.stream()
					.map(s -> s.valorDesejado)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal valorTotalCredito = lista.stream()
					.map(s -> s.valorTotalParcelas)
					.reduce(BigDecimal.ZERO, BigDecimal::add);

			simulacoes.add(new SimuProduct(
					codigoProduto,
					lista.get(0).nomeProduto,
					taxaMediaJuro,
					valorMedioPrestacao,
					valorTotalDesejado,
					valorTotalCredito));
		}
		return new ResponseDia(dia, simulacoes);
	}

	@GET()
	@Path("/all")
	public ResponseAll getAll(
			@QueryParam("pagina") Integer pagina,
			@QueryParam("qtdRegistrosPagina") Integer qtdRegistrosPagina) {
		if (pagina == null || pagina < 1) {
			pagina = 1;
		}
		if (qtdRegistrosPagina == null || qtdRegistrosPagina < 1) {
			qtdRegistrosPagina = 10;
		}

		List<Simulacao> simulacoes = repo.findAll().page(pagina - 1, qtdRegistrosPagina).list();
		List<Registro> registros = simulacoes.stream()
				.map(s -> new Registro(s.id, s.valorDesejado, s.prazo, s.valorTotalParcelas))
				.collect(Collectors.toList());

		return new ResponseAll(pagina, repo.count(), registros.size(), registros);
	}
}
