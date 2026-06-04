# Honeypot Anti-Assalto — Documento Técnico do Projeto

> Aplicativo Android pessoal de defesa contra roubo de celular desbloqueado em corridas de rua / vida urbana em São Paulo. Funciona como armadilha (honeypot) disfarçada de app de banco.

---

## 1. Contexto e problema

### 1.1. O problema real

São Paulo tem alto índice de roubo de celular durante atividades de rua (corridas, deslocamentos a pé, transporte público). O cenário de ataque mais comum e mais danoso é:

1. Vítima está com celular **já desbloqueado** (usando música, GPS de corrida, mensagens)
2. Ladrão arranca o aparelho da mão
3. Tem **janela de 30 segundos a alguns minutos** antes do auto-lock da tela
4. Durante essa janela, abre apps de banco, faz PIX/transferências, esgota limites
5. Prejuízo financeiro pode chegar a dezenas de milhares de reais antes que a vítima consiga bloquear

### 1.2. Por que defesas convencionais falham

- **Limite de PIX baixo**: ladrões experientes alteram limite via SMS/biometria se conseguem
- **Modo seguro do banco**: cada banco tem o seu, fragmentado, exige biometria que pode ser exigida sob coação
- **Disfarce de ícone passivo**: ladrão sabe que tem app de banco no celular e exige acesso
- **Lock automático**: tempo padrão é longo demais (30s-5min) pra ser útil

### 1.3. A insight central

O ladrão tem **objetivo previsível** (achar e abrir o app do banco) e **age sob pressão de tempo**. Isso é o cenário ideal pra um **honeypot reativo**: um app que parece o banco, mas qualquer interação dispara contramedidas.

---

## 2. Solução proposta

### 2.1. Conceito

Aplicativo Android que se apresenta visualmente como um app de banco. Ao ser aberto:

- Mostra splash + tela de login convincente
- **Senha correta (só o dono sabe)** → fecha silenciosamente, sem efeitos
- **Qualquer outra entrada** → dispara protocolo de emergência:
  - Trava o celular via Device Admin API
  - Captura localização GPS atual
  - Tira foto silenciosa com câmera frontal
  - Envia tudo via Telegram Bot pro contato de emergência
  - (Opcional) Toca alarme em volume máximo

### 2.2. Princípios de design

| Princípio                           | Implicação                                                        |
| ----------------------------------- | ----------------------------------------------------------------- |
| **Senha curta (4 dígitos)**         | Dono lembra sob estresse; ladrão não chuta certo em 2 tentativas  |
| **Splash realista**                 | Vende a ilusão nos primeiros 2 segundos críticos                  |
| **Erro "amigável" na 1ª tentativa** | Convida ladrão a tentar de novo → ganha tempo pra GPS/foto/upload |
| **Lock no 3º erro**                 | Garante travamento mesmo se ladrão desistir                       |
| **Geofence "estou em casa"**        | App real funciona sem fricção quando em segurança                 |
| **Trigger secundário**              | Botão de volume 5x ou shake pattern dispara protocolo manualmente |

### 2.3. Escopo

- **In-scope**: Android only, uso pessoal, distribuição via APK sideload, sem Play Store
- **Out-of-scope**: iOS, multi-usuário, comercialização, conformidade Play Store
- **Out-of-scope (projeto separado futuro)**: disfarce visual dos ícones de apps reais já instalados (Itaú real, Nubank real, etc). Essa funcionalidade exige construir um launcher próprio ou um icon pack companion pro Nova/Lawnchair, o que é um produto distinto com escopo, complexidade e ciclo de vida próprios. Decisão consciente: este projeto foca **apenas no honeypot reativo**. Quando este estiver maduro e funcionando, o disfarce de ícones vira um app pessoal separado.
- **Possível futuro neste mesmo app**: integração com Open Finance pra reduzir limites de bancos reais via API quando trigger é acionado

### 2.4. Como o disfarce funciona na prática (sem app de launcher)

Como o disfarce ativo dos ícones reais está fora do escopo, o sistema de defesa depende de configurações manuais do próprio celular:

- **Apps reais de banco**: escondidos via Samsung Secure Folder, Xiaomi Second Space, ou pasta oculta de launchers como Niagara
- **Atalhos com cara neutra**: criados manualmente pra acessar os apps reais com nome tipo "Calculadora", "Notas"
- **Honeypot deste projeto**: aparece na home com cara de banco, é o que o ladrão clica

