package org.api.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.api.dto.ParcelaDTO;
import org.api.dto.ResultadoDTO;
import org.api.dto.SimulationRequest;
import org.api.dto.SimulationResponse;
import org.api.model.Produto;
import org.api.model.Simulacao;
import org.api.repository.ProdutoRepository;
import org.api.repository.SimulacaoRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SimulacaoService {

    @Inject
    ProdutoRepository produtoRepository;

    @Inject
    SimulacaoRepository simulacaoRepository;

    @Inject
    RedisService ds;

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

    public List<Produto> listarProdutosCached() {
        // Try Redis first
        String cached = ds.get("produtos");
        if (cached != null) {
            // naive cache: ids separados por vírgula não é suficiente para objetos; manter simples: não usar cache se não existir serializer
            // Em produção: use Redis JSON/codec. Aqui, retornamos do DB direto.
        }
        List<Produto> list = produtoRepository.listAll();
        // skip set to Redis for simplicity in skeleton
        return list;
    }

    private Optional<Produto> escolherProduto(BigDecimal valor, int prazo) {
        for (Produto p : listarProdutosCached()) {
            boolean prazoOk = (prazo >= p.minimoMeses) && (p.maximoMeses == null || prazo <= p.maximoMeses);
            boolean valorOk = (valor.compareTo(p.valorMinimo) >= 0) && (p.valorMaximo == null || valor.compareTo(p.valorMaximo) <= 0);
            if (prazoOk && valorOk) return Optional.of(p);
        }
        return Optional.empty();
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

    @Transactional
    public SimulationResponse simular(SimulationRequest req) {
        var produtoOpt = escolherProduto(req.valorDesejado(), req.prazo());
        if (produtoOpt.isEmpty()) {
            throw new IllegalArgumentException("Nenhum produto compatível com valor/prazo informados.");
        }
        var produto = produtoOpt.get();
        BigDecimal taxa = produto.taxaJurosMensal;
        List<ParcelaDTO> sac = calcularSAC(req.valorDesejado(), taxa, req.prazo());
        List<ParcelaDTO> price = calcularPRICE(req.valorDesejado(), taxa, req.prazo());
        List<ResultadoDTO> resultados = List.of(new ResultadoDTO("SAC", sac), new ResultadoDTO("PRICE", price));

        BigDecimal total = resultados.stream()
                .flatMap(r -> r.parcelas().stream())
                .map(ParcelaDTO::valorPrestacao)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        Simulacao s = new Simulacao();
        s.codigoProduto = produto.codigo;
        s.valorDesejado = req.valorDesejado();
        s.prazo = req.prazo();
        s.dataReferencia = LocalDate.now();
        s.valorTotalParcelas = total;
        simulacaoRepository.persist(s);

        // Enviar para EventHub (placeholder no esqueleto)
        // EventHubProducer.send(simulationJson)

        return new SimulationResponse(
                s.id,
                produto.codigo,
                produto.nome,
                taxa,
                resultados
        );
    }
}
