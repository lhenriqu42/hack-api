package org.api.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.api.dto.ParcelaDTO;
import org.api.dto.ResultadoDTO;
import org.api.dto.SimulationRequest;
import org.api.dto.SimulationResponse;
import org.api.model.Produto;
import org.api.repository.sqlserver.ProdutoRepository;
import org.api.service.RedisQueueService.QueueStruct;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SimulacaoService {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

    @Inject
    ProdutoRepository produtoRepository;

    @Inject
    RedisQueueService redisQueueService;

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

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

    @Transactional
    public SimulationResponse simular(SimulationRequest req) {
        List<Produto> produtos = produtoRepository.filterProducts(req.valorDesejado(), req.prazo());
        if (produtos.isEmpty()) {
            throw new IllegalArgumentException("Nenhum produto compatível com valor/prazo informados.");
        }
        Produto p = produtos.getFirst();

        BigDecimal taxa = p.taxaJurosMensal;
        List<ParcelaDTO> sac = calcularSAC(req.valorDesejado(), taxa, req.prazo());
        List<ParcelaDTO> price = calcularPRICE(req.valorDesejado(), taxa, req.prazo());
        List<ResultadoDTO> resultados = List.of(new ResultadoDTO("SAC", sac), new ResultadoDTO("PRICE", price));

        // VERIFICAR A VARACIDADE -------------------------------------------
        BigDecimal total = resultados.stream()
                .flatMap(r -> r.parcelas().stream())
                .map(ParcelaDTO::valorPrestacao)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        // ---------------------------------------------

        // Gerar um ID único do tipo Long
        long simulacaoId = ID_GENERATOR.getAndIncrement();

        // BACKGROUND PROCCESSING REDIS QUEUE
        QueueStruct data = new QueueStruct(
                simulacaoId,
                p.codigo,
                p.nome,
                p.taxaJurosMensal,
                req.valorDesejado(),
                req.prazo(),
                LocalDate.now(),
                total);
        redisQueueService.enqueue(data);

        SimulationResponse response = new SimulationResponse(
                simulacaoId,
                p.codigo,
                p.nome,
                p.taxaJurosMensal,
                resultados);
        return response;
    }
}