Ou seja: o honeypot **substitui visualmente** o app do banco na home (o ladrão vê ele e clica), enquanto o app real fica acessível só pra você por caminhos alternativos. Não há necessidade técnica de modificar ícones de outros apps pra essa estratégia funcionar.

---

## 3. Stack tecnológica

### 3.1. Decisão: **Kotlin nativo Android**

Validação contra alternativas:

| Stack                | Por que NÃO                                                                                                                                                      |
| -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| React Native         | Device Admin API exige módulo nativo Kotlin de qualquer jeito → pior dos dois mundos. Sem cross-platform benefit (iOS fora). Pegadinhas de background execution. |
| Flutter              | Mesmos problemas do RN + comunidade menor pra APIs Android sensíveis.                                                                                            |
| Java                 | Funciona, mas boilerplate excessivo. Kotlin é o padrão Android moderno.                                                                                          |
| Kotlin Multiplatform | Overkill. Sem necessidade real de compartilhar código.                                                                                                           |

**Justificativa Kotlin nativo:**

- Acesso direto a `DevicePolicyManager`, `CameraX`, `FusedLocationProviderClient` sem bridge
- Setup mais rápido até primeiro APK no dispositivo
- Documentação Android oficial assume Kotlin
- Foreground Service + ciclo de vida controlado nativamente

### 3.2. Bibliotecas e ferramentas

- **Linguagem**: Kotlin 1.9+
- **Build**: Gradle (Kotlin DSL) + Android Studio
- **Min SDK**: API 26 (Android 8.0) — cobre 95%+ dos dispositivos
- **Target SDK**: API 34 (Android 14)
- **UI**: Jetpack Compose (mais simples que XML pra telas custom)
- **Câmera**: CameraX (`androidx.camera:camera-camera2`, `camera-lifecycle`, `camera-core`)
- **Localização**: Google Play Services Location (`com.google.android.gms:play-services-location`)
- **HTTP**: OkHttp + Retrofit (pro Telegram)
- **Concorrência**: Kotlin Coroutines
- **Persistência (config)**: DataStore Preferences
- **Geofence**: Geofencing API do Google Play Services

### 3.3. Backend

**Telegram Bot** via Bot API. Sem servidor próprio.

- Custo: zero
- Setup: 5 minutos via `@BotFather`
- Latência: 1-3s
- Confiabilidade: alta
- Bonus: histórico de alertas fica no próprio chat do Telegram

---

## 4. Arquitetura

### 4.1. Visão de alto nível

```
┌─────────────────────────────────────────────────────────┐
│                     APK INSTALADO                       │
│                                                         │
│  ┌──────────────────┐      ┌──────────────────────┐   │
│  │   SetupActivity  │      │    MainActivity      │   │
│  │  (1ª execução)   │      │  (tela login fake)   │   │
│  │  - Permissões    │      │  - Splash banco      │   │
│  │  - Device Admin  │      │  - Input senha       │   │
│  │  - Config senha  │      │  - Validação         │   │
│  │  - Token TG bot  │      └─────────┬────────────┘   │
│  └──────────────────┘                │                 │
│                                       │ senha errada    │
│                                       ▼                 │
│                          ┌────────────────────────┐    │
│                          │  EmergencyService      │    │
│                          │  (Foreground Service)  │    │
│                          └──┬──────┬──────┬──────┘    │
│                             │      │      │            │
│                  ┌──────────┘      │      └─────────┐  │
│                  ▼                 ▼                ▼  │
│         ┌────────────────┐ ┌──────────────┐ ┌────────┐│
│         │ LocationHelper │ │ CameraHelper │ │Notifier││
│         │  (GPS atual)   │ │ (foto silen.)│ │  (TG)  ││
│         └────────┬───────┘ └──────┬───────┘ └───┬────┘│
│                  │                │             │     │
│                  └────────┬───────┴─────────────┘     │
│                           ▼                            │
│              ┌────────────────────────────┐           │
│              │   DevicePolicyManager       │           │
│              │   .lockNow()                │           │
│              └────────────────────────────┘           │
└─────────────────────────────────────────────────────────┘
                              │
                              ▼ HTTPS
                  ┌─────────────────────────┐
                  │   Telegram Bot API      │
                  │   sendPhoto + caption   │
                  └───────────┬─────────────┘
                              ▼
                  ┌─────────────────────────┐
                  │  Seu chat pessoal/grupo │
                  │  (você + família)       │
                  └─────────────────────────┘
```

