package org.api.controller;

import java.math.BigDecimal;
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
import jakarta.ws.rs.core.MediaType;

@Path("/simulacoes/por-produto-dia")
@Produces(MediaType.APPLICATION_JSON)
public class AggregationResource {

	@Inject
	SimulacaoRepository repo;

	@GET
	public Map<String, Object> porProdutoDia() {
		LocalDate hoje = LocalDate.now();
		List<Simulacao> sims = repo.find("dataReferencia", hoje).list();
		Map<Integer, List<Simulacao>> porProduto = sims.stream().collect(Collectors.groupingBy(s -> s.codigoProduto));
		List<Map<String, Object>> simulacoes = new ArrayList<>();
		for (var entry : porProduto.entrySet()) {
			var list = entry.getValue();
			BigDecimal taxaMedia = BigDecimal.ZERO; // placeholder (dependeria do produto)
			BigDecimal valorMedioPrestacao = list.stream()
					.map(s -> s.valorTotalParcelas.divide(new BigDecimal(s.prazo), 2, java.math.RoundingMode.HALF_UP))
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			if (!list.isEmpty())
				valorMedioPrestacao = valorMedioPrestacao.divide(new BigDecimal(list.size()), 2,
						java.math.RoundingMode.HALF_UP);

			BigDecimal valorTotalDesejado = list.stream().map(s -> s.valorDesejado).reduce(BigDecimal.ZERO,
					BigDecimal::add);
			BigDecimal valorTotalCredito = list.stream().map(s -> s.valorTotalParcelas).reduce(BigDecimal.ZERO,
					BigDecimal::add);

			Map<String, Object> m = new HashMap<>();
			m.put("codigoProduto", entry.getKey());
			m.put("descricaoProduto", "Produto " + entry.getKey());
			m.put("taxaMediaJuro", taxaMedia);
			m.put("valorMedioPrestacao", valorMedioPrestacao);
			m.put("valorTotalDesejado", valorTotalDesejado);
			m.put("valorTotalCredito", valorTotalCredito);
			simulacoes.add(m);
		}
		Map<String, Object> out = new HashMap<>();
		out.put("dataReferencia", hoje.toString());
		out.put("simulacoes", simulacoes);
		return out;
	}
}
