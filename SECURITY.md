# Política de Segurança - Burpee Log

Este documento descreve as diretrizes de segurança, o modelo de ameaças e o processo para reportar vulnerabilidades do aplicativo **Burpee Log**.

---

## 1. Modelo de Ameaças (Threat Model)

O **Burpee Log** é um aplicativo Android desenvolvido em Kotlin e Jetpack Compose, voltado para registro de treinos dos métodos *Six-Count* e *Navy Seal* do programa *Busy Dad Training*.

### Características Principais de Arquitetura:
- **100% Offline:** O aplicativo não requer nem solicita permissão de rede (`android.permission.INTERNET`). Não há servidores, endpoints remotos, telemetria de terceiros, Firebase nem comunicação com nuvem. Plugins e dependências do Google Services / Firebase foram totalmente removidos do projeto.
- **Armazenamento Estritamente Local:** Todos os dados de treinos são persistidos no banco de dados local Room (`burpee_log.db`), localizado no diretório interno seguro da aplicação (`MODE_PRIVATE`), isolado pela sandbox do Android (Linux UID separation) e criptografado por hardware via File-Based Encryption (FBE) nativo do Android.
- **Sem Autenticação Externa:** Não são mantidas credenciais, senhas, chaves de API nem tokens de sessão.
- **Exportação e Compartilhamento Controlados:** Arquivos CSV gerados para exportação são gravados exclusivamente no subdiretório isolado de cache `csv_exports/` e compartilhados via `FileProvider` restrito, com permissões temporárias de leitura (`FLAG_GRANT_READ_URI_PERMISSION`).
- **Defesa em Profundidade contra Injeção de Fórmulas:** Valores exportados em formato CSV passam por neutralização automática de caracteres que poderiam executar comandos em planilhas externas (Google Sheets, Microsoft Excel ou LibreOffice).

---

## 2. Postura e Controles de Segurança Implementados (OWASP MASVS / Mobile Top 10)

| Categoria OWASP | Controle Implementado no Burpee Log |
| :--- | :--- |
| **M1: Credenciais** | Ausência total de segredos, chaves ou tokens no repositório e código-fonte. |
| **M2: Cadeia de Suprimentos** | Dependências estritamente necessárias fixadas no Gradle. Bibliotecas de rede desnecessárias removidas/comentadas. |
| **M3: Autenticação & Componentes** | Componentes exportados minimizados (`MainActivity` como único ponto de entrada para o Launcher). Serviços e Provedores não-exportados (`android:exported="false"`). |
| **M4: Validação de Entrada & Saída** | Parser de CSV com limites estritos de tamanho (máx. 2 MB, 10.000 linhas, 1.000 caracteres por linha), validação de limites de repetições (0 a 5.000), validação de datas (ano 2000 até data atual) e neutralização de injeção de fórmulas CSV (`=`, `+`, `-`, `@`, `\t`, `\r`). Consultas Room 100% parametrizadas. |
| **M5: Comunicação de Rede** | Sem permissão de internet. `android:usesCleartextTraffic="false"` ativado no manifesto. |
| **M6: Privacidade** | Princípio do menor privilégio em permissões. Nenhum dado sensível em logs ou notificações. Notificação de treino não persistente após o encerramento. |
| **M7: Proteção do Binário** | R8/ProGuard com minificação (`isMinifyEnabled = true`) e remoção de recursos não usados (`isShrinkResources = true`) ativados no build release. |
| **M8: Configuração de Segurança** | Regras explícitas de extração de dados e backup (`data_extraction_rules.xml` e `backup_rules.xml`) protegendo arquivos de cache e permitindo apenas a base de dados oficial. |
| **M9: Armazenamento Seguro** | Base Room interna em sandbox privada. FileProvider restrito ao diretório `csv_exports/`. |
| **M10: Criptografia** | Criptografia em repouso garantida pelo SO Android (File-Based Encryption). Sem criptografia de software fraca ou customizada. |

---

## 3. Como Reportar Vulnerabilidades

Agradecemos a colaboração da comunidade de segurança para manter o Burpee Log seguro.

Se você identificou uma vulnerabilidade de segurança:
1. **Mensagem Privada / Security Advisory:** Preferencialmente, abra uma solicitação através do recurso de **Security Advisory privado do repositório no GitHub**.
2. **Issues do GitHub:** Caso o repositório não disponha de advisories privados, abra uma Issue rotulada como `[Segurança]` detalhando os passos para reprodução, versão do aplicativo e impacto identificado.
3. **Prazo de Resposta:** Nossa meta é avaliar e responder a notificações de vulnerabilidade em até 48 horas úteis.

Por favor, não divulgue publicamente detalhes técnicos de uma vulnerabilidade antes que uma correção esteja disponível.
