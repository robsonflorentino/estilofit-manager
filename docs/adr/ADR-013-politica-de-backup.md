# ADR-013 — Política de Backup do Banco de Dados

## Status
Aceito

## Data
2026-09-01

## Contexto
O banco de dados contém informações críticas do negócio: produtos, estoque, fornecedores, histórico de vendas e contas a receber. A perda desses dados causaria prejuízo direto à operação da loja. É necessário definir como os backups serão realizados e armazenados.

## Decisão
Backup **manual e periódico** via `pg_dump`, armazenado localmente e em nuvem (Google Drive). Responsabilidade do Administrador do sistema.

---

## Procedimento de Backup

### Comando para gerar o backup
```bash
# Conectar ao banco do Railway e exportar dump comprimido
pg_dump \
  --host=<DB_HOST> \
  --port=<DB_PORT> \
  --username=<DB_USERNAME> \
  --dbname=<DB_NAME> \
  --format=custom \
  --compress=9 \
  --file="estilofit_backup_$(date +%Y%m%d_%H%M%S).dump"
```

> A senha será solicitada, ou pode ser passada via variável de ambiente `PGPASSWORD=<senha>`.

### Onde encontrar as credenciais
As credenciais do banco de produção estão no painel do Railway:
`Projeto → estilofit-api → Variables → DB_HOST, DB_PORT, DB_USERNAME, DB_PASSWORD, DB_NAME`

---

## Frequência Recomendada

| Situação                          | Frequência         |
|-----------------------------------|--------------------|
| Operação normal                   | Semanal (domingo)  |
| Antes de qualquer deploy grande   | Imediato           |
| Antes de ajustes manuais no banco | Imediato           |
| Final de mês (fechamento)         | Sempre             |

---

## Armazenamento dos Backups

| Local              | Como                          | Retenção sugerida |
|--------------------|-------------------------------|-------------------|
| Google Drive       | Pasta `EstiloFit/Backups/DB`  | Últimos 4 backups |
| Computador local   | Pasta de backup pessoal       | Últimos 2 backups |

> Nunca manter apenas uma cópia. A regra mínima é: **2 cópias em locais diferentes**.

---

## Procedimento de Restore

Em caso de necessidade de restaurar o banco:

```bash
# 1. Conectar ao banco de destino (pode ser um banco novo ou o existente zerado)
# 2. Executar o restore
pg_restore \
  --host=<DB_HOST> \
  --port=<DB_PORT> \
  --username=<DB_USERNAME> \
  --dbname=<DB_NAME> \
  --clean \
  --if-exists \
  estilofit_backup_20260901_120000.dump
```

> `--clean` apaga os objetos existentes antes de restaurar.
> `--if-exists` evita erros se o objeto não existir ainda.

### Testando o restore (recomendado periodicamente)
```bash
# Criar banco temporário para testar o restore sem afetar produção
createdb estilofit_restore_test
pg_restore --dbname=estilofit_restore_test estilofit_backup_20260901_120000.dump
# Verificar dados
psql --dbname=estilofit_restore_test --command="SELECT COUNT(*) FROM sales;"
# Apagar banco de teste
dropdb estilofit_restore_test
```

---

## Nomeclatura dos Arquivos

```
estilofit_backup_YYYYMMDD_HHMMSS.dump

Exemplos:
  estilofit_backup_20260901_120000.dump  ← backup de 01/09/2026 às 12:00
  estilofit_backup_20261231_235900.dump  ← backup de fechamento de ano
```

---

## Checklist de Backup Mensal

Ao final de cada mês, o Administrador deve:

- [ ] Gerar o dump do banco de produção
- [ ] Verificar o tamanho do arquivo (se for 0 bytes, algo deu errado)
- [ ] Fazer upload para o Google Drive na pasta correta
- [ ] Manter apenas os 4 backups mais recentes no Drive (excluir os mais antigos)
- [ ] Registrar a data do último backup bem-sucedido

---

## Limitações e Riscos

| Risco                              | Mitigação                                          |
|------------------------------------|----------------------------------------------------|
| Esquecimento do backup semanal     | Criar lembrete recorrente no celular (domingo)     |
| Arquivo corrompido                 | Testar restore periodicamente (a cada 2 meses)     |
| Google Drive fora do ar            | Manter cópia local simultaneamente                 |
| Perda de dados entre backups       | Aceito — backups manuais têm essa limitação        |

---

## Evolução Futura

Quando migrar para AWS (ver ADR-012), o backup pode ser automatizado com:
- **RDS Automated Backups** — snapshots diários automáticos com retenção configurável
- **AWS Backup** — backup centralizado com políticas de retenção
- Custo adicional pequeno (~R$ 10–30/mês dependendo do tamanho)

---

## Justificativa
- Backup manual é adequado para o porte atual do negócio
- `pg_dump` com formato `custom` gera arquivos comprimidos e permite restore seletivo de tabelas
- Google Drive é gratuito, acessível e familiar para a proprietária
- O custo zero justifica a solução manual enquanto o negócio está em fase inicial

## Consequências
- Perda máxima de dados (RPO) = tempo desde o último backup (até 7 dias em operação normal)
- A disciplina de executar o backup é responsabilidade humana — não há automação
- O Administrador deve treinar o Gestor para executar o backup em caso de ausência
