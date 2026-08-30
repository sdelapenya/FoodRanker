# HANDOFF — FoodRanker (Play Store + producto)

**Actualizado:** 2026-08-17
**Código (PC):** `e:\FoodRanker` · **Código (servidor):** `/home/sergio/lab/apps/FoodRanker`
**GitHub:** https://github.com/sdelapenya/FoodRanker (público)
**Gitea:** ssh://git@192.168.1.19:222/sdelapenya/foodranker.git — **por SSH puerto 222**, el HTTP 3000 solo escucha en loopback

## Sync

Se desarrolla en PC **y** en servidor (Cursor), y las ramas divergen. Antes de tocar:
`git status`, `git log --oneline -5` y comprobar `merge-base` con el otro remote.
El 2026-08-04 se mergeó una rama del servidor que divergía 13 commits (10 conflictos a mano).

---

## LO SIGUIENTE (retomar aquí)

### ✅ Octava sesión (2026-08-28/29): moderación, Premium arreglado, "Qué pido aquí", code review

**Retomar exactamente aquí**: el AAB de `versionCode 6` está generado en local
(`app/build/outputs/bundle/release/app-release.aab`, 15,57 MB) con **todo** lo de esta
sesión, pero **no se ha subido a Play Console todavía**. Y los testers de la prueba cerrada
**siguen sin reclutarse** — se preparó el Google Group (`foodranker-testers@googlegroups.com`)
y el track en sesiones anteriores, pero nunca se llegó a mandar el mensaje de invitación a
nadie. Los dos pasos pendientes, en este orden:
1. Subir el AAB de `versionCode 6` a Play Console → Probar y publicar → Prueba cerrada
   (mismo proceso ya hecho varias veces: crear versión, subir el `.aab`, notas, enviar a
   revisión — suele aprobarse en horas).
2. Reclutar a los 12 testers mínimos (contactos + grupos de Telegram/Facebook de testers
   Android) y contar 14 días desde que el 12º entra, antes de poder pedir producción.

**Lo que se cerró esta sesión** (commits `1424d61`…`1c28424`, todo en `main`, nada pusheado
a `origin`/`gitea` — pendiente comprobar cuándo se retome):

- **Cumplimiento de Play**: Billing Library 7.0.0→8.0.0 (exigido por Google antes del
  31-ago), reCAPTCHA y SoLoader forzados a versión parcheada — los tres iban con plazo real.
- **Sistema de moderación**: reportar plato (conectado, el código llevaba muerto desde
  siempre) y reportar comentarios (nuevo, mismo umbral de 3+ para ocultar), baneo manual de
  usuarios vía `functions/scripts/manageUser.js ban/unban <uid>` (desactiva la cuenta +
  revoca tokens). Se encontró y arregló un bug real: la regla de `reports` bloqueaba su
  propia transacción de dedup.
- **Premium arreglado de raíz**: `isPremium` en Firestore nunca lo escribía nada (ni compra
  real ni el temporal por anuncio) — el badge de Premium **nunca funcionó**, para nadie.
  `BillingManager` ahora combina las tres señales (compra real / temporal / regalado) sin
  que se pisen. Nuevo: `manageUser.js grant-premium/revoke-premium <uid>` para regalar
  Premium a mano (amigos, familia). El intersticial de `PlateDetail` también se saltaba
  Premium — ya no.
- **"Qué pido aquí"**: pantalla nueva (icono de ubicación en Discover), platos ya puntuados
  cerca por GPS real, diseñada para no gastar cuota de Places salvo que el usuario lo pida
  explícitamente. Radio ampliado para Premium.
- **Nuevo beneficio Premium**: ver quién ha dado like/guardado tu plato, y tope diario de
  platos más alto.
- **`/code-review --effort high`** sobre todo el repo: 4 bugs reales arreglados (límite de
  `whereIn` a 30, `reportCount` de comentarios legacy rompía la regla, tokens no revocados al
  banear), 1 falso positivo descartado tras comprobarlo en el emulador (no fiarse del
  análisis automático a ciegas).
- **`firestore.rules` desplegadas a producción** dos veces esta sesión (moderación primero,
  luego saves+comments), ambas verificadas en el emulador local antes de desplegar.
- **`.gitignore`**: `firebase.json`/`.firebaserc` habían quedado sin commitear sin necesidad
  (no tienen secretos) — ya están en el repo.

Detalle completo de cada pieza en memoria: `project_recaptcha_soloader_warning.md` (RESUELTO).

### ✅ Séptima sesión (2026-08-23): ficha de Play Store completada y prueba cerrada enviada a revisión

