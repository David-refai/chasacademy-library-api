# Säkerhetsrapport — Library API (Individuell Labb 2k5)

Den här rapporten är en uppföljning av säkerheten i Library API, kopplat till Individuell
Labb 2k5. Labb 1 hade redan JWT-autentisering, rollbaserad behörighet (`@PreAuthorize`),
striktare CORS, validering och en circuit breaker — så för den här labben letade jag efter
sårbarheter som inte redan var täckta av det arbetet. Jag hittade tre som kändes verkliga
och relevanta för just den här applikationen, inte bara teoretiska checklistepunkter.

## Sammanfattning

| # | OWASP-kategori | Fanns i | Löst med |
|---|---|---|---|
| 1 | A02:2021 — Cryptographic Failures | `application.yml` (JWT-signeringsnyckel) | Tog bort standardvärdet, appen kräver nu en riktig nyckel |
| 2 | A05:2021 — Security Misconfiguration | `application.yml` + `SecurityConfig.java` (H2-konsolen) | Avstängd som standard, kräver JWT om man slår på den |
| 3 | A06:2021 — Vulnerable and Outdated Components | `pom.xml` (inga beroenden skannades) | OWASP Dependency-Check, kopplat till CI |

---

## 1. A02 — Cryptographic Failures (hårdkodad JWT-nyckel)

### Identifiering
I `application.yml` såg signeringsnyckeln för JWT-tokens ut så här:

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:library-api-super-secret-key-minimum-256-bits-long-for-hmac-sha256}
```

Kommentaren ovanför sa uttryckligen "in production app.jwt.secret should come from Vault" —
men det fanns ingenting i koden som faktiskt tvingade fram det. Om man glömde sätta
miljövariabeln `JWT_SECRET` eller starta Vault, startade appen ändå, helt tyst, med
nyckeln som står skrivet rakt upp och ner i en publik GitHub-repo. Den här nyckeln är inte
en hemlighet längre i det ögonblicket koden pushas — vem som helst som läser repot kan
signera sin egen JWT-token med exakt samma nyckel som servern använder, sätta
`"role": "ROLE_ADMIN"` i token, och loggas in som administratör utan att känna till ett
enda riktigt lösenord.

### Åtgärd
- Tog bort standardvärdet helt: `secret: ${JWT_SECRET}` utan fallback.
- Utan `JWT_SECRET` satt (via miljövariabel eller Vault) **vägrar appen nu starta**, med
  ett tydligt fel: `Could not resolve placeholder 'JWT_SECRET'`. Det här testade jag
  manuellt — appen kraschar direkt vid uppstart istället för att tyst falla tillbaka på
  en känd nyckel.
- README uppdaterad med två sätt att sätta en riktig nyckel: miljövariabel för snabb
  lokal utveckling, eller Vault (`vault kv put secret/library-api app.jwt.secret=...`)
  precis som applikationen redan var förberedd för.

### Analys & prioritering
Det här fick högst prioritet eftersom hela autentiseringssystemet i Labb 1 — JWT, roller,
`@PreAuthorize` — bygger på antagandet att bara servern känner till signeringsnyckeln. Om
nyckeln läcker spelar det ingen roll hur bra rollbehörigheten är implementerad: en
angripare som kan signera sina egna tokens kan ge sig själv vilken roll som helst och
strunta helt i `@PreAuthorize("hasRole('ADMIN')")`. Skillnaden mellan "väldigt säkert
system" och "helt öppet system" var här en enda rad konfiguration som råkade vara
synlig för alla på GitHub. Att låta appen vägra starta utan en riktig nyckel är en
medveten "fail closed"-strategi — hellre att utvecklaren märker felet direkt vid
uppstart i en testmiljö, än att en okänd, fungerande nyckel av misstag följer med till
en skarp miljö.

---

## 2. A05 — Security Misconfiguration (öppen H2-konsol)

### Identifiering
H2-konsolen var påslagen som standard och helt öppen för obehöriga:

```yaml
h2:
  console:
    enabled: true
