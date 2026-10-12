# Changelog

Todas as alterações notáveis neste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e este projeto adere ao [Versionamento Semântico](https://semver.org/lang/pt-BR/).

## [1.0.0] - 2026-10-12

### Lançamento Inicial

Primeira versão oficial do **Burpee Log**, aplicativo Android offline e focado em privacidade para praticantes do Busy Dad Program.

#### Funcionalidades
- **Cronômetro de 20 minutos com Intervalos Dinâmicos**:
  - Sessões estruturadas de 20 minutos com contagem regressiva e indicação de repetição.
  - Padrões flexíveis de cadência (ex.: `[10]` para uma repetição a cada 10s, ou `[30, 20]` para alternância minuto a minuto).
  - Cálculo automático das repetições previstas com base na cadência configurada.
  - Alertas sonoros (bips em tons distintos) e retorno tátil via vibração a cada repetição.
  - Suporte à execução em segundo plano com a tela desligada através de Foreground Service com notificação persistente.
- **Resumo e Registro da Sessão**:
  - Ajuste rápido das repetições efetivamente realizadas ao final do treino.
  - Campo de anotações opcionais para registrar percepção de esforço, ritmo ou comentários.
- **Painel de Progresso & Estatísticas**:
  - Gráfico de mapa de calor estilo GitHub cobrindo os últimos 12 meses, com coloração proporcional ao recorde pessoal do dia.
  - Filtro do mapa de calor por Six-Count, Navy Seal ou ambos combinados.
  - Acompanhamento semanal de minutos (meta de 80 minutos / 4 sessões por semana).
  - Contador de sequência de semanas consecutivas completadas (streak).
  - Total geral de repetições e recorde pessoal (PR) para Six-Count e Navy Seal.
- **Histórico e Gestão de Treinos**:
  - Lista completa de sessões com filtragem por exercício.
  - Gráfico de evolução do desempenho ao longo do tempo.
  - Edição de repetições e observações de treinos anteriores, além de exclusão de registros.
- **Importação e Exportação de Dados**:
  - Importação de treinos via arquivo CSV (formatos de data `dd/MM/yyyy` e `yyyy-MM-dd`).
  - Pré-visualização com detecção e descarte automático de duplicidades antes de salvar.
  - Exportação de backup completo em CSV com proteção contra injeção de fórmulas de planilhas.
- **Segurança e Privacidade**:
  - 100% offline: sem permissão de acesso à internet (`INTERNET`), sem bibliotecas de rastreamento ou telemetria.
  - Sem necessidade de cadastro, autenticação ou login em nuvem.
  - Banco de dados local seguro com Room (SQLite) no armazenamento interno do app.
  - Validação estrita de limites numéricos e sanitização de dados no parser CSV.
  - Build de release protegido com minificação, ofuscação (R8/Proguard) e encolhimento de recursos.