Se completó lo que faltaba de la ficha ("Presencia en Google Play Store"): nombre,
descripción breve y completa, icono, gráfico destacado, 3 capturas, categoría ("Comer y
beber"), etiquetas ("Comida y bebida" + "Restaurante") y datos de contacto públicos
(`sdelapenya1991@gmail.com`). Las 11 declaraciones de "Contenido de la aplicación" seguían
completas de la sesión anterior. Con eso, las 11 de 11 tareas de "Termina de configurar tu
aplicación" quedaron cerradas.

**Bloqueante real encontrado (esperado, ya documentado antes)**: Play no deja pedir acceso
a producción sin pasar antes por una **prueba cerrada con mínimo 12 testers durante 14
días** — política de Google para cuentas de desarrollador nuevas. Se montó:

- **Google Group `foodranker-testers@googlegroups.com`** (creado con la cuenta correcta,
  `sdelapenya1991@gmail.com` — no confundir con `sergiodelapenya1991@`), configurado como
  "cualquier usuario de la Web puede unirse", para poder sumar testers de grupos de
  Facebook/Telegram sin pedirles el Gmail de antemano.
- **Track "Prueba cerrada - Alpha"** en Play Console, con ese Google Group como lista de
  testers, todos los países marcados, y `sdelapenya1991@gmail.com` como canal de feedback.
- **AAB regenerado con `versionCode 2`** (el `1` ya estaba usado por la prueba interna del
  2026-08-21): `versionCode` en `app/build.gradle.kts` subido de 1 a 2, `bundleRelease`
  reejecutado, BUILD SUCCESSFUL, 15,46 MB. El de `versionCode 1` queda obsoleto.
- **Enviado a revisión** (2026-08-23): la ficha completa + las 11 declaraciones + esta
  versión de prueba cerrada, todo junto, quedó en estado "Cambios en revisión".

⚠️ **Aviso real encontrado al subir el AAB, con plazo pero ya resuelto en código**: Play
detectó una dependencia transitiva de **reCAPTCHA Enterprise
(`com.google.android.recaptcha:recaptcha` 18.1.2) con una vulnerabilidad crítica**
(viene de `firebase-auth`, que la arrastra internamente aunque FoodRanker solo use Google
Sign-In), parcheada en la 18.4.0, y **SoLoader desactualizado** (0.10.1, viene de
`cloudinary-android` → `fresco:2.6.0`, riesgo de crash en dispositivos solo 64 bits,
corregido en 0.10.4). Google daba 90 días desde el envío (límite aprox. **2026-11-21**).

**Arreglado el mismo día** forzando las dos dependencias transitivas directamente en
`app/build.gradle.kts`, sin tocar el Firebase BOM ni Kotlin:
```
implementation("com.google.android.recaptcha:recaptcha:18.4.0")
implementation("com.facebook.soloader:soloader:0.10.4")
```
Confirmado con `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` que
ambas resuelven a la versión parcheada. `compileReleaseKotlin` en verde. **Se probó primero
subir el Firebase BOM (32.7.0 → 34.18.0)**, que es como Google "querría" que se arreglara,
pero esa vía obliga a quitar el sufijo `-ktx` de todas las dependencias Firebase → obliga a
subir Kotlin (2.0.21 → 2.3.20) para que el compilador lea los metadatos nuevos → rompe Kapt
(el que usa Hilt), cuyo arreglo real es migrar a KSP. Se revirtió todo (nunca se commiteó) y
se quedó con el forzado quirúrgico. Detalle completo en memoria
`project_recaptcha_soloader_warning.md`.

**Pendiente**: el fix ya está en el código pero **no se ha regenerado ni resubido el AAB**
(la v2 ya estaba en revisión en la prueba cerrada, no merecía la pena interrumpirla). Subir
`versionCode` a 3 y regenerar (`bundleRelease`) la próxima vez que se toque una versión,
antes de pedir acceso a producción como muy tarde. De paso se confirmó que
`firebase-dynamic-links` es dependencia muerta (Google apagó el servicio en 2025, cero
referencias en el código) — limpieza pendiente sin prisa, no se tocó para no mezclar con
este arreglo.

**Retomar aquí**: cuando Google apruebe la prueba cerrada (horas, no días), ir a Play
Console → Probar y publicar → Pruebas → Prueba cerrada → Testers, copiar el enlace de
unión, y compartirlo junto con el enlace del Google Group con los 4-5 contactos del usuario
más gente de grupos de Telegram/Facebook de testers Android en español, hasta reunir 12.
Desde que el 12º acepte y abra la app, contar 14 días antes de poder pedir acceso a
producción.

### ✅ Sexta sesión (2026-08-22): capturas de Play Store hechas y producción limpiada

Se publicaron 4 platos de prueba reales desde el Redmi (conectado por ADB, USB debugging)
para sacar capturas de pantalla para la ficha de Play Store, siguiendo el mismo patrón
verificado el 2026-08-20 (publicar por el flujo normal de la app, con fotos reales de
Pexels vía `PEXELS_API_KEY` de `local.properties`, luego borrar): Pizza Margarita
Artesanal, Tortilla de la Abuela, Roll Tempura Picante, Hamburguesa Especial de la Casa —
los 4 en Bar Casa Benito, Toledo.

**La app no tiene botón de borrar plato para el autor** (el lápiz de "Mis platos" solo deja
editar la descripción) — se borraron los 4 documentos de `plates` con un script Admin SDK
(mismo patrón ADC de siempre), lo que disparó `onPlateDeleted` igual que si se hubiera
borrado desde la app. Verificado, no supuesto: `plates`, `ratings`, `comments` y `saves`
volvieron a 0. De paso se encontraron **6 venues residuales** en la colección `venues` —
5 de sesiones de prueba anteriores (Madrid y Toledo) más el de hoy, ninguno con platos
asociados — y se borraron los 6 con el visto bueno del usuario, ejecutados por él mismo
desde su propia PowerShell (el clasificador de seguridad de Claude Code bloquea escrituras
destructivas directas a producción vía Admin SDK aunque el usuario confirme en el chat; hay
que dárselo para que lo ejecute él, o añadir una regla de permiso Bash). Producción vuelve
a estar en 0 en las 5 colecciones.

Se concedió permiso de ubicación al Redmi vía `pm grant` (no por el diálogo real) para
poder probar `resolveVenue` sin fricción — sin importancia, ya se había verificado el flujo
real de permisos antes.

**Capturas conseguidas y guardadas** en `docs/play-store-screenshots/` (fuera del
scratchpad de sesión, que no persiste): `01-ranking.png` (ranking con contenido real),
`02-detalle-plato.png` (detalle de "Tortilla de la Abuela"), `03-perfil.png` (perfil de
Sergio, 720 XP/Crítico Gastronómico/logros). Listas para usar en la ficha de Play Store —
recortar/adaptar tamaño si Play lo exige, pero el contenido ya es bueno.

Curiosidad menor encontrada de paso: la pestaña Liga pedía "añade tu ciudad en el perfil"
aunque el perfil ya mostraba "Madrid" — no investigado, no bloqueante, anotar si se repite.

---

**No queda código pendiente para subir a Play.** Las 11 declaraciones de "Contenido de la
aplicación" (incluida Data safety) están **completas**. Lo que falta es el resto de la
ficha de Play Store: **capturas de pantalla, descripciones**. La clasificación de
contenido y la declaración de audiencia/UGC ya están hechas (ver más abajo).

### ✅ Sexta sesión (2026-08-22): "Contenido de la aplicación" completado

Las 11 declaraciones obligatorias de **Play Console → Política y programas → Contenido de
la aplicación** están cerradas:

✅ Política de Privacidad · ✅ Anuncios · ✅ Datos de inicio de sesión · ✅ Aplicaciones
gubernamentales (No) · ✅ Funciones financieras (No) · ✅ Aplicaciones de salud (No) · ✅ ID
de publicidad (Sí, AdMob) · ✅ Permisos de fotos y vídeos · ✅ Clasificaciones del contenido
(12-14+ según región, sin violencia/sexo/lenguaje) · ✅ Contenido y audiencia objetivo
(**18+ únicamente**) · ✅ **Seguridad de los datos**

Respuestas que quedaron en "Seguridad de los datos", por si hay que repetirlas o auditarlas
(**tipos de datos** → paso 3; **recogido/compartido/finalidad/opcional** → paso 4):

| Tipo | Recogido | Compartido | Finalidad | Opcional | Notas |
|---|---|---|---|---|---|
| Ubicación aproximada/precisa | Sí | No | Funcionalidad de la app | Sí | Procesada de forma efímera (no se guarda, solo se usa en el momento de buscar locales); Google Places es proveedor de servicio, no "compartición" |
| Nombre | Sí | No | Funcionalidad de la app, gestión de cuenta | No | Viene del login de Google |
| Correo electrónico | Sí | No | Gestión de cuenta | No | No se muestra a otros usuarios (no se guarda en `users/{uid}`, que es legible por cualquiera) |
| ID de usuario | Sí | No | Funcionalidad de la app | No | |
| Fotos | Sí | No | Funcionalidad de la app | Sí | Solo si publicas un plato; Cloudinary es proveedor de servicio |
| Interacciones en la app | Sí | No | Analítica, funcionalidad de la app | Sí | Eventos de `AnalyticsManager` sin PII, solo IDs y categorías |
| Historial de búsqueda en la app | Sí | No | Funcionalidad de la app | Sí | |
| Registros de fallos | Sí | No | Analítica | No | Crashlytics |
| Diagnósticos | Sí | No | Analítica | No | Firebase Analytics |
| ID de publicidad (dentro de "IDs de dispositivo o de otro tipo") | Sí | **Sí, con Google AdMob** | Publicidad o marketing | No | Único bloque con "compartido: sí" real — AdMob usa el ID con fines propios |
| Otros ID (token FCM, misma categoría que el ID de publicidad) | Sí | No | Funcionalidad de la app (notificaciones) | No | |
| Todo lo demás (financiero, salud, mensajes, archivos, calendario, contactos, navegación web, audio) | **No recopilado** | — | — | — | |

Preguntas generales del paso 2: cifrado en tránsito → Sí; método de cuenta → solo OAuth;
eliminación parcial sin borrar cuenta → No; URL de eliminación de cuenta →
`https://sdelapenya.github.io/FoodRanker/delete-account.html`.

⚠️ **Trampa real, dos veces seguidas**: el asistente de "Seguridad de los datos" **no
autoguarda entre pasos** — un F5 a media tarea borra todo desde el último "Guardar" y hay
que repetirlo entero. Guardar como borrador al terminar cada paso, no solo al final.

⚠️ **Curiosidad sin importancia, verificada dos veces**: la categoría "Ubicación" no
aparece en el resumen "Vista previa de la ficha de Play Store" (ni en "Datos recogidos" ni
desplegando "Mostrar detalles"), aunque en los pasos 3 y 4 del asistente esté marcada como
"Completado". Es un fallo visual del resumen de Play Console, no de los datos guardados —
confirmado comprobando el estado real en los pasos 3/4, que sí la reflejan.

### ✅ Lo que se cerró el 2026-08-21 (quinta sesión)

- **Cuenta de la app creada en Play Console** y subida a **prueba interna**: primera versión
  (`1.0`) publicada y disponible para testers, sin esperar revisión. `Producción` sigue
  inactivo a propósito — hace falta pasar antes por la prueba cerrada de 12 testers/14 días
  que exige Google a las cuentas de desarrollador nuevas.
- **Gráfico destacado 1024×500** (`03fa790`) para la ficha de Play, mismo estilo del icono.
- **Tercera huella SHA-1 encontrada y registrada** (`7b7197d`): al activar "Firma de
  aplicaciones de Play", Google re-firma el AAB con una clave propia — el login de Google
  roto en cualquier build instalada desde Play real hasta que se registra esa huella. Ya
  registrada en Firebase y en Google Cloud Console, verificada byte a byte.
- ⚠️ **Sigue sin propagarse 24h+ después** (`eb9f8df`): con todo verificado del lado de
  Google (huella, cliente OAuth, consentimiento en producción, caché de Play Services del
  Redmi borrada del todo), el login sigue fallando en el build de Play — instantáneo y
  silencioso, sin ningún error en logcat. Aislado con una prueba concluyente: un APK de
  release firmado con el keystore local (huella de siempre) **funciona perfectamente** en el
  mismo momento y dispositivo. No es un bug nuestro. **Reintentar el login en el build de
  Play el 2026-08-23 (domingo) o después** — detalle completo en memoria
  `project_play_signing_sha1.md`. Mientras tanto, el Redmi tiene instalada la build firmada
  con el keystore local (funciona bien), no la de Play.
- **Cuenta de revisión de Google Play creada**: `foodrankerreview@gmail.com`, dada de alta en
  la declaración "Datos de inicio de sesión" de Play Console (la contraseña **no se guarda
  aquí** — el repo es público — solo vive en Play Console y en quien la creó). Sirve para que
  el equipo de revisión de Google entre en la app; no tiene Premium contratado.
- **Bug real encontrado y arreglado en el sitio de gh-pages**: `foodranker.app@gmail.com`
  estaba publicado como contacto en privacidad/términos/portada desde antes de esta sesión,
  pero esa cuenta **nunca se creó** — cualquier correo enviado ahí se habría perdido.
  Sustituido por `sdelapenya1991@gmail.com` en las 4 páginas (commit `02088ac` en `gh-pages`).
  De paso se creó y publicó **`delete-account.html`** (commit `3c13291`), la página que exige
  Play Console para la declaración de eliminación de cuentas — describe los pasos en la app y
  un método alternativo por email para quien no la tenga instalada.
- **Hueco de producto anotado**: no existe forma de bloquear o banear usuarios, solo reportar
  contenido (que se oculta con 3+ reportes). Pendiente si la app crece y aparece contenido
  ofensivo de verdad — ver `project_no_user_blocking.md`.

### ✅ Lo que se cerró el 2026-08-20 (cuarta sesión)

- **Cuenta de desarrollador creada**: verificación de identidad aprobada, se está rellenando
  el formulario "Crear aplicación" en Play Console (`applicationId` `com.app.foodranker`,
  idioma predeterminado cambiado a español). El botón "Crear aplicación" estaba bloqueado
  hasta que se aprobó la identidad — normal, tarda de horas a días.
- **Icono rehecho** (`e53d91c`): el launcher (plato + estrella genérico) no se parecía en
  nada a `FoodRankerMark`, el logo real de la splash screen. Se recalculó la geometría exacta
  del mark y se sustituyó `ic_launcher_foreground.xml` + `ic_launcher_background.xml`
  (naranja sólido → blanco, el domo ya es naranja). De paso se encontró que
  `app/src/main/ic_launcher-playstore.png` (el icono 512×512 para la ficha de Play) era el
  placeholder genérico "Aa" de Android Studio, nunca sustituido — también rehecho, RGBA de 32
  bits, desde el mismo `pathData`. Detalles en memoria `project_icon_mismatch.md`.
- **Índice de Firestore arreglado** (`c091282`): `plates` por `city`+`averageScore` estaba
  definido en `firestore.indexes.json` pero nunca se había desplegado con éxito (un índice de
  un solo campo mal declarado abortaba el deploy completo). La pestaña "Cerca" del Discover
  llevaba rota desde siempre por esto. Verificado en el emulador publicando y luego borrando
  4 platos de prueba reales — sí, el índice funciona, y sí, se limpiaron después (XP
  revertido, imágenes borradas de Cloudinary, cero rastro en producción). Ver
  `project_no_seed_data.md`.

### ✅ Lo que se cerró el 2026-08-18 (tercera sesión)

- **URL pública de privacidad y términos**: publicada en GitHub Pages (rama `gh-pages`,
  commit `37912ba`), verificada respondiendo 200. Ver "Bloqueantes de Play Store" para las
  dos URL y la trampa de Data safety que las acompaña. Ya no es un bloqueante.
- **App Check**: cliente integrado con Play Integrity, verificado en el AAB. **El
  enforcement sigue sin activar a propósito** — el orden para activarlo está en
  "Bloqueantes de Play Store" → punto 2. Antes era el único pendiente técnico; ahora es
  trabajo de consola, no de código.
- **`main` pusheado a `origin` y a `gitea`**: los tres están alineados en `b8ce568`. El
  clon de trabajo del servidor (`/home/sergio/lab/apps/FoodRanker`) sigue sin actualizar —
  un `git pull` ahí lo pone al día, no hay nada que rescatar de ese lado (se comprobó el
  2026-08-17, ver más abajo).

### ✅ Lo que se cerró el 2026-08-17 (segunda sesión)

Los 5 ficheros que estaban sin commitear se revisaron, se les encontró **un bug**, se
arreglaron, se commitearon y **se desplegó todo**. No queda nada a medias de aquello:

| Pieza | Estado |
|---|---|
| `firestore.rules` — lista blanca `hasOnly` en `users/create` | **DESPLEGADA** |
| `revertAuthorXP()` + borrado de imagen en Cloudinary | **DESPLEGADA** y verificada en producción |
| `createdAt` en `AuthRepository` | **En el AAB nuevo** |
| Textos legales reescritos | **En el AAB nuevo** (comprobado en `classes.dex`) |

Las 13 Cloud Functions se redesplegaron en `europe-west1`.

**Secrets de Cloudinary** (por si hay que rehacerlos): `CLOUDINARY_CLOUD_NAME`,
`CLOUDINARY_API_KEY` y `CLOUDINARY_API_SECRET` están en Secret Manager, versión 1. Las dos
últimas **estaban vacías en `local.properties`** y hubo que sacarlas de
console.cloudinary.com → *Settings → API Keys*: la app sube con un upload preset *unsigned*,
así que nunca hicieron falta hasta ahora. Se ponen con
`npx firebase functions:secrets:set <NOMBRE>` **desde `e:\FoodRanker`** — desde otra carpeta
falla con "No currently active project", porque el proyecto sale del `.firebaserc` del repo.

**Verificación en producción** (2026-08-17, no deducida): se subió a Cloudinary una imagen
que no es comida con el preset unsigned, se creó el plato en `pending` vía Admin SDK con un
usuario temporal de 500 XP, y se dejó actuar a la moderación. Resultado en los logs:

```
16:19:29 moderateplateimage  Plate verify_tmp__... rejected (not food)
16:19:31 onplatedeleted      Cascade delete complete for plate verify_tmp__...
16:19:32 onplatedeleted      Imagen foodranker/plates/nbn8xovombttuzocx0sy borrada de Cloudinary (ok)
```

El XP del autor se quedó en **500**, que es justo lo que prueba la guarda nueva: ese plato
nunca estuvo `approved`, así que no había nada que revertir. Usuario temporal borrado
después; producción sin residuos.

**El bug que tenía `revertAuthorXP` (encontrado leyendo el flujo, no ejecutándolo)**:
`onPlateDeleted` llamaba a `revertAuthorXP` para **todo** plato borrado, pero
`deleteRejectedPlate` también borra platos — los que la moderación tumba estando en
`pending`. A esos el autor nunca cobró los 50 + 5 (el XP lo concede `approveplate`, y solo
al aprobar), así que la reversión le habría **quitado 55 XP que jamás recibió** cada vez que
Vision le rechazara una foto. Arreglado con una guarda `status === "approved"` en
`onPlateDeleted`. Segundo ajuste del mismo estilo: los −10 por valoración recibida ahora
cuentan solo las que tienen `processed === true`, que son exactamente las que dieron XP
(`onRatingCreated` se sale antes de conceder nada si el plato no estaba aprobado).

**El clon del servidor se comprobó antes de commitear**: `servidor/main` = `c86909e`, un
único commit del 19 de julio que solo toca `docs/HANDOFF.md` (`Documenta merge pendiente…`),
superado por completo. Local iba 27 commits por delante. No había nada que rescatar.

### El resto del estado

**La build de RELEASE está verificada de punta a punta en el Redmi (2026-08-12).** Salvo lo
de arriba, no queda código pendiente para subir a Play: lo que falta es **todo de Play
Console** (ver "Bloqueantes de Play Store" más abajo).

Lo verificado en la release firmada con la clave de producción:

- **Login con Google: OK, sin `DEVELOPER_ERROR`.** La huella de release está bien registrada
  en Firebase y el `google-services.json` que lleva el APK es el nuevo.
- **Búsqueda de locales: OK.** `searchNearby` devolvió `Bar Casa Benito —
  C. Quintanas, 38, 45950 Toledo, España`. La clave de Places de Android admite la huella
  de release. Cero errores de Places en el log.
- **Moderación de imagen: OK y de verdad.** Una foto que no era comida fue **rechazada**
  ("No hemos detectado comida en esta foto") y una de un plato pasó. La CF
  `validateFoodImage` responde bien desde la release.
- **AdMob: OK.** El `BannerAd` renderiza (anuncio de prueba, el Redmi está en
  `ADMOB_TEST_DEVICE_IDS`).
- **FCM: OK.** El `fcmToken` quedó guardado en el documento del usuario nuevo.

⚠️ **El AAB se regeneró el 2026-08-17** para meter `createdAt` y los textos legales nuevos.
Lo verificado en el Redmi el 2026-08-12 sigue valiendo: entre una build y otra solo cambian
esas dos cosas, ningún flujo de los de arriba. Aun así, el AAB nuevo **no se ha probado en
móvil** — si hay ocasión, un login con cuenta nueva confirma `createdAt` de una pasada.

### El bloqueante que se encontró y se arregló: ningún usuario nuevo podía registrarse

Al entrar con una cuenta de Google que **nunca había entrado**, el login de Google se
completaba pero la app moría en "No tienes permisos para realizar esta acción" y el log
decía `Write failed at users/{uid}: PERMISSION_DENIED`.

Causa: `AuthRepository` crea el perfil con `userRef.set(newUser)`, pasando el data class
`User`. El mapper de Firestore deriva el nombre del campo del getter de Kotlin, y a un
`Boolean` llamado `isPremium` (getter `isPremium()`) **le quita el prefijo `is`**: en el
documento el campo se llama **`premium`**. La regla exigía
`request.resource.data.isPremium == false`, un campo que no existe en el payload → la
condición falla → `PERMISSION_DENIED`. `xp`, `level`, `referralCount` e `id` no llevan
prefijo y se serializan igual, por eso fallaba solo esa línea.

Comprobado sobre producción, no deducido: el documento de usuario que ya existía tiene el
campo `premium` y **no tiene** ningún `isPremium`; y las reglas desplegadas el 2026-08-10
llevaban `isPremium` tal cual.

**Por qué no se había visto nunca**: hay **dos cuentas de Google** en el Redmi. Las pruebas
de login anteriores se hicieron con la que ya tenía documento, así que iban por la rama
`update`, que sí funciona. La rama `create` no se había ejecutado desde que se escribió la
regla. Habría afectado al **100 % de los usuarios reales**.

Arreglo (desplegado el 2026-08-12): la regla comprueba las dos grafías con
`get(campo, false)`, así no depende de ese detalle del serializador y sigue impidiendo que
nadie se cree premium. **No se tocó el modelo a propósito**: anotarlo con `@PropertyName`
haría que los usuarios nuevos guardaran `isPremium` mientras el documento antiguo tiene
`premium` (dos formatos en la misma colección, más una migración), y además obligaría a
regenerar el AAB.

---

Identidad canónica plato+local, **fase 1 ACTIVA en producción** desde el 2026-08-10.
Diseño completo en [VENUES.md](VENUES.md).

**Fase 1 verificada de punta a punta en el Redmi el 2026-08-11.** Ciclo completo probado:
elegir local → `resolveVenue` → publicar → reintentar con otra grafía. Resultados:

- `resolveVenue` respondió a la primera. Log: `Venue dado de alta: ChIJnZJ7qkXqQQ0RC8zo9tQxyIo
  — Bar Casa Benito (Toledo)`. La clave de servidor funciona y `city` se extrae bien de
  `addressComponents`.
- El id determinista quedó `ChIJnZJ7qkXqQQ0RC8zo9tQxyIo__espaguetis-con-tomate`.
- **El deduplicado funciona**: publicar "Espaguetis Con Tomate" en el mismo local cayó en ese
  mismo documento y la app ofreció "Ese plato ya está aquí → Valorarlo" en vez de duplicar.
  La comprobación pasa **antes** de subir a Cloudinary, así que un duplicado no deja imagen
  huérfana.
- Moderación: Vision aprobó la foto y el plato pasó a `status=approved` solo.
- Los datos de prueba (plato, rating y venue) se borraron después. Producción vuelve a estar
  a 0 en plates, ratings, comments, saves y venues.

Topes de cuota de Places puestos el 2026-08-11: `SearchNearbyRequest`, `SearchTextRequest` y
`GetPlaceRequest` a 500/día; `Autocomplete`, `GetPhotoMedia`, `SearchMedia` y
`SearchReviewPosts` a **0** (la app no los usa; los dos últimos estaban en "Ilimitado").
Las "per minute per user" se dejaron sin tocar a propósito: la clave de servidor cuenta
todas las llamadas de la CF como un solo usuario y un tope bajo estrangularía `resolveVenue`.

### Pendiente de la identidad canónica

1. ~~La dirección canónica queda en inglés~~ — **arreglado el 2026-08-11**: `resolveVenue`
   manda `languageCode=es` (constante `PLACES_LANGUAGE`). Verificado resolviendo el mismo
   local otra vez: ahora guarda `country: "España"`. Si algún día la app deja de ser solo en
   castellano, hay que **decidir** en qué idioma vive la identidad canónica, no localizarla
   por usuario: eso obligaría a duplicar venues, que es lo que esta colección evita.
2. **App Check: cliente LISTO desde el 2026-08-18 (`10aaf6b`), enforcement AÚN NO.**
   `FoodRankerApp.onCreate()` instala el proveedor lo primero, antes de cualquier otro uso
   de Firebase. Play Integrity en release, proveedor de depuración en debug, desdoblado en
   `app/src/release` y `app/src/debug` — **no** un `if (BuildConfig.DEBUG)`, porque la clase
   de debug entra por `debugImplementation` y no existe en release.

   El enforcement se dejó **desactivado a propósito**, y este es el orden correcto para
   activarlo, no antes:

   1. Firebase Console → App Check → registrar la app Android con **Play Integrity**.
   2. Subir el AAB a Play, aunque sea a un track **interno**. Play Integrity solo valida
      builds distribuidas por Google Play: hasta ese momento un APK instalado a mano **no
      obtiene token**, así que activar el enforcement ahora dejaría las callables sin
      responder en el Redmi.
   3. Dar de alta el **token de depuración** que el proveedor de debug escribe en logcat
      (`Enter this debug secret into the allow list…`), o el emulador se queda fuera. Es por
      instalación: cada emulador o reinstalación genera uno nuevo.
   4. Mirar en la consola las métricas de App Check hasta ver tráfico **verificado**, y solo
      entonces activar el enforcement. En las callables v2 no se activa desde la consola:
      se añade `enforceAppCheck: true` a las opciones de cada `onCall` en
      `functions/src/index.ts` y se redespliega. Son **cinco**: `validateFoodImage` (l. 365),
      `awardAdXp` (979), `getLeagueId` (1013), `resolveVenue` (1295) y `deleteUserAccount`
      (1391). Empezar por `resolveVenue` y `validateFoodImage`, que son las que gastan cuota
      de Places y de Vision.

   Mientras tanto el log seguirá diciendo `{auth: VALID, app: MISSING}` en las llamadas sin
   token, que es lo esperado: en monitorización pasan igual.

   ⚠️ Ojo con `.gitignore`: la regla `release/` sin ruta ignoraba `app/src/release/` y dejaba
   el source set fuera del repo (arreglado en `e62223e`). Si en otro clon falla la release
   con "unresolved reference AppCheckInstaller", es esto.
3. ~~Borrar un plato no revierte el XP ni borra la imagen~~ — **desplegado y verificado el
   2026-08-17** (ver arriba). `revertAuthorXP()` descuenta 50 + 5 + 10 por valoración
   recibida **solo si el plato estaba `approved`**, más el XP de liga vía el sello del rating
   del autor. `deletePlateImage()` firma un `destroy` contra la API de Cloudinary, y **antes
   comprueba que ningún otro plato apunte a la misma `imageUrl`**: `imageUrl` la escribe el
   cliente, así que sin esa comprobación cualquiera podría publicar un plato con la foto de
   otro y borrarlo para destruírsela.
4. ~~`createdAt` del usuario se queda a 0~~ — **arreglado y en el AAB del 2026-08-17**.
   `AuthRepository` pasa `System.currentTimeMillis()` al crear el perfil.
5. ~~La regla `create` de `users` no tiene lista blanca de campos~~ — **desplegada el
   2026-08-17**. `users/create` lleva `keys().hasOnly([...])` con los 13 campos de `User` y
   `badges.size() == 0`. Ojo con la trampa: en la lista va **`premium`**, no `isPremium`, y
   añadir un campo al modelo sin añadirlo aquí rompe **todo** registro nuevo.

⚠️ Las reglas exigen `venueId`, `dishSlug` y que el id del plato sea `{venueId}__{dishSlug}`.
Cualquier APK anterior a `7bdf715` **no puede publicar platos** contra producción. Si el móvil
falla al publicar, lo primero es reinstalar la build actual.

---

## Estado actual

### Producción está VACÍA a propósito
0 platos, 0 ratings, 0 comments, 0 saves, 0 venues (comprobado el 2026-08-12). En `users`
hay **2 documentos**: las dos cuentas de Google del Redmi. El segundo lo creó la
verificación del 2026-08-12 y se deja a propósito, para que la sesión del móvil siga
iniciada. Se borraron los 129 platos sembrados (ninguno
era real) porque tenían geografía imposible ("París, Japón") y hacían que el perfil de
Sergio dijera "85 platos publicados" como si fuera un bot. La app queda con el empty
state honesto. **No volver a sembrar datos ficticios**: son puntuaciones inventadas en una
app cuyo valor es que las puntuaciones sean creíbles.

### Qué funciona
- Core loop: Google Sign-In → Discover → like/rate/save → publicar (Cloudinary) → Perfil
- XP/badges server-side (Cloud Functions + Admin SDK; el cliente no los escribe)
- Liga semanal por ciudad con clawback de XP si el plato se rechaza
- Moderación de imagen fail-closed (Vision en servidor)
- Billing implementado y **real** (el precio 2,99 €/mes lo devuelve Google Play)
- Borrado de cuenta (lo exige Play)

### Verificado en móvil real (Redmi Note 10S, Android 13) el 2026-08-09
Login con Google completo (logout + login, sin `DEVELOPER_ERROR`), push FCM entregadas
dos veces, token FCM conservado tras re-login, arranque sin crashes. Antes de esto el QA
solo se había hecho en emulador **sin cuenta de Google**, así que nada de esto estaba
comprobado.

### Cloud Functions (europe-west1)
`moderatePlateImage`, `onRatingCreated`, `onRatingUpdated`, `onNotificationCreated`,
`onReferralCreated`, `onPlateDeleted`, `onCommentCreated`, `onChallengeUpdated`,
`awardAdXp`, `deleteUserAccount`, `validateFoodImage`, `resolveVenue`.

---

## Bloqueantes de Play Store

### Código — resuelto
- `gma_ad_services_config`: existe (lo aporta el AAR de AdMob)
- Permisos de localización: se recuperaron **con uso real** (locales cercanos). Hay que
  declararlos en Data safety
- API keys en el binario: la de Vision se eliminó del APK (ahora la llamada la hace la CF
  `validateFoodImage` con service account) y **la clave se borró de Google Cloud**. La de
  Pexels nunca llegaba al binario: R8 elimina `MealDBSeeder` en release porque está tras
  `if (!BuildConfig.DEBUG) return`

### Falta — todo de Play Console, no de código
- ~~URL HTTPS pública de privacidad y términos~~ — **PUBLICADAS el 2026-08-18**, verificadas
  con HTTP 200 y UTF-8 correcto:
  - Privacidad: https://sdelapenya.github.io/FoodRanker/privacy.html
  - Términos: https://sdelapenya.github.io/FoodRanker/terms.html

  Viven en la rama **huérfana `gh-pages`** de `origin` (commit `37912ba`), servida por GitHub
  Pages desde `/(root)`. **No se usó `/docs` de main a propósito**: Pages habría publicado
  `docs/HANDOFF.md` como página web. Los textos son los de `PrivacyPolicyScreen.kt` y
  `TermsOfServiceScreen.kt` pasados a HTML **sin cambiar una palabra** — si se tocan los
  textos de la app, hay que tocar también el sitio, porque Play compara la política
  declarada con lo que hace la app. Para editarlo:
  `git worktree add <carpeta> gh-pages`, commit y `git push origin gh-pages`.

  ⚠️ Al rellenar **Data safety**, la política declara **ubicación, fotos, correo y datos de
  diagnóstico**: las cuatro categorías tienen que aparecer marcadas o Play rechaza por
  incoherencia con la política.
- Formulario de Data safety (email, fotos, ubicación, datos de uso)
- Ficha: capturas, descripciones, icono, gráfico destacado
- Cuestionario de clasificación de contenido
- Declaración de contenido generado por usuarios (hay moderación y reportes, hay que
  declararlos)

### SHA-1
| | SHA-1 |
|---|---|
| Debug | `00:56:2C:9F:18:0E:7F:6C:EB:03:BB:3F:7A:03:B5:CE:F9:82:34:C7` |
| Release (clave de carga, la del keystore local) | `27:78:23:4B:D8:97:89:FE:86:23:28:F4:F7:65:10:23:15:E1:1A:59` |
| **Firma de Play** (App Signing key, la que Google usa de verdad para lo que llega a los usuarios) | `B6:D0:BF:6D:59:E8:DC:52:2E:0D:AC:E8:1C:B7:16:05:BA:90:00:DF` |

⚠️ **Trampa real, no teórica** (encontrada el 2026-08-20 al probar el primer build subido a
prueba interna): al aceptar "Firma de aplicaciones de Play" en la creación de la app, Google
**vuelve a firmar** el AAB con una clave propia antes de repartirlo — el APK que le llega al
usuario final **no** lleva la huella del keystore local, sino esta tercera huella nueva. El
login de Google (Firebase Auth) falló en el Redmi con el build instalado desde Play real
hasta que se registró esta huella — con solo las dos primeras (debug + carga) no basta en
cuanto la app se distribuye por Play, aunque esas dos sigan haciendo falta para las builds
locales (`installDebug`, `bundleRelease` firmado a mano).

Se encuentra en **Play Console → Protegida con Play → Protección de Play Store → "Protege la
clave de firma de aplicación" → Gestiona la firma de aplicaciones de Play**, bloque "Clave de
firma de aplicación" → "Clave clásica" (no la "Clave criptográfica poscuántica", es para
otra cosa).

