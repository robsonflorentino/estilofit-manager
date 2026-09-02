# ADR-002 — Framework Backend: Spring Boot

## Status
Aceito

## Data
2026-09-01

## Contexto
Com Kotlin definido como linguagem, é necessário escolher o framework que vai sustentar a API REST, injeção de dependência, acesso a dados e segurança.

## Decisão
Utilizar **Spring Boot 3.x** como framework principal do backend.

## Justificativa
- Ecossistema maduro com suporte de longo prazo
- Suporte nativo ao Kotlin com extensões dedicadas
- Spring Security para autenticação e autorização com RBAC
- Spring Data JPA para acesso ao banco de dados com Hibernate
- Spring Validation para validação de entrada de dados
- Amplamente documentado e com grande comunidade

## Consequências
- Startup time mais lento comparado a frameworks reativos (Ktor, Quarkus), mas aceitável para o volume esperado
- Necessário JDK 17+ (requisito do Spring Boot 3.x)
- Build com Gradle (Kotlin DSL)
