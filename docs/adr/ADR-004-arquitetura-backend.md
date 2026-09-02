# ADR-004 — Arquitetura do Backend: Arquitetura em Camadas (Layered Architecture)

## Status
Aceito

## Data
2026-09-01

## Contexto
É necessário definir como o código do backend será organizado internamente para garantir separação de responsabilidades, testabilidade e manutenibilidade ao longo do tempo.

## Decisão
Adotar **Arquitetura em Camadas** com separação clara entre:

```
Controller (API) → Service (Regras de Negócio) → Repository (Acesso a Dados)
```

Com os seguintes pacotes por módulo de domínio:

```
br.com.estilofitudi.<domínio>/
├── controller/      ← endpoints REST, DTOs de request/response
├── service/         ← regras de negócio, orquestração
├── domain/          ← entidades de domínio, enums
├── repository/      ← interfaces Spring Data JPA
├── mapper/          ← conversão entre entidades e DTOs
└── exception/       ← exceções específicas do domínio
```

## Justificativa
- Mais simples e direto que Clean Architecture ou Hexagonal para o tamanho e complexidade do projeto
- Familiar para a maioria dos desenvolvedores Spring Boot
- Facilita testes unitários por camada (mock de repositórios nos services)
- Separação suficiente para evitar acoplamento excessivo

## Módulos de Domínio Previstos
- `product` — produtos e variações
- `inventory` — estoque e movimentações
- `supplier` — fornecedores e lotes de entrada
- `sale` — vendas e itens de venda
- `user` — usuários e perfis de acesso
- `report` — relatórios e indicadores
- `promotion` — alertas e sugestões de promoção
- `settings` — configurações do sistema (ex: tempo para alerta de promoção)

## Consequências
- Estrutura de pacotes mais verbosa, mas previsível
- Regras de negócio concentradas na camada de service, facilitando testes
- Se a complexidade crescer significativamente no futuro, pode ser evoluído para Hexagonal sem grandes rupturas