**Revisar si la clave de Places tiene el mismo problema**: la restricción por huella de la
clave de Android en Google Cloud Console (ver más abajo, "Falta de las huellas") solo tiene
las dos primeras huellas registradas a fecha de este hallazgo. Si la búsqueda de locales
falla en un build instalado desde Play real (aunque funcionara en local), es este mismo
patrón — añadir esta tercera huella a la restricción de la clave ahí también.

⚠️ **Sigue sin funcionar 24h+ después de registrar la huella** (2026-08-21): con la huella
verificada byte a byte en Firebase y en Google Cloud Console, la pantalla de consentimiento
OAuth en "En producción", y los datos de Google Play Services borrados del todo en el Redmi,
el login sigue fallando en el build de Play — instantáneo y silencioso, sin ningún error en
logcat. Aislado con una prueba concluyente: un APK de release firmado con el keystore local
(huella de carga, la de siempre) **funciona perfectamente** en el mismo momento y
dispositivo. Descarta cualquier problema de código; es propagación de Google más lenta de lo
normal, o algo sin pulir en el rollout de la firma dual "preparada para computación
cuántica (beta)" que aparece junto a la clave clásica en Play Console. Detalle completo en
memoria `project_play_signing_sha1.md`. Reintentar el login en el build de Play el
2026-08-23 (domingo) o después.