```

```java
.requestMatchers("/h2-console/**").permitAll()
...
.headers(headers -> headers.frameOptions(frame -> frame.disable()))
```

H2-konsolen är i praktiken ett webbgränssnitt som kör godtycklig SQL mot databasen. Med
`permitAll()` och frame-skyddet avstängt kunde **vem som helst**, utan att logga in,
öppna `/h2-console` i en webbläsare och köra `SELECT * FROM users` — vilket hade gett
dem alla användarnamn och lösenordshashar direkt, eller `DROP TABLE books` för att
radera hela databasen. Det fanns ingen miljö-koll (t.ex. `@Profile("dev")`) som
förhindrade detta i en driftsatt container — och just det är vad den här labben handlar
om: nu containeriserar och deployar vi appen, vilket gör det här till en verklig, inte
bara teoretisk, risk.

### Åtgärd
- `enabled: ${H2_CONSOLE_ENABLED:false}` — avstängd som standard, går att slå på lokalt
  via miljövariabel för felsökning.
- Tog bort `.requestMatchers("/h2-console/**").permitAll()` helt. Konsolen faller nu
  under samma regel som allt annat: `.anyRequest().authenticated()`. Även om man slår på
  den lokalt krävs en giltig JWT-token — något en vanlig webbläsare som navigerar direkt
  till `/h2-console` inte skickar med, så den är i praktiken oåtkomlig utan att
  medvetet bygga en autentiserad request.
- Tog bort `frameOptions(frame -> frame.disable())`, som bara fanns för att låta
  H2-konsolens iframe fungera. Appen får nu tillbaka sitt vanliga klickjacking-skydd
  (`X-Frame-Options`) på alla andra sidor.
- Testat manuellt: utan miljövariabeln satt svarar `/h2-console` med `403` istället för
  att visa konsolen.

### Analys & prioritering
Den här sårbarheten är speciellt allvarlig eftersom den ger en angripare direkt åtkomst
till **rådata** — inte bara till ett enda API-anrop, utan till hela databasen, inklusive
tabeller som aldrig exponeras via något REST-endpoint. Den krävde inte ens en bugg i
applikationslogiken; standardinställningen `enabled: true` (bra för utveckling, farlig i
drift) hade följt med rakt in i Docker-imagen om jag inte ändrat den. Jag prioriterade
den direkt efter JWT-nyckeln eftersom de två tillsammans representerar de två enklaste
vägarna in i systemet: en stulen/läckt nyckel (#1) eller en helt oskyddad bakdörr som
kringgår autentiseringen totalt (#2).

---

## 3. A06 — Vulnerable and Outdated Components

### Identifiering
Projektet har ett tiotal tredjepartsbibliotek (Spring Security, Spring Data Redis, Spring
Cloud Vault, Resilience4j, JJWT, Bucket4j, H2, med flera) men det fanns **ingen
automatisk process** för att upptäcka kända säkerhetsbrister (CVE:er) i något av dem
eller deras egna beroenden. En kritisk sårbarhet i exempelvis ett JSON- eller
HTTP-bibliotek skulle kunna ligga oupptäckt på obestämd tid.

### Åtgärd
- Lade till **OWASP Dependency-Check-pluginet** i `pom.xml`, konfigurerat med
  `failBuildOnCVSS=7` — ett beroende med en hög eller kritisk sårbarhet stoppar bygget.
- Pluginet körs medvetet **inte** automatiskt vid `mvn test`/`mvn package`, så den
  vanliga utvecklingsloopen förblir snabb. Det körs istället som ett eget jobb,
  `dependency-check`, i `ci-cd-pipeline.yml`, på varje push/pull request — och
  **blockerar** `docker-build-push`-jobbet om det misslyckas. En sårbar build kan
  därmed aldrig nå Docker Hub.
- HTML/JSON-rapporten sparas som en artefakt i GitHub Actions för felsökning.

### Analys & prioritering
Den här sårbarheten skiljer sig från de andra två på ett viktigt sätt: den ligger inte i
kod jag själv skrivit, utan i kod jag är **beroende av**. Det gör henne svårare att
upptäcka manuellt — man läser inte säkerhetsbulletiner för tio bibliotek varje vecka i
praktiken — men lika farlig om en ny CVE dyker upp i en version vi redan använder.
Tröskeln CVSS 7 (Hög) valdes istället för en strängare gräns på bara Kritiska (CVSS 9),
eftersom en hög-allvarlig brist i ett bibliotek som körs i varje request (t.ex. Spring
Security) är en reell risk, inte bara en teoretisk. Den hamnade på tredje plats eftersom
den, till skillnad från #1 och #2, inte är en bugg i applikationen — den är en
kontinuerlig övervakningsuppgift som nu sker automatiskt i CI istället för att aldrig
ske alls.

---

## Vad jag medvetet INTE räknade som en av de tre

Min lärare flaggade i sin feedback på Labb 1 att `RateLimitFilter` sparar IP-adresser i
en `ConcurrentHashMap` som aldrig rensas, vilket i drift leder till en minnesläcka. Det
är ett korrekt och viktigt påpekande, men jag valde att inte räkna det som en av de tre
OWASP-sårbarheterna här — det är i grunden ett resursläckage/prestandaproblem snarare än
en sårbarhet som direkt kan utnyttjas för obehörig åtkomst, dataläckage eller
attacker, och låg redan dokumenterad som en framtida förbättring i lärarens
kommentar. De tre jag valde är alla sårbarheter som en angripare aktivt kan utnyttja
*idag*, vilket kändes mest relevant för den här labbens fokus på OWASP Top 10.