### 4.2. Componentes

#### `SetupActivity`

Executada apenas no primeiro uso. Responsável por:

- Solicitar permissões (câmera, localização precisa + background, foreground service, post notifications)
- Direcionar usuário pra ativar Device Admin via `ACTION_ADD_DEVICE_ADMIN`
- Configurar PIN de 4 dígitos (armazenado hashed via SHA-256 no DataStore)
- Configurar token do Telegram Bot e chat ID
- Configurar geofence "casa" (lat/long + raio)
- Configurar imagem/nome do "banco" (Itaú-like, Nubank-like, ou genérico)

#### `MainActivity`

Activity principal. Mostrada quando o ícone do app é clicado.

- `onCreate` → mostra splash do "banco" (1.5s)
- Após splash → tela de PIN com input numérico
- Botão "Entrar":
  - Hash do input == hash salvo → `finishAffinity()` (sai silenciosamente)
  - Hash diferente → conta tentativa, mostra Toast "Senha incorreta", dispara `EmergencyService` (1ª tentativa) ou trava direto (3ª tentativa)

#### `EmergencyService`

Foreground Service que executa o protocolo de emergência. Foreground porque Android mata serviços background em segundos; foreground sobrevive minutos.

- Notificação silenciosa (canal `IMPORTANCE_MIN`) pra cumprir requisito de FG service sem alertar o ladrão
- Coroutine que dispara em paralelo:
  - GPS via `FusedLocationProviderClient.getCurrentLocation()`
  - Foto frontal via CameraX (sem preview, ImageCapture direto)
- Quando ambos prontos (ou timeout de 5s) → envia pro Telegram
- Após envio (ou após 3s, o que vier primeiro) → chama `DevicePolicyManager.lockNow()`

#### `MyDeviceAdminReceiver`

BroadcastReceiver herdando de `DeviceAdminReceiver`. Necessário pra registrar o app como administrador do dispositivo. Sem isso, `lockNow()` não funciona.

#### `LocationHelper`

Wrapper sobre `FusedLocationProviderClient`. Retorna `Location` ou null em timeout de 3s.

#### `CameraHelper`

Wrapper sobre CameraX. Inicializa câmera frontal sem preview, tira 1 foto, retorna `ByteArray` JPEG. Timeout 3s.

#### `TelegramNotifier`

Cliente OkHttp/Retrofit pro endpoint `https://api.telegram.org/bot<TOKEN>/sendPhoto`. Multipart com foto + caption contendo timestamp + link Google Maps.

#### `GeofenceReceiver` (opcional, fase 2)

BroadcastReceiver pra eventos de geofence. Quando usuário entra na geofence "casa", seta flag `safe_mode=true` no DataStore. Quando sai, `safe_mode=false`. Em `safe_mode=true`, abrir o app pula a tela fake e abre direto o app real do banco via intent.

### 4.3. Fluxo de dados

**Fluxo feliz (você mesmo testando):**

```
Tap no ícone → MainActivity → splash → PIN correto → finishAffinity()
```

**Fluxo de assalto:**

```
Tap no ícone (pelo ladrão)
  → MainActivity → splash → PIN errado
  → EmergencyService inicia (foreground)
     ├→ LocationHelper.getCurrentLocation()  [paralelo]
     ├→ CameraHelper.captureSilent()          [paralelo]
     └→ aguarda ambos (max 5s)
  → TelegramNotifier.send(location, photo)
  → DevicePolicyManager.lockNow()
  → tela trava, ladrão precisa do PIN do Android pra continuar
```

**Fluxo "estou em casa" (fase 2):**

```
GeofenceReceiver detecta entrada em casa → DataStore safe_mode=true
Tap no ícone → MainActivity checa safe_mode → abre intent do banco real
```