**Las tres están registradas en Firebase.** La de release y la de firma de Play se leyeron y
se dieron de alta con el CLI, sin pasar por la consola:

```
npx firebase apps:android:sha:list   1:350322634794:android:42b4b2e91a8df170c4d353
npx firebase apps:android:sha:create 1:350322634794:android:42b4b2e91a8df170c4d353 <SHA1>
npx firebase apps:sdkconfig ANDROID  1:350322634794:android:42b4b2e91a8df170c4d353 --out <fichero>
```

`app/google-services.json` se regeneró: añade un `oauth_client` nuevo para la huella de
release (`350322634794-eg19d602...`), y el diff es solo esas 8 líneas.

⚠️ **`app/google-services.json` está en `.gitignore`**, así que ese cambio vive **solo en este
PC**. El clon del servidor sigue con el fichero viejo y compilaría una release sin login.
Copiarlo a mano, o volver a bajarlo con `apps:sdkconfig`.

### Falta de las huellas — no se puede hacer desde aquí
1. ~~Restringir la clave de Places de Android a la huella de release~~ — **hecho y ahora
   verificado ejecutándolo**. Ya constaba en la Cloud Console el 2026-08-12 (la clave de
   Android tiene las **dos** filas `com.app.foodranker`, debug y release, y la restricción de
   API en Places API (New)), y ese mismo día la build de release **devolvió locales de verdad**
   en el paso 2 de AddPlate. `gcloud` no está instalado en el PC, así que la configuración de
   la clave no se puede leer desde la terminal; la prueba práctica es esta.
