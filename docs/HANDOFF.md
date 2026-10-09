# HANDOFF — FoodRanker (Play Store + producto)

**Actualizado:** 2026-10-09
**Código (PC):** `e:\FoodRanker` · **Código (servidor):** `/home/sergio/lab/apps/FoodRanker`
**GitHub:** https://github.com/sdelapenya/FoodRanker (público)
**Gitea:** ssh://git@192.168.1.19:222/sdelapenya/foodranker.git — **por SSH puerto 222**, el HTTP 3000 solo escucha en loopback

## Sync

Se desarrolla en PC **y** en servidor (Cursor), y las ramas divergen. Antes de tocar:
`git status`, `git log --oneline -5` y comprobar `merge-base` con el otro remote.
El 2026-08-04 se mergeó una rama del servidor que divergía 13 commits (10 conflictos a mano).

---

## LO SIGUIENTE (retomar aquí)

### ✅ SEGUNDA TANDA EN EL REDMI: cinco textos más, y las 3 pantallas que faltaban (2026-10-09)

Commit `33c0d79`. Probado con `sdelapenya1991@` (0 seguidores), para poder tocar sin que le
llegue nada a nadie.

**Las tres pantallas que quedaban sin abrir** — "Qué pido aquí", lista de seguidores e
invitar amigos — **abren sin crash en inglés**. Con eso, los nueve ViewModels a los que se
les inyectó `@ApplicationContext` están ejercitados en el móvil.

**Cinco textos más, todos encontrados mirando la pantalla:**
- "Qué pido aquí" **mentía**: tras buscar decía "tampoco hemos encontrado locales cerca"
  **con la lista de locales justo debajo**. La condición era de dos casos y hacen falta
  tres. Nuevo `nrb_empty_pick`.
- El título de la lista de seguidores salía **"Seguidores" con la app en inglés**: lo ponía
  `titleFor()` en el companion object del ViewModel. Ahora es un id de recurso en el estado.
- El título de la ficha mientras carga decía **"Detalle"**; la hoja de editar plato
  **"Editar <nombre>"** concatenando; la pestaña del ranking **"Top"** a pelo.
- El nombre de respaldo era **"Usuario" en ocho sitios**. ⚠️ Ese **se persiste** en
  `addedByUserName`/`userName`, así que queda en el idioma de quien escribe, igual que el
  país del local. `AuthRepository` no tenía contexto: se le inyecta.

#### ⚠️ Por qué se colaron los cinco, y los tres de antes

**Palabras SUELTAS en castellano.** `scan_es.py` descartaba de golpe cualquier literal sin
espacios, porque casi todos son identificadores. Ahora conserva el que parece texto y no
código: **inicial mayúscula y resto minúsculas**, lo que deja fuera `UNCHECKED_CAST`,
`MissingPermission` y `cardScale`. Con esa sola regla aparecieron estos cinco.

Y antes, "Platos publicados" se coló por ir de **argumento posicional** de un composable
propio; `scan_ui.py` mira ahora también esa posición, exigiendo que el texto lleve un
espacio o un `
` para no ahogarse en rutas y nombres de animación.

❗ **La lección, por si sirve en la próxima sesión:** las comprobaciones automáticas
verifican que la clave exista en los dos idiomas y que los marcadores cuadren — **no que el
inglés esté en inglés**. Todo lo encontrado en el móvil salió de mirar pantallas, no de
compilar. Media hora con el móvil vale más que otra vuelta de scripts.

#### ⚠️ Dos trampas del móvil

- **MIUI desactiva "Instalar vía USB" cada cierto tiempo**: funcionó por la mañana y por la
  tarde volvió `INSTALL_FAILED_USER_RESTRICTED`. Hay que reactivarlo.
- **Al crear un local, la app manda el idioma del móvil a Places.** El documento del local
  es único y compartido, así que **publicar con la app en inglés dejaría "Spain" para
  todos**. Si se prueba publicar, hacerlo con la app en castellano.

#### Lo que sigue sin probarse

El **envío real** de un plato. La pantalla se recorrió entera en los dos idiomas, pero el
botón se protege solo con un aviso en línea, así que los mensajes de error de
`AddPlateViewModel` (`vm_name_required` y compañía) son defensivos y no se pueden disparar
sin publicar. Están en el APK en los dos idiomas y son `getString` de una línea.

---

### ✅ PROBADO EN EL REDMI REAL, CON SESIÓN Y EN LOS DOS IDIOMAS (2026-10-09)

Lo que quedaba sin probar ya está probado. Redmi Note 10S (Android 13), APK de release
v17 instalado por USB, sesión de Google real (`sergiodelapenya1991@`, 2 seguidores, 8
platos, 410 XP), sobre los datos de producción (50 platos, 12 usuarios, 58 valoraciones).

⚠️ **MIUI bloquea `adb install`** con `INSTALL_FAILED_USER_RESTRICTED` hasta activar
**Instalar vía USB** y **Depuración USB (ajustes de seguridad)** en opciones de
desarrollador. Alternativa si se resiste: `adb push` del APK a `/sdcard/Download` e
instalarlo tocando el fichero. Y hay que **desinstalar la de Play primero**: la firma
local no coincide con la de Play.

#### Lo que se confirmó

- **La liga abre sin reventar**, en los dos idiomas. Era el crash que habría llevado el
  primer AAB de la v17: con el código anterior, tocar "Liga" petaba.
- **Un aviso llega UNA sola vez**, con la app en primer plano, que es cuando dibujan los
  dos caminos. Comprobado con un aviso de prueba creado con el Admin SDK sobre la propia
  cuenta y contando en `dumpsys notification`: en inglés `❤️ New like / Prueba FoodRanker
  liked "Tortilla de prueba"` y en castellano `❤️ Nuevo me gusta / ... le ha dado like a
  "Tortilla de prueba"`. Con comillas rectas, idénticas a las del servidor. Documento de
  prueba borrado después.
- **`saveCurrentLanguage()` funciona en producción**, verificado por primera vez: el campo
  `language` pasó a `"es"` al entrar y a `"en"` al cambiar el idioma de la app. ⚠️ Hasta
  que se publique la v17, **nadie tiene ese campo**, así que todos los push salen en
  castellano por el respaldo.
- **El formato numérico sigue al idioma**: `★ 9,9` y `25,00 €` en castellano, `★ 9.9` y
  `25.00 €` en inglés. Y el **€ se mantiene en inglés** porque sale del país del local, no
  del idioma: era el objetivo de la fase 0.
- **Premium en inglés dice "⭐ Subscribe", sin precio**, que es el arreglo: antes habría
  puesto "2,99 €/mes" a alguien en Reino Unido.
- La hoja de editar plato pide **"Descripción" / "Description"**, no "Bio".
- Plurales, estados vacíos con la consulta interpolada, pestañas con contador
  ("Mis platos (8)"), la fila **Idioma** presente (Android 13), y ninguna de las cinco
  cadenas que borraba el reductor provocó un `NotFoundException`.

#### ⚠️ El fallo que solo se ve mirando la pantalla

La cabecera de la liga salía **"Termina en2d 10:23:59· Reinicia cada lunes"**, pegada.
**Android RECORTA los espacios del principio y el final de un `<string>`**, y
`lg_ends_in` / `lg_restart` confiaban en ellos. Se arregla **entrecomillando el valor**
(`"Termina en "`), que es como se le dice a aapt que los respete. Venía de la fase 1.
Ninguna comprobación lo cazaba porque los recursos existen y encajan; ahora
`check_strings.py` lo detecta.

#### Lo que sigue sin probarse

Publicar un plato de verdad: la cuenta tiene **2 seguidores reales** y publicar les
mandaría un aviso. Se recorrió la pantalla sin enviar. Con una cuenta sin seguidores
(`sdelapenya1991@`, 0 seguidores) se podría cerrar ese hueco.
Tampoco se puede probar aquí el **precio real de Play** (el sideload no tiene billing) ni
el **reparto por idioma del AAB**, que solo se ve instalando desde Play.

---

### ✅ CERRADO LO QUE QUEDABA DE CÓDIGO (2026-10-09)

**85 claves muertas fuera.** Eran de la nomenclatura anterior (`error_*`, `empty_*`,
`league_*`, `action_*`, `profile_*`, `premium_*`) más la `prof_bio` que quedó huérfana al
separar la descripción de plato de la biografía. Importaba más de lo normal porque con el
reductor de recursos apagado **ya viajaban en el APK**. No se usó la lista de lint:
`tools/i18n/find_unused.py` busca cada clave en todas las formas en que se puede referenciar
(`R.string.x`, `R.plurals.x`, `@string/x`, `@plurals/x`) por el fuente, los recursos y el
manifest, y respeta una lista de intocables que lee el SDK por nombre
(`default_web_client_id` y compañía). Quedan 579 claves propias y **cero sin referencia**.

**`lintDebug` pasa limpio, por primera vez.** `BannerAd.kt` casteaba `LocalContext` a
`ComponentActivity`, que es el error `ContextCastToActivity` que venía de `b03790a`: un
`Context` no siempre es una Activity. Ahora usa `LocalActivity.current` (disponible desde
activity-compose 1.10, el proyecto va con la 1.12.1). El ViewModel se le sigue pidiendo a la
Activity **a propósito**: Premium es uno para toda la app, y `hiltViewModel()` sin argumento
daría uno por pantalla.

#### ✅ Resuelta la duda del reductor en las versiones ya publicadas

Quedó apuntado que había que mirar Crashlytics por si el reductor se había llevado algún
recurso en la v15 o la v16. **No hace falta: se comprobó directamente.** Se compiló la v16
exacta (`427d945`) en un worktree aparte, con el reductor encendido como se publicó, y se
cruzó lo que su código pide contra lo que su APK trae.

**La v16 define 70 cadenas pero su código solo pide dos**: `app_name` (desde el manifest) y
`default_web_client_id` (desde `AuthRepository`). Las dos están. Hasta la fase 1 el texto iba
clavado en Kotlin, así que **el reductor nunca tuvo nada que llevarse**. El riesgo nació con
el paso a recursos y se cogió antes de publicar. Tema cerrado.

#### Estado de la verificación automática

Sobre el APK de release ya minificado: **579 de 579 claves propias presentes en los dos
idiomas**. Más paridad de marcadores, argumentos suficientes, los 22 avisos idénticos a los
del servidor, la ficha de Play dentro de los límites, cero claves sin usar, cero literales
sin extraer, `lintDebug` sin errores y los tests en verde.

Las pantallas de dentro ya **se probaron en el Redmi real** (ver más arriba).

---

### 🟥 REPASO DE LA FASE 2: dos fallos propios encontrados y arreglados (2026-10-09)

Al releer el diff con calma salieron **dos cosas que yo mismo había roto** y que ya estaban
dentro del primer AAB de la v17. El AAB se ha regenerado.

#### 1. La pantalla de liga reventaba al abrirla

```kotlin
parts[1].takeIf { parts.size == 2 }   // MAL
```

**El receptor de `takeIf` se evalua ANTES que su condicion.** `weekKey` empieza vacía (su
valor inicial en `LeagueUiState`) y la cabecera se pinta antes de que cargue la liga, así
que `"".split("-W")` daba una lista de un elemento y `parts[1]` lanzaba
`IndexOutOfBoundsException`. **Era un crash garantizado, no un caso raro.** El código
original compraba el tamaño primero; al pasarlo a recurso me comí esa comprobación.

Arreglado poniendo el `takeIf` sobre la lista, que es el patrón correcto y el que ya usaba
`DeepLinkParser`:

```kotlin
weekKey.split("-W").takeIf { it.size == 2 }?.get(1)?.takeIf { it.isNotBlank() }
```

La lógica sale del composable a `numeroDeSemana()` y tiene cinco pruebas
(`NumeroDeSemanaTest`). El primer caso, la clave vacía, es exactamente el que fallaba.
⚠️ **Esto no lo pilló ninguna de las comprobaciones de idiomas, porque no es un problema de
idiomas**: era lógica cambiada de paso. Las pantallas de dentro necesitan sesión de Google
y no se pudieron probar en el emulador, así que lo que queda sin test sigue sin red.

#### 2. Cada like y cada comentario se habrían visto DOS veces