### 4.4. Permissões necessárias (AndroidManifest)

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<!-- Device Admin é declarado via <receiver>, não <uses-permission> -->
```

### 4.5. Segurança dos dados sensíveis

- **PIN**: nunca armazenado em texto plano. SHA-256 + salt aleatório no DataStore.
- **Token Telegram**: armazenado no DataStore (criptografia at-rest do Android cobre).
  - Em fase 2, mover pro Android Keystore via EncryptedSharedPreferences.
- **Foto e localização**: enviadas e descartadas localmente. Sem persistência.
- **APK**: não comitar token Telegram no Git. Usar `local.properties` + BuildConfig.

---

## 5. Roteiro de implementação

### Fase 1 — MVP funcional (objetivo: 2 fins de semana)

**Sprint 1 (FDS 1) — Núcleo do honeypot**

- [ ] Setup Android Studio + projeto Kotlin novo
- [ ] AndroidManifest com permissões e receiver Device Admin
- [ ] `MyDeviceAdminReceiver` + `device_admin_policies.xml`
- [ ] `SetupActivity` simples: campo de PIN, botão "Ativar Device Admin", campo de token Telegram + chat ID
- [ ] Persistência via DataStore (PIN hashed, token, chat ID)
- [ ] `MainActivity` com splash + tela de PIN + lógica de validação
- [ ] `EmergencyService` mínimo: só dispara `lockNow()` após 3s
- [ ] Teste: instalar no celular, configurar, simular "assalto" com amigo

**Sprint 2 (FDS 2) — Coleta de evidência**

- [ ] `LocationHelper` com FusedLocationProviderClient
- [ ] `CameraHelper` com CameraX silencioso (front camera)
- [ ] `TelegramNotifier` com OkHttp + multipart upload
- [ ] Integração no `EmergencyService`: coroutine paralela, timeout, envio, lock
- [ ] Polimento visual da tela fake (logo, cores, splash)
- [ ] Teste end-to-end: simulação completa com recebimento no Telegram

### Fase 2 — Robustez e usabilidade (objetivo: 1-2 fins de semana adicionais)

- [ ] Geofence "casa" → modo seguro automático
- [ ] Trigger secundário: 5x botão de volume → dispara emergência manualmente
- [ ] Múltiplas "personas" de banco (Itaú-like, Nubank-like, Bradesco-like) selecionáveis
- [ ] Histórico local de triggers (pra debugar falsos positivos)
- [ ] EncryptedSharedPreferences pro token
- [ ] Notification channel com `IMPORTANCE_MIN` pro FG service ser invisível
- [ ] Modo dry-run: dispara tudo MENOS o `lockNow()`, pra testar sem se trancar fora

### Fase 3 — Avançado (opcional)

- [ ] Integração Open Finance: ao disparar, chama API pra reduzir limite PIX a R$ 0
- [ ] Alarme sonoro em volume máximo (opcional, configurable)
- [ ] Detector de corrida via ActivityRecognition API → ativa modo "alerta" automático

---

## 6. Pontos de atenção e armadilhas

### 6.1. Não se tranque fora do próprio celular

- **Sempre** ter o PIN do Android (sistema operacional) anotado em local seguro
- Implementar **modo dry-run** desde o início pra testar sem ativar `lockNow()` real
- PIN do app **diferente** do PIN do desbloqueio do Android

### 6.2. Background execution é hostil no Android moderno

- Foreground Service é mandatório (Service comum morre em segundos)
- Localização background exige permissão explícita "Permitir o tempo todo"
- Android 14 (API 34) exige tipo de FG service declarado no manifest (`foregroundServiceType="camera|location"`)
- Em Xiaomi/Huawei/OnePlus, otimização agressiva de bateria pode matar até FG service — adicionar app à whitelist manualmente

### 6.3. Câmera silenciosa tem indicador visual

- Android 12+ mostra ponto verde no canto superior direito quando câmera é usada
- Não há como contornar legitimamente
- Mitigação: foto é rápida (< 1s), ladrão sob pressão dificilmente nota

### 6.4. Esconder o app real do banco

- Samsung Secure Folder (Samsung) ou Second Space (Xiaomi/POCO)
- Niagara Launcher tem feature de "hidden apps"
- Acessar app real via atalho com nome neutro ("Calculadora", "Notas")
- Documentar essa parte no README do projeto, fora do escopo do app

### 6.5. Marca registrada

- Uso pessoal de logos do Itaú/Nubank/Bradesco em APK não distribuído: cinza legal mas tolerado
- **Não compartilhar APK** com terceiros — aí vira distribuição não autorizada
- Alternativa segura: criar identidade visual genérica ("Banco Central S.A.", "Caixa Digital")

### 6.6. Senha sob coação

- Se ladrão exige "abre o banco e me mostra o saldo", você pode digitar o PIN correto pra escapar do trigger
- Mas ainda assim o app real está escondido → ladrão vê tela em branco ou erro
- Considerar **PIN reverso**: PIN correto fechado o app, MAS PIN "reverso" (ex: 4 dígitos invertidos) dispara trigger silencioso E mostra tela falsa de "saldo R$ 12,43" pra enganar

---

## 7. Critérios de sucesso

MVP é considerado pronto quando:

1. ✅ Você instala o APK no seu celular sem erros
2. ✅ Configura PIN, Device Admin, e token Telegram em uma tela de setup
3. ✅ Abre o app, vê splash convincente de banco, digita PIN correto, app fecha
4. ✅ Abre o app, digita PIN errado:
   - Recebe foto sua + localização no Telegram em ≤ 5 segundos
   - Tela do celular trava em ≤ 5 segundos
5. ✅ Funciona offline parcialmente: GPS funciona, foto funciona, lock funciona; só o envio Telegram falha (e nesse caso o envio é tentado de novo quando volta a conectar)
6. ✅ Você sobrevive a uma simulação de assalto com um amigo: ele pega seu celular desbloqueado, abre o "app do banco", o ladrão "fictício" é travado e você recebe alerta

---

## 8. Setup de ambiente — VS Code + Claude Code + Gradle CLI

Fluxo de desenvolvimento: **VS Code como editor + Claude Code como copilot + Gradle CLI pro build + Android Studio instalado em paralelo** (usado só pra emulador AVD, debugger visual e Layout Inspector quando precisar). O projeto Gradle abre nos dois IDEs sem conflito — fonte da verdade é o `build.gradle.kts`.

### 8.1. Instalações necessárias

1. **Android Studio Hedgehog** (ou mais recente) — instalar mas **não usar como editor principal**
   - Durante o setup, deixa marcado "Android SDK", "Android SDK Platform-Tools", "Android Virtual Device"
   - Isso instala `adb`, `emulator`, e o Android SDK que o Gradle vai usar

2. **Variáveis de ambiente** (Linux/macOS — adicionar no `~/.zshrc` ou `~/.bashrc`):

   ```bash
   export ANDROID_HOME="$HOME/Android/Sdk"  # ou ~/Library/Android/sdk no Mac
   export PATH="$PATH:$ANDROID_HOME/platform-tools"
   export PATH="$PATH:$ANDROID_HOME/emulator"
   export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin"
   ```

   No Windows, adicionar via "Editar variáveis de ambiente do sistema".

3. **JDK 17** (Android Gradle Plugin 8+ exige) — verificar com `java -version`
   - Se não tiver: `brew install openjdk@17` (Mac) ou pacote `openjdk-17-jdk` (Linux)

4. **Extensões do VS Code:**
   - `fwcd.kotlin` — Kotlin Language (syntax + autocomplete)
   - `vscjava.vscode-gradle` — Gradle for Java (interface visual pra tasks)
   - `mathiasfrohlich.Kotlin` — syntax highlight melhorado
   - `naco-siren.gradle-language` — syntax do `build.gradle.kts`
   - (opcional) `redhat.java` — Language Support pra Java, ajuda com algumas integrações

5. **Claude Code** — já instalado no fluxo do usuário. Roda no terminal do VS Code direto.

### 8.2. Criar o projeto

Como não vamos usar o wizard do Android Studio, criar a estrutura manualmente é cansativo. Caminho mais rápido:

**Opção A — Criar pelo Android Studio (recomendado pra primeira vez)**

1. Abre Android Studio
2. "New Project" → "Empty Activity" (com Compose)
3. Configurações:
   - Name: `Honeypot`
   - Package: `com.guilherme.honeypot` (ou o que preferir)
   - Language: Kotlin
   - Minimum SDK: API 26 (Android 8.0)
   - Build configuration: Kotlin DSL (`build.gradle.kts`)
4. Finish, espera o Gradle sync terminar
5. **Fecha o Android Studio**, abre o diretório no VS Code: `code .`

**Opção B — Via CLI usando template**

```bash
# Precisa do gradle CLI instalado: brew install gradle (Mac) ou sdkman
gradle init --type kotlin-application --dsl kotlin
# Depois editar manualmente pra virar projeto Android — mais trabalhoso
```

Recomendo a Opção A. Você só precisa abrir o Android Studio uma vez pra gerar o esqueleto, depois nunca mais (se quiser).

### 8.3. Configurar `build.gradle.kts` (Module)

Adicionar ao bloco `dependencies`:

```kotlin
dependencies {
    // CameraX
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")

    // Localização
    implementation("com.google.android.gms:play-services-location:21.1.0")

    // HTTP (Telegram)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Persistência
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Já vem do template Compose, conferir:
    // implementation("androidx.activity:activity-compose:...")
    // implementation(platform("androidx.compose:compose-bom:..."))
}
```

Depois rodar `./gradlew build` pra baixar tudo.

### 8.4. Telegram Bot

1. Conversa com `@BotFather` no Telegram → `/newbot` → escolher nome + username → guardar **token**
2. Conversa com `@userinfobot` → guardar seu **chat ID**
3. Testar:
   ```bash
   curl -X POST "https://api.telegram.org/bot<TOKEN>/sendMessage" \
        -d "chat_id=<CHAT_ID>" \
        -d "text=Teste do bot"
   ```
4. Guardar token e chat ID em `local.properties` (NÃO commitar):
   ```properties
   telegram.bot.token=123456:ABCdef...
   telegram.chat.id=123456789
   ```
5. Expor via BuildConfig no `build.gradle.kts (Module)`:
   ```kotlin
   android {
       defaultConfig {
           val localProps = Properties().apply {
               load(rootProject.file("local.properties").inputStream())
           }
           buildConfigField("String", "TELEGRAM_TOKEN", "\"${localProps.getProperty("telegram.bot.token")}\"")
           buildConfigField("String", "TELEGRAM_CHAT_ID", "\"${localProps.getProperty("telegram.chat.id")}\"")
       }
       buildFeatures {
           buildConfig = true
       }
   }
   ```
6. Acessar no código: `BuildConfig.TELEGRAM_TOKEN`

### 8.5. Celular físico

1. No celular: Configurações → Sobre o telefone → toca 7x em "Número da versão" → ativa modo desenvolvedor
2. Opções do desenvolvedor → ativa "Depuração USB"
3. Conecta no PC via USB, aceita o fingerprint
4. Verifica conexão: `adb devices` (deve listar o aparelho)

### 8.6. Emulador (alternativa, mas evite pra este projeto)

CameraX, GPS e Device Admin funcionam mal/parcialmente em emulador. Use celular físico. Mas se precisar:

```bash
# Listar AVDs existentes (criados via Android Studio AVD Manager)
emulator -list-avds