2. **Cuando Play genere su clave de App Signing** aparecerá una **tercera** huella. Hay que
   añadirla en Firebase (`apps:android:sha:create`) **y** en la clave de Places. Sin eso, la
   app que descargan los usuarios de Play no es la que tú firmaste y el login vuelve a
   romperse.

### AAB
Regenerado el **2026-08-20** desde el commit `e53d91c` (incluye el icono nuevo):
`app/build/outputs/bundle/release/app-release.aab`, 15,46 MB, `BUILD SUCCESSFUL in 4m 18s`.
`versionCode` sigue en **1** a propósito: ningún AAB anterior llegó a subirse a Play, así
que no hay nada que superar. Keystore y credenciales en `local.properties` (fuera de git).
El AAB del 2026-08-18 (`e62223e`, App Check) queda superado por este — llevaba el icono
viejo (plato + estrella genérico).

Verificado, no supuesto:

- `jarsigner -verify` → `jar verified`. Los avisos de "certificate chain is invalid" y
  "self-signed" son los normales de una clave de subida propia, no un problema.
  `jarsigner` no está en el PATH: vive en
  `C:\Program Files\Android\Android Studio\jbr\bin\jarsigner.exe`.
- **Los textos legales llegan al binario**: se extrajo el AAB y se buscaron cadenas en
  `classes.dex`, que es la única forma fiable (ver "Trampas": leer `BuildConfig.java` no
  dice qué entra). Salen `Ley aplicable` y `Google Cloud Vision`, apartados que solo existen
  en la reescritura. Ojo al buscar: hay que respetar los acentos del fuente
  (`Suscripción`, no `Suscripcion`), o no encuentra nada aunque esté.
