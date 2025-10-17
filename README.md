"# Hash-Table-Project" 
# Relatório do Projeto: Tabelas Hash em Java

## 1. Implementação

O projeto foi desenvolvido em **Java** com o objetivo de analisar o desempenho de diferentes implementações de tabelas hash utilizando **encadeamento** e **endereçamento aberto** (rehashing). O código completo está concentrado no arquivo `HashTableProject.java`.

### Estrutura da Implementação

* **Classe `Registro`**: representa um registro com código de 9 dígitos.
* **Funções Hash**: três variações foram implementadas para avaliar o impacto da função hash no desempenho.
* **Tabelas Hash**:

  * **Encadeamento** (listas encadeadas em cada posição do vetor);
  * **Endereçamento aberto** com sondagem linear;
  * **Endereçamento aberto** com sondagem quadrática e duplo hash.
* **Medições**: o código mede automaticamente o tempo de inserção, tempo de busca, número de colisões, tamanho das maiores listas/aglomerados e gaps (espaços vazios entre elementos).
* **Resultados**: os resultados são exportados automaticamente em formato CSV para a pasta `results/`.

---

## 2. Escolha das Funções Hash

Foram implementadas três funções hash distintas:

1. **Função do Resto da Divisão**: ( h(k) = k \mod m )

   * Simples e eficiente, mas pode gerar padrões de colisão previsíveis dependendo do tamanho da tabela.
2. **Função da Multiplicação (Knuth)**: ( h(k) = \lfloor m (k A \mod 1) \rfloor ), com ( A = (\sqrt{5} - 1)/2 )

   * Distribui melhor os valores em relação ao módulo puro, reduzindo agrupamentos.
3. **Função `hashCode()` do Java (ajustada para inteiros)**:

   * Aproveita a dispersão interna da função hash nativa da linguagem.

Essas funções foram escolhidas para representar abordagens distintas: uma simples (módulo), uma aritmética (multiplicação) e uma de dispersão mais genérica (`hashCode`).

---

## 3. Tamanhos dos Conjuntos e das Tabelas

Foram definidos três tamanhos diferentes para os vetores da tabela hash, mantendo um fator de escala de 10 entre cada um:

| Tamanho da Tabela | Descrição                        |
| ----------------- | -------------------------------- |
| 1.000             | Pequena, alta taxa de colisão    |
| 10.000            | Média, comportamento equilibrado |
| 100.000           | Grande, baixa taxa de colisão    |

Os conjuntos de dados gerados possuem:

| Conjunto   | Quantidade de Registros |
| ---------- | ----------------------- |
| Conjunto 1 | 100.000                 |
| Conjunto 2 | 1.000.000               |
| Conjunto 3 | 10.000.000              |

Cada registro é um número inteiro de 9 dígitos, gerado aleatoriamente, com **seeds fixos** para garantir que todas as funções hash utilizem os mesmos dados.

---

## 4. Gráficos dos Resultados e Comparativos

Os gráficos gerados a partir dos arquivos CSV mostram as relações entre tempo, colisões e eficiência para cada função hash e tipo de tabela.

### a) Tempo de Inserção

* O **encadeamento** apresentou inserções mais rápidas para tabelas menores, pois não exige sondagens sucessivas.
* O **endereçamento aberto com sondagem linear** teve desempenho inferior em altas taxas de ocupação, devido ao aumento das colisões.
* O **endereçamento quadrático** e o **duplo hash** reduziram as colisões em comparação à sondagem linear, resultando em tempos intermediários.

### b) Tempo de Busca

* Para tabelas com baixa ocupação, o **endereçamento aberto duplo** foi o mais rápido, pois evitou clusters longos.
* Em tabelas densas, o **encadeamento** teve desempenho mais previsível e consistente.

### c) Colisões e Listas Encadeadas

* O encadeamento mostrou maior número total de colisões registradas, mas com menor impacto no tempo de busca.
* O endereçamento aberto reduziu o número de colisões, mas formou **grandes aglomerados** (clusters), especialmente na sondagem linear.

### d) Gaps entre Elementos

* O **gap médio** foi menor no encadeamento devido à dispersão mais uniforme dos ponteiros.
* No endereçamento linear, o **maior gap** ocorreu nas tabelas de 100.000 elementos, indicando clusters densos.

---

## 5. Explicação: Melhor Função e Justificativa

Após comparar os resultados, a **função da multiplicação (Knuth)** demonstrou o melhor equilíbrio geral entre velocidade e distribuição, especialmente quando combinada com **endereçamento quadrático**.

| Função Hash           | Tipo de Rehashing | Desempenho Geral                          |
| --------------------- | ----------------- | ----------------------------------------- |
| Resto da Divisão      | Encadeamento      | Bom em tabelas pequenas, ruim em grandes  |
| Multiplicação (Knuth) | Quadrático        | Melhor equilíbrio geral                   |
| `hashCode()`          | Duplo Hash        | Rápido em buscas, mais lento em inserções |

---

## 6. Conclusão

O estudo mostrou que **não existe uma função hash universalmente superior** — a escolha depende da densidade da tabela e do método de tratamento de colisões. Entretanto:

* Para grandes volumes e tabelas bem dimensionadas, a **função da multiplicação com rehashing quadrático** apresentou o melhor desempenho global.
* O **encadeamento** é mais previsível e fácil de implementar, mas ocupa mais memória.
* O **endereçamento aberto** é mais eficiente em termos de espaço, porém sensível à taxa de ocupação e à escolha da função hash.

Assim, para aplicações que exigem **alta escalabilidade e desempenho equilibrado**, recomenda-se o uso da **função de multiplicação combinada com rehashing quadrático**, conforme comprovado pelos resultados obtidos.