Esta es la menos evidente de toda la sesión. **El id de la notificación sale del
`hashCode()` del propio texto** (`NotificationHelper.show`), a propósito: un aviso llega por
el push que redacta Cloud Functions **y** por el escuchador de Firestore que redacta la app,
y en primer plano **los dos dibujan**. Coinciden en el id solo si redactan igual, y entonces
el segundo reemplaza al primero. Al mover esos textos a recursos les cambié las comillas
rectas del servidor por tipográficas (`« »`), y con eso el hash ya no coincide: dos
notificaciones por cada like, comentario, valoración, plato nuevo y resultado de moderación.

Los 22 textos del cliente están ahora alineados **al carácter** con `TEXTOS` de
`functions/src/index.ts`, y lo vigila `tools/i18n/check_notif_parity.py` (probado al revés:
con una coma de más, falla). ⚠️ **Una `"` sin escapar en `strings.xml` se la come aapt**
(abre un tramo literal), así que van como `\"`.

De paso se arreglaron **dos divergencias que venían de antes, no de esta sesión**: el
cliente decía "Ganaste la liga semanal." y el servidor "Ganaste la liga semanal. Nueva
semana, a defender el puesto."; y en el otro caso el cliente se dejaba el puesto. O sea que
**un resultado de liga ya salía duplicado en la v16**, solo que la liga cierra los lunes y
nadie lo había reportado todavía. `notif_league_b_other` lleva ahora el puesto como
argumento.

#### Lo que se revisó y estaba bien

El orden de los nueve argumentos de `share_plate_text`; los dos estados vacíos de Explorar;
las tres claves de perfil que estaban compartidas entre sitios distintos; `DeepLinkParser`
(ya usaba el patrón correcto); y que los nombres de nivel y logro del aviso salen del campo
que escribe el servidor en los dos caminos, así que ahí no hay duplicado posible.

---

### 🟢 v17 (1.4) AAB GENERADO Y FIRMADO — listo para subir (2026-10-09)

`app/build/outputs/bundle/release/app-release.aab`, copiado también a `release/`.
**versionCode 17, versionName 1.4**, 15,22 MB, firmado con `foodranker-keystore.jks`.

⚠️ **Corrección de lo que decía este documento:** ponía que la v16 se había generado pero
no subido. **La v16 (1.3) SÍ está subida**, confirmado por Sergio en Play Console, así que el
versionCode 16 estaba quemado y de ahí el salto a 17. Play rechaza un AAB con un versionCode
ya usado aunque esa versión esté archivada o no se publicara nunca: **antes de generar,
mirar el versionCode más alto subido, no fiarse de este fichero.**

**Lo que lleva respecto a la v16**: las fases 1 y 2 de idiomas (la app entera en inglés y
castellano, avisos incluidos), la moneda y el enlace de reservas por país del local, el
selector de idioma, y los dos textos legales en los dos idiomas.

Texto de la ficha de Play y de las novedades, en los dos idiomas: `docs/play-store-listing.md`.
Hay que pegarlo en Play Console (→ Crecimiento → Presencia en Play Store), que es trabajo de
consola, no de código. **Las novedades también son por idioma**: si solo se rellena la del
idioma por defecto, a quien tenga el móvil en inglés le llegan en castellano.

#### ⚠️ Dos ajustes de empaquetado, los dos necesarios

**1. `bundle { language { enableSplit = false } }`.** Play reparte el AAB en trozos e instala
solo el idioma del móvil. Con un selector de idioma eso se rompe: quien tenga el móvil en
castellano y ponga la app en inglés no tendría esos recursos instalados. Lo recomienda la
documentación de "Per-app language preferences" de Android. Verificado decodificando
`BundleConfig.pb` del AAB: la dimensión LANGUAGE sale con `negate=true` (ABI sigue partido,
que eso sí interesa).

**2. `isShrinkResources = false`.** ⚠️ **Esto era un fallo real a punto de publicarse.** El
reductor de recursos estaba **borrando cinco cadenas que el código sí pide**: `cd_like`,
`cd_unlike`, `vm_saved`, `vm_unsaved` y `vm_save_fail`. Aparecían en su lista de "Unused
resources" de `app/build/outputs/mapping/release/resources.txt` **aunque las clases
compiladas de release leen esos campos de `R`** (comprobado con `javap`), y desaparecían de
`resources.arsc`. `cd_like` lo pinta **cada tarjeta de plato**, así que era un
`Resources.NotFoundException` en la pantalla principal, **solo en release y nunca en debug**.
No se encontró la causa de por qué su análisis no las ve. Coste de apagarlo: 8,12 → 8,49 MB,
**370 KB**, a cambio de no tener que adivinar cuál se llevará la próxima vez.

Las versiones anteriores se compilaron con el reductor encendido, pero **ya se comprobó que
ahí no rompió nada** (ver "Resuelta la duda del reductor" más arriba): la v16 solo pedía dos
cadenas y las dos estaban.

#### Cómo se verificó el artefacto

Sobre el APK de release ya minificado, no sobre el fuente:
- Las **211 claves** de la fase 2, presentes en los dos idiomas (211 de 211).
- **899 recursos de texto** con las dos variantes, ninguno nuestro suelto.
- versionCode 17 / versionName 1.4 en el manifest final; AAB firmado (`KEY0.RSA`).
- `./gradlew assembleDebug testDebugUnitTest` en verde.

El error de lint de `BannerAd.kt` ya está arreglado (ver más arriba): `lintDebug` pasa
limpio.

**Retomar aquí:** subir el AAB al canal de prueba cerrada, pegar la ficha en inglés y las
novedades de los dos idiomas, y rehacer las tres capturas con la app en inglés (necesitan
sesión de Google y contenido real, ver `docs/play-store-listing.md`).

---

### 🔵 IDIOMAS: FASE 2 COMPLETA — nada visible queda clavado (2026-10-09)

Commit `e234005`. Va en el AAB de la **v17 (1.4)**, ya generado (más arriba).

La fase 1 dejó las pantallas, pero no el texto que no vive en ellas. Un barrido del código
encontró **170 cadenas visibles todavía en castellano**. Con los 212 recursos nuevos de esta
fase son **659 claves**, todas en los dos idiomas (la única suelta es `default_web_client_id`,
que es la credencial de OAuth) más 6 plurales.

Lo traducido aquí: avisos de la campana, canales de notificación, recordatorio diario,
mensajes de los nueve ViewModels, textos para compartir (plato, perfil, invitación), explorar,
perfil, invitar, premium, liga, seguidores, tendencias y los dos textos legales.

**Las notificaciones tenían dos redacciones.** El mismo aviso llega por FCM (lo redacta Cloud
Functions) o por el listener de Firestore (lo redacta la app), y el del cliente seguía en
castellano para todos. Los textos del cliente replican ahora la tabla `TEXTOS` de
`functions/src/index.ts`. ⚠️ **Al añadir un tipo de aviso hay que tocar los dos sitios**, o un
comentario se lee de dos maneras según por dónde entre.

**Nueve ViewModels** no podían leer recursos: se les inyecta `@ApplicationContext`, como ya
hacían `AddPlateViewModel` y `ErrorMapper`. Se descartó mover los mensajes al estado como ids
de recurso: obligaba a cambiar el tipo del estado y todos sus consumidores para lo mismo.

#### Tres cosas que estaban mal, no solo sin traducir

- **El título de los Términos seguía en castellano en los dos idiomas.** El script de la
  sesión anterior buscaba "Términos de Uso" y el texto real era "Términos de Servicio": el
  reemplazo no encajó y no avisó. ⚠️ De ahí la regla nueva: **toda sustitución automática
  tiene que fallar si no encaja exactamente una vez**.
- **El precio de Premium por defecto era "2,99 €/mes"** — lo que veía alguien en Reino Unido
  mientras Play no respondía, y para siempre si no responde. Y al precio que sí da Play, que
  ya viene en su moneda y su formato, se le pegaba "/mes" a mano. Sin precio real el botón ya
  no inventa ninguno (`prm_subscribe_plain`).
- **La ficha de plato pedía la "Bio" del plato en inglés**: reutilizaba `prof_bio`, la clave
  del perfil. ⚠️ Tres claves estaban compartidas entre sitios que dicen cosas distintas
  (`prof_bio`, `prof_bio_hint`, `prof_delete_detail`). **Antes de reutilizar una clave, mirar
  dónde se usa ya.**

#### ⚠️ Trampas nuevas de esta fase

- **Un `%` literal en una cadena que se pide SIN argumentos no se puede escapar.** "100%
  seguro" lo marcaba lint como marcador de formato roto, y `%%` se habría visto literal
  porque `getString(id)` sin argumentos no formatea. Se reformuló el texto.
- **El separador decimal y el de miles cambian de idioma**: el rango de precio pasa a ser una
  cadena con argumento (`det_price_range`), no una concatenación.
- **El castellano tiene la cantidad `many` en CLDR.** Sin ella Android cae en `other` (que
  aquí sirve), pero lint avisa. Los 6 plurales la llevan.
- Los literales de **una sola palabra** ("Buscando...", "Todos", "Limpiar") se escapan de
  cualquier búsqueda por palabras castellanas. Hay que buscar **por posición** (lo que va en
  un `Text(`, `title=`, `label=`, `contentDescription=`...), no por idioma.

#### Cómo se verificó (cuatro comprobaciones sobre el APK ya construido)

Los scripts están en el scratchpad de la sesión; si hacen falta otra vez, merece la pena
rehacerlos, son cortos:

1. **Paridad de claves y de marcadores entre idiomas.** Un `%1$s` en uno y `%1$d` en el otro
   no falla al compilar: **revienta en ejecución y solo en ese idioma**. Es el fallo que hay
   que cazar antes de publicar.
2. **Argumentos suficientes en cada llamada.** El compilador no mira que `getString` pase
   tantos argumentos como marcadores tiene la cadena; si faltan, `MissingFormatArgumentException`
   en cuanto se pinta esa pantalla.
3. **Cobertura sobre el APK** (`aapt2 dump resources`): 900 recursos de texto con las dos
   variantes, ninguno nuestro suelto. Los 9 que faltan son de `cloudinary-android-ui`, cuyas
   pantallas la app no abre (solo usa `MediaManager`).
4. **Lint** sin errores de recursos. ⚠️ Queda **un error de lint preexistente y ajeno**:
   `BannerAd.kt:22` (`ContextCastToActivity`), de `b03790a`. `lintDebug` falla por eso.

En el emulador: alta, login y los dos textos legales recorridos de principio a fin en inglés
y en castellano. Lo de dentro necesita login con Google y no se automatizó.

⚠️ **El AVD `FoodRanker_Test` tiene el `/data` lleno** (5,4 G de 5,8 G) y rechaza instalar
("Requested internal only, but not enough space"). No se puede inspeccionar sin root y la
imagen es de Play Store. Se usó `Eligr_Pixel_35`. `Pixel_6` no arranca: le falta la imagen
android-34. ⚠️ **Tras un `adb install`, comprobar que dice `Success`**: falló en silencio y
se estuvo verificando el build anterior.

#### Textos legales (traducidos, pendiente decisión de fondo)

Ambos en los dos idiomas. La política decía "si resides en la **Unión Europea**", que
**excluye al Reino Unido** desde el Brexit: ahora dice "en el Espacio Económico Europeo o en
el Reino Unido" y nombra las dos autoridades (AEPD / ICO). Se añadió el apartado 0, quién
responde de los datos. Los Términos siguen rigiéndose por la legislación española, con la
salvedad de los derechos del consumidor en su país.

❗ **Pendiente de Sergio, no es traducir:** quién figura como responsable del tratamiento y
con qué base jurídica. Eso no lo redacta Claude.

---

### 🔶 IDIOMAS: FASE 0 Y FASE 1 COMPLETAS (2026-10-08)

La app se puede usar entera en inglés y castellano. 454 claves, todas en los dos idiomas (la
única sin traducir es `default_web_client_id`, que es la credencial de OAuth).