- **App Check llega al binario**: no sirve buscar `AppCheckInstaller` (R8 ofusca los nombres
  de clase propios en release), pero las cadenas del SDK de Firebase, que R8 no toca, salen
  en `classes2.dex` y en `AndroidManifest.xml`: `firebaseappcheck.googleapis.com`,
  `PlayIntegrity`, `play.core.integrity`.

Ojo con `local.properties`: los valores llevan **escapes de Java** (`E\:\\FoodRanker\\...`).
Al leerlo desde PowerShell hay que desescapar `\:` y `\\` o la ruta del keystore no resuelve.

---

## No abrir de nuevo (ya decidido)

- **No sembrar datos ficticios** — ver arriba
- **No anunciar Premium que no existe**: `isPremium` solo controla `BannerAd` y el badge.
  La lista de beneficios se recortó a los dos reales
- **No aplicar el stub "Próximamente" a Premium**: el Billing es real y funciona
- `ChallengeViewModel.participate()` está eliminado a propósito (era farming de XP sin
  publicar). El XP del reto lo concede `onChallengeUpdated` al publicar
- `isPremium`/`xp`/`level`/`badges`: solo Admin SDK. Nunca desde cliente
- `ratings` tiene `allow delete: if false`

---

## Producto — valoración honesta (2026-08-09)

