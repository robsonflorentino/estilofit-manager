# CI/CD — GitHub Actions

Dois workflows automatizam testes e publicação de imagens.

## CI — `.github/workflows/ci.yml`
Dispara em **push e pull request** para `develop` e `main`.
- **Backend**: JDK 21, roda `./gradlew test` (os testes de integração usam Testcontainers, que funciona no runner Ubuntu porque ele já tem Docker).
- **Frontend**: Node 20, `npm ci` e `npm run build` (o build inclui o typecheck do TypeScript).

Serve como rede de segurança: nada quebrado entra na `develop`/`main` sem o CI acusar.

## Release — `.github/workflows/release.yml`
Dispara quando uma **tag `v*`** é empurrada (ex.: `v1.1.0`).
1. Roda os testes do backend (não publica se falhar).
2. Builda e publica duas imagens no **GitHub Container Registry (GHCR)**:
   - `ghcr.io/robsonflorentino/estilofit-manager-api`
   - `ghcr.io/robsonflorentino/estilofit-manager-web`
   Cada uma recebe as tags `:<versão>` (ex.: `:1.1.0`) e `:latest`.

O login no GHCR usa o `GITHUB_TOKEN` automático (permissão `packages: write`) — não precisa configurar segredo.

### Como publicar uma nova versão
```bash
# 1. atualize a versão (build.gradle.kts, package.json) e o CHANGELOG.md, faça o merge na main
# 2. crie e empurre a tag
git tag -a v1.1.0 -m "EstiloFit Manager v1.1.0"
git push origin v1.1.0
```
O workflow Release roda sozinho e publica as imagens. Veja-as em
**GitHub → repositório → Packages**.

> Na primeira publicação, os pacotes nascem privados. Para o VPS puxar sem autenticação,
> torne-os públicos em Packages → package → Settings → Change visibility (ou faça login no
> GHCR no VPS com um token de leitura).

## Próximo passo — Deploy contínuo (pós-VPS)
Quando o VPS estiver no ar, dá para automatizar o deploy via SSH no workflow de Release.
Será necessário configurar **secrets** no repositório:
- `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY` (chave privada)

O passo de deploy conectaria no VPS e rodaria `docker compose pull && docker compose up -d`
usando as imagens recém-publicadas. Deixamos documentado para adicionar quando o servidor existir.