**Avisos del servidor (DESPLEGADO)**: la app guarda `language` en el perfil al entrar, junto
al token FCM, y Cloud Functions elige el texto con ese campo (cae al castellano si falta).
⚠️ `language` hubo que añadirlo a la lista blanca de `users` en firestore.rules. Verificado
con el emulador que editar perfil y guardar el token siguen funcionando (6/6).
Los nombres de nivel y logro se traducen en el servidor SOLO para el push: la lista de la app
los resuelve con la suya, para que no acaben con dos nombres distintos.

**Elegir idioma**: `locales_config.xml` + `android:localeConfig` → Android 13+ pone su propio
selector por app; hay una fila "Idioma" en el perfil que lleva ahí. En Android 12 y anteriores
esa pantalla no existe: la fila se oculta y la app sigue el idioma del móvil.

**Lo que sigue en castellano**: nada visible. Ver la fase 2 más arriba.

#### ⚠️ Trampas aprendidas

Se abre la app a usuarios en Reino Unido (la prima de Sergio, en Edimburgo).

**Fase 0 — lo que corrompía datos (DESPLEGADO).**
- El precio se guardaba sin moneda y con el € escrito a mano. La moneda sale ahora del país
  del LOCAL, no del móvil de quien mira: el precio lo fija el sitio.
- Los locales se pedían a Places fijados en castellano, y el documento del local es **único y
  compartido**: un bar de Edimburgo habría quedado como "Edimburgo, Reino Unido" PARA TODOS.
  Ahora la app manda el idioma de su móvil al dar de alta un sitio.
- Eso obliga a guardar `countryCode` (ISO), porque el nombre del país pasa a venir en varios
  idiomas y deja de servir para comparar. ⚠️ El logro Globetrotter contaba países por nombre:
  habría contado "España" y "Spain" como dos.
- ⚠️ `countryCode` hubo que añadirlo a la lista blanca de `plates` en firestore.rules o se
  rompía TODA publicación. Verificado con el emulador (3/3).
- Backfill hecho: 33 locales y 47 platos (ES y FR).

**Fase 1 — las pantallas de entrada (NO desplegado, va en la v17).**
Onboarding, login, ranking, publicar y ficha de plato: ~250 textos fuera del código, en
`values/` (inglés, por defecto) y `values-es/`. Las 18 categorías también.
Lo que quedaba (perfil, liga, explorar, premium, notificaciones, legales) se cerró en la
fase 2, más arriba.

**Elegir idioma**: `locales_config.xml` + `android:localeConfig` hacen que Android 13+ ponga
su propio selector por app. Hay una fila "Idioma" en el perfil que lleva ahí. En Android 12 y
anteriores no existe esa pantalla: la fila se oculta y la app sigue el idioma del móvil.

#### ⚠️ Trampas aprendidas con los recursos
- `stringResource` solo se puede llamar desde un `@Composable`. Los literales de listas
  definidas a nivel de fichero (páginas del onboarding) pasan a `@StringRes Int`, y lo que va
  dentro de un `remember` se lee FUERA y entra como clave.
- Un **enum no puede guardar el texto**: se construye al cargar la clase, antes de saber el
  idioma. Las categorías guardan `@StringRes val nameRes`.
- En `strings.xml`: un apóstrofo suelto NO compila (va el tipográfico), un `&` tampoco
  (`Soups & stews` tumbó la build), y los saltos de línea tienen que ser la secuencia
  escapada, no un salto real, que Android colapsa.
- Los plurales NO se hacen con un `if (n == 1)`: eso solo acierta en castellano e inglés.
  Van con `<plurals>` (votos, racha, "lo repetirían").
- ⚠️ **Al compilar, no filtrar la salida de Gradle por `^e: `**: los errores de recursos no
  llevan ese prefijo, y `$?` tras una tubería es el del último comando, no el de Gradle. Así
  se coló un `&` sin escapar que tumbaba la build entera.

- `ErrorMapper` lo llaman 21 sitios desde ViewModels que no pueden leer recursos: se le pasa
  el contexto al arrancar (como AdManager) y la DECISIÓN de qué error es va aparte del texto,
  porque sus tests comparaban el mensaje en castellano y se rompieron al traducirlo.
- Un `remember` no admite `stringResource` dentro: se lee fuera y entra como clave.
- Dentro de un `onClick` tampoco: se resuelve antes, en ámbito componible.

**El enlace de TheFork** ya elige dominio y ruta por el país del local (thefork.co.uk, .fr,
.it, .pt, .de), no por el idioma de quien mira: lo que se busca es ese sitio.

---

### ✅ v16 (1.3) SUBIDA (generada el 2026-10-07) — confirmado por Sergio el 2026-10-09

⚠️ Este apartado decía "sin subir" y era **falso**: la v16 está en Play. Por eso la
siguiente es la v17.

`app/build/outputs/bundle/release/app-release.aab`. Verificada en el emulador: arranca sin
crashes, sesión intacta, notificaciones y perfil correctos.

**Lo que lleva** (18+ commits acumulados): login híbrido, "Qué pido aquí" con locales que ya
llevan a publicar, roturas con fuente grande, y todo el trabajo de notificaciones.

#### El bug que reportó un tester, y su causa real

"Llegan los avisos de me gusta pero no los de comentario; en la campana sí aparece."

No era FCM. Se comprobó descargando el código desplegado (tenía su caso) y con una prueba
real que dejó `push enviado: tipo=comment` en los registros. **La app muestra avisos por DOS
caminos**: el push y un escuchador en tiempo real en `NotificationRepository`. Ese escuchador
solo conocía `like` y `rating`; el resto caía en un `else` que lo descartaba sin rastro.
⚠️ **Al añadir un tipo de notificación hay que tocar CUATRO sitios**: el switch de
`onNotificationCreated`, el escuchador de `NotificationRepository`, el mapeo manual de
`NotificationsViewModel` (campo a campo: lo que no esté ahí llega vacío sin fallar) y los
textos de `NotificationsScreen`.

#### Liga: ahora se cierra de verdad

`closeWeeklyLeague`, lunes 03:00 Europe/Madrid, ya programada y ENABLED. Premia 50/30/15 XP
al podio y da el distintivo `league_winner` al primero, con contador `leagueWins`.
- El XP del premio va al contador global, **no al de la liga**: ganar no da ventaja la semana
  siguiente.
- Se marca `closedAt` **antes** de repartir: un reintento no premia dos veces.
- Empates: gana quien llegó antes a esa puntuación (la W40 acabó empatada a 45).
- ⚠️ `leagueWins` va **fuera** del data class `User` a propósito: la regla de creación usa
  `hasOnly()` con los campos del modelo, así que añadirlo allí sin tocar `firestore.rules`
  **rompería el alta de cualquier usuario nuevo**.

#### Avisos nuevos ya desplegados y activos

comentario, plato aprobado, seguidor, resultado de liga, subida de nivel y logro desbloqueado.
Los tipos que una versión antigua no conoce caen en un respaldo genérico, por eso **todos
llevan un `plateName` legible** aunque no vayan sobre un plato.

#### Recompensas: NO tocar todavía (decidido con datos, 2026-10-07)

- 4 de 7 usuarios activos tienen **0 platos de otros en su ciudad**: no pueden valorar nada
  aunque quieran. De 51 valoraciones, solo **8** son a platos ajenos; el resto es el auto-voto.
- 3 personas activas de 12 en 7 días. Patricia ha subido 31 de 43 platos.
- El cuello de botella es **densidad geográfica**, no motivación. Ninguna recompensa lo mueve.
- Ideas buenas, para cuando haya escala: `docs/REWARDS.md` §4.

**Pendiente:** subir el AAB, pushear (son ya 20 commits), y los 12 testers que Play exige.

---

### ✅ FUENTES GRANDES (2026-09-30) — arreglado lo que se rompía de verdad

No se tocaron los 298 `fontSize` ni las 134 alturas fijas a ciegas: `sp` ya escala solo y
barrerlas todas era mucho riesgo para nada. En vez de eso se puso el emulador a
**`font_scale 1.8`** (`adb shell settings put system font_scale 1.8`) y se recorrió la app
viendo qué se rompía. Roturas encontradas y corregidas:

1. **Pestañas del ranking**: "Siguiendo" se partía a media palabra y dejaba la "o" sola en
   una segunda línea que desbordaba la pestaña. `TabRow` reparte el ancho a partes iguales y
   tiene altura fija, así que se fuerza una línea con puntos suspensivos. Es un compromiso:
   con fuente enorme se lee "Siguien…", pero el layout deja de deformarse.
2. **Cabecera de la liga**: eran tres `Text` en un `Row`, y al crecer cada uno envolvía por su
   cuenta — salía "· Reinicia" ARRIBA y "lunes" suelto debajo. Ahora es un único `Text` con
   `buildAnnotatedString`, así que el salto de línea lo decide el texto.
3. **Publicar plato**: "Paso X de 2" iba dentro del título de la `TopAppBar`, que tiene altura
   fija, y salía cortado por la mitad. Se ha movido junto a la barra de progreso.
4. **Nombres de la liga**: `entry.userName.take(8)` cortaba a 8 caracteres **siempre**, con
   cualquier fuente — "Sergio De La Peña" salía "Sergio D" y "Patricia Nuñez", "Patricia".
   Esto no era cuestión de accesibilidad, llevaba roto desde siempre.
5. **Splash**: decía `v1.0` escrito a mano mientras la app iba por la 1.2. Ahora sale de
   `BuildConfig.VERSION_NAME`.

**Verificado en el emulador a 1.8 y otra vez a 1.0**, para confirmar que con fuente normal no
se rompe nada (las pestañas siguen completas y el podio de la liga ahora se lee mejor que
antes).

**Lo que NO se ha tocado, a propósito:** chips de logros y de categoría se siguen cortando con
fuente enorme. Son contenedores de ancho fijo con texto corto; arreglarlos obliga a rediseñar
esas filas y no compensa.

---

### 🔶 NOTIFICACIONES ARREGLADAS (2026-09-27) — código listo, SIN desplegar ni publicar

Revisión del sistema de notificaciones. **Entregar no fallaba**: los 11 usuarios tienen token
FCM y de las 8 valoraciones a platos ajenos 7 habían notificado. Lo que pasaba es que había
eventos que no avisaban de nada. Arreglado:

1. **Comentar no avisaba a nadie** (7 comentarios reales en platos ajenos, 0 notificaciones).
   El tipo `comment` no existía. Ahora lo crea `onCommentCreated`.
   ⚠️ La notificación va **antes** del corte por XP a propósito: el XP de comentario solo se
   da una vez por usuario y plato, así que colgarla al final habría avisado solo del primer
   comentario de cada persona.
2. **Aprobar un plato no avisaba**: solo se notificaba el rechazo, así que publicar y que
   fuera bien era indistinguible de que se hubiera perdido. `approveplate` crea ahora
   `moderation_approved`, dentro de `if (awarded)` — ese es el flag de primera aprobación
   real, y en un reintento vale null y no duplica.
3. **El recordatorio diario usaba el canal social**, no el suyo: silenciarlo desde Android
   silenciaba también los likes, y al revés. `CHANNEL_DAILY` estaba declarado pero no lo
   usaba nadie. Además ya no notifica sin sesión iniciada.
4. **Agujero cerrado**: la regla de `notifications` no restringía el `type`, así que
   cualquiera podía crear en el buzón de otro un `moderation_rejected` — y como dispara push,
   le llegaba un "tu plato no cumple las normas" que parecía del sistema. Ahora el cliente
   solo puede crear `like`; el resto lo escribe el Admin SDK.

De paso, el canal en primer plano se resuelve por tipo (antes todo caía en el social).

**Probado:** 10/10 en el emulador (`scratchpad/rulestest/notifRules.mjs`), incluido que el
`like` del cliente sigue funcionando. `tsc --noEmit` y `compileDebugKotlin` en verde.
Lo que NO está probado end-to-end son los dos triggers nuevos: el emulador de functions no
sirve aquí porque `moderatePlateImage` borra los platos de prueba. Se verifica en producción
tras desplegar, comentando desde una cuenta en un plato de la otra.

⚠️ **Java para el emulador**: firebase-tools exige Java 21+. El JBR de Android Studio vale
(21.0.6), pero **en Git Bash hay que meterlo en el PATH en formato POSIX**
(`/c/Program Files/Java/jdk-26/bin`) — con `C:/...` los dos puntos parten el PATH y acaba
cogiendo el Java viejo del sistema, con un error que parece de versión y no lo es.

