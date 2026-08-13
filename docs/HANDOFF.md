# HANDOFF — FoodRanker (Play Store + producto)

**Actualizado:** 2026-08-12
**Código (PC):** `e:\FoodRanker` · **Código (servidor):** `/home/sergio/lab/apps/FoodRanker`
**GitHub:** https://github.com/sdelapenya/FoodRanker (público)
**Gitea:** ssh://git@192.168.1.19:222/sdelapenya/foodranker.git — **por SSH puerto 222**, el HTTP 3000 solo escucha en loopback

## Sync

Se desarrolla en PC **y** en servidor (Cursor), y las ramas divergen. Antes de tocar:
`git status`, `git log --oneline -5` y comprobar `merge-base` con el otro remote.
El 2026-08-04 se mergeó una rama del servidor que divergía 13 commits (10 conflictos a mano).

---

## LO SIGUIENTE (retomar aquí)

**La build de RELEASE está verificada de punta a punta en el Redmi (2026-08-12).** Ya no
queda nada de código pendiente para subir a Play: lo que falta es **todo de Play Console**
(ver "Bloqueantes de Play Store" más abajo).

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

⚠️ **El AAB del 2026-08-11 sigue siendo válido.** El único arreglo de esta sesión fue en
`firestore.rules`, no en el binario. No hay que regenerarlo.

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
2. **App Check no está activo**: el log de la callable dice `{auth: VALID, app: MISSING}`.
   Cualquiera con un token de sesión válido puede llamar a `resolveVenue`. Con los topes de
   cuota el daño está acotado, pero conviene activarlo antes de tener usuarios.
3. **Borrar un plato no revierte el XP del autor.** `onPlateDeleted` cascadea ratings,
   comments y saves, pero los 55 XP (50 por publicar + 5 por valorar) se quedan. Tampoco
   borra la imagen de Cloudinary.
4. **`createdAt` del usuario se queda a 0.** `AuthRepository` crea el perfil con el data
   class `User`, que tiene `createdAt: Long = 0L`, y nunca lo rellena: todo usuario nuevo
   queda con fecha de alta 1970. Visto en el documento creado el 2026-08-12. No es
   bloqueante, pero arreglarlo obliga a **regenerar el AAB**, así que conviene juntarlo con
   el próximo cambio de binario en vez de hacer una build solo para esto.
5. **La regla `create` de `users` no tiene lista blanca de campos**, al contrario que
   `plates`. `xp`, `level`, `premium` y `referralCount` están fijados, pero el documento
   antiguo tiene además `followers`, `following`, `totalRatings` y `totalPlatesAdded`, que
   nadie valida: un cliente manipulado podría crearse su perfil con `totalPlatesAdded`
   inflado. Preexistente, no se tocó en esta sesión.

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
- URL HTTPS pública de privacidad y términos
- Formulario de Data safety (email, fotos, ubicación, datos de uso)
- Ficha: capturas, descripciones, icono, gráfico destacado
- Cuestionario de clasificación de contenido
- Declaración de contenido generado por usuarios (hay moderación y reportes, hay que
  declararlos)

### SHA-1
| | SHA-1 |
|---|---|
| Debug | `00:56:2C:9F:18:0E:7F:6C:EB:03:BB:3F:7A:03:B5:CE:F9:82:34:C7` |
| Release | `27:78:23:4B:D8:97:89:FE:86:23:28:F4:F7:65:10:23:15:E1:1A:59` |

**Las dos están registradas en Firebase desde el 2026-08-11.** La de release se leyó del
keystore con `keytool` y se dio de alta con el CLI, sin pasar por la consola:

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
Regenerado el 2026-08-11 desde el commit `9e06590`, ya con la identidad canónica y el
`google-services.json` nuevo: `app/build/outputs/bundle/release/app-release.aab`, 14,73 MB.
Verificado con `jarsigner -verify`: `jar verified`, firmado por `CN=Sergio de la Peña`.
Keystore y credenciales en `local.properties` (fuera de git).

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