# Subir um
emulator -avd Pixel_7_API_34 &
```

### 8.7. `.gitignore`

Garantir que não commite credenciais:

```gitignore
local.properties
*.keystore
*.jks
.gradle/
build/
.idea/
*.iml
captures/
.cxx/
```

---

## 9. Fluxo de trabalho diário no VS Code

Equivalentes ao seu fluxo `yarn start` / `yarn android` do RN:

### 9.1. Criar um Makefile na raiz do projeto

```makefile
.PHONY: build install run logs watch clean uninstall debug

# Variáveis — ajustar conforme seu package
PACKAGE := com.guilherme.honeypot
MAIN_ACTIVITY := .MainActivity

# Build do APK debug
build:
	./gradlew assembleDebug

# Instala no celular conectado
install:
	./gradlew installDebug

# Build + instala + abre o app (equivalente a "yarn android")
run: install
	adb shell am start -n $(PACKAGE)/$(MAIN_ACTIVITY)

# Logs do app (equivalente ao Metro logs)
logs:
	adb logcat -s "Honeypot:*" "AndroidRuntime:E" "*:E"

# Rebuild contínuo a cada save (equivalente a "yarn start" do Metro)
watch:
	./gradlew --continuous assembleDebug

# Limpa cache de build
clean:
	./gradlew clean