**Compatibilidad**: los tipos nuevos degradan bien en las versiones ya instaladas —
`NotificationsScreen` tiene `else -> "🔔"` y "Nueva notificación sobre X", y el texto del push
lo construye el servidor. Por eso **el servidor se puede desplegar sin esperar a publicar la
app**.

**Pendiente:** desplegar `functions` + `firestore.rules`, y decidir qué hacer con el resto de
la revisión (agrupar notificaciones, ajustes de avisos, notificación de seguidor,
"Cerca de mí" sin `onClick`, fuentes grandes).

---

### ✅ LOGIN HÍBRIDO IMPLEMENTADO Y VERIFICADO (2026-09-23) — sin publicar todavía

**Problema que resuelve.** Quien tiene de navegador por defecto uno sin Chrome Custom Tabs
(el de Xiaomi, reportado por testers reales) **no podía entrar**: el flujo por navegador de
Firebase da error con él. Con Chrome funcionaba.

**Qué se hizo.** `AuthRepository.signInWithGoogle` intenta ahora dos caminos en orden:

1. `tryNativeSignIn` — Credential Manager (hoja nativa de Android, sin navegador).
2. Si ese no concluye, `signInWithBrowser` — **el flujo de siempre, sin tocar**.

Cancelar **no** es fallo: si alguien cierra la hoja de cuentas se vuelve a Idle en silencio
(`SignInCancelledException`), no se le abre un navegador detrás ni se le enseña un error.

**Verificado en el emulador, los dos caminos:**

| Caso | Resultado |
|------|-----------|
| Nativo, build debug | Hoja nativa → dentro. Sin navegador. |
| Nativo, **build release (R8)** | Hoja nativa → dentro. R8 no rompe Credential Manager, y no hizo falta añadir reglas a `proguard-rules.pro`. |
| Nativo roto a la fuerza | `W/AuthRepository: Login nativo no disponible (RuntimeException), se usa el navegador` → Custom Tab en `accounts.google.com` → dentro. |

Los dos caminos acaban en **el mismo UID** (`bwUmH8m1hRMzfHQ3UNzSMu3avVY2` en la prueba),
así que nadie parte su cuenta ni pierde progreso según por dónde entre.

**⚠️ TRAMPA, para no perder el tiempo repitiéndola:** poner un `default_web_client_id`
falso en `strings.xml` **no sirve** para probar el respaldo. Play Services del emulador
emitió el token igualmente y el login nativo funcionó, así que parecía que el respaldo había
entrado cuando en realidad nunca llegó a ejecutarse. La única forma fiable fue lanzar una
excepción a mano dentro de `tryNativeSignIn`, compilar y mirar el logcat.

**Riesgo asumido, conocido de antes:** el login nativo ya falló dos veces en builds reales de
Play (ver "Décima sesión" más abajo), y eso no se puede reproducir ni en emulador ni en debug.
La diferencia es que ahora ese fallo **no deja a nadie fuera**: cae al navegador. Si hubiera
que dar marcha atrás, basta con borrar `tryNativeSignIn`.

**Pendiente inmediato:** publicar la **v15** (el AAB que hay en Play Console es anterior a
esto — hay que regenerarlo). Sigue pendiente también "Cerca de mí" (los locales sugeridos no
tienen `onClick`, `NearbyDishesScreen.kt:109-114`) y los textos que se rompen con fuente
grande (134 alturas fijas, 22 `maxLines = 1`).

---

### 🔍 EN CURSO (2026-09-19): auditoría del sistema de valoraciones — rediseño en discusión

El usuario plantea **reconstruir el sistema de valoración** (es el núcleo de la app). Antes de
tocar nada se auditó cómo funciona hoy, de punta a punta. **Los problemas de abajo quedan
APUNTADOS Y PENDIENTES a propósito**: primero se decide el rediseño (qué ejes se valoran, si
entra un campo de precio real), y después se revisa cuáles de estos quedan corregidos de
rebote y cuáles hay que arreglar aparte. No arreglar ninguno por separado todavía.

**Cómo funciona hoy, en corto**: 3 sliders de 1-10 con paso 0,5 (Sabor / Presentación /
Precio-Calidad, `Rating.kt`), nota = media aritmética simple de los 3. Documento de ID fijo
`{plateId}_{userId}` → un voto por usuario y plato, editable tantas veces como se quiera
(`PlateDetailViewModel.editRating`), **nunca borrable** (`firestore.rules:163`,
`allow delete: if false`, para evitar el exploit borrar-y-revotar). La media del plato la
recalcula **siempre el servidor** desde los 3 sub-scores, ignorando el `averageScore` que
manda el cliente (`onRatingCreated`, `functions/src/index.ts:584-588`), en transacción
idempotente vía `processed: true`. Al publicar un plato, el autor se auto-vota en el mismo
batch (`AddPlateViewModel.kt:305-355`) y `approveplate` siembra el plato con esa nota y
`totalRatings = 1` (`index.ts:446-454`). XP: 5 al votante, 10 al dueño, 55 al publicar.

**Problemas detectados, por impacto:**

1. **El ranking es manipulable y estadísticamente injusto — el problema gordo.** Se ordena por
   `averageScore` puro en TODAS las pantallas (`DiscoverViewModel.kt:240`, `PlateRepository.kt:20`,
   `ExploreViewModel.kt:155`, `TrendingViewModel.kt:50`, y el puesto en la ciudad en
   `PlateDetailViewModel.kt:161-171`). Un plato con **1 voto de 10,0 queda por delante de uno
   con 40 votos de 9,7**. Y ese primer voto es siempre el del propio autor: hoy cualquiera puede
   publicar, auto-puntuarse 10/10/10 y encabezar el ranking de su ciudad. Con 9 usuarios no se
   nota; con 200 el ranking pierde toda credibilidad. Solución estándar si no lo resuelve el
   rediseño: **media bayesiana** (estilo IMDb) en un campo nuevo del plato para ordenar,
   dejando `averageScore` intacto para mostrar (⚠️ haría falta añadir índices compuestos nuevos
   en `firestore.indexes.json`, y ojo con la mina ya documentada de desplegar índices).
2. **La "valoración rápida del feed" no existe: es código muerto.**
   `DiscoverViewModel.submitRating` (`:371-430`) y `submitRatingAnalytics` (`:295`) no los llama
   NADIE — verificado con grep, el único punto de entrada real es
   `PlateDetailScreen.kt:787` → `PlateDetailViewModel.submitRating`. Arrastra consigo la
   **misión diaria de votos**, que llama a `dailyMissionManager.incrementVote()` desde dentro de
   esa función muerta (`DiscoverViewModel.kt:413-425`): **hoy la misión diaria nunca avanza**.
   Para votar hay que entrar al detalle y abrir un bottom sheet de 3 sliders — fricción alta.
3. **El detalle carga 50 valoraciones sin `orderBy`.** `PlateDetailViewModel.kt:110-113` hace
   `whereEqualTo("plateId").limit(50)` y ordena en cliente (`:175`). Con >50 votos Firestore
   devuelve 50 arbitrarias y `hasUserRated` (`:189`) puede dar falso negativo: el usuario vería
   el botón de valorar y al pulsarlo recibiría "Ya valoraste este plato". **El índice ya existe**
   (`firestore.indexes.json:69-76`, `plateId+createdAt`), solo falta usarlo. Arreglo barato.
4. **Nadie sabe qué significa un 7,2.** 3 sliders que arrancan en 5, sin referencia de qué es
   "normal". La nota final sale con granularidad 1/6 (7,1666…) y se muestra con `%.1f`, así que
   dos platos que se ven idénticos ("★ 8,3") se ordenan distinto sin explicación visible.
5. **Menores, ya conocidos**: no existe trigger `onRatingDeleted`, así que si el Admin SDK borra
   ratings en cascada la media del plato queda obsoleta (mismo hueco ya documentado en
   `wipe-content` más abajo); `onRatingUpdated` (`index.ts:796`) recalcula con
   `(avg*n - oldAvg + newAvg)/n`, que acumula error de coma flotante con ediciones repetidas y
   no tiene job de reconciliación; y `ratings` es de **lectura pública sin auth**
   (`firestore.rules:140`) mientras que `comments` sí exige `isSignedIn()` (`:168`).

**➡️ DISEÑO YA ACORDADO (2026-09-19): ver `docs/RATINGS.md`** — documento nuevo con el
rediseño completo, el porqué de cada decisión y el plan de implementación por fases.
**Nada implementado todavía.** En corto, lo acordado con el usuario:
- **Solo vota quien declara haber probado el plato**; el like pasa a significar "me apetece".
  GPS marca el voto como verificado, pero **nunca bloquea**.
- **3 ejes con pesos desiguales**: Sabor 50 % · Presentación 20 % · Satisfacción 30 %.
  Sale `valueScore` (Precio/Calidad, era una opinión sobre un hecho que la app no conocía),
  entra `satisfactionScore` ("¿te quedas satisfecho?", formulado así para que el 10 siga
  siendo el óptimo y el eje sea promediable).
- **Precio en euros exactos** como dato objetivo, obligatorio al subir y confirmable al votar.
  Mediana con fecha, nunca media. La UI no promete más de lo que sabe.
- **"¿Lo volverías a pedir?"** binario → `%` estilo Rotten Tomatoes, **junto a la nota, no en
  lugar de ella**, y fuera del ranking de momento.
- **Ranking por media bayesiana** en un campo nuevo `rankingScore`; `averageScore` se queda
  como lo que se muestra. Esto resuelve de paso el auto-voto del autor (problema 1).

**➡️ IMPLEMENTADO EN LOCAL (2026-09-19). NADA DESPLEGADO NI PUBLICADO.**
Fases 0 y 1 completas: cliente y Cloud Functions compilando, reglas validadas contra el
emulador (15/15). De los 5 problemas de arriba quedan corregidos el **1** (bayesiana),
el **2** (código muerto borrado), el **3** (carga de valoraciones) y la parte de
`onRatingDeleted` del **5**.

**✅ SERVIDOR YA DESPLEGADO Y MIGRADO (2026-09-20).** Hecho y verificado:

1. ✅ `firestore.rules` desplegadas (aceptan los dos formatos a la vez).
2. ✅ Cloud Functions desplegadas, incluidas las dos nuevas `onRatingDeleted` y
   `refreshGlobalStats`. **Ojo para la próxima**: el primer intento falló porque faltaba
   habilitar `cloudscheduler.googleapis.com` (lo pide la función programada); el propio CLI la
   habilita y **basta con repetir el comando**.
3. ✅ Migración aplicada: **53 documentos** (26 valoraciones + 27 platos) + `stats/global`.
4. ✅ Verificado contra producción (solo lectura): 8/8 — todos los aprobados con
   `rankingScore`, cada nota = media de sus valoraciones, cada bayesiana correcta, el
   `valueScore` histórico intacto en las 33 valoraciones.

**El ranking nuevo ya está vivo y hace lo que se buscaba**: "pulpo a la gallega" (10,00 con
**un** voto) ha dejado de ser el primero; ahora encabezan "Paella" (9,62 con 3 votos) y "Lomo
de orza" (9,61 con 2). El cambio de nota más grande de los 33 fue de **0,38 puntos**, y **no
hubo ningún descuadre de votos** — por eso se decidió no avisar a los testers todavía.

5. ✅ **Índices desplegados y verificados** (los ejecutó el usuario a mano: el clasificador de
   seguridad bloquea ese comando, y con razón, porque es el que puede borrar índices). Hicieron
   falta **dos** despliegues por la trampa de la dirección (ver abajo). Ninguno de los dos pidió
   borrar nada, confirmando que el cambio fue puramente aditivo.
6. ✅ **Las 8 consultas reales de la v13 probadas contra producción**: ranking principal,
   cercanos, por categoría, aprobados, aprobados+categoría, puesto en la ciudad, job de
   estadísticas y carga de valoraciones. Todas devuelven datos.

