# Design System — EstiloFit Manager

Referência visual para o desenvolvimento do frontend. Derivado da identidade visual da marca EstiloFit.

**Inspiração de layout:** estrutura AdminLTE — sidebar fixa à esquerda, topbar, área de conteúdo — adaptada com identidade própria (dark mode + roxo vibrante) e construída em React + Tailwind CSS, sem jQuery ou Bootstrap.

---

## 1. Paleta de Cores

### Cores Primárias

| Token                   | Hex           | Uso                                                         |
|-------------------------|---------------|-------------------------------------------------------------|
| `brand-purple`          | `#CC00FF`     | Cor de destaque — botões primários, links ativos, badges    |
| `brand-purple-hover`    | `#AA00CC`     | Estado hover de elementos com brand-purple                  |
| `brand-purple-light`    | `#E566FF`     | Versão clara para foco, backgrounds suaves                  |
| `brand-purple-muted`    | `#CC00FF1A`   | Background translúcido para item ativo no menu e cards      |

### Cores de Fundo (Dark Mode — Padrão)

| Token                   | Hex       | Uso                                                  |
|-------------------------|-----------|------------------------------------------------------|
| `bg-base`               | `#1A1A1A` | Background principal da aplicação                    |
| `bg-surface`            | `#242424` | Cards, sidebar, topbar, painéis                      |
| `bg-surface-raised`     | `#2E2E2E` | Modais, dropdowns, tooltips                          |
| `bg-surface-hover`      | `#333333` | Estado hover de itens de lista e menu                |
| `bg-input`              | `#1F1F1F` | Fundo de campos de formulário                        |

### Cores de Texto (Dark Mode)

| Token                   | Hex       | Uso                                                  |
|-------------------------|-----------|------------------------------------------------------|
| `text-primary`          | `#E8E8E8` | Títulos e textos principais                          |
| `text-secondary`        | `#A0A0A0` | Textos de suporte, labels, subtítulos                |
| `text-muted`            | `#666666` | Placeholders, textos desabilitados                   |
| `text-on-purple`        | `#FFFFFF` | Texto sobre fundos brand-purple                      |

### Cores de Borda (Dark Mode)

| Token                   | Hex       | Uso                                                  |
|-------------------------|-----------|------------------------------------------------------|
| `border-default`        | `#333333` | Bordas padrão de cards e inputs                      |
| `border-subtle`         | `#2A2A2A` | Divisores, separadores                               |
| `border-focus`          | `#CC00FF` | Borda de foco em inputs e selects                    |

### Cores Semânticas

| Token                   | Hex         | Uso                                                       |
|-------------------------|-------------|-----------------------------------------------------------|
| `success`               | `#22C55E`   | Confirmações, estoque OK, venda confirmada, parcela recebida |
| `success-muted`         | `#22C55E1A` | Background de badges de sucesso                           |
| `warning`               | `#F59E0B`   | Estoque baixo, parcelas próximas do vencimento            |
| `warning-muted`         | `#F59E0B1A` | Background de badges de aviso                             |
| `danger`                | `#EF4444`   | Erros, estoque zerado, venda cancelada                    |
| `danger-muted`          | `#EF44441A` | Background de badges de erro                              |
| `info`                  | `#3B82F6`   | Informações neutras, dicas                                |
| `info-muted`            | `#3B82F61A` | Background de badges informativos                         |

---

## 2. Tipografia

### Família de Fontes
- **Principal:** `Inter` — moderna, legível, ideal para dashboards e formulários
- **Google Fonts:** importar apenas os pesos 400, 500, 600, 700

### Escala Tipográfica

| Token       | Tamanho | Peso | Uso                                        |
|-------------|---------|------|--------------------------------------------|
| `text-xs`   | 12px    | 400  | Labels de tabela, metadados, badges        |
| `text-sm`   | 14px    | 400  | Corpo de texto, inputs, descrições         |
| `text-base` | 16px    | 400  | Texto padrão                               |
| `text-lg`   | 18px    | 500  | Subtítulos de seção                        |
| `text-xl`   | 20px    | 600  | Títulos de card, totais de dashboard       |
| `text-2xl`  | 24px    | 700  | Títulos de página                          |
| `text-3xl`  | 30px    | 700  | Números de destaque no dashboard (KPIs)    |