# Desinstala o app
uninstall:
	adb uninstall $(PACKAGE)

# Build + instala + roda + abre logs em paralelo
debug: install
	adb shell am start -n $(PACKAGE)/$(MAIN_ACTIVITY)
	adb logcat -s "Honeypot:*" "AndroidRuntime:E"
```

final unique powershell:
./gradlew installDebug; adb shell am start -n com.guilherme.honeypot/.ui.MainActivity

### 9.2. Comandos do dia a dia

| Quero...                    | Comando          |
| --------------------------- | ---------------- |
| Rodar o app no celular      | `make run`       |
| Ver logs em tempo real      | `make logs`      |
| Rebuild + ver logs          | `make debug`     |
| Limpar tudo                 | `make clean`     |
| Desinstalar                 | `make uninstall` |
| Apenas verificar se compila | `make build`     |

### 9.3. Hot reload?

Kotlin/Compose não tem o mesmo "hot reload" instantâneo do React Native. As opções:

- **Build incremental do Gradle**: `./gradlew --continuous assembleDebug` (re-compila ao salvar, ~5-15s)
- **Compose Hot Reload** (preview only): só funciona no Android Studio, infelizmente. Permite ver mudanças de UI Compose em segundos sem rebuild.
- **Workaround pra VS Code**: salva → `make run` → testa. Pra UI iterativa de Compose, vale abrir Android Studio só pra preview da UI, codar no VS Code, e rodar do terminal.

### 9.4. Debug

**Sem debugger visual no VS Code (limitação real):**

- Use `Log.d("Honeypot", "mensagem")` extensivamente
- Filtra com `adb logcat -s "Honeypot:*"`
- Pra erro de crash: `adb logcat -s "AndroidRuntime:E"`

**Com debugger visual (precisa Android Studio):**

- Abre o mesmo projeto no AS em paralelo
- Marca breakpoints, clica "Attach Debugger to Android Process"
- Funciona junto com VS Code aberto, sem conflito

### 9.5. Usando Claude Code efetivamente neste projeto

Claude Code brilha pra:

- **Boilerplate Android**: pede pra ele gerar `AndroidManifest.xml` completo com todas as permissões, ou o `MyDeviceAdminReceiver` com policies
- **APIs desconhecidas**: pede exemplos completos de CameraX silencioso, FusedLocationProvider com timeout, multipart upload pro Telegram
- **Refactor**: "extrai essa lógica do MainActivity pra um ViewModel"
- **Gradle**: configs do `build.gradle.kts` são chatas, Claude Code resolve rápido

Comandos úteis pra dar contexto:

```bash
# Adiciona docs Android ao contexto (Claude Code Pro)
# Antes de tarefas complexas:
claude "leia o arquivo HONEYPOT_ANTI_ASSALTO.md e os arquivos em app/src/main/kotlin/, depois implemente o EmergencyService conforme seção 4.2"
```

Boa prática: mantém o `HONEYPOT_ANTI_ASSALTO.md` na raiz do projeto pra Claude Code sempre ter o contexto da arquitetura disponível.

---

## 10. Referências técnicas

- [Device Administration overview](https://developer.android.com/work/device-admin)
- [CameraX](https://developer.android.com/training/camerax)
- [FusedLocationProviderClient](https://developer.android.com/training/location/retrieve-current)
- [Foreground services](https://developer.android.com/develop/background-work/services/foreground-services)
- [Telegram Bot API — sendPhoto](https://core.telegram.org/bots/api#sendphoto)
- [Geofencing API](https://developer.android.com/training/location/geofencing)
- [Gradle command-line](https://docs.gradle.org/current/userguide/command_line_interface.html)
- [ADB documentation](https://developer.android.com/tools/adb)

---

## 11. Notas finais

Este é um projeto de **uso pessoal**, sem distribuição. Não é solução universal de segurança, é uma camada **a mais** em defesa em profundidade:

- **Camada 1**: Limites baixos nos apps de banco reais (via configuração de cada banco)
- **Camada 2**: App real escondido em Secure Folder / Second Space
- **Camada 3**: Este honeypot
- **Camada 4** (humana): conhecimento dos contatos de emergência dos bancos, treino mental

A eficácia real depende mais da disciplina das camadas 1, 2 e 4 do que da sofisticação técnica deste app. Construa este, mas não pule as outras.