⚠️ **Trampa nueva aprendida — la DIRECCIÓN del índice importa.** El "puesto en la ciudad"
(`status == X AND city == Y AND rankingScore > N`) **no lleva `orderBy`**, y entonces Firestore
ordena por el campo del rango en **ASCENDENTE**. El índice se había creado `DESCENDING` (que es
lo que sirve para los rankings) y la consulta fallaba. Hizo falta un índice aparte con
`rankingScore ASC`. Detalle en `docs/RATINGS.md`.
De paso, eso explica por qué el badge **"#N en tu ciudad" no se ha visto nunca**: la consulta
equivalente con `averageScore` tampoco tenía índice y falla en silencio (tiene un `catch` que
devuelve 0). Ahora puede empezar a aparecer — no es nada nuevo, es algo viejo que se arregla.

7. ✅ **UI VERIFICADA EN EL EMULADOR ANDROID, CICLO COMPLETO** (`FoodRanker_Test`, cuenta de
   pruebas `bwUmH8m1…` "Sergio" de Madrid — **ojo, NO es la cuenta real del usuario**, que es
   `rNpwrHo3…` "Sergio De La Peña"). Verificado contra producción real: ranking ordenado por
   `rankingScore`, la puerta "¿Has probado este plato?", los 3 sliders, la nota ponderada en
   vivo, el teclado decimal del precio, la validación del tope, el botón bloqueado hasta
   contestar el binario, y la publicación de una valoración de verdad sobre "Bravas de Bimi"
   (plato del usuario), que mostró **"👍 1 de 1 lo repetiría · 💰 1 persona pagó 9,80 €"**.
   Después se limpió todo: rating borrado, XP global y de liga revertidos y notificación
   eliminada. **`onRatingDeleted` validado**: el plato volvió exacto a 8,93 · 2 votos ·
   repiten 0/0 · **sin precio** (probando también el arreglo del "precio fantasma").
   Producción re-verificada después: coherente.

🐛 **BUG REAL CAZADO AQUÍ, y solo aparecía en el móvil: con las reglas desplegadas NADIE podía
valorar** (`PERMISSION_DENIED` en cada intento). Un data class de Kotlin **serializa todos sus
campos**, así que el cliente seguía mandando `valueScore: 0` (el eje retirado, con su valor por
defecto) y la regla exigía 1..10. Arreglado con `legacyScoreOk()` y **reglas redesplegadas**.
Las pruebas del emulador no lo cazaron porque construían los documentos a mano en vez de
replicar lo que serializa el cliente. Detalle y lección en `docs/RATINGS.md`.

8. ✅ **Verificación por GPS implementada y probada (2026-09-20).**
   `VenueRepository.isAtVenue(lat, lng)` compara la posición real con la del local (200 m,
   `Rating.VENUE_RADIUS_METERS`); la usan el valorar y el publicar, y la UI pinta
   **"📍 en el local"** junto al nombre. **No pide el permiso a propósito**: sin permiso el
   voto sale sin verificar, y ya está. Probados los dos caminos: a 433,7 m del local dio
   `false` (correcto) y, subiendo el radio a 500 m temporalmente, `true` con su distintivo en
   pantalla. ⚠️ **El GPS del emulador `FoodRanker_Test` está congelado** en 40.4168,-3.7038 y
   no hay forma de moverlo (`adb emu geo fix` y la consola por telnet dicen OK pero no
   actualizan nada) — de ahí el truco del radio para probar el caso positivo.
9. ✅ **Arreglado de paso**: la lista de valoraciones seguía pintando `MiniScore("💰",
   valueScore)`, el eje retirado, así que las valoraciones nuevas habrían mostrado "💰 0.0".
   Ahora enseña 🍽️ (satisfacción) solo cuando existe, y en las antiguas nada.

### 🔶 v13 SUBIDA A PLAY (2026-09-21) — pendiente de aprobación y verificación

**AAB `versionCode 13` / `versionName 1.1` subido a Prueba cerrada.** Es la primera versión
con `versionName` distinto: las 12 anteriores se llamaban todas "1.0" y en Play Console eran
indistinguibles.

Verificado antes de subir: el `versionCode`/`versionName` leídos **del propio artefacto** (no
del `build.gradle`) con `aapt2 dump badging`, y que la build de **release** (con R8 y
`shrinkResources`, que es lo que va a Play) arranca sin crashes hasta el login. ⚠️ **El flujo
autenticado en release NO se pudo probar**: instalar la release obliga a desinstalar la debug
por la firma, y eso borra la sesión. Ese flujo sí quedó verificado de punta a punta en debug.
El riesgo de R8 sobre los modelos está cubierto por `-keep class ...data.model.** { *; }` en
`proguard-rules.pro`, que incluye los campos nuevos de `Rating` y `Plate`.

**RETOMAR AQUÍ cuando Google apruebe:**

1. Instalar en el Redmi **desde Play**, no un APK local. ⚠️ Play sirve versiones cacheadas:
   `am force-stop com.android.vending`, reabrir la ficha y pulsar **"Actualizar"**, no
   "Instalar" (pasó con la v12). Confirmar que dice `versionCode 13`.
2. **Probar el flujo autenticado en release**: valorar un plato y comprobar que se guarda —
   es lo único que no se ha visto funcionar fuera de debug.
3. **Avisar a los 9 testers de que actualicen.** Hasta que lo hagan verán el ranking con el
   orden antiguo y no podrán aportar precio ni contestar al "¿lo volverías a pedir?".
4. Semanas después, cuando el parque haya rotado: endurecer las reglas (exigir los campos
   nuevos, prohibir `valueScore`).
✅ **Check-in de 24 h implementado y probado (2026-09-20).** Sin él, la verificación solo
pillaba a quien valoraba con el plato delante; lo normal es escribir la valoración al salir o
ya en casa. `VenueCheckInStore` anota por qué locales se ha pasado (`SharedPreferences`,
**local a propósito**: es un distintivo que no pondera nada y guardarlo en servidor sería
registrar por dónde se mueve la gente), e `isAtVenue` lo mira **antes** que el GPS.
**Coste cero**: los check-ins salen de lo que "Qué pido aquí" ya tenía resuelto — ni una
llamada más a Places, Firestore ni GPS, y de hecho ahorra la lectura de ubicación cuando hay
check-in válido. Solo se anotan locales a ≤ 200 m, no los 550/1.600 m que lista la pantalla.
Probado con el truco del radio: se registra el check-in con 500 m, se vuelve a 200 (con lo que
el GPS a 433,7 m ya **no** puede verificar), se vota y sale `verifiedAtVenue: true` — solo
pudo ser por el check-in.

Detalle completo, con el porqué de cada decisión, en `docs/RATINGS.md`.

---

**3 bugs reportados por el usuario (2026-09-17), arreglados y VERIFICADOS en el Redmi real
(2026-09-18).** `versionCode` subido a 12, AAB generado. Verificación en dispositivo:
instalada la build de depuración sobre el Redmi (hubo que desinstalar la v11 de Play primero
por firma distinta; se avisó y se confirmó con el usuario antes de hacerlo), con la cuenta
`sergiodelapenya1991@gmail.com`:
- **Texto del comentario**: escrito un comentario de prueba en "pulpo a la gallega", texto
  perfectamente legible (letra oscura sobre blanco). Nota aparte: MIUI reportaba modo oscuro
  del sistema activo (el teclado sí se veía oscuro) pero la app seguía renderizando en claro —
  parece un tema propio de MIUI no relacionado con este fix (con el color ahora fijo, el fallo
  original no puede repetirse pase lo que pase con el tema).
- **Nombre en comentario nuevo**: el comentario de prueba salió como "Sergio De La Peña", no
  "Usuario". De paso, confirmado también el backfill: la valoración de "Dani De la" en ese
  mismo plato ya se ve bien.
- **Icono de notificación**: con la app en segundo plano, se creó una valoración de prueba
  desde otra cuenta (Sandra) directamente por Admin SDK para disparar el push real de Android
  — el sistema pintó la notificación con el icono correcto (tenedor y cuchillo naranja),
  confirmando que la meta-data del manifest funciona para notificaciones que Android renderiza
  él mismo, no solo las que pasan por `onMessageReceived`. Limpiados después la valoración de
  prueba, la media/contador del plato, la XP revertida y las notificaciones generadas — sin
  dejar rastro en producción.

**v12 YA EN PRODUCCIÓN Y VERIFICADA (2026-09-19).** Subido el AAB a Play Console (prueba
cerrada) sin incidencias — el único aviso que salió ("vista de extremo a extremo") estaba
etiquetado a la v11 y ya resuelto en código desde antes, no bloqueaba nada. Reinstalada la
app real de Play en el Redmi (no la build de depuración de la verificación anterior):

- **Ojo con esto para la próxima**: Play Store, al pulsar "Instalar" desde la ficha, sirvió
  primero la v11 en caché (versionCode seguía en 11 tras "instalar"). Hubo que forzar el
  cierre de Play Store (`am force-stop com.android.vending`), reabrir la ficha
  (`market://details?id=com.app.foodranker`) y pulsar **"Actualizar"** explícitamente para
  que bajara la v12 de verdad. Mismo patrón ya visto en sesiones anteriores con versiones
  servidas obsoletas — no fiarse de que "Instalar" trae siempre la última.
- Confirmado `versionCode=12` instalado, la app arranca sin crashes (logcat limpio de
  `AndroidRuntime`/`FATAL`).

No queda nada pendiente de esta tanda de arreglos. Próxima sesión: retomar la edición de
nombre de plato (más abajo) o lo que surja.

Bugs arreglados en detalle:

1. **Texto invisible al escribir un comentario.** El campo de comentarios en `PlateDetailScreen`
   vive dentro de una `Card` con `containerColor = SurfaceWhite` fijo (no sigue el tema), pero
   el color del texto del `OutlinedTextField` sí seguía el tema — en modo oscuro salía texto
   claro sobre fondo blanco, invisible mientras se escribe. Fix: `focusedTextColor`/
   `unfocusedTextColor` fijados a `TextPrimary` explícitamente.
2. **Comentarios (y en general nombres nuevos) mostrando "Usuario" en vez del nombre real,
   incluso en cuentas cuyo perfil SÍ tiene el nombre bien.** Causa real: `addComment` y
   `submitRating` (`PlateDetailViewModel`), la valoración rápida del feed (`DiscoverViewModel`)
   y publicar plato (`AddPlateViewModel`) leían `auth.currentUser?.displayName` directamente
   para guardar `userName`/`addedByUserName` — el mismo campo que ya se sabía poco fiable tras
   el login por navegador (ver el fix de "Usuario" de la sesión del 8-9 sept., que solo tocó
   `AuthRepository`/el perfil, no estos 4 sitios). La prueba: la cuenta de Sergio mostraba
   "Sergio De La Peña" en una valoración antigua pero "Usuario" en un comentario nuevo, MISMA
   cuenta, MISMA sesión. Fix: nuevo `resolveCurrentUserNameAndPhoto()` (duplicado en los 3
   ViewModels, seguidos el patrón ya existente en el repo de pequeños helpers repetidos en vez
   de compartidos) que lee `users/{uid}.name` de Firestore — la fuente de verdad ya corregida
   en el login — con `displayName` solo como último recurso si falla la lectura.
3. **Icono de notificación distinto para likes/comentarios que para los recordatorios de la
   app.** Causa: `FoodRankerMessagingService.onMessageReceived` (que sí usa el icono correcto,
   `NotificationHelper.show`) **solo se ejecuta con la app en primer plano**; con la app en
   segundo plano o cerrada, Android pinta él mismo las notificaciones con bloque `notification`
   (todas las que manda `admin.messaging().send()` en `functions/src/index.ts` — likes,
   comentarios, moderación) usando el icono por defecto, y el manifest no declaraba ninguno.
   Los recordatorios (`DailyReminderWorker`) nunca lo sufrieron porque son locales (WorkManager,
   sin FCM) y siempre pasan por `NotificationHelper.show`. Fix: meta-data
   `com.google.firebase.messaging.default_notification_icon`/`..._color` en
   `AndroidManifest.xml`, apuntando a los mismos `ic_notification`/`notification_color` que ya
   usa el código. Confirmado en el manifest fusionado (`processDebugMainManifest`).

