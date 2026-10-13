# Burpee Log

Aplicativo Android **offline** para registrar treinos do **Busy Dad Program**: sessões de 20 minutos alternando **Six-Count** e **Navy Seal burpees**, com cronômetro personalizado, histórico e um painel de progresso inspirado no gráfico de contribuições do GitHub.

> **Este projeto foi feito completamente com Inteligência Artificial.**
> O planejamento, a definição das funcionalidades e os prompts foram criados com a ajuda do **Claude** (Anthropic), e todo o código foi gerado pelo **Google AI Studio**. Nenhuma linha foi escrita manualmente. A pessoa responsável pelo projeto definiu as ideias, revisou os resultados e testou o app.

## Sobre o projeto

O [Busy Dad Program](https://www.youtube.com/@busydadtraining) é um protocolo simples de treino: **80 minutos por semana**, divididos em **4 sessões de 20 minutos**, alternando dois exercícios, o burpee Six-Count e o burpee Navy Seal. O Burpee Log foi criado para quem segue essa rotina e quer registrar cada sessão, controlar o intervalo entre as repetições e acompanhar a evolução ao longo do tempo.

Este é um projeto independente e **não é afiliado ao canal nem ao programa Busy Dad Training**.

## Funcionalidades

- **Cronômetro personalizado de 20 minutos**, com padrão de intervalos entre as repetições:
  - Exemplo `[10]`: 1 repetição a cada 10 segundos.
  - Exemplo `[30, 20]`: no primeiro minuto, 1 repetição a cada 30 s; no segundo, a cada 20 s; e o ciclo se repete.
  - Cálculo automático do total de repetições previstas.
- **Bipe e vibração** a cada repetição, funcionando mesmo com a tela apagada.
- **Resumo da sessão**: ajuste das repetições realmente feitas e observação opcional.
- **Painel estilo GitHub**: mapa de calor dos últimos 12 meses em que a cor representa a quantidade de repetições do dia (Six-Count, Navy Seal ou ambos, em relação ao seu recorde).
- **Resumo semanal**: minutos da semana (meta de 80), sequência de semanas completas, total de repetições e recordes.
- **Histórico** com filtros, edição, exclusão e gráfico de evolução.
- **Importação de treinos antigos** por arquivo CSV (por exemplo, exportado do Google Sheets) com as colunas `data`, `six-counts` e `navyseals`.
- **Exportação para CSV** para backup.
- **100% offline e privado**: sem login, sem internet e sem coleta de dados. Tudo fica salvo no seu aparelho.

## Importando seus treinos

Prepare um arquivo CSV com cabeçalho e uma linha por dia:

```csv
data,six-counts,navyseals
01/09/2026,150,
03/09/2026,,60
```

- Datas aceitas: `dd/MM/yyyy` e `yyyy-MM-dd`.
- Células vazias ou com zero significam que aquele exercício não foi feito no dia.
- Antes de gravar, o app mostra uma prévia e ignora registros duplicados (mesma data e mesmo tipo).

## Como instalar e testar

O projeto é **open source**. Qualquer pessoa pode instalar e testar.

**Opção 1: instalar o APK (recomendado)**
1. Acesse a página de [Releases](https://github.com/rcmnzs/burpee-log/releases) e baixe o arquivo `.apk` da versão mais recente (ex.: `BurpeeLog-v1.0.0.apk`).
2. No Android, autorize a instalação de apps de fontes desconhecidas caso solicitado pelo navegador ou gerenciador de arquivos.
3. Abra o APK baixado e confirme a instalação.
4. **Como atualizar:** Para atualizar para uma nova versão, baixe o APK mais recente na página de Releases e instale diretamente por cima do aplicativo atual. Se ambas as versões forem assinadas com a mesma chave (o que acontece automaticamente nas releases oficiais), o sistema Android atualizará o app mantendo todo o seu histórico de treinos e configurações intactos.

**Opção 2: compilar o código**
1. Instale o [Android Studio](https://developer.android.com/studio).
2. Clone o repositório:
```bash
   git clone https://github.com/rcmnzs/burpee-log.git
```
3. Abra a pasta no Android Studio, aguarde a sincronização do Gradle e execute o app em um emulador ou aparelho.

## Tecnologias

- Kotlin
- Jetpack Compose (Material 3)
- Room (banco de dados local)
- Foreground Service para o cronômetro com tela apagada

## Segurança

O código passou por uma auditoria baseada no **OWASP Mobile Top 10** realizada por IA, e as correções foram aplicadas. Como todo o código é gerado por IA, recomendamos revisão antes de qualquer uso crítico. Veja o arquivo [SECURITY.md](SECURITY.md) para detalhes e para reportar vulnerabilidades.

## Avisos

- O app é fornecido **"como está"**, sem garantias.
- Este aplicativo **não oferece orientação médica**. Burpees são exercícios intensos; consulte um profissional de saúde antes de iniciar qualquer programa de treino.

## Contribuindo

Sugestões, relatos de problemas e melhorias são bem-vindos. Abra uma **Issue** ou envie um **Pull Request**.

## Licença

Distribuído sob a licença **MIT**. Veja o arquivo [LICENSE](LICENSE).
