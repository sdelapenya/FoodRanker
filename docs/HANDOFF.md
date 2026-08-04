# HANDOFF — FoodRanker (Play Store + producto)

**Actualizado:** 2026-07-18  
**Código (servidor):** `/home/sergio/lab/apps/FoodRanker`  
**GitHub:** https://github.com/sdelapenya/FoodRanker  
**También:** copia en disco duro portátil del PC de Sergio (no en este servidor)  
**Auditorías:** `lab/marca-personal/apps-android/AUDITORIA-PLAY-*.md` + `AUDITORIA-PRODUCTO-USO-*.md`

## Sync (importante)

Cambios en el **servidor** no llegan solos al portátil. Flujo:

1. Commit + push a GitHub desde el servidor (cuando Sergio lo pida).
2. En el PC: `git pull` en la carpeta del disco / clone local.
3. No editar a ciegas en dos sitios sin pull previo.

## Qué funciona

- Core loop: Google Sign-In → Discover → like/rate/save → Add plate (Cloudinary) → Profile
- Explore, comments, follows, reports, onboarding, splash
- **XP/badges server-side** (Cloud Functions + Admin SDK; el cliente no los escribe)
- Liga semanal por ciudad, con clawback de XP si el plato se rechaza
- Ads (AdMob) en otros puntos; Google Play Billing implementado
- Package `com.app.foodranker`; minify release configurado

## Producto — hecho 2026-07-18 (confianza)

- Challenge: CTA solo navega a publicar; **XP solo al publicar** (ya no farm sin post)
- Sin interstitial tras publicar plato
- Seed MealDB: UI long-press + **bloqueado fuera de DEBUG**
- Empty state honesto (sin relleno inventado)
- Home / Trending: rutas existen pero **no enlazadas** desde Discover

## Merge de esta rama con main (2026-08-04)

Esta rama partía de `fccc93d`, sin los 13 commits de `main` (seguridad, liga, CFs).
Al mergear se resolvió así:

- Challenge XP: se **eliminó** `ChallengeViewModel.participate()` en vez de dejarlo no-op.
  `AddPlateViewModel.checkAndCompleteChallenge()` ya registraba la participación al publicar,
  y la CF `onChallengeUpdated` otorga el XP. El banner ya no llama al ViewModel.
- Premium: **no** se aplicó el stub “Próximamente”. La ruta ya no es huérfana (hay botón en
  Perfil) y el Billing es real. En su lugar se recortó la lista de beneficios a los dos que la
  app entrega hoy (sin anuncios, badge) — los otros tres no estaban implementados.
- Seed DEBUG y empty state: `main` ya tenía equivalentes mejores; se conservó el tono honesto.

## Qué NO está hecho

### Play (bloquea Store)

- Privacy/Terms URL HTTPS en Console
- `@xml/gma_ad_services_config` ausente
- Location permissions sin uso
- API keys Vision/Pexels en BuildConfig
- Listing: capturas, Data safety, UGC
- `autoVerify="true"` en intent-filters https sin `assetlinks.json` publicado

### Producto (siguiente valor)

- Discovery por ciudad / cerca
- Identidad canónica plato+venue
- City MVP con contenido real

### Ya resuelto en main (no volver a abrirlo)

- `isPremium` / XP escribibles por cliente en rules → **cerrado**: solo Admin SDK los escribe
- XP/scores server-side → **hecho**: Cloud Functions en `europe-west1`

## Próximo paso

1. Probar en móvil real: login Google, Billing, push FCM y AdMob nunca se probaron
   (el QA se hizo en emulador sin cuenta de Google).
2. AAB de release: regenerar tras este merge — el generado el 2026-07-02 es de `e4c0a87`.
3. Producto: ciudad-first; Play P0 cuando toque publicar.

## Journal

- `lab/marca-personal/docs/journal/2026-07-18-cursor-foodranker-confianza.md`