---

## 3. Ícones

Utilizar **Lucide React** — traço fino, estilo consistente, tree-shakable (só importa o que usa).

**Regra:** todo item de menu, botão de ação, KPI card e seção de formulário deve ter um ícone. Nenhuma ação deve ser identificada apenas por texto.

### Ícones por Módulo

| Módulo                   | Ícone Lucide          | Uso no menu |
|--------------------------|-----------------------|-------------|
| Dashboard                | `LayoutDashboard`     | ✅           |
| Produtos                 | `Shirt`               | ✅           |
| Estoque                  | `Package`             | ✅           |
| Entrada de Mercadoria    | `PackagePlus`         | sub-item    |
| Histórico de Movimentações | `ClipboardList`     | sub-item    |
| Fornecedores             | `Truck`               | ✅           |
| Vendas                   | `ShoppingBag`         | ✅           |
| Nova Venda               | `Plus`                | sub-item    |
| Contas a Receber         | `CreditCard`          | ✅           |
| Fluxo Projetado          | `TrendingUp`          | sub-item    |
| Relatórios               | `BarChart2`           | ✅           |
| Pró-labore               | `Wallet`              | sub-item    |
| Margem de Lucro          | `PieChart`            | sub-item    |
| Alertas / Promoções      | `Tag`                 | ✅ (com badge) |
| Usuários                 | `Users`               | ✅ (Admin)   |
| Configurações            | `Settings`            | ✅ (Admin)   |
| Notificações             | `Bell`                | topbar      |
| Perfil do usuário        | `UserCircle`          | topbar      |
| Logout                   | `LogOut`              | topbar      |

### Ícones de Ação em Tabelas

| Ação           | Ícone Lucide  |
|----------------|---------------|
| Ver detalhes   | `Eye`         |
| Editar         | `Pencil`      |
| Excluir        | `Trash2`      |
| Confirmar      | `Check`       |
| Cancelar       | `X`           |
| Dar baixa      | `CheckCircle` |
| Filtrar        | `Filter`      |
| Exportar       | `Download`    |
| Adicionar      | `Plus`        |

### Ícones de KPI no Dashboard

| KPI                     | Ícone Lucide    |
|-------------------------|-----------------|
| Total vendido no mês    | `DollarSign`    |
| Número de vendas        | `ShoppingBag`   |
| Itens em estoque        | `Package`       |
| Pró-labore estimado     | `Wallet`        |
| A receber (parcelas)    | `CreditCard`    |
| Alertas ativos          | `AlertTriangle` |

---

## 4. Layout Geral (Inspiração AdminLTE)

```
┌─────────────────────────────────────────────────────────────┐
│                        TOPBAR (64px)                        │
│  ☰  [Logo EstiloFit]    Breadcrumb        🔔  👤 Nome ▾    │
├───────────────┬─────────────────────────────────────────────┤
│               │                                             │
│   SIDEBAR     │           ÁREA DE CONTEÚDO                  │
│   (240px)     │                                             │
│               │   ┌──────────────────────────────────────┐  │
│  🏠 Dashboard │   │  Título da Página + ação principal   │  │
│  👕 Produtos  │   └──────────────────────────────────────┘  │
│  📦 Estoque ▾ │                                             │
│    ┣ Entrada  │   ┌────────┐ ┌────────┐ ┌────────┐ ┌─────┐ │
│    ┗ Histórico│   │  KPI 1 │ │  KPI 2 │ │  KPI 3 │ │KPI4 │ │
│  🚚 Forneceds │   └────────┘ └────────┘ └────────┘ └─────┘ │
│  🛍 Vendas   │                                             │
│  💳 Contas   │   ┌──────────────────┐ ┌─────────────────┐  │
│  📊 Relatórios│  │   Tabela/Lista   │ │   Card lateral  │  │
│  🏷 Alertas  │   │                  │ │                 │  │
│  ─────────── │   └──────────────────┘ └─────────────────┘  │
│  👥 Usuários  │                                             │
│  ⚙ Config    │                                             │
│               │                                             │
└───────────────┴─────────────────────────────────────────────┘
```