**Backfill aplicado (2026-09-17): `scripts/backfillUserNames.js`, 55 documentos reparados en
producción.** El bug 2 de arriba no era solo de aquí en adelante — el usuario detectó en una
captura que un comentario de Patricia no mostraba NINGÚN nombre (ni "Usuario", vacío del
todo: `displayName` puede venir como cadena vacía `""`, no solo `null`, y `?: "Usuario"` no
cubre ese caso). Escaneando `comments`/`ratings`/`plates.addedByUserName` contra el nombre
real en `users/{uid}.name`: **2 comentarios, 29 valoraciones y 24 platos** con el nombre en
blanco o en "Usuario", casi todos de Patricia (`I6ZAVZjsQjZTo0irlc5PVWgiQgw1`) y Dani
(`2aJJ1rrTARUUMAUiiFLiQUVhoPh1`) — básicamente todo lo que habían publicado/valorado hasta
ahora. Aplicado con `--confirm` tras revisar la simulación; verificado después que el escaneo
vuelve a dar 0 desajustes. Solo toca el campo del nombre, nada de XP/votos/fechas. El script
se queda en el repo por si hiciera falta repetir el escaneo más adelante (p.ej. si aparece
otro sitio del código con el mismo fallo).

**Añadido (2026-09-10): `wipe-content` en `manageUser.js`.** El baneo (`ban <uid>`) solo bloqueaba
el acceso; ahora hay una acción aparte para borrar también el contenido de un usuario cuando
haga falta — deliberadamente separada del baneo (no todo baneo merece borrar contenido):

```
node scripts/manageUser.js wipe-content <uid>            # simulación: cuenta, no borra
node scripts/manageUser.js wipe-content <uid> --confirm  # borra de verdad
```

Borra sus platos (uno a uno, dejando que `onPlateDeleted` ya desplegado haga su cascada
habitual: XP revertida, ratings/comments/saves de ese plato, imagen de Cloudinary — no se
reimplementa nada de eso), sus valoraciones dadas y sus comentarios. No toca `users/{uid}`
(el perfil se queda, vacío) ni la cuenta de Auth — para eso está el borrado de cuenta propio
(`deleteUserAccount`), pensado para que lo dispare el propio usuario, no un admin sobre otra
cuenta. Limitación conocida y documentada en el propio script: al borrar valoraciones dadas
en platos de OTROS, no recalcula el `averageScore`/`totalRatings` de esos platos ni revierte
la XP que ganó su autor por recibirlas — mismo hueco que ya tenía `deleteUserAccount`, no se
introduce nada nuevo. Probado en modo simulación contra una cuenta real (15 platos, 15
valoraciones) sin tocar nada; el borrado real (`--confirm`) no se ha probado todavía por no
tener a quién aplicárselo — probarlo de verdad la próxima vez que haga falta banear a alguien.

**Nota (2026-09-10): v12 en espera, acumulando cambios.** El long-press de abajo es el único
cambio de cliente sin publicar desde la v11 (`versionCode` sigue en 11 a propósito). Se decidió
no subir versión solo por esto — esperar a tener algo más, p.ej. la edición de nombre de plato
pendiente más abajo, antes de pedir otra subida a Play. El ajuste de moderación (`d1fc58e`) no
cuenta para esto: es solo Cloud Function, ya desplegado y activo sin necesidad de nueva versión.

**Verificado (2026-09-09): borrar plato + long-press para editar/eliminar.** El usuario pidió
revisar que el botón de eliminar funcionara bien, y añadir un gesto de pulsación larga sobre
la miniatura para abrir editar/eliminar sin depender del icono pequeño de la esquina.

- **Botón de eliminar**: confirmado en dos rutas independientes. (1) El propio usuario borró y
  resubió a mano el plato real "Cortante patatas revolconas" (el del autocorrector, ver nota de
  abajo) — log de `onPlateDeleted`: XP revertida (-55), cascada de ratings/comments/saves
  completa, imagen borrada de Cloudinary. (2) Prueba en emulador (`FoodRanker_Test`, cuenta
  `sdelapenya1991@gmail.com`): publicado un plato de prueba con una de las fotos de postre ya
  validadas contra SafeSearch, borrado desde el nuevo gesto, mismo resultado limpio en los logs.
- **Long-press añadido**: `PlateGridItem` en `ProfileScreen.kt` pasó de `Card(onClick = ...)` a
  `Card(modifier = modifier.combinedClickable(onClick, onLongClick))` — toque corto sigue
  navegando al detalle, pulsación larga (solo en `Mis platos`, propios) abre el mismo
  `EditPlateSheet` que ya tenía "Guardar" (descripción) y "Eliminar plato". No se creó un menú
  nuevo: reutiliza el sheet ya probado. Verificado en el accessibility tree del emulador
  (`long-clickable="true"` en el nodo del card) y con la interacción real (long-press abre el
  sheet, toque corto abre el detalle, sin cruce entre los dos gestos). El icono de lápiz de la
  esquina se mantiene tal cual, como entrada alternativa.
- Icono de editar (miniatura, esquina superior izquierda) sin cambios de comportamiento.

**Pendiente (2026-09-09): editar el nombre de un plato ya subido.** Un tester escribió
"Coulant..." y el corrector del teclado se lo cambió a "Cortante patatas revolconas" sin que
se diera cuenta; hoy no hay forma de arreglarlo salvo borrar el plato y volver a subirlo (ya
posible desde v11). Se pidió explícitamente: **solo el nombre**, no la puntuación — la
puntuación es un agregado (`averageScore`/`totalRatings` de todos los votos + XP de liga ya
concedida) y tocarla es más delicado, se deja fuera a propósito.

El nombre no es un campo suelto: forma parte del ID del documento (`{venueId}__{dishSlug}`,
ver `docs/VENUES.md`, identidad canónica plato+local). Antes de tocar código, decidir:
- **Caso simple y seguro**: el plato solo tiene el voto del propio usuario (`totalRatings == 1`
  y `addedByUserId == uid`) → renombrar es solo mover a un `dishSlug` nuevo derivado del nombre
  corregido, sin nadie más afectado.
- **Caso con más gente**: si otro usuario ya valoró ese mismo plato+local con el nombre
  correcto, el nuevo `dishSlug` coincidiría con un documento ya existente → habría que fusionar
  (votos, media, entradas de liga de cada usuario) en vez de solo renombrar. Decidir si se
  permite este caso en la primera versión o se bloquea el botón de editar nombre cuando
  `totalRatings > 1` (más simple, y probablemente suficiente para la fase de testers).

Piezas a tocar: `ProfileViewModel.kt` (nueva función tipo `updatePlateName`, análoga a
`updatePlateDescription` pero moviendo el documento en vez de solo `update()`), `EditPlateSheet`
en `ProfileScreen.kt` (campo de nombre editable), y revisar `firestore.rules` si el cambio de ID
del documento necesita permisos distintos a un `update` normal.

**Aviso pendiente de AdMob, sin prisa pero no olvidar**: Google avisó por email (2026-09-09,
"Upcoming changes to coarse location collection in Google Mobile Ads SDK") de que una futura
versión del GMA SDK usará también la ubicación aproximada para anuncios si el usuario ya dio
permiso de ubicación a la app (FoodRanker lo pide para "Cerca"/"Qué pido aquí" —
`ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION`). Es opt-out, no opt-in: se activa solo al
actualizar `play-services-ads` (ahora en `23.0.0`) a la versión nueva, sin que haga falta tocar
código. Cuando llegue el momento de subir esa dependencia: (1) decidir si activar el flag para
desactivarlo, (2) revisar si la política de privacidad ya cubre este uso o hay que ampliarla.

### 🔶 Decimocuarta sesión (2026-09-08): v11 lista — arregla el nombre vacío de testers nuevos

**Retomar exactamente aquí**: AAB `versionCode 11` generado en local
(`app/build/outputs/bundle/release/app-release.aab`), **todavía no subido a Play Console**.
Falta el paso de siempre: subir, esperar aprobación, verificar en un build real de Play.

**Bug encontrado por el usuario, mismo día de reclutar testers**: los 3 primeros testers
reales se registraron con el nombre "Usuario" en vez del suyo (la foto sí salía bien). Las 2
cuentas del propio usuario, con meses de antigüedad desde el flujo nativo antiguo, no lo
sufrían — pero no porque el flujo actual les trajera el nombre de nuevo hoy, sino porque
Firebase conserva el que ya tenían guardado de entonces (`firebaseUser.displayName` no se
vacía solo entre logins).

**Causa, en `AuthRepository.signInWithGoogle()`**: el flujo OAuth por navegador
(`OAuthProvider("google.com")`) no pedía el scope `profile` explícitamente — Firebase
documenta que, a diferencia del proveedor nativo de Google, los proveedores OAuth genéricos no
lo incluyen por defecto. Y aun pidiéndolo, para proveedores genéricos el nombre puede venir
solo en `additionalUserInfo.profile` (los claims en bruto de Google), no en
`firebaseUser.displayName` ya procesado — el código descartaba ese dato por completo.

**Arreglo**: `.setScopes(listOf("email", "profile"))` en el `OAuthProvider`, y
`additionalUserInfo.profile` como respaldo tanto para nombre como para foto. **Sin verificar
con una cuenta que nunca haya usado la app** (no había ninguna a mano) — queda pendiente de
confirmar con el próximo tester real. El usuario les va a pedir a los 3 afectados que
actualicen a la v11 **y cierren sesión y vuelvan a entrar** (no basta con actualizar: una
sesión ya iniciada no vuelve a pasar por el flujo de login solo por seguir abierta).

### ✅ Decimotercera sesión (2026-09-07): v10 aprobada — pero la clave de Places también estaba mal (bug nuevo, ya arreglado)

**La v10 se aprobó y se probó en el Redmi real**, pero salió un bug nuevo, sin relación con el
login: **no dejaba buscar/añadir restaurante ni completar la publicación de un plato**. Mismo
patrón de fondo que el del login (huella de firma no registrada), pero en un sitio distinto:

**Causa**: la clave `PLACES_API_KEY` del cliente ("FoodRanker Places Android" en Google Cloud
Console → Credenciales) solo tenía autorizada la huella de **debug/carga** en su restricción de
Android. Ni la huella clásica de firma de Play ni la real del canal de tester estaban en la
lista — verificado reproduciendo la llamada real (`places.googleapis.com/v1/places:searchText`
con cabeceras `X-Android-Package`/`X-Android-Cert`) con cada huella por separado.

**Arreglo**: añadidas a mano en Google Cloud Console (el usuario lo hizo, la clasificación del
harness bloqueó hacerlo por API con el token de ADC — con razón, es una acción sensible):
- `B6:D0:BF:6D:59:E8:DC:52:2E:0D:AC:E8:1C:B7:16:05:BA:90:00:DF` (clásica de Play)
- `9E:6D:CB:79:07:78:AD:01:BE:7B:4E:77:A6:0E:78:BE:EA:F5:0E:71` (canal de tester interno)

Tardó los ~5 minutos que avisa Google Cloud en propagarse. **Verificado en producción real**:
el usuario publicó un plato real completo (foto + local buscado por Places + categoría),
`status: approved` tras pasar Vision API. Sigue sin borrar a propósito, queda como prueba.

**Pendiente de revisar en otra sesión, sin prisa**: dado que tanto Firebase Auth como la clave
de Places tenían huellas de firma incompletas, merece la pena auditar **todas** las claves/API
restringidas por huella en Google Cloud Console (Cloudinary no aplica, usa upload preset
unsigned) para ver si hay más con el mismo hueco, en vez de esperar a que cada una falle por
separado.

**De paso, en esta sesión**: regalado Premium permanente a un tester
(`patring81@gmail.com`, uid `I6ZAVZjsQjZTo0irlc5PVWgiQgw1`) vía
`manageUser.js grant-premium`. Solo 3 usuarios registrados en total en la app a día de hoy.

**Auditoría completa de las 4 claves de API del proyecto** (vía Cloud API Keys API,
`apikeys.googleapis.com`, con el mismo patrón de ADC ya documentado): "FoodRanker Places
Server" y "FoodRanker Places Android" (ya arregladas), y las dos automáticas de Firebase
("Android key"/"Browser key", gestionadas por la sincronización de huellas de
`firebase apps:android:sha`, ya completa). Ninguna otra tiene el mismo hueco. AdMob no usa
este mecanismo (se autoriza por App ID, no por huella) — confirmado además que muestra
anuncios reales en el build de Play. **No queda ningún cabo suelto del tipo "huella de firma
que falta" en todo el proyecto.**