Lo bueno: la ingeniería está por encima de la media indie (autoridad server-side,
moderación fail-closed, borrado en cascada, auditorías de seguridad pasadas) y el diseño
visual es coherente y comercial.

El riesgo real no es la calidad, es la **densidad**: una app de rankings no vale nada sin
masa crítica *en una ciudad*. Con un usuario, la Liga está vacía y el ranking no significa
nada. Y la gamificación (XP, niveles, badges, ligas, misiones, rachas, referidos, premium)
va por delante del valor probado: es un stack de retención completo sobre un bucle que
ningún usuario real ha validado.

El ángulo con potencial es el que ya está elegido: rankear **platos**, no restaurantes.
Google Maps tiene ganado "¿es bueno este restaurante?"; nadie ha resuelto "¿qué pido
aquí?". Por eso la identidad canónica plato+local no es un refactor cosmético: es la pieza
que hace que la idea funcione.

Recomendación: **City MVP** — un barrio, contenido real, veinte personas. Si esas veinte
vuelven, hay algo. No retrasarlo puliendo más funcionalidad; lo que falta no es código.

---

## Trampas conocidas (ahorran tiempo)

- **El asistente de "Seguridad de los datos" en Play Console NO autoguarda entre pasos.**
  Un F5 a media tarea borra todo lo tecleado desde el último "Guardar" o "Guardar como
  borrador" y el asistente vuelve al principio. Guardar como borrador **al terminar cada
  paso** (Tipos de datos, Uso y gestión de datos), no solo al final del todo
- **ADB solo funciona en PowerShell**, no en Git Bash. Y Git Bash convierte `/sdcard/...`
  en rutas de Windows: usar `//sdcard/...` o hacerlo desde PowerShell
- **`local.properties`**: no añadir líneas con `Add-Content` sin salto previo. Ya pasó una
  vez que `PLACES_API_KEY=` quedó pegado a `KEY_PASSWORD` y corrompió la contraseña del
  keystore. Usar Edit sobre una línea existente
- **Reglas de `plates`**: tienen lista blanca explícita de campos. Añadir un campo nuevo
  sin tocarla hace fallar **toda** creación de platos
- **R8 y BuildConfig**: leer el `BuildConfig.java` generado NO dice qué llega al binario.
  Para saberlo, extraer el AAB y buscar en los `.dex`
- **Emulador en frío**: da ANR por `lowmemorykiller` al arrancar mientras GMS y Play Store
  se actualizan. No es un bug de la app — comprobar `logcat | grep lowmemorykiller` antes
  de investigar
- **Selector de fotos de MIUI**: hay que confirmar con "Hecho", no basta tocar la foto
- **MIUI bloquea las instalaciones NUEVAS por ADB** (`INSTALL_FAILED_USER_RESTRICTED`), pero
  deja pasar las **actualizaciones** del mismo paquete y firma. Por eso `gradlew installDebug`
  funciona y meter la release (otra firma, tras desinstalar) no. No es problema de ruta:
  se probó también `pm install` desde `/data/local/tmp`, con `-i com.android.vending` y con
  `--user 0`, y da el mismo error. Solución: instalarlo a mano desde Archivos → Descargas,
  o activar *Ajustes → Ajustes adicionales → Opciones de desarrollador → Instalar vía USB*
  (Xiaomi suele exigir cuenta Mi y SIM con datos para dejar activarlo)