---

## 5. Sidebar Detalhada

### Estrutura
- **Largura:** `240px` expandida / `64px` recolhida (ícone apenas)
- **Background:** `#242424`
- **Borda direita:** `1px solid #2A2A2A`
- **Toggle:** botão `☰` na topbar colapsa/expande a sidebar

### Logo
- Área do logo: `64px` de altura, padding `16px`
- Logo padrão (fundo escuro) quando sidebar expandida
- Apenas ícone/símbolo quando recolhida

### Item de Menu

**Estado normal:**
```
[ícone 20px]  Nome do módulo          (padding: 12px 16px)
```
- Ícone: `text-secondary` (`#A0A0A0`)
- Texto: `text-secondary`
- Border-radius: `8px` (dentro do padding da sidebar)

**Estado hover:**
- Background: `#333333`
- Ícone e texto: `text-primary` (`#E8E8E8`)

**Estado ativo:**
- Background: `#CC00FF1A` (roxo translúcido)
- Ícone e texto: `#CC00FF`
- Barra lateral esquerda: `3px solid #CC00FF`

**Sub-itens (accordion):**
- Recuo: `16px` a mais
- Ícone menor: `16px`
- Fundo diferenciado: `#1F1F1F`
- Animação suave de expand/collapse (200ms ease)

### Grupos de Menu
```
PRINCIPAL
  🏠 Dashboard
  👕 Produtos
  📦 Estoque
  🚚 Fornecedores
  🛍 Vendas
  💳 Contas a Receber
  📊 Relatórios
  🏷 Alertas         [badge com número]

ADMINISTRAÇÃO  (visível só para Admin)
  👥 Usuários
  ⚙ Configurações
```
- Separador de grupo: label em `text-xs` `text-muted`, `uppercase`, `letter-spacing: 0.1em`

### Badge de Notificação no Menu
- Alertas de promoção e contas a receber mostram um badge circular com o total de pendências
- Badge: background `#CC00FF`, texto branco, `font-size: 11px`, posicionado à direita do label

---

## 6. Topbar Detalhada

- **Height:** `64px`
- **Background:** `#242424`
- **Borda inferior:** `1px solid #2A2A2A`
- **Padding:** `0 24px`

### Elementos (esquerda → direita)
1. **Botão toggle sidebar** — ícone `Menu` ou `PanelLeftClose`, `text-secondary`
2. **Breadcrumb** — `Home / Módulo / Página atual`, `text-sm`, `text-muted` para pais, `text-primary` para atual
3. **Espaço flexível**
4. **Sino de notificações** — ícone `Bell` com badge vermelho se houver alertas não vistos
5. **Avatar + nome do usuário** — dropdown com `Meu Perfil` e `Sair`

---

## 7. Área de Conteúdo

### Page Header
Todo módulo começa com um cabeçalho de página:
```
┌─────────────────────────────────────────────────────┐
│  [Ícone do módulo 24px]  Título da Página           │
│  Descrição curta da página (text-secondary, text-sm)│
│                              [+ Botão de ação prim.]│
└─────────────────────────────────────────────────────┘
```

### KPI Cards (Dashboard)
```
┌──────────────────────────┐
│  [Ícone 32px]            │
│                          │
│  R$ 4.850,00             │  ← text-3xl, font-bold
│  Total Vendido no Mês    │  ← text-sm, text-secondary
│                          │
│  ▲ 12% vs mês anterior   │  ← text-xs, success ou danger
└──────────────────────────┘
```
- Background: `bg-surface`
- Borda: `1px solid border-default`
- Borda superior: `3px solid brand-purple` (destaque)
- Border-radius: `12px`
- Ícone colorido com `brand-purple` ou cor semântica do indicador