### 🔶 Duodécima sesión (2026-09-06/07): v10 enviada a revisión — falta aprobación + verificación final

**Retomar exactamente aquí**: la v10 (AAB `e:\FoodRanker\app\build\outputs\bundle\release\app-release.aab`,
`versionCode 10`) está **enviada a revisión** en Play Console (Prueba cerrada). En cuanto Google
la apruebe:
1. Desinstalar FoodRanker del Redmi e instalar desde el enlace real de prueba cerrada
   (`https://play.google.com/apps/testing/com.app.foodranker`) — **no** un APK/AAB local. Si Play
   sirve una build antigua (`versionCode` bajo) en vez de la v10, es la trampa ya documentada de
   la pista de "tester interno" con prioridad sobre "prueba cerrada": forzar refresco de Play
   Store (`am force-stop com.android.vending` + reabrir la ficha) o comprobar que el enlace usado
   es el de la pestaña "Testers" de "Prueba cerrada", no el de "Prueba interna".
2. Probar login de Google de punta a punta ahí (con logcat en vivo si hay cualquier duda,
   filtrando `INVALID_CERT_HASH`/`FirebaseAuth`) — la v9 ya lo confirmó funcionando en un build
   real de Play, pero la v10 lleva más cambios encima y no se ha probado todavía en ese canal
   exacto.
3. Si todo va bien, retomar el reclutamiento de testers (mensaje ya redactado en sesiones
   anteriores de este documento).

**Qué lleva la v10 respecto a la v9** (v9 solo llevaba la migración de Firebase, nada de esto):
- Liga arreglada (índice de Firestore que faltaba) + fichero de índices sincronizado con
  producción (ver "Undécima sesión" más abajo).
- Aviso de 16 KB de Play resuelto (Fresco excluido, APK −2 MB).
- Edge-to-edge pulido: API obsoleta fuera, iconos de barra de estado legibles con recuento de
  referencias (`LightStatusBarIcons` en `Theme.kt`), perfil ya no se corta con la barra de
  navegación, podio de la liga ya no recorta el XP.
- Botón nuevo para eliminar un plato propio (`ProfileScreen` → editar plato → "Eliminar plato"),
  con confirmación. Verificado de punta a punta en producción real: borró el plato de prueba
  `PRUEBA TECNICA - BORRAR` y limpió Cloudinary/XP/valoraciones vía `onPlateDeleted`.
- ANR real arreglado: `MobileAds.initialize()` bloqueaba `Application.onCreate()` en el hilo
  principal, reproducido como "FoodRanker isn't responding" en el emulador de Android 15. Movido
  a un hilo aparte.
- **Dos pasadas de `/code-review`** sobre todo el diff de la sesión, con verificación en
  dispositivo de cada arreglo (no solo lectura de código): borrado de plato que fallaba en
  silencio pese a decir "irreversible" (dos rutas distintas del mismo método), condición de
  carrera real en los iconos de la barra al navegar entre pantallas de cabecera oscura seguidas,
  posible bypass de cierre de sesión por `pendingAuthResult` sin limpiar (guarda defensiva,
  verificado login→logout→login en el Redmi), falta de manejo de excepciones en el hilo de
  AdMob, y tema del sistema obsoleto si cambia con una pantalla abierta.

**Nada pendiente de borrar en producción** — el plato de prueba ya se borró con el botón nuevo
(ver arriba), incluyendo su imagen en Cloudinary y el XP revertido.

**Pendiente sin empezar, sin prisa**: App Check — la API `firebaseappcheck.googleapis.com` no
está habilitada en el proyecto de Cloud (403 en logcat), pero con el enforcement desactivado no
afecta a nada visible.

**Idea para una sesión futura, no urgente**: valorar volver al selector nativo de cuentas
(Credential Manager) para el login en vez del flujo por navegador actual — mejor experiencia
(un toque en vez de abrir Chrome), pero solo tiene sentido intentarlo con calma y verificando en
Play real, ahora que se sabe diagnosticar este tipo de bug en minutos. Ver conversación de la
duodécima sesión para el razonamiento completo de por qué no se hizo ya.

### ✅✅ Undécima sesión (2026-09-05): LOGIN ARREGLADO DE VERDAD + liga rota + 16 KB + edge-to-edge

**El bug del login está resuelto y verificado en un build real de Play (v9).** La causa era
una huella de firma que no aparece en ninguna consola: el APK que Play entrega por el canal
de **tester interno** viene **re-firmado por Google con un certificado propio**:

```
SHA-1: 9e6dcb790778ad01be7b4e77a60e78beeaf50e71
DN:    CN=Android, OU=Android, O=Google Inc., L=Mountain View, ST=California, C=US
```

No era ninguna de las registradas (debug, carga, firma de Play `b6d0bf6d...`, poscuántica).
Firebase la rechazaba con `INVALID_CERT_HASH` y el login abortaba antes de abrir el
navegador. **Arreglado registrándola en Firebase** (`firebase apps:android:sha:create`).
Ojo: **esto no requiere ninguna versión nueva de la app** — es configuración de backend, la
v9 ya instalada empezó a funcionar sola.

**Cómo se encontró, y cómo hacerlo la próxima vez lo PRIMERO**: extrayendo el APK real del
dispositivo en vez de fiarse de las consolas:
```
adb shell pm path com.app.foodranker
adb pull <ruta>/base.apk
apksigner verify --print-certs -v base.apk
```

**Aviso importante sobre la migración de esta sesión**: se migró Kapt→KSP, Kotlin 2.0.21→
2.3.21, Hilt 2.48→2.58 y Firebase BOM 32.7.0→34.18.0 con la hipótesis de que el SDK viejo
no sabía calcular la huella de la firma dual. **Esa hipótesis era falsa**: el
`INVALID_CERT_HASH` seguía igual con el SDK nuevo. La migración se queda porque aporta otras
cosas (ver abajo), pero no arregló el login.

**Lo demás que se arregló, todo verificado en el Redmi (Android 13) y en el emulador
FoodRanker_Test (Android 15)**:

1. **La liga no cargaba para NINGÚN usuario** (`FAILED_PRECONDITION`): faltaba el índice de
   `leagues/{id}/entries` por `xp` descendente. Causa: `firestore.indexes.json` declaraba un
   `fieldOverride` de `entries.xp` **solo** con ámbito `COLLECTION_GROUP`, y declarar un
   override **desactiva los índices automáticos** del campo en los ámbitos no listados.
   Índice desplegado; la liga ya carga.
2. **Mina enterrada**: `firestore.indexes.json` declaraba 4 índices cuando producción tiene
   10. Cualquier `firebase deploy --only firestore:indexes` **habría borrado 6 índices vivos**.
   Sincronizado con producción.
3. **La liga engañaba**: al fallar la carga, `city` se quedaba vacío y la pantalla decía
   "añade tu ciudad" a usuarios que sí la tenían. Nuevo flag `loadFailed` + estado de error
   con reintento, y log en el `catch` (antes no registraba nada).
4. **Aviso de 16 KB de Play resuelto**: las 3 `.so` mal alineadas eran de Fresco, que entraba
   por `cloudinary-android-download` (la app no lo usa: solo `MediaManager` para subir, y
   Coil para mostrar). Excluido `com.facebook.fresco` → solo quedan 2 `.so`, ambas a 16 KB, y
   el APK baja de 10,0 a 8,06 MB. De paso caen `recaptcha` y `soloader` forzados a mano, ya
   innecesarios.
5. **Edge-to-edge**: quitado `window.statusBarColor` (API obsoleta que marcaba Play), nuevo
   helper `LightStatusBarIcons()` para las pantallas de cabecera oscura (perfil, liga, login)
   donde los iconos del sistema quedaban ilegibles, y arreglado el perfil, cuyo contenido
   quedaba tapado por la barra de navegación de 3 botones.

**Pendiente de subir**: todo lo del cliente (puntos 3, 4 y 5) está commiteado pero **solo la
v9 está en Play**, y la v9 no lleva nada de eso. Hay que generar una **v10**. Los puntos 1 y
2 son de servidor y ya están vivos.

**Pendiente de borrar (datos de prueba en producción)**: se publicó un plato de prueba real
para verificar la subida a Cloudinary: `PRUEBA TECNICA - BORRAR` (pizza, Telepizza El Álamo,
7,0), su rating asociado (ojo: **nunca `delete` en `ratings`**), un venue de "Telepizza El
Álamo" (dato canónico legítimo, se puede dejar) y la imagen en Cloudinary
(`foodranker/plates`).

**Pendiente sin empezar**: App Check. La API `firebaseappcheck.googleapis.com` no está
habilitada en el proyecto de Cloud (403 en logcat), pero con el enforcement desactivado no
afecta a nada visible — las callables funcionan con token placeholder.

### 🔶 Décima sesión (2026-09-02): el bug de login REAPARECE en v7 — la migración a Credential Manager no lo arregló; nueva pista (firma poscuántica de Play) y huellas registradas, pendiente de propagación

**Retomar exactamente aquí**: el usuario probó el login en un Redmi con la v7 instalada desde el
enlace real de Play (`https://play.google.com/apps/testing/com.app.foodranker`) y **falla con el
mismo síntoma de siempre** — se atasca en el selector de cuenta, sin pasar. Lo reprodujo también
su hermano. Es decir: **la migración de la novena sesión (`GoogleSignInClient` → Credential
Manager) NO arregló el bug real**, aunque sí era un cambio correcto y recomendado por Google.

**Por qué no lo arregló — diagnóstico con logcat en vivo (Redmi por ADB, Android 13)**: se repitió
exactamente la misma secuencia de la sesión del 21 de agosto:
1. `ActivityTaskManager: START ... cmp=com.app.foodranker/com.google.android.gms.auth.api.signin.internal.SignInHubActivity` —
   **la app sigue cayendo en las activities legacy de Google Sign-In**, pese a que `AuthScreen.kt`
   ya usa `CredentialManager.getCredential()`. Motivo: `androidx.credentials:credentials-play-services-auth`
   está fijado en la versión **1.3.0** (por el problema de Kapt/metadatos de Kotlin, ver sesión 9),
   y en un dispositivo sin proveedor nativo de Credential Manager (Android 13, sin la API de
   sistema de Android 14+) esa librería **hace de wrapper y cae por debajo al mismo backend legacy
   de siempre**. Cambiamos la API que usa la app, pero no el camino real que toma en este móvil.
2. `SignIn: [Activity,SignInChimeraActivity] Failed to record the consent.` — **pista nueva que no
   se había visto antes tan claramente**: el fallo ocurre dentro del propio proceso de Google Play
   Services (`com.google.android.gms`, no en el nuestro) justo al intentar grabar el consentimiento
   de la cuenta.
3. `AutoManageHelper: Unresolved error while connecting client. Stopping auto-manage.` — el mismo
   log de siempre, la app vuelve a `MainActivity` sin éxito ni error visible.
4. En el segundo intento (tras el cambio de abajo) apareció además:
   `SignIn: Couldn't fetch app's branding information, but continuing without it.` — coherente con
   que Google aún no tenga indexado algo del lado servidor para esta app/huella.

**Nueva hipótesis, con buen encaje pero sin confirmar del todo — firma poscuántica de Play**: en
Play Console → Protegida con Play → Firma de aplicaciones, FoodRanker está inscrita (sin que nadie
lo pidiera, es el nuevo comportamiento por defecto de Google al subir un AAB) en **firma híbrida
poscuántica** ("Preparada para la computación cuántica (beta)", esquema de firma APK v3.2: firma
clásica + ML-DSA). Confirmado por búsqueda web: *"By default, when you upload your app bundle,
your app is automatically enrolled in quantum-ready, hybrid signing with Google-generated keys"*.
La huella SHA-1 **clásica** coincide exactamente con la ya registrada en Firebase
(`b6d0bf6d59e8dc522e0dace81cb71605ba9000df`, verificado carácter a carácter) — así que la huella
"de siempre" está bien. Pero había una **segunda huella, la de la clave poscuántica, que nunca se
había registrado en ningún sitio**:
- SHA-1: `99935593de3c2de12d1d455856e594d9a72ca55a`
- SHA-256: `989f8551e61c6e95062f64ece41dd02e44cdfded8045a2bd7d3c4db727e71193`