- **AdMob**: el Redmi está registrado como dispositivo de prueba vía
  `ADMOB_TEST_DEVICE_IDS` en `local.properties`. Pulsar anuncios reales propios es tráfico
  inválido y suspende cuentas
- **fail2ban en el servidor**: reintentar SSH en bucle banea la IP del PC y **tira las
  sesiones remotas de Cursor**. Solo jail `sshd`, puerto 22
- **VS Code no conecta al servidor con `terminalRemoteResolver`** (2026-08-17): pasa cuando
  Remote-SSH está en canal **pre-release**. VS Code estable cruza los `enabledApiProposals`
  de la extensión con una lista blanca fija de su `product.json`, y el error muestra **la
  lista blanca**, no lo que declara la extensión — parece que falta la declaración cuando sí
  está. `--enable-proposed-api` en `argv.json` **no** lo arregla. Solución: volver a la
  estable y dejarla fijada (`code --install-extension ms-vscode-remote.remote-ssh@0.124.0
  --force`, que deja `pinned=True`). Ojo: el `code` del PATH es **Cursor**; hay que usar la
  ruta larga de VS Code
- **Capturas de pantalla por ADB**: `adb exec-out screencap -p > f.png` **corrompe el PNG** en
  PowerShell (le mete BOM y recodifica). Hacer `adb shell screencap -p /sdcard/s.png` y
  `adb pull`
- **`adb` no está en el PATH**: vive en `C:\Users\User\AppData\Local\Android\Sdk\platform-tools`
- **Al pilotar la app por ADB, el teclado tapa la mitad inferior**. Un `input tap` sobre una
  categoría acaba escribiendo una letra en el campo de texto. Cerrar el teclado con
  `input keyevent 4` antes de tocar nada de abajo
- **`input keyevent 4` solo cierra el teclado si el teclado está abierto**; si no, es un
  "atrás" normal y te saca de la pantalla, perdiendo la foto ya elegida en AddPlate.
  Comprobar antes con `dumpsys input_method | Select-String mInputShown`
- **`input text "dos palabras"` se corta en el primer espacio**: se escribió solo
  "Espaguetis" de "Espaguetis de prueba". Usar `%s` en vez de espacios
- **No adivinar coordenadas de una captura: leer `uiautomator dump`.** El FAB central "Subir"
  no está en el centro de la pantalla, está en **x≈401** (el `<node>` de "Subir" sale con
  `bounds=[0,0][0,0]`, pero su contenedor pulsable es `[275,2050][528,2177]`). Se perdieron
  varios taps buscándolo en x=540
- **El `uiautomator dump` se corta en y=2177** aunque la pantalla tenga 2400 px. Los botones
  de la franja inferior (el "Hecho" del photopicker, los tabs) aparecen con la `y` recortada:
  hay que tocar ~25 px por debajo del `bounds` que dice el dump
- **Para probar el paso 2 de AddPlate hace falta una foto que Vision acepte** (la moderación
  es fail-closed y rechaza lo que no sea comida). En vez de ir probando las fotos personales
  del móvil, bajar una con la `PEXELS_API_KEY` de `local.properties`, `adb push` a
  `/sdcard/Pictures/`, `MEDIA_SCANNER_SCAN_FILE`, y borrarla al terminar. Wikimedia devuelve
  **429** a este tipo de descargas, no perder tiempo con ella
- **Una imagen borrada de Cloudinary sigue devolviendo 200 un buen rato**: la sirve la caché
  del CDN. Se perdió tiempo dando por fallido un `destroy` que había respondido `ok`. Para
  comprobarlo de verdad, pedir la URL con un parámetro cualquiera
  (`...png?cb=<algo-aleatorio>`), que cambia la clave de caché y va al origen → 404
- **`firebase functions:log` pagina de forma engañosa**: `--only <fn> -n 30` devolvió
  entradas de **8 días antes** y ninguna de hacía 2 minutos. Para mirar logs recientes de
  verdad, ir a la API de Cloud Logging (`POST logging.googleapis.com/v2/entries:list`, filtro
  `resource.labels.service_name="<nombre en minúsculas>"` + `timestamp>=...`, `orderBy:
  "timestamp desc"`). El token se saca igual que para las reglas. Ojo con el filtro de
  tiempo: hay que trabajar en **UTC**, y aquí se puso una hora que aún no había llegado, con
  lo que salió "sin entradas" pareciendo que la función no se había ejecutado
- **`curl -u` no existe en PowerShell**: `curl` es alias de `Invoke-WebRequest`. Hay que
  llamar a `curl.exe` con la extensión
- **`firebase functions:secrets:set` desde fuera del repo** falla con "No currently active
  project": el proyecto sale del `.firebaserc`, así que hay que estar en `e:\FoodRanker` (o
  pasar `--project foodranker-51270`)
- **`jarsigner` no está en el PATH**: vive en
  `C:\Program Files\Android\Android Studio\jbr\bin\jarsigner.exe`
- **Leer las reglas realmente desplegadas** (no fiarse del fichero local) con la API de
  Firebase Rules: `GET firebaserules.googleapis.com/v1/projects/<proj>/releases/cloud.firestore`
  → `rulesetName`, y luego `GET /v1/<rulesetName>` trae el contenido. El access token se saca
  del refresh_token de `firebase-tools.json` contra `oauth2.googleapis.com/token`
- **En PowerShell, `$env:VAR` NO persiste entre llamadas de herramienta**: cada comando abre
  un shell nuevo. Un token hay que pedirlo y usarlo en la **misma** invocación
- **El analizador de comandos bloquea `-split '/'`** (lo lee como una ruta a borrar). Para
  partir rutas usar otra cosa, o imprimir el `name` completo
- **`firebase deploy` falla con "User code failed to load. Timeout after 10000"**: casi
  nunca es el código. El CLI arranca el módulo y le pide la especificación por HTTP, y en
  Windows esos 10 s se quedan cortos. Comprobar primero que carga
  (`node -e "require('./lib/index.js')"`) y desplegar con `$env:FUNCTIONS_DISCOVERY_TIMEOUT="120"`
- **Comandos del proyecto: PowerShell en el PC, no el bash del servidor.** Ya pasó pegar
  `cd e:\FoodRanker\functions` en la sesión SSH: bash se come las barras invertidas
  (`e:FoodRankerfunctions`) y el `npx` acaba corriendo donde no hay `firebase-tools`
- **Scripts Admin SDK**: ADC temporal con el refresh_token de
  `~/.config/configstore/firebase-tools.json`, client_id
  `563584335869-fgrhgmd47bqnekij5i8b5pr03ho849e6.apps.googleusercontent.com`, secret
  `j9iVZfS8kkCEFUPaAeJV0sAi`. Escribir el JSON **sin BOM**
  (`New-Object System.Text.UTF8Encoding $false`) y borrarlo al terminar.
  No sirve para `createCustomToken` (necesita service account) y el login por
  email/password está deshabilitado en el proyecto