### Tabelas
- Header: background `#2E2E2E`, texto `text-secondary`, `text-xs uppercase`
- Linha zebrada: linhas pares com background `#1F1F1F`
- Hover na linha: background `#2A2A2A`
- Ações da linha: ícones à direita, visíveis no hover da linha
- Paginação: no rodapé da tabela, com `text-sm`
- Coluna de status: sempre usa Badge colorido

### Formulários
- Cada campo tem: label com ícone pequeno + input + mensagem de erro
- Labels: `text-sm`, `text-secondary`, `font-medium`
- Campos obrigatórios: asterisco `*` em `brand-purple`
- Mensagem de erro: `text-xs`, `danger`, com ícone `AlertCircle`
- Botões de ação no rodapé: primário à direita, secundário à esquerda
- Formulários complexos usam layout em card com seções separadas por divisores

### Modais
- Overlay: `rgba(0,0,0,0.7)`
- Modal: background `bg-surface-raised`, border-radius `16px`, max-width `560px`
- Header do modal: título com ícone + botão `X` para fechar
- Footer: botões de ação

---

## 8. Componentes Específicos por Módulo

### Registro de Venda
- Interface de "carrinho": busca de produto com autocomplete, lista de itens adicionados
- Ícone `Search` no campo de busca
- Cada item da lista mostra ícone `Shirt`, nome, variação, quantidade (+ / -) e preço
- Resumo fixo no rodapé: subtotal, desconto, total, forma de pagamento

### Dashboard — Card de Alerta de Promoção
```
┌─────────────────────────────────────────┐
│  🏷 [Produto] – [Tam] / [Cor]           │
│  ⏱ 75 dias sem venda                   │
│  De: R$ 89,90  →  Por: R$ 44,95        │
│                    [Ignorar] [Aplicar]  │
└─────────────────────────────────────────┘
```

### Contas a Receber — Timeline por Mês
- Parcelas agrupadas em colunas por mês (Outubro, Novembro, Dezembro...)
- Cada coluna mostra o total a receber no topo
- Parcelas individuais listadas abaixo com status visual
- Botão `CheckCircle` para dar baixa individualmente

---

## 9. Responsividade

| Breakpoint | Comportamento                                        |
|------------|------------------------------------------------------|
| Desktop    | Sidebar fixa expandida (240px) + conteúdo total      |
| Tablet     | Sidebar recolhida (64px, ícones apenas)              |
| Mobile     | Sidebar oculta, abre como drawer via overlay         |

---

## 10. Animações e Transições

| Elemento              | Animação                          | Duração  |
|-----------------------|-----------------------------------|----------|
| Sidebar collapse      | `width` + `opacity`               | 200ms ease |
| Sub-menu accordion    | `max-height` + `opacity`          | 200ms ease |
| Hover em botão        | `background-color`                | 150ms ease |
| Hover em linha tabela | `background-color`                | 100ms ease |
| Modal open/close      | `opacity` + `scale` (0.95 → 1)   | 200ms ease |
| Toast/notificação     | `translateY` + `opacity` (slide up) | 300ms ease |
| Badge de alerta       | Pulso sutil em `box-shadow`       | 2s infinite |

---

## 11. Tema Tailwind CSS

Configuração para `tailwind.config.js`:

```js
theme: {
  extend: {
    colors: {
      brand: {
        purple:         '#CC00FF',
        'purple-hover': '#AA00CC',
        'purple-light': '#E566FF',
        'purple-muted': 'rgba(204, 0, 255, 0.10)',
      },
      bg: {
        base:             '#1A1A1A',
        surface:          '#242424',
        'surface-raised': '#2E2E2E',
        'surface-hover':  '#333333',
        input:            '#1F1F1F',
      },
      border: {
        default: '#333333',
        subtle:  '#2A2A2A',
        focus:   '#CC00FF',
      },
    },
    fontFamily: {
      sans: ['Inter', 'sans-serif'],
    },
    borderRadius: {
      card: '12px',
      btn:  '8px',
      modal: '16px',
    },
    boxShadow: {
      card:  '0 1px 3px rgba(0,0,0,0.4)',
      modal: '0 20px 60px rgba(0,0,0,0.6)',
      focus: '0 0 0 2px rgba(204,0,255,0.3)',
    },
  },
}
```