Hipótesis: si Play Services, al validar la firma del paquete recibido de Play (firmado con dos
certificados), usa o consulta la huella poscuántica en algún punto de ese flujo legacy, y esa
huella no está en la lista de clientes OAuth Android conocidos, el registro de consentimiento
fallaría exactamente así — sin tocar código, sin reconstruir nada, es un fallo puramente de
configuración en el backend de Google.

**Acción tomada (2026-09-02, con la app ya instalada, sin recompilar nada)**: registradas ambas
huellas poscuánticas en Firebase vía CLI:
```
npx firebase apps:android:sha:create 1:350322634794:android:42b4b2e91a8df170c4d353 99935593de3c2de12d1d455856e594d9a72ca55a --project foodranker-51270
npx firebase apps:android:sha:create 1:350322634794:android:42b4b2e91a8df170c4d353 989f8551e61c6e95062f64ece41dd02e44cdfded8045a2bd7d3c4db727e71193 --project foodranker-51270
```
Ahora hay 5 huellas totales en Firebase (ver `firebase apps:android:sha:list`). **Probado a los
pocos minutos de registrar y sigue fallando igual** — pero el precedente de la sesión del 21 de
agosto es que incluso con la huella correcta ya registrada, tardó **horas** en propagarse (de
viernes a domingo). No se puede descartar la hipótesis todavía solo por esto.

**Pendiente exacto para retomar**:
1. Esperar unas horas (o hasta el día siguiente) y volver a probar el login en el Redmi **sin
   necesidad de logcat** — solo abrir la app e intentarlo. Si funciona, el bug queda resuelto de
   verdad esta vez y hay que actualizar este documento y la memoria correspondiente.
2. Si sigue fallando pasado ese tiempo razonable de propagación, la hipótesis de la huella
   poscuántica queda descartada (o al menos insuficiente) y toca ir a la opción robusta: **mover
   el login de Google a un flujo OAuth por navegador** (Firebase Auth `OAuthProvider` +
   Chrome Custom Tabs / `startActivityForSignInWithProvider`), que valida por `client_id` +
   `redirect_uri` y **no depende en absoluto** de que Play Services valide la firma del paquete —
   esquiva del todo este problema (y cualquier futuro relacionado con cómo evolucione la firma de
   apps de Google), a cambio de más trabajo de implementación y un cambio de experiencia (abre
   navegador en vez del selector nativo de cuentas).
3. Alternativa de menor prioridad, mencionada pero no elegida: migrar Hilt de Kapt a KSP para
   poder subir `androidx.credentials`/`googleid` a versiones más nuevas, por si manejan mejor la
   firma híbrida — mucho trabajo, sin garantía (el dispositivo de prueba no tiene proveedor nativo
   de Credential Manager de todos modos, así que probablemente seguiría cayendo al mismo backend
   legacy).

**No mandar el mensaje de reclutamiento de testers todavía** — sigue aplicando el mismo motivo de
la novena sesión, ahora con más razón: ya se "confirmó arreglado" una vez y ha reaparecido.

**Git**: nada de código tocado esta sesión (el cambio fue solo registrar huellas en Firebase, no
requiere commit). `main` sigue en `e674d19`.

### ✅ Novena sesión (2026-09-01): login de Google migrado a Credential Manager — v7 en revisión

**Retomar exactamente aquí**: el AAB de `versionCode 7` está subido y enviado a revisión en
Play Console (Prueba cerrada - Alpha) — pendiente de que Google lo apruebe (la v6 tardó
horas). **Antes de reclutar testers**, en cuanto se apruebe la v7:
1. Desinstalar FoodRanker del Redmi e instalar la nueva versión **desde el enlace real de
   Play** (`https://play.google.com/apps/testing/com.app.foodranker`, no un APK local) y
   probar el login de Google de punta a punta ahí — el bug que se arregló esta sesión solo
   se había visto en builds distribuidas por Play real, así que la prueba de verdad es esa,
   no la del debug local que ya se hizo.
2. Si funciona, retomar el reclutamiento de testers (mensaje ya redactado, ver más abajo).

**El bug de login (que llevaba reapareciendo desde la sesión del 2026-08-21) se diagnosticó
y arregló de raíz esta sesión**: no era la huella SHA-1 de firma de Play (esa parte estaba
bien, verificado varias veces). Con logcat en vivo desde el Redmi (conectado por ADB) se vio
que el selector de cuentas de Google completaba bien, pero justo al volver a la app aparecía
`AutoManageHelper: Unresolved error while connecting client. Stopping auto-manage.` — la app
se quedaba sin ningún resultado (ni éxito ni error), exactamente el síntoma de "no pasa de
la selección de cuenta". El mismo log llevaba el aviso de Google:
`You are using the deprecated legacy Google Sign-In APIs from play-services-auth SDK.
Please migrate to the Credential Manager APIs`. Búsqueda web confirmó que todo el paquete
`com.google.android.gms.auth.api.signin` (el que usaba FoodRanker) está deprecado y Google
lo está retirando progresivamente — encaja con que funcionara una vez y volviera a fallar
sin que nada del lado de FoodRanker cambiara.

**Arreglo**: migrado el flujo de login en
[AuthScreen.kt](../app/src/main/java/com/app/foodranker/ui/screens/auth/AuthScreen.kt) de
`GoogleSignInClient` (legacy) a **Credential Manager**
(`CredentialManager.getCredential()` + `GetGoogleIdOption`), que es lo que Google recomienda
ahora. `AuthRepository.signInWithGoogle(idToken: String)` no se tocó — solo cambia cómo se
consigue el `idToken`, no qué se hace con él. Compatible con el mismo `google-services.json`
y `default_web_client_id` de siempre (mismo proyecto Firebase, no hace falta tocar nada de
consola).

⚠️ **Trampa real con las versiones de las librerías**: las últimas estables de
`androidx.credentials` (1.6.0) y `googleid` (1.2.0) están compiladas con metadatos de
Kotlin 2.1+/2.2+ que **Kapt no puede leer** (`error: Unable to read Kotlin metadata due to
unsupported metadata kind: null` — mismo problema documentado ya para el intento de subir el
Firebase BOM en la séptima sesión). Se usaron versiones anteriores compatibles con Kapt:
`androidx.credentials:credentials:1.3.0`, `androidx.credentials:credentials-play-services-auth:1.3.0`,
`com.google.android.libraries.identity.googleid:googleid:1.1.1`. Compila y funciona bien —
si en el futuro se migra Hilt de Kapt a KSP, se podría subir a las versiones más nuevas.

**Verificado en el Redmi (build de debug, instalada a mano por MIUI)**: login completo, sin
fallos, con el flujo nuevo. Pendiente verificar en un build real de Play (ver arriba, punto
1) antes de dar el bug por cerrado del todo — el síntoma original solo se vio en builds de
Play, nunca en debug local, así que la prueba definitiva es esa.

**Trampas de esta sesión, por si se repiten**:
- Instalar un build de **firma distinta** a la que ya tiene el dispositivo falla con
  `INSTALL_FAILED_UPDATE_INCOMPATIBLE` — hay que desinstalar la app primero.
- MIUI bloquea instalaciones **nuevas** por ADB tras desinstalar
  (`INSTALL_FAILED_USER_RESTRICTED: Install canceled by user` — literal, es un diálogo de
  confirmación en el móvil que expira si nadie lo toca). Reintentar `adb install -r` directo
  (no vía `gradlew installDebug`) a veces basta la segunda vez.
- Los exploradores de archivos de MIUI a veces no listan los `.apk` en Descargas aunque
  estén ahí (verificado con `adb shell ls`) — no vale la pena perseguirlo, ir directo a
  `adb install -r`.

**Git**: todo pusheado, `main` sincronizado en local/`origin`/`gitea` en `1818766`.

**Bug real, reproducido hoy por el usuario y por su hermano**: instalando la app desde el
enlace de prueba real de Play, el login con Google se queda colgado en el selector de
cuenta — mismo síntoma exacto que el bug de la huella SHA-1 de firma de Play documentado en
`project_play_signing_sha1.md` (sesión 2026-08-21). La diferencia esta vez: **el usuario
confirma que a él sí le había funcionado en algún momento después del 21 de agosto** en un
build real de Play, así que no es que la propagación nunca se completara — parece haber
**reaparecido**.

Auditoría de código hecha hoy sin el móvil delante (el usuario estaba en el trabajo, sin
acceso al Redmi): **no se encontró ninguna causa en el código**.
- `app/google-services.json` (local, fuera de git) tiene las 3 huellas OAuth — verificado
  con `npx firebase apps:android:sha:list` **y** leyendo el JSON: debic, release y la de
  firma de Play (`b6d0bf6d...`) las 3 presentes.
- `default_web_client_id` de `strings.xml` coincide con el cliente web del
  `google-services.json`.
- Ningún commit reciente (Billing 8.0.0, reCAPTCHA/SoLoader, moderación, Premium, "Qué pido
  aquí") toca `AuthScreen.kt`, el `AndroidManifest.xml` ni nada del flujo de login.
- Detalle del código que explica el síntoma "sin ningún error visible": en
  `AuthScreen.kt`, el `launcher` del selector de cuentas solo pone `errorMessage` si
  `resultCode == RESULT_OK` pero falla la `ApiException`. Si Google cierra el selector con
  `RESULT_CANCELED` (que es lo que pasaría si bloquea por la huella o cualquier motivo a
  nivel de Play Services), **la app no muestra nada** — coherente con que la causa esté un
  nivel por debajo del código de la app.

**Pendiente para la próxima vez que haya acceso al Redmi**: conectar por ADB
(`& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices`, el `adb` normal no
está en el PATH) y sacar `logcat` en vivo mientras se intenta el login, filtrando por
`Auth`/`GoogleSignIn`/`ApiException`, para ver si es exactamente el mismo cierre silencioso
de antes o algo distinto. Sin ese log no hay más que investigar por código — ya se agotó esa
vía hoy.

**Pendiente sin prisa para otra sesión — avisos de calidad de Play Console** (vistos ya en la
versión 6 y siguen en la 7, que se aprobó igual para prueba cerrada — **no bloqueantes**, no
son vulnerabilidades, sin relación con el bug del login):
1. **Alineación de 16 kB**: alguna librería nativa del AAB está compilada con una versión
   antigua del NDK sin alinear a páginas de memoria de 16 kB ("Tu aplicación podría fallar en
   dispositivos de 16 kB" / "Vuelve a compilar tu aplicación con la alineación de bibliotecas
   nativas de 16 KB"). Hay que identificar qué dependencia nativa lo causa (candidatos:
   SoLoader, Places, alguna de Firebase) y actualizarla.
2. **Vista de extremo a extremo (edge-to-edge)** — visto por primera vez en la sesión del
   2026-09-02: como `targetSdk = 36`, Android 15+ fuerza edge-to-edge por defecto y Play avisa
   de que la app usa APIs/parámetros obsoletos para gestionarlo, y de que puede no mostrarse
   bien para todos los usuarios. Requiere revisar `enableEdgeToEdge()` y el manejo de
   `WindowInsets` pantalla por pantalla, con pruebas visuales en un dispositivo/emulador
   Android 15+ — trabajo de UI, no un cambio de una línea. Sin empezar todavía.

**No mandar el mensaje de reclutamiento de testers hasta confirmar que el login funciona**:
si a los 12 testers les pasa lo mismo que al hermano del usuario, se quema la primera
impresión con gente que cuesta mucho convencer de que lo vuelva a intentar. El mensaje ya
está redactado (dos versiones, una directa y otra con más gancho) con los enlaces reales:
- Grupo: `https://groups.google.com/g/foodranker-testers`
- Opt-in de prueba cerrada: `https://play.google.com/apps/testing/com.app.foodranker`
(Ojo: NO usar `https://play.google.com/apps/internaltest/...` — es de la pista de prueba
interna, no cuenta para el requisito de 12 testers/14 días de la prueba cerrada.)

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
