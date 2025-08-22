package org.api.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.api.database.postgres.model.Simulacao;
import org.api.database.postgres.repository.SimulacaoRepository;
import org.api.database.sqlserver.model.Produto;
import org.api.database.sqlserver.repository.ProdutoRepository;
import org.api.dto.ParcelaDTO;
import org.api.dto.QueueStruct;
import org.api.dto.ResponseAll;
import org.api.dto.ResponseDia;
import org.api.dto.ResultadoDTO;
import org.api.dto.SimulationRequest;
import org.api.dto.SimulationResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SimulacaoService {

	private record CalcInfo(
			List<ParcelaDTO> sac,
			List<ParcelaDTO> price,
			List<ResultadoDTO> resultados,
			BigDecimal totalSac,
			BigDecimal totalPrice,
			BigDecimal total

	) {

	}

	private static final AtomicLong COUNTER = new AtomicLong(0);

	@Inject
	ProdutoRepository produtoRepository;

	@Inject
	CacheService cacheService;

	@Inject
	SimulacaoRepository simulacaoRepository;

	@Inject
	RedisQueueService redisQueueService;

	private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

	private static long generateUniqueId() {
		long timestamp = System.currentTimeMillis();
		long counter = COUNTER.getAndUpdate(x -> (x + 1) % 1000);
		return timestamp * 1000 + counter;
	}

	private String generateCacheKey(SimulationRequest req) {
		return String.format("valorDesejado:%s-prazo:%d", req.valorDesejado(), req.prazo());
	}

	private String generateCacheKey(SimulationRequest req, BigDecimal taxa) {
		return String.format("valorDesejado:%s-prazo:%d-taxa:%d", req.valorDesejado(), req.prazo(), taxa);
	}

	private List<ParcelaDTO> calcularSAC(BigDecimal principal, BigDecimal taxaMensal, int meses) {
		List<ParcelaDTO> parcelas = new ArrayList<>();
		BigDecimal amortizacaoConst = principal.divide(new BigDecimal(meses), 10, RoundingMode.HALF_UP);
		BigDecimal saldo = principal;
		for (int n = 1; n <= meses; n++) {
			BigDecimal juros = saldo.multiply(taxaMensal, MC).setScale(2, RoundingMode.HALF_UP);
			BigDecimal prestacao = amortizacaoConst.add(juros).setScale(2, RoundingMode.HALF_UP);
			BigDecimal amort = amortizacaoConst.setScale(2, RoundingMode.HALF_UP);
			parcelas.add(new ParcelaDTO(n, amort, juros, prestacao));
			saldo = saldo.subtract(amortizacaoConst, MC);
		}
		return parcelas;
	}

	private List<ParcelaDTO> calcularPRICE(BigDecimal principal, BigDecimal taxaMensal, int meses) {
		List<ParcelaDTO> parcelas = new ArrayList<>();
		// PMT = P * i / (1 - (1+i)^-n)
		BigDecimal i = taxaMensal;
		BigDecimal um = BigDecimal.ONE;
		BigDecimal fator = um.add(i, MC).pow(meses, MC);
		BigDecimal pmt = principal.multiply(i, MC).divide(um.subtract(um.divide(fator, MC), MC), MC);
		BigDecimal saldo = principal;
		for (int n = 1; n <= meses; n++) {
			BigDecimal juros = saldo.multiply(i, MC).setScale(2, RoundingMode.HALF_UP);
			BigDecimal prestacao = pmt.setScale(2, RoundingMode.HALF_UP);
			BigDecimal amortizacao = prestacao.subtract(juros, MC).setScale(2, RoundingMode.HALF_UP);
			parcelas.add(new ParcelaDTO(n, amortizacao, juros, prestacao));
			saldo = saldo.subtract(amortizacao, MC);
		}
		return parcelas;
	}

	private Produto getProduto(SimulationRequest req) {
		String cacheKey = generateCacheKey(req);
		var produtoCached = cacheService.get(cacheKey);
		if (produtoCached.isPresent()) {
			return (Produto) produtoCached.get();
		}
		List<Produto> produtos = produtoRepository.filterProducts(req.valorDesejado(), req.prazo());
		if (produtos.isEmpty()) {
			throw new IllegalArgumentException(
					"Infelizmente não temos nenhum produto que atenda a sua solicitação no momento 😓");
		}
		cacheService.put(cacheKey, produtos.getFirst());
		return produtos.getFirst();
	}

	private CalcInfo getCalcInfo(SimulationRequest req, BigDecimal taxa) {
		String cacheKey = generateCacheKey(req, taxa);
		var cached = cacheService.get(cacheKey);
		if (cached.isPresent()) {
			return (CalcInfo) cached.get();
		}
		List<ParcelaDTO> sac = calcularSAC(req.valorDesejado(), taxa, req.prazo());
		List<ParcelaDTO> price = calcularPRICE(req.valorDesejado(), taxa, req.prazo());
		List<ResultadoDTO> resultados = List.of(new ResultadoDTO("SAC", sac), new ResultadoDTO("PRICE", price));
		BigDecimal totalSac = sac.stream()
				.map(ParcelaDTO::valorPrestacao)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.HALF_UP);
		BigDecimal totalPrice = price.stream()
				.map(ParcelaDTO::valorPrestacao)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.HALF_UP);
		// pegando a media por falta de esclarecimento
		BigDecimal total = totalSac.add(totalPrice).divide(new BigDecimal(2), 2, RoundingMode.HALF_UP);
		CalcInfo calcInfo = new CalcInfo(sac, price, resultados, totalSac, totalPrice, total);
		cacheService.put(cacheKey, calcInfo, 60 * 5); // cache por 5 minutos
		return calcInfo;
	}

	@Transactional
	public SimulationResponse simular(SimulationRequest req) {
		Produto produto = getProduto(req);
		BigDecimal taxa = produto.taxaJurosMensal;
		// faz as contas e retorna os resultados
		CalcInfo calc = getCalcInfo(req, taxa);

		// Gerar um ID único do tipo Long
		long simulacaoId = generateUniqueId();

		// BACKGROUND PROCCESSING REDIS QUEUE
		QueueStruct data = new QueueStruct(
				simulacaoId,
				produto.codigo,
				produto.nome,
				produto.taxaJurosMensal,
				req.valorDesejado(),
				req.prazo(),
				LocalDate.now(),
				calc.total);
		redisQueueService.enqueue(data);

		SimulationResponse response = new SimulationResponse(
				simulacaoId,
				produto.codigo,
				produto.nome,
				produto.taxaJurosMensal.stripTrailingZeros(),
				calc.resultados);
		return response;
	}

	@Transactional
	public ResponseAll getAllSimulacoes(Integer pagina, Integer qtdRegistrosPagina) {
		List<Simulacao> simulacoes = simulacaoRepository.findAll().page(pagina - 1, qtdRegistrosPagina).list();
		List<ResponseAll.Registro> registros = simulacoes.stream()
				.map(s -> new ResponseAll.Registro(s.id, s.valorDesejado, s.prazo, s.valorTotalParcelas))
				.collect(Collectors.toList());
		return new ResponseAll(pagina, simulacaoRepository.count(), registros.size(), registros);
	}

	@Transactional
	public ResponseDia getSimulacoesPorProduto(LocalDate dia) {
		List<Simulacao> sims = simulacaoRepository.find("dataReferencia", dia).list();
		Map<Integer, List<Simulacao>> porProduto = sims.stream().collect(Collectors.groupingBy(s -> s.codigoProduto));

		List<ResponseDia.SimuProduct> simulacoes = new ArrayList<>();
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

			simulacoes.add(new ResponseDia.SimuProduct(
					codigoProduto,
					lista.get(0).nomeProduto,
					taxaMediaJuro,
					valorMedioPrestacao,
					valorTotalDesejado,
					valorTotalCredito));
		}
		return new ResponseDia(dia, simulacoes);
	}

}