---

## 13. Autenticação e Proteção de Rotas no Frontend

### Regra geral
Toda rota da aplicação — sem exceção — exige usuário autenticado. A única rota pública é `/login`.

### Componentes de proteção
- `<PrivateRoute>` — redireciona para `/login` se não há token válido em memória
- `<RoleRoute roles={[...]}>` — exibe página `/403` se a role do usuário não está na lista
- `<RoleGuard roles={[...]}>` — oculta elementos de UI (botões, menus, abas) para roles não autorizadas

### Armazenamento do token
- **Access token:** mantido apenas em memória (variável de estado React/Zustand) — nunca em `localStorage` ou `sessionStorage`
- **Refresh token:** armazenado como `httpOnly cookie` — não acessível por JavaScript

### Renovação automática
- Quando o access token expira durante uso, o frontend tenta renová-lo silenciosamente via refresh token
- Se o refresh token também expirou, o usuário é redirecionado para `/login` com mensagem informativa

### O que o frontend esconde por role
| Elemento                              | ADMIN | MANAGER | SELLER |
|---------------------------------------|:-----:|:-------:|:------:|
| Menu Usuários                         |  ✅   |   ❌    |   ❌   |
| Menu Configurações do sistema         |  ✅   |   ❌    |   ❌   |
| Menu Configurações de negócio         |  ✅   |   ✅    |   ❌   |
| Menu Fornecedores                     |  ✅   |   ✅    |   ❌   |
| Menu Estoque (entrada/histórico)      |  ✅   |   ✅    |   ❌   |
| Menu Contas a Receber                 |  ✅   |   ✅    |   ❌   |
| Menu Relatórios (loja toda)           |  ✅   |   ✅    |   ❌   |
| Menu Relatórios > Pró-labore          |  ✅   |   ✅    |   ❌   |
| Alertas de promoção                   |  ✅   |   ✅    |   ❌   |
| Botão Cancelar Venda                  |  ✅   |   ✅    |   ❌   |
| Relatório das próprias vendas         |  ✅   |   ✅    |   ✅   |
| Registrar Venda                       |  ✅   |   ✅    |   ✅   |
| Consultar Estoque disponível          |  ✅   |   ✅    |   ✅   |

> **Importante:** ocultar no frontend é UX, não segurança. O backend valida a role em todo endpoint independentemente.


```
SIDEBAR
│
├── 🏠 Dashboard
│
├── 👕 Produtos
│   ├── 📋 Listagem
│   ├── ➕ Novo Produto
│   └── 🔢 Variações
│
├── 📦 Estoque
│   ├── 📊 Visão Geral
│   ├── 📥 Entrada de Mercadoria
│   └── 📜 Histórico de Movimentações
│
├── 🚚 Fornecedores
│   ├── 📋 Listagem
│   └── ➕ Novo Fornecedor
│
├── 🛍 Vendas
│   ├── 📋 Listagem
│   ├── ➕ Registrar Venda
│   └── 🔍 Detalhe da Venda
│
├── 💳 Contas a Receber
│   ├── ⏳ Parcelas Pendentes
│   └── 📅 Fluxo Projetado por Mês
│
├── 📊 Relatórios
│   ├── 🛍 Vendas do Período
│   ├── 🏆 Produtos Mais Vendidos
│   ├── 🥧 Margem de Lucro
│   └── 💰 Pró-labore Estimado  [🔒 Admin]
│
├── 🏷 Alertas de Promoção  [badge]
│
── ── ── ── (seção Admin) ── ── ── ──
│
├── 👥 Usuários  [Admin]
│
└── ⚙ Configurações  [Admin]
    ├── Margem de lucro padrão
    ├── Dias para alerta de promoção
    ├── Limite mínimo de estoque
    ├── Canais de venda
    └── Tamanhos disponíveis
```
