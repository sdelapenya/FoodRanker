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
- Explore, comments, follows, reports, XP/badges (client-side), onboarding, splash
- Ads (AdMob) en otros puntos; Billing esqueleto
- Package `com.app.foodranker`; minify release configurado

## Producto — hecho 2026-07-18 (confianza)

- Challenge: CTA solo navega a publicar; **XP solo al publicar** (ya no farm sin post)
- Sin interstitial tras publicar plato
- Seed MealDB: UI long-press + **bloqueado fuera de DEBUG**
- Empty state honesto (sin relleno inventado)
- Premium: stub “Próximamente” (sin 24h fake); ruta sigue huérfana
- Home / Trending: rutas existen pero **no enlazadas** desde Discover

## Qué NO está hecho

### Play (bloquea Store)

- Privacy/Terms URL HTTPS en Console
- `@xml/gma_ad_services_config` ausente
- Location permissions sin uso
- API keys Vision/Pexels en BuildConfig
- `isPremium` / XP escribibles por cliente en rules
- Listing: capturas, Data safety, UGC

### Producto (siguiente valor)

- Discovery por ciudad / cerca
- Identidad canónica plato+venue
- XP/scores server-side
- City MVP con contenido real

## Próximo paso

1. Sergio: **pull** en PC/disco tras push de estos cambios.
2. Producto: ciudad-first o seguir podando trust; Play P0 cuando toque publicar.

## Journal

- `lab/marca-personal/docs/journal/2026-07-18-cursor-foodranker-confianza.md`
