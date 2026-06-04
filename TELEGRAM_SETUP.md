# Configuração do Telegram

Este guia explica como criar um bot do Telegram e obter as credenciais que o honeypot precisa pra enviar foto + localização quando o PIN errado é digitado.

## 1. Criar o bot via BotFather

1. Abra o Telegram (no celular ou no Desktop)
2. Busque pelo usuário `@BotFather` e abra a conversa
3. Envie `/newbot`
4. O BotFather vai pedir um **nome** (qualquer coisa, ex: "Meu Honeypot")
5. Depois ele pede um **username** que precisa terminar em `_bot` (ex: `meu_honeypot_bot`)
6. Ele responde com uma mensagem contendo o **token**, algo como:
   ```
   1234567890:ABCdefGHIjklMNOpqrSTUvwxYZ123456789
   ```
7. **Salve esse token** — é a primeira credencial que vamos usar

## 2. Obter seu Chat ID

O Telegram identifica conversas por número. Você precisa do seu chat ID pessoal pra que o bot saiba pra quem mandar o alerta.

1. Busque pelo usuário `@userinfobot` e abra a conversa
2. Envie `/start`
3. Ele responde com seus dados, incluindo `Id: 123456789`
4. **Salve esse número** — é a segunda credencial

## 3. Iniciar conversa com seu bot (passo obrigatório)

O Telegram bloqueia bots de iniciarem conversa. Você precisa enviar a primeira mensagem.

1. Abra o link do seu bot (BotFather te deu) ou busque pelo username `@meu_honeypot_bot`
2. Aperte **Iniciar** ou envie qualquer mensagem (ex: `oi`)
3. Pronto — o bot agora pode te mandar mensagens

## 4. Configurar no app

1. Abrir o Honeypot
2. Tela de **Setup** (configuração inicial)
3. Preencher:
   - **PIN** de 4 dígitos (que você vai usar pra abrir o app sem disparar o alerta)
   - **Telegram Bot Token** (do passo 1)
   - **Telegram Chat ID** (do passo 2)
4. Salvar

## 5. Testar

1. Feche e abra o app de novo
2. Digite uma senha **diferente** do PIN configurado
3. O app vai:
   - Tirar foto silenciosa com a câmera frontal
   - Capturar a localização GPS
   - Enviar pro seu Telegram com link do Google Maps
   - Travar a tela do celular

**Em até 10 segundos** a mensagem deve chegar no seu Telegram.

## Resiliência (entrega garantida)

Se o celular estiver sem internet quando o alerta dispara, o app **salva o alerta em disco** e usa o WorkManager pra tentar reenviar automaticamente assim que a rede voltar. Isso significa:

- Modo avião → o alerta fica na fila
- Sem 4G/Wi-Fi → idem
- App matado/celular reiniciado → o WorkManager continua tentando
- Quando a rede volta, o alerta é enviado em background

## Troubleshooting

**Mensagem não chega?**
- Confira no `adb logcat -s "Honeypot:*"` os logs de envio
- Verifique se você iniciou a conversa com o bot (passo 3) — sem isso, o Telegram bloqueia mensagens
- Confira se o token e chat ID estão corretos na tela de Setup

**Como recuperar o token se perdi?**
- Volte no `@BotFather` → `/mybots` → escolha seu bot → `API Token`

**Como deletar o bot?**
- `@BotFather` → `/deletebot` → escolha o bot
