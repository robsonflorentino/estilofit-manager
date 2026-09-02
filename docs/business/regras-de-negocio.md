# Regras de Negócio — EstiloFit Manager

## Sumário
1. [Produto e Variações](#1-produto-e-variações)
2. [Fornecedores e Entrada de Mercadoria](#2-fornecedores-e-entrada-de-mercadoria)
3. [Estoque](#3-estoque)
4. [Vendas](#4-vendas)
5. [Contas a Receber](#5-contas-a-receber)
6. [Promoções](#6-promoções)
7. [Canais de Venda](#7-canais-de-venda)
8. [Usuários e Perfis de Acesso](#8-usuários-e-perfis-de-acesso)
9. [Relatórios e Indicadores](#9-relatórios-e-indicadores)
10. [Configurações do Sistema](#10-configurações-do-sistema)

---

## 1. Produto e Variações

### RN-001 — Estrutura do Produto
Um produto representa uma peça de roupa em sua forma genérica (ex: "Blusa Listrada"). Um produto por si só não tem estoque — o estoque é controlado por variação.

### RN-002 — Variações de Produto
Cada produto pode ter uma ou mais variações. Uma variação é a combinação de **tamanho** e **cor**.

Exemplos de variações para "Blusa Listrada":
- M / Azul
- M / Rosa
- G / Azul

### RN-003 — SKU de Variação
Cada variação recebe um **SKU único** gerado automaticamente pelo sistema no formato:
```
{PREFIXO}-{SEQUENCIAL}-{TAMANHO}-{COR}
Exemplo: BLS-001-M-AZL
```
O SKU é imutável após a criação da variação.

### RN-004 — Campos do Produto
Um produto deve ter obrigatoriamente:
- Nome
- Descrição
- Categoria (ex: Blusas, Calças, Vestidos, Saias)

Campos opcionais:
- Imagem (funcionalidade prevista para versão futura)

### RN-005 — Campos da Variação
Uma variação deve ter obrigatoriamente:
- SKU (gerado automaticamente)
- Tamanho (ex: PP, P, M, G, GG, XGG)
- Cor
- Margem de lucro (herdada da configuração global, mas sobrescritível por variação)

O preço de venda **não é informado manualmente** — ele é calculado automaticamente com base no custo real e na margem configurada (ver RN-006).

### RN-006 — Preço de Custo e Cálculo do Preço de Venda
O preço de custo de uma variação não é informado no cadastro do produto. Ele é calculado automaticamente no momento da **entrada de mercadoria**, considerando o custo unitário informado no lote e o rateio do frete (ver RN-009).

Após o cálculo do custo real, o sistema **recalcula automaticamente o preço de venda sugerido** com base na margem de lucro:

```
Preço de venda = custo_real_unitário × (1 + margem / 100)

Exemplo com margem de 100%:
  Custo real = R$ 40,00
  Preço de venda = R$ 40,00 × (1 + 1,00) = R$ 80,00
```

**Regras:**
- A margem padrão é definida nas configurações do sistema (padrão: 100%)
- Cada variação pode ter uma margem individual que sobrescreve a margem global
- O preço de venda é **recalculado automaticamente** sempre que o custo médio da variação é atualizado (nova entrada de lote)
- O usuário pode ajustar manualmente o preço de venda final de uma variação, mas o sistema exibe a margem efetiva resultante para manter a transparência
- O preço de venda nunca é alterado retroativamente em vendas já confirmadas (ver RN-018)

---

## 2. Fornecedores e Entrada de Mercadoria

### RN-007 — Fornecedores
O sistema suporta múltiplos fornecedores. Cada fornecedor deve ter:
- Nome / Razão Social
- Contato (telefone, e-mail ou WhatsApp)
- Informações opcionais: CNPJ, endereço, observações

### RN-008 — Lote de Entrada
Uma entrada de mercadoria é registrada como um **lote**, vinculado a um fornecedor, e contém:
- Data da entrada
- Fornecedor
- Valor do frete da remessa
- Um ou mais itens, cada um com:
  - Variação do produto
  - Quantidade recebida
  - Preço de custo unitário (sem frete)

### RN-009 — Custo Real com Frete Rateado
O frete da remessa deve ser rateado entre todas as variações do lote de forma **proporcional ao valor de custo de cada item**.

**Fórmula:**
```
Participação do item = (custo_unitário × quantidade) / custo_total_sem_frete
Frete alocado ao item = frete_total × participação_do_item
Custo real unitário = custo_unitário + (frete_alocado_ao_item / quantidade)
```

**Exemplo:**
- Lote com 2 itens, frete R$ 200,00
  - Item A: 10 unidades × R$ 30,00 = R$ 300,00 (50% do custo total)
  - Item B: 5 unidades × R$ 60,00 = R$ 300,00 (50% do custo total)
- Frete alocado ao Item A: R$ 100,00 → R$ 10,00/unidade
- Frete alocado ao Item B: R$ 100,00 → R$ 20,00/unidade
- Custo real: Item A = R$ 40,00/un | Item B = R$ 80,00/un

### RN-010 — Atualização do Custo Médio
Quando uma variação já tem custo registrado (entrou em lote anterior), o custo real é atualizado pelo **custo médio ponderado**:
```
Custo médio = (estoque_atual × custo_atual + quantidade_nova × custo_novo) / (estoque_atual + quantidade_nova)
```

### RN-011 — Atualização de Estoque na Entrada
Ao confirmar um lote de entrada, o estoque de cada variação é incrementado com a quantidade recebida. A operação deve ser atômica (transação de banco de dados).

---

## 3. Estoque

### RN-012 — Estoque por Variação
O estoque é controlado individualmente por variação de produto (combinação de tamanho e cor). Não existe estoque em nível de produto.

### RN-013 — Movimentações de Estoque
Toda alteração de estoque deve gerar um registro de movimentação com:
- Tipo: ENTRADA, SAÍDA (venda), AJUSTE
- Quantidade
- Data e hora
- Usuário responsável
- Referência (número do lote ou número da venda)

### RN-014 — Ajuste Manual de Estoque
Apenas usuários com perfil **Administrador** ou **Gestor** podem realizar ajustes manuais de estoque. Todo ajuste deve ter uma justificativa obrigatória.

### RN-015 — Estoque Negativo
O sistema **não permite** que o estoque de uma variação fique negativo. Ao registrar uma venda, o sistema deve validar a disponibilidade de cada item antes de confirmar.

---

## 4. Vendas

### RN-016 — Estrutura da Venda
Uma venda contém:
- Data e hora
- Canal de venda (Instagram, WhatsApp, Presencial, etc.)
- Forma de pagamento (Dinheiro, PIX, Cartão de Débito, Cartão de Crédito)
- Parcelamento (quando aplicável — apenas para cartão de crédito)
- Taxa da maquininha em percentual (obrigatória quando parcelado no cartão de crédito)
- Desconto (valor fixo ou percentual, opcional)
- Um ou mais itens, cada um com:
  - Variação do produto
  - Quantidade
  - Preço unitário no momento da venda

### RN-017 — Itens de Venda
Uma venda pode conter múltiplos itens (peças diferentes ou a mesma peça em quantidades diferentes). Não há limite de itens por venda.

### RN-018 — Preço no Momento da Venda
O preço unitário registrado na venda é o **preço vigente da variação no momento do lançamento**. Alterações futuras no preço de venda da variação não afetam vendas já registradas.

### RN-019 — Desconto
Um desconto pode ser aplicado sobre o valor total da venda:
- Desconto percentual (ex: 10%) ou valor fixo (ex: R$ 20,00)
- O desconto não pode resultar em valor de venda negativo ou zerado

### RN-020 — Confirmação da Venda
Ao confirmar uma venda:
1. O sistema valida que todas as variações têm estoque suficiente
2. O estoque de cada variação é decrementado
3. A movimentação de estoque é registrada
4. A venda é salva com status **CONFIRMADA**

Todo esse processo deve ser atômico (transação de banco de dados).

### RN-021 — Cancelamento de Venda
Uma venda confirmada pode ser cancelada apenas por **Administrador** ou **Gestor**. Ao cancelar:
1. O estoque das variações é restaurado
2. A movimentação de estoque do tipo AJUSTE é registrada
3. A venda recebe status **CANCELADA**

### RN-022 — Vendas Parceladas
Vendas parceladas só são permitidas na forma de pagamento **Cartão de Crédito**. Ao registrar uma venda parcelada:

1. O número de parcelas deve ser informado (mínimo 2)
2. A taxa da maquininha (em %) deve ser informada — normalmente repassada ao cliente, já embutida no valor final da venda
3. O sistema gera automaticamente os **lançamentos de contas a receber**, um por parcela, com vencimento em D+30, D+60, D+90... a partir da data da venda
4. O valor de cada parcela = `final_amount / número_de_parcelas` (valor já com taxa, se repassada)
5. O valor líquido de cada parcela = `valor_parcela × (1 - taxa_maquininha / 100)` — registrado para fins de relatório de receita real

---

## 5. Contas a Receber

### RN-023 — Geração de Parcelas
As parcelas de contas a receber são geradas automaticamente no momento da confirmação de uma venda parcelada no cartão de crédito. Não é possível criar parcelas manualmente.

### RN-024 — Vencimento das Parcelas
As datas de vencimento são calculadas automaticamente:
```
Parcela 1: data_venda + 30 dias
Parcela 2: data_venda + 60 dias
Parcela N: data_venda + (N × 30) dias
```

### RN-025 — Campos de uma Parcela
Cada parcela contém:
- Número da parcela (1/3, 2/3, 3/3)
- Data de vencimento
- Valor bruto da parcela
- Valor líquido da parcela (descontada a taxa da maquininha)
- Status: PENDING (aguardando), RECEIVED (recebida)
- Data de recebimento (preenchida ao dar baixa)

### RN-026 — Baixa de Parcela
O recebimento de uma parcela deve ser confirmado manualmente por um usuário com perfil **Administrador** ou **Gestor**. Ao dar baixa:
- O status da parcela muda para RECEIVED
- A data de recebimento é registrada
- O valor líquido entra no cálculo de receita realizada do mês

### RN-027 — Cancelamento de Venda Parcelada
Se uma venda parcelada for cancelada, todas as parcelas ainda não recebidas são canceladas automaticamente. Parcelas já recebidas permanecem no histórico com status RECEIVED, e o sistema exibe um aviso de que houve estorno.

### RN-028 — Visibilidade das Parcelas
- **Dashboard**: exibe o total a receber nos próximos 30 dias (parcelas com vencimento no período)
- **Relatório de contas a receber**: lista todas as parcelas pendentes com data de vencimento e valor
- **Relatório de fluxo de caixa projetado**: agrupa os recebimentos futuros por mês

---

## 6. Promoções

### RN-029 — Alerta de Promoção por Tempo de Estoque
O sistema monitora o tempo que cada variação está sem registrar uma venda. Quando esse tempo supera o **limite configurável** (padrão: 60 dias), o sistema gera um alerta de promoção para aquela variação.

### RN-030 — Sugestão de Desconto
O alerta de promoção sugere automaticamente um desconto de **50% sobre o preço de venda vigente** da variação. Essa sugestão é apenas informativa — não altera o preço automaticamente.

### RN-031 — Exibição do Alerta
Os alertas de promoção são exibidos no **dashboard** do sistema, visíveis para perfis Administrador e Gestor. O alerta exibe:
- Nome do produto e variação (tamanho/cor)
- Dias sem venda
- Preço atual
- Preço sugerido com 50% de desconto

### RN-032 — Resolução do Alerta
Um alerta pode ser resolvido quando:
- Uma venda da variação é registrada (alerta removido automaticamente)
- O usuário descarta o alerta manualmente (com opção de "lembrar em X dias")

---

## 7. Canais de Venda

### RN-033 — Canais Disponíveis
Os canais de venda são configuráveis pelo Administrador. Canais padrão iniciais:
- Instagram
- WhatsApp
- Presencial

### RN-034 — Extensibilidade de Canais
Novos canais podem ser adicionados pelo Administrador sem necessidade de alteração no código. Canais podem ser desativados (sem exclusão) para preservar o histórico de vendas.

---

## 8. Usuários e Perfis de Acesso

### RN-035 — Perfis de Acesso
### RN-035 — Perfis de Acesso
O sistema possui três perfis fixos com hierarquia bem definida:

**ADMIN (Administrador do Sistema)**
Nível mais alto. Responsável pela operação técnica da plataforma — não é o dono da loja. Tem acesso a tudo para fins de suporte, auditoria e configuração do sistema.
- Único perfil que pode gerenciar usuários (criar, editar, ativar/desativar)
- Único perfil que pode acessar configurações técnicas do sistema
- Acessa todos os módulos e relatórios de negócio (para suporte)
- Pode operar qualquer funcionalidade

**MANAGER (Gestor — Proprietário da Loja)**
Responsável pelo negócio. Acesso completo a toda a operação comercial da loja.
- Gerencia produtos, variações, fornecedores e estoque
- Registra entradas de mercadoria e lotes
- Registra e cancela vendas
- Gerencia contas a receber e dá baixa em parcelas
- Acessa **todos** os relatórios de negócio, incluindo pró-labore e fluxo de caixa
- Configura regras de negócio: margem de lucro padrão, canais de venda, tamanhos
- Visualiza alertas de promoção
- **Não gerencia usuários nem configurações técnicas do sistema**

**SELLER (Vendedor)**
Operador de vendas. Acesso restrito ao essencial para atender clientes.
- Registra vendas
- Consulta estoque disponível
- Visualiza relatório das **suas próprias vendas** (filtrado pelo usuário logado)
- Não acessa: fornecedores, produtos, estoque gerencial, relatórios da loja, pró-labore, contas a receber, usuários ou configurações

A tabela completa de permissões por funcionalidade está na ADR-005.

### RN-036 — Criação de Usuários
Apenas o Administrador pode criar, editar e desativar usuários. Um usuário desativado não pode fazer login, mas seus registros históricos são preservados.

### RN-037 — Autenticação
O acesso ao sistema é feito por e-mail e senha. A senha deve ter no mínimo 8 caracteres. Senhas são armazenadas com hash BCrypt.

---

## 9. Relatórios e Indicadores

### RN-038 — Estoque Atual
Exibe o estoque atual de todas as variações, com filtros por produto, categoria, tamanho e cor. Indica variações com estoque zerado ou abaixo de um limite mínimo configurável.

### RN-039 — Vendas do Período
Exibe o total de vendas em um período selecionado, com agrupamento por:
- Canal de venda
- Produto / Variação
- Forma de pagamento

### RN-040 — Produtos Mais Vendidos
Ranking dos produtos e variações com maior volume de vendas (por quantidade e por valor) no período selecionado.

### RN-041 — Margem de Lucro por Produto
Calcula a margem de lucro de cada variação com base no custo médio (incluindo frete rateado) e no preço de venda registrado nas vendas do período.

```
Margem = ((preço_venda - custo_médio) / preço_venda) × 100
```

### RN-042 — Contas a Receber e Fluxo de Caixa Projetado
Relatório que exibe as parcelas de vendas parceladas ainda não recebidas, agrupadas por mês de vencimento. Permite que a proprietária saiba com antecedência o dinheiro garantido a entrar.

Exibe:
- Total a receber por mês (próximos 3 meses)
- Detalhamento por venda (cliente, valor da parcela, vencimento)
- Valor líquido (já descontada a taxa da maquininha quando não repassada)

### RN-043 — Pró-labore Estimado
Calcula o valor que a proprietária pode retirar como pró-labore no mês sem comprometer a operação da loja.

**Acesso:** perfis **Administrador** e **Gestor**. O Gestor (proprietária da loja) é a principal destinatária deste relatório. O Administrador acessa para fins de suporte.

**Fórmula:**
```
Receita do mês        = soma das vendas confirmadas no mês (valor pago, sem parceladas a receber)
Custos do mês         = custo das mercadorias vendidas (CMV) + frete dos lotes do mês
Lucro bruto           = Receita do mês - Custos do mês
Capital de giro mín.  = média do valor gasto com fornecedores nos últimos 3 meses
Pró-labore sugerido   = Lucro bruto - Capital de giro mínimo
```

**Regras:**
- O cálculo considera apenas valores já recebidos (sem contas a receber)
- O capital de giro mínimo é estimado automaticamente com base na média dos últimos 3 meses de compras com fornecedores
- Se não houver histórico suficiente (menos de 3 meses), usa a média do histórico disponível
- Se o resultado for negativo ou zero, o sistema exibe aviso de que não há margem para retirada no mês

---

## 10. Configurações do Sistema

### RN-044 — Configurações Gerenciáveis
Apenas o Administrador pode alterar as configurações do sistema:

| Configuração                        | Padrão   | Descrição                                              |
|-------------------------------------|----------|--------------------------------------------------------|
| Margem de lucro padrão              | 100%     | Margem usada para calcular o preço de venda das variações |
| Dias para alerta de promoção        | 60 dias  | Tempo sem venda para gerar alerta de 50% off           |
| Estoque mínimo para alerta          | 2 unid.  | Quantidade mínima antes de alertar estoque baixo       |
| Canais de venda ativos              | 3 canais | Instagram, WhatsApp, Presencial                        |

### RN-045 — Tamanhos e Cores
Os tamanhos disponíveis para variações são configuráveis pelo Administrador. Padrão inicial:
- Tamanhos: PP, P, M, G, GG, XGG
- Cores: definidas livremente por texto no cadastro da variação
