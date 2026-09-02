# ADR-006 — Frontend: React + TypeScript + Tailwind CSS

## Status
Aceito

## Data
2026-09-01

## Contexto
O sistema precisa de uma interface web para que a proprietária e sua equipe gerenciem estoque, vendas, fornecedores e relatórios. A interface deve ser funcional, responsiva e de fácil manutenção.

## Decisão
Utilizar **React 18** com **TypeScript**, **Vite** como bundler, **Tailwind CSS** para estilização e **shadcn/ui** como biblioteca de componentes.

## Stack Frontend

| Camada           | Tecnologia          |
|------------------|---------------------|
| Framework        | React 18            |
| Linguagem        | TypeScript          |
| Bundler          | Vite                |
| Estilização      | Tailwind CSS        |
| Componentes UI   | shadcn/ui           |
| Roteamento       | React Router v6     |
| Estado global    | Zustand             |
| Requisições HTTP | Axios               |
| Formulários      | React Hook Form     |
| Validação        | Zod                 |
| Gráficos         | Recharts            |

## Justificativa
- React é amplamente adotado, com grande ecossistema e facilidade de encontrar desenvolvedores
- TypeScript reduz bugs em tempo de desenvolvimento, especialmente em formulários e chamadas de API
- Vite oferece hot reload e builds muito mais rápidos que Create React App
- Tailwind CSS permite estilização rápida sem sair do HTML/JSX
- shadcn/ui fornece componentes acessíveis e customizáveis sem lock-in de biblioteca
- Zustand é mais simples que Redux para o tamanho do projeto
- React Hook Form + Zod é a combinação mais eficiente para formulários com validação tipada

## Consequências
- Necessário Node.js 18+ no ambiente de desenvolvimento
- Bundle otimizado pelo Vite para produção
- Comunicação com o backend exclusivamente via API REST (JSON)
