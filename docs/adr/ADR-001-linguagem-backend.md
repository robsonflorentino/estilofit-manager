# ADR-001 — Linguagem do Backend: Kotlin

## Status
Aceito

## Data
2026-09-01

## Contexto
O sistema estilofit-manager precisa de um backend robusto, com tipagem forte e boa integração com o ecossistema Java/Spring, dado que a equipe já possui familiaridade com esse ambiente.

## Decisão
Utilizar **Kotlin** como linguagem principal do backend.

## Justificativa
- Sintaxe mais concisa que Java, reduzindo boilerplate (data classes, null safety, extension functions)
- Totalmente interoperável com o ecossistema Java/Spring
- Null safety nativo reduz erros em tempo de execução
- Suporte oficial do Spring Boot para Kotlin
- Coroutines para programação assíncrona quando necessário

## Consequências
- Necessário JDK 17+ no ambiente de build e produção
- Desenvolvedores com background Java se adaptam rapidamente
- Compilação ligeiramente mais lenta que Java puro, mas negligenciável no contexto do projeto
