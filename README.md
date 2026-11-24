# SB Finance 3.0 — Android (Kotlin + Jetpack Compose)

Recriação do app **SB Finance** em português do Brasil, com identidade visual roxo/índigo inspirada no layout de e-wallet enviado pelo usuário. Mantém o pacote `com.sbfinance.app` e `minSdk 26`.

## Funcionalidades entregues no código-fonte

- **Início:** saldo real (movimentações registradas, sem futuras), saldo projetado depois das próximas contas, receitas/despesas mensais, gráfico e histórico recente.
- **Lançamentos:** entradas e saídas, categorias, datas, pesquisa, filtros, edição e exclusão.
- **Texto natural:** `Gastei 5 reais com pastel`, `Recebi 2500 de salário`; o interpretador local propõe valor, título e categoria, **sempre pede confirmação** antes de registrar.
- **Despesas fixas/parceladas:** recorrência mensal, total e progresso das parcelas, vencimentos e atrasos, `Marcar paga` lança exatamente uma despesa vinculada ao período e avança o vencimento. Histórico de pagamentos não desaparece quando a conta é removida.
- **Notificações locais:** tentativas de aviso 3 dias antes, 1 dia antes, na data do vencimento e um dia depois. O Android pode adiar esses alarmes por economia de bateria, e exige autorização no Android 13+.
- **Gráficos:** evolução das despesas, receitas/despesas do mês e comparação por categoria.
- **Assistente:** regras locais de análise, diagnóstico de gastos, recomendações baseadas em limites e vencimentos, sem mandar informações pessoais à internet. **Não contém IA generativa em nuvem**.
- **Metas e orçamentos:** cadastro de objetivos, valor reservado (indicador independente do saldo), tetos de despesa por categoria e aviso ao excedê-los.
- **Privacidade:** dados persistidos localmente em `SharedPreferences` JSON, backup e restauração por arquivo JSON, opção de apagar dados. Sem conta, anúncios ou conexão bancária.

## Como abrir no Windows e gerar o APK

1. Extraia todo este ZIP para, por exemplo, `I:\SBFinance_3_AndroidStudio` (não extraia dentro do projeto antigo).
2. Instale o Android Studio, Android SDK 36 e JDK 17 ou mais recente. Você já pode usar o JDK 21.
3. Execute **`PREPARAR_GRADLE.bat`** uma vez. Ele baixa `gradle-wrapper.jar` do site oficial do Gradle e valida SHA-256. Requer internet nesta etapa.
4. Abra a pasta `SBFinance_3_AndroidStudio` no Android Studio e aguarde a sincronização Gradle.
5. Use **Run** no Android Studio, com um Android conectado por USB e depuração USB autorizada.
6. Para gerar um APK de testes, execute **`GERAR_APK.bat`** na pasta raiz. O arquivo aparece em `app\build\outputs\apk\debug\app-debug.apk`.

> **Estado de validação:** o código Android foi produzido nesta sessão, mas **não foi compilado nem instalado** aqui, pois este ambiente não contém Android SDK/Gradle. Portanto, não há APK pré-compilado; a primeira sincronização/build precisa acontecer no seu PC. O preview web foi verificado por testes automatizados de lógica, mas a inspeção em browser ficou bloqueada por política do ambiente.

## Prévia web

Na pasta ao lado do ZIP há `SBFinance_3_Preview.html` — uma prévia interativa autônoma no navegador. Ela usa `localStorage`, contém **dados fictícios explicitamente identificados** para mostrar o visual, e permite apagar a demonstração com **Começar com meus dados**. A versão Android nativa começa sem registros.

## Observações importantes

- Nenhuma conta bancária é sincronizada, portanto não há importação automática via Open Finance.
- O backup JSON contém dados financeiros em **texto legível**. Guarde-o em local seguro; o app não o criptografa.
- Os lembretes podem atrasar conforme as políticas de bateria do Android; para avisos exatos, será necessário um desenho adicional sujeito às permissões de alarmes do sistema.
- Ao excluir o **último** pagamento vinculado a uma conta, a parcela é reaberta e o histórico é corrigido; pagamentos anteriores precisam ser desfeitos do mais recente para o mais antigo.
- Saldo projetado soma a **próxima cobrança de cada conta ativa**, não todas as mensalidades futuras de um contrato.
- Para usar IA generativa real será necessário integrar um backend seguro; não coloque chaves de API dentro do APK.

## Paleta e UI

- Roxo principal: `#7115FA`
- Roxo claro: `#8D42FF`
- Índigo profundo: `#23034C`
- Fundo: `#F7F6FB`
- Texto: `#190E28`
- Tipografia do sistema/Material 3 e cartões arredondados, com barra inferior identificada.
