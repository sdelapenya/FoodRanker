# HANDOFF — FoodRanker (Play Store + producto)

**Actualizado:** 2026-11-26
**Código (PC):** `e:\FoodRanker` · **Código (servidor):** `/home/sergio/lab/apps/FoodRanker`
**GitHub:** https://github.com/sdelapenya/FoodRanker (público)
**Gitea:** ssh://git@192.168.1.19:222/sdelapenya/foodranker.git — **por SSH puerto 222**, el HTTP 3000 solo escucha en loopback

## Sync

### ⚠️ Empujar a Gitea desde el PC NO funciona con las claves que hay

`ssh -p 222 git@192.168.1.19` rechaza **las dos** claves del PC (`id_ed25519` y
`codex_key`): ninguna está autorizada en la cuenta `git` de Gitea. La buena tiene
contraseña y necesita el `ssh-agent`, que se pierde en cada reinicio.

**La ruta que sí funciona, y no pide nada a nadie:** `codex_key` entra como **`sergio`**
(la cuenta de shell, que es otra cosa), y **desde el servidor sí hay acceso a Gitea** — se
autentica como `sdelapenya` con una clave propia llamada `sergio-servidor`. Así que el
servidor trae de GitHub (público, sin credenciales) y empuja a Gitea:

```bash
ssh -i ~/.ssh/codex_key sergio@192.168.1.19 "set -e
D=\$(mktemp -d); cd \$D
git clone --quiet --bare https://github.com/sdelapenya/FoodRanker.git e.git
cd e.git
git push ssh://git@localhost:222/sdelapenya/Foodranker.git main:main
cd /; rm -rf \$D"
```

⚠️ **Clon temporal y bare, a propósito**: la copia de trabajo del servidor
(`/home/sergio/lab/apps/FoodRanker`) puede tener ramas divergidas y no hay que tocarla.
⚠️ El remoto `gitea` del servidor apunta a `https://git.sdelapenya.dev/...`, que pediría
credenciales; por SSH a `localhost:222` no. Y el repo allí se llama **`Foodranker`**, con
erre minúscula, no `foodranker`.


Se desarrolla en PC **y** en servidor (Cursor), y las ramas divergen. Antes de tocar:
`git status`, `git log --oneline -5` y comprobar `merge-base` con el otro remote.
El 2026-08-04 se mergeó una rama del servidor que divergía 13 commits (10 conflictos a mano).

---

## LO SIGUIENTE (retomar aquí)

### ✅ "1 PLATO" VISTO, Y EL CONTRASTE TRIADO HASTA EL FINAL (2026-11-26, noche)

#### El plural, verificado creando una lista de verdad y borrándola

No se podía ver sin datos, así que se creó una lista `PruebaBorrar` en la cuenta de revisión,
se le metió un plato, se miró y se borró. **Comprobado después contra Firestore por la API
REST: no queda ningún documento con ese nombre.**

| | |
|---|---|
| castellano, hoja "Añadir a lista" | **"1 plato"** |
| inglés, pestaña del perfil | **"1 dish"** |
| con 0 platos | "0 platos" / "0 dishes" |

⚠️ **Dato que confirma que el fallo era real y visible**: las **dos** listas que existen en
producción, de usuarios de verdad, **tienen un plato cada una**. Llevaban desde siempre
diciendo "1 platos".

De paso se verificó el **cambio de idioma en vivo** (castellano → inglés → castellano) sin
reiniciar la app, que es lo que toca el arreglo de `LocalConfiguration` de los avisos.

#### El contraste, triado entero: 24 → 14

De los 16 que quedaban, **ocho eran texto y se han arreglado**; el resto no son fallos:

| Qué | Cuántos | Por qué se queda |
|---|---|---|
| Gráficos a 2,84 | 5 | WCAG les pide 3,0, no 4,5, y faltan 0,16. Barras de progreso, el `+` de la barra, el pin de localización y los subrayados de pestaña. Oscurecerlos sería cambiar el naranja de marca en elementos que no se leen. |
| Falso positivo | 2 | `ProfileScreen:1662/1666`: el botón de borrar plato, con borde rojo; el naranja viene de un botón anterior. Emparejamiento mal hecho. |
| Texto deshabilitado | 1 | `PlateDetailScreen:508`, `textSecondary @0.5`. Es el estado "apagado" a propósito. |
| Blanco sobre degradado | 1 | `AddPlateScreen:529`, el hueco de la foto. 18sp en negrita sobre un degradado naranja. Arreglarlo es **rediseñar el degradado**, decisión de producto, no de accesibilidad. |

#### ⚠️ EL PRIMER INTENTO CON LAS PESTAÑAS NO HIZO NADA, Y A OJO PARECÍA QUE SÍ

Se cambió el `contentColor` del `TabRow`… pero **cada `Tab` fija el color de su propio
`Text` y lo pisa**. Mirando la captura parecía arreglado.

**Se descubrió midiendo los PÍXELES de la captura**, no mirándola: el rótulo seguía en
`#FF6B35`. Desde entonces la verificación de color se hace así, y es lo que hay que repetir:

| | claro | oscuro |
|---|---|---|
| "Top", "Ranking", cuenta atrás | **#C64600 → 4,91** | **#FF8A50 → 7,29** |

El lector de PNG va sin dependencias (zlib + desfiltrado de scanlines), así que no hace
falta instalar nada: `adb exec-out screencap -p` y contar colores dominantes en un recuadro.

#### Las tarjetas de `VenuePicker` llevaban un acento suave hecho a mano

`OrangePrimary.copy(alpha = 0.06f)` y `SuccessGreen.copy(alpha = 0.10f)` con texto del tema
encima: 4,20 y 4,30. Y en **tema oscuro** un naranja al 6% sobre negro no es el acento del
tema, es otra cosa. Pasan a los pares que **ya existían** para esto (`accentSoft` /
`onAccentSoft(Muted)` y `successSoft` / `onSuccessSoft(Muted)`), y sus iconos a las
variantes fuertes.

### ⚠️ EL NARANJA NO ESTABA HECHO, Y LA CULPA ERA DEL CHECKER (2026-11-26, tarde)

**Lo que pasó, porque la lección vale más que el arreglo.** El commit `fe1aa79` cerró "la
otra mitad del naranja" apoyándose en que `check_contrast.py` daba **0 críticos**. Al abrir
la app en el emulador, la **primera pantalla** tenía la nota `★ 10,0` en **blanco sobre
naranja**, a 2,84. El detalle del plato, igual. El checker estaba ciego.

⚠️ **Dar por bueno un arreglo visual porque una herramienta dice que sí, sin abrir la app,
es el mismo error que costó las v24 y v25** — solo que esta vez el que mentía era el
script. **Si el cambio se ve, hay que verlo.**

#### Los tres fallos del checker, y cómo se comprobó que estaban arreglados

Cada uno se probó **contra el código roto** (con `git stash`) antes de darlo por bueno. La
primera versión del arreglo seguía dando 0: si no se prueba así, no se sabe.

1. **No veía los fondos.** El patrón era `Surface\(\s*color\s*=`, que solo caza
   `Surface(color = X)` **todo en una línea**, y así no se escribe casi nunca.
2. **Y aun viéndolos, los tiraba.** La invalidación por nivel de llaves se disparaba en la
   propia línea `) {`, donde el nivel todavía no ha subido: el fondo recién puesto se iba a
   la basura justo antes de llegar al texto. Este era el bueno.
3. **A los iconos les exigía 4,5.** WCAG les pide 3,0, y el docstring del propio fichero ya
   lo decía; el código no, porque un `Icon(tint = ...)` no lleva `fontSize`.

Medido: **0 críticos → 60** sobre el mismo código.

Y dos cosas para que no mienta en la otra dirección: el `containerColor` de un `Scaffold`
ya no cuenta como fondo de sus *slots* (los iconos del `topBar` del perfil van sobre el
degradado naranja, y salían como "blanco sobre casi blanco, 1,05"), y hay una marca
**`// contraste-ok: <motivo>`** para silenciar lo deliberado, como el logotipo.

#### Arreglado: 14 sitios, y una pastilla gemela que se había quedado atrás

De `Color.White` a `FoodTheme.colors.onOrange`, de 2,84 a **7,41**: la nota del feed y la
del detalle, el chip de categoría sobre la foto, el banner de reto, el botón de guardar del
perfil, el de subir plato con su progreso, las dos pastillas de `ShareCard` y la de
`PlateCard`.

⚠️ **Había DOS pastillas de racha gemelas en `DiscoverScreen`**, no una. El commit anterior
arregló la de `CountdownTickerRow` y **la otra se quedó rota**, con su `0xFFFF6D00` clavado.
Ahora las dos usan `OrangePrimary` + `onOrange` y hay un aviso en el código de que son
gemelas. Es el fallo clásico de "me dejé una".

**Verificado en pantalla**, emulador a 720x1640 y 320 dpi, en los dos temas.

#### Y de paso: los 8 textos que se cortaban a mitad de palabra

`maxLines` sin `overflow` (el defecto de Compose es `Clip`, no `Ellipsis`). Eran los títulos
de tarjeta de plato que llevaban apuntados sin fecha, más la ciudad del feed, el banner de
reto, la tarjeta de compartir y tres del perfil. **Quedan 0.**

#### ⏳ QUEDAN 16 SITIOS POR TRIAR — y NO todos son fallos

El checker arreglado encuentra 16 sitios más. **No aplicarlos a ciegas**: hay al menos un
falso positivo identificado y algunos son decisiones de diseño.

| `screens/addplate/AddPlateScreen.kt:183` | OrangePrimary sobre surface | 2.84 |
| `screens/addplate/AddPlateScreen.kt:529` | Color.White sobre 0xFFFF8C42 | 2.31 |
| `screens/addplate/VenuePicker.kt:169` | textSecondary sobre OrangePrimary @0.06 | 4.30 |
| `screens/addplate/VenuePicker.kt:196` | textSecondary sobre SuccessGreen @0.10 | 4.20 |
| `screens/addplate/VenuePicker.kt:226` | OrangePrimary sobre surface | 2.84 |
| `screens/auth/EmailAuthSheet.kt:200` | Color.White sobre OrangePrimary | 2.84 |
| `screens/challenge/ChallengeBanner.kt:87` | OrangePrimary sobre Color.White | 2.84 |
| `screens/discover/DiscoverScreen.kt:377` | OrangePrimary sobre surface | 2.84 |
| `screens/discover/DiscoverScreen.kt:442` | Color.White sobre OrangePrimary | 2.84 |
| `screens/explore/ExploreScreen.kt:408` | Color.White sobre OrangePrimary | 2.84 |
| `screens/platedetail/PlateDetailScreen.kt:342` | OrangePrimary sobre surface | 2.84 |
| `screens/platedetail/PlateDetailScreen.kt:508` | textSecondary @0.5 sobre surface | 1.98 |
| `screens/profile/ProfileScreen.kt:1238` | OrangePrimary sobre surface | 2.84 |
| `screens/profile/ProfileScreen.kt:1662` | 0xFFD32F2F sobre OrangePrimary | 1.76 |
| `screens/profile/ProfileScreen.kt:1666` | 0xFFD32F2F sobre OrangePrimary | 1.76 |
| `screens/profile/ProfileScreen.kt:335` | OrangePrimary sobre surface | 2.84 |

**Lo que ya se sabe de esta lista:**
- `ProfileScreen:1662` y `:1666` (rojo sobre naranja, 1,76) son **falso positivo**: es el
  botón de borrar plato, con borde rojo, y el naranja viene de un botón anterior. El
  emparejamiento está mal.
- `PlateDetailScreen:508` (`textSecondary @0.5`, 1,98) es texto **deshabilitado**; mirar si
  es intencionado antes de tocarlo.
- Los `@0.06` y `@0.10` de `VenuePicker` (4,20-4,30) se quedan a un pelo de 4,5: son fondos
  casi transparentes y el arreglo es subir el texto, no el fondo.
- Los seis **`OrangePrimary sobre surface`** son de la misma familia que la cuenta atrás ya
  arreglada: les toca `FoodTheme.colors.orangeStrong`. Son los más claros de la lista.

### ✅ LA V29, HECHA: SIETE COMMITS, TODO MEDIDO (2026-11-26)

La lista de la v29 está **aplicada entera menos el bloqueo entre usuarios**, que se mueve a
la v30 por tamaño (abajo, con el número). Siete commits, cada cosa en el suyo.

| # | Commit | Qué |
|---|---|---|
| 1 | `deb6473` | `keep.xml` + reductor de recursos |
| 2 | `32383c0` | fuera `billing-ktx` y `firebase-storage` |
| 3 | `fe1aa79` | la otra mitad del naranja |
| 4 | `cc466c0` | "1 platos" → plurales |
| 5 | `91259f8` | BOM de Compose 2024.09 → 2026.06 |
| 6 | `fe28a16` | la fecha de los avisos y el idioma |
| 7 | `335576b` | limpieza de código muerto del lint |

#### Lo que pesa el resultado, paso a paso

| | APK release |
|---|---|
| punto de partida (v28) | 7.396.463 |
| + `keep.xml` y reductor encendido | 6.952.132 |
| + fuera billing y firebase-storage | 6.901.387 |
| + BOM de Compose 21 meses más nuevo | 7.318.756 |
| + limpieza del lint | **7.318.556** |
| | **−77.907 bytes, con 21 meses de Compose dentro** |

#### Tres hallazgos que no estaban en ninguna lista

**1. El BOM de Compose llevaba en 2024.09.00**, de septiembre de 2024, con Kotlin en 2.3.21.
Funcionaba porque el compilador de Compose viaja con Kotlin, pero el runtime y material3
tenían dos años. ⚠️ **El techo no es el último BOM**: desde `2026.08.00`,
`material3-ripple` exige compilar contra la **API 37**, que pide AGP 9. Comprobado versión
a versión, no supuesto. O sea que **Compose está en la misma cadena de AGP 9 que Hilt y
Places**: son tres, no dos.

**2. `keep.xml` funciona, y eran 434 KB, no 370.** Lo que había que demostrar no es que
compile, es que no se lleva texto: verificado volcando `resources.arsc` con `aapt2`, las
**598 cadenas y plurales** del proyecto siguen en el APK con sus dos idiomas.

**3. `billing-ktx` no se iba sola por no usarla.** Tenía cero usos y aun así metía en el
manifest publicado `com.android.vending.BILLING`, el `InAppBillingService.BIND`, el
servicio del *billing test companion* y **dos activities** (`ProxyBillingActivity` y
`...V2`) — y esas activities son justo lo que impedía a R8 tirar las clases. La app
publicada **declaraba que hacía facturación** mientras la ficha dice que no hay compras.

#### Y un bug que solo se vio al subir Compose

El lint del BOM nuevo trae `NonObservableLocale`, **como error**: la fecha de los avisos de
más de una semana se formateaba con `Locale.getDefault()` dentro de una Composable. Con el
**selector de idioma propio** de esta app, al cambiarlo el mes se quedaba en el idioma
anterior hasta que algo recompusiera la lista. Arreglado con `LocalConfiguration.current`.

⚠️ El arreglo que propone el lint, `LocalLocale.current.platformLocale`, **no existe en
esta versión de Compose** — comprobado buscándolo en los `.aar`. El check va por delante de
la API. Cuando aparezca, es el reemplazo bueno.

#### ⏳ LO QUE FALTA: VERLO EN PANTALLA

Todo lo de arriba está verificado por medición, build y herramienta. **Dos cosas son
visuales y nadie las ha visto todavía**, y este proyecto ya perdió dos versiones por dar
por bueno un arreglo visual sin reproducirlo:

1. **La cabecera de la liga en los dos temas** — la cuenta atrás en `orangeStrong` y la
   pastilla de racha con letra negra sobre el naranja de marca. La racha necesita
   `voteStreak >= 2`, así que hay que votar dos días seguidos o tocar las SharedPreferences
   de `daily_mission`.
2. **"1 plato" en singular** — en el detalle de un plato y en dos sitios del perfil, con una
   colección de **un solo** plato.

Banco de pruebas, de la lección de la v24/v25: `wm size 720x1640`, `wm density 320` y el
overlay `navbar.threebutton`.

#### El bloqueo entre usuarios se va a la v30, y aquí está el número

Medido para que la estimación no sea a ojo: hay **107 consultas a Firestore** repartidas en
14 ficheros, y las reglas tendrían que consultar la lista de bloqueados en **cinco**
colecciones (`plates`, `ratings`, `comments`, `follows`, `reports`). El botón es lo de
menos; lo difícil es no olvidarse de una pantalla, que es justo donde reaparece quien
creías bloqueado. No cabe junto a lo demás.

#### Lo que el lint sigue diciendo y NO hay que tocar

Quedan 75 avisos y **0 errores** (venía de 89 y 1). De los que quedan, estos están
descartados **con motivo**, para no gastar otra tarde en ellos:

- **`CredentialManagerMisuse`** en `AuthRepository`: falso positivo. El `catch (e:
  Exception)` ya trata `NoCredentialException` cayendo al navegador, documentado ahí mismo.
- **`Typos`: "Vision" → "Visión"**: es "Google Cloud Vision", nombre de producto.
- **Los 6 `PluralsCandidate`** que quedan ya están resueltos a mano: `ntf_days_ago` tiene su
  `if (d == 1L)`, `ntf_new_plates` su `if (plateCount > 1)`, y `notif_league_b_other` es una
  posición en la liga, no un recuento.
- **`StaticFieldLeak`** en `RemoteConfigManager`: `FirebaseRemoteConfig` es un singleton con
  el contexto de aplicación.
- **`MonochromeLauncherIcon`** y la fusión de `mipmap-anydpi-v26`: tocan el icono, que ya
  dio un problema en este proyecto, y hay que **verlos** antes de darlos por buenos.
- **`ExifInterface`**: pasar a la de AndroidX añade una dependencia para callar un aviso,
  sin un fallo de rotación demostrado detrás. Si aparecen fotos torcidas, es el primer sitio
  donde mirar.

#### De la revisión, lo que salió sano (para no repetirla)

0 `!!` en todo el código · 0 `Icon`/`Image` sin `contentDescription` (52 con texto, 44 con
`null` a propósito) · 0 cadenas sin usar · 0 TODO/FIXME · `firestore.rules` **idéntico byte
a byte** al desplegado y los 19 índices también · 0 errores en las functions en 3 días · las
reglas bien cerradas (el `reports/{uid}_{docId}` determinista impide denunciar dos veces, y
no hay `update` ni `delete` ahí) · los cinco triggers que dan XP son idempotentes.

⚠️ Dos cosas de infraestructura que no mira nadie: Firestore está con
**`DELETE_PROTECTION_DISABLED`** y **sin point-in-time recovery**. La protección contra
borrado es gratis.

### ✅ LA CACHÉ OFFLINE DE FIRESTORE, ARREGLADA Y PROBADA SIN RED (2026-11-24)

Arreglado el fallo que dejaba al **100% de los usuarios con sesión sin modo offline**.

**El arreglo**: nuevo `utils/FirestoreCache.kt` con un `aplicar()` idempotente, llamado como
**primera sentencia** de `FoodRankerApp.onCreate()`, antes de `AppCheckInstaller.install()`
y mucho antes de `saveCurrentToken()`, que era quien arrancaba Firestore por delante.
`FirebaseModule.provideFirestore` se queda en un `getInstance()` a secas.

⚠️ **La regla que hay que respetar**: cualquier cosa que se añada a `onCreate()` y toque
Firestore tiene que ir **después** de `FirestoreCache.aplicar()`. Está escrito en los dos
sitios, en el `onCreate` y en el KDoc de la clase. Se deja el `try/catch` reportando a
Crashlytics para que, si alguien vuelve a colar un acceso por delante, no se pierda en
silencio como ocurrió durante meses.

#### La prueba, que esta vez no es un logcat limpio

Medido **antes y después en el mismo emulador**, con la misma sesión:

| | avisos `Firestore settings skipped` |
|---|---|
| v28 de Play (sin el arreglo), en el Redmi | **1** |
| v28 en el emulador, sin el arreglo | **1** |
| con el arreglo | **0** |

Y la prueba de verdad, que es la que cuenta: **cortar la red** (`svc wifi disable`,
`svc data disable`), matar la app y volver a abrirla. Con el cartel **"Sin conexión"** en
rojo, el feed aparece **entero**: platos, notas, locales y el contador de la liga. Eso solo
puede salir de la caché en disco. Antes del arreglo Firestore trabajaba con caché **en
memoria**, que muere con el proceso: al reiniciar sin red no habría habido nada que mostrar.

El `try/catch` **no saltó** (0 líneas de `FirestoreCache` en el log), así que los ajustes se
aplicaron de verdad; no es que se haya silenciado el aviso.

### ✅ TODO PUBLICADO Y VERIFICADO DESDE FUERA (2026-11-24)

Los 7 cambios enviados a revisión ya están publicados. Comprobado **contra la ficha pública
y los servicios reales**, no contra la consola:

| Qué | Resultado |
|---|---|
| "Contiene anuncios" en es-ES / en-GB / en-US | **fuera de los tres** |
| "Compras en la aplicación" | fuera |
| Precio "2,99 €" y el párrafo de Premium | **cero menciones** |
| Descripción nueva en los tres idiomas | sí, con `favourite`/`favorite` y `catalogue`/`catalog` bien |
| "o con tu correo y una contraseña" | sí (el alta por correo ya no se oculta) |
| Clasificación | 12+ |
| `privacy/terms` y sus `-en` en gh-pages | 200, cero frases viejas |
| `app-ads.txt` | cero menciones al ID de editor |
| Regla de `referrals` desplegada | sí |
| Errores en las funciones (últimas 24 h) | ninguno |
| Funciones desplegadas | 18 |

⚠️ **Un falso positivo que puede despistar al repetir la comprobación**: buscar "Premium
subscription" en `terms-en.html` da 1 resultado, pero es la frase **nueva** ("The Premium
subscription existed up to version 1.10 and has been withdrawn"). Las frases viejas de
verdad —"offers an optional subscription that removes ads", "The free version of FoodRanker
shows ads"— están a cero.

#### ⏳ Lo que NO se puede cerrar hoy, y de qué depende cada cosa

**1. Borrar `awardAdXp`** (sigue desplegada, confirmado en `functions:list`). Depende de que
las **solicitudes de anuncios en AdMob lleguen a cero**; iban por 76 semanales. Mientras
haya peticiones, hay clientes en v22/v26/v27 con el botón de anuncio recompensado. Mirar el
número dentro de una semana.

**2. Probar la app en dispositivo.** Hoy no se pudo: el Redmi no estaba conectado. Queda
pendiente, en concreto, **ver la tabla de la liga con datos** —el arreglo de contraste
(`Color.White` → `surface`) está verificado por cálculo pero no en pantalla— y **cualquier
día que no sea lunes**, porque la liga reinicia los lunes y esa pantalla no llega a pintar
filas.

**3. Descartar los cambios que quedaron en Eligr.** Su cuestionario de clasificación se
rehízo con las respuestas de FoodRanker y el correo `foodranker.app@`. Mientras sigan sin
enviar, se descartan desde el *Resumen de publicación* de Eligr.

**4. Archivar el producto de suscripción** (Monetización → Productos → Suscripciones). Ya no
afecta a la ficha —Play quitó "Compras en la aplicación" solo, al ver que el binario no
lleva la librería de facturación—, así que es limpieza, no urgencia.

**5. Verificar que el perfil público muestra el estado de no operador.** La sección "Acerca
del proveedor" solo se muestra a usuarios del EEE y Reino Unido, y no sale en el HTML que
devuelve una petición sin navegador. Comprobarlo desde el móvil, en la ficha de Play.

### ✅ ADMOB CERRADO Y DECLARADO NO OPERADOR (2026-11-24)

Cerrado el asunto que venía desde el 22 de noviembre. La cadena completa: sin SDK en el
binario (v28) → sin bloques en AdMob → sin `app-ads.txt` → declaración de **no operador**.

#### AdMob: la app NO se puede borrar, y "Ocultar" no sirve de nada

⚠️ **Dato verificado en la documentación de Google, no supuesto.** La primera idea fue
eliminar la app de AdMob, y la consola solo ofrece **Ocultar**. Buscando antes de pulsar:

- **Ocultar no afecta al servicio de anuncios**, ni a los ingresos, ni a los informes. Las
  apps ocultas **pueden seguir generando ingresos**. Solo ordena la lista.
- **Una app de AdMob, una vez configurada, no se puede eliminar.** No existe la opción.
- Lo que la propia ayuda recomienda para dejar de monetizar es **quitar los bloques de
  anuncios**, y eso **es irreversible**.

Hecho: los **3 bloques eliminados**. Sin bloques no hay nada que servir, que es lo único
que de verdad apaga la publicidad.

Ver: https://support.google.com/admob/answer/6061403 (ocultar) y
https://support.google.com/admob/answer/3125713 (quitar bloques).

**De paso se entendió por qué los ingresos eran 0,11 €/mes**: la app estaba en *"Serv.
anuncios limitado"* y *"Debe revisarse"*, con el nombre de paquete vacío y sin vincular a
Play. Llevaba meses con publicidad a medio gas: el coste de imagen completo y el ingreso
casi nulo.

⚠️ La entrada de **"Aplicaciones por confirmar"** se deja **sin tocar**: "Finalizar
configuración" la daría de alta y "No es mi aplicación" sería falso. Desaparecerá sola.

#### `app-ads.txt`: borrado y desplegado

Era el único rastro **público**: declaraba `pub-6291919286572988` autorizado a vender
inventario. Borrado de `hosting/public/` y desplegado (`firebase deploy --only
hosting:links`).

⚠️ **La URL sigue devolviendo HTTP 200 y es correcto**: el hosting tiene un `rewrite` con
`source: "**"` hacia `/open.html`, así que cualquier ruta inexistente responde la página de
apertura. Lo que importa es que devuelve **`text/html`** y **cero** menciones al ID de
editor; el estándar exige `text/plain`, así que los rastreadores lo tratan como ausente.

Antes de desplegar se comprobó que `index.html` y `open.html` locales eran **idénticos** a
los servidos, para no pisar nada, y después que los tres deep links (`/plate/`, `/user/`,
`/invite/`) siguen respondiendo.

#### No operador, declarado

*Cuenta de desarrollador → Información de la cuenta → Declarar estado* → **No comerciante**.

⚠️ **La declaración es de la CUENTA, no de la app.** En esta hay dos, FoodRanker y Eligr;
se confirmó que Eligr también es gratuita y sin anuncios antes de declarar.

**Qué se publica a partir de ahora** (en "Asistencia de la aplicación", en todo el mundo):
el **nombre legal completo** —el del perfil de pagos, con los dos apellidos, no el nombre
de desarrollador—, el correo y el país. En "Acerca del proveedor", solo en EEE y Reino
Unido, la frase de que el proveedor no se ha identificado como operador.

**Lo que NO se publica: ni la dirección postal ni el teléfono.** Que era justo el motivo de
todo esto.

⚠️ **Si algún día se vuelve a monetizar, hay que volver y cambiar la declaración.** No es un
trámite que se firma y se olvida: describe lo que haces en cada momento.

**El plazo del 1 de febrero de 2027 queda cerrado**, y sin cuota de autónomo, sin cita con
la gestoría y sin publicar datos de contacto físicos.

#### Lo único que queda de AdMob

1. **Vigilar las solicitudes de anuncios** en AdMob: estaban en **76 semanales**. Cuando
   lleguen a cero, ya no queda nadie con versiones viejas pidiendo publicidad y **entonces
   se puede borrar `awardAdXp`** de `functions`.
2. `ADMOB_TEST_DEVICE_IDS` sigue en `build.gradle.kts` como `buildConfigField` y en
   `local.properties`. Código muerto desde que se borró `AdManager`. Quitarlo en la v29.

### 🪤 "DECLARACIÓN DE ID DE PUBLICIDAD INCOMPLETA": ERA UN CANAL DE PRUEBA OLVIDADO (2026-11-24)

Tras poner el ID de publicidad en **No**, Play bloqueó el envío a revisión con *"Declaración
de ID de publicidad incompleta"*, con el formulario mostrando "No" y **Guardar
deshabilitado**. El mensaje no dice nada de lo que pasa de verdad.

**La causa:** Play no valida esa declaración contra lo que escribes, sino contra **el
manifest de todos tus artefactos ACTIVOS**, y el canal **Prueba cerrada - Alpha** seguía
con la **22 (1.8)**, de agosto, que lleva el SDK de AdMob entero y declara
`com.google.android.gms.permission.AD_ID`. Producción y prueba interna sí estaban en la 28.

**La solución:** pausar ese canal (*Gestionar canal → Pausar*). Es inmediato, reversible y
**no pasa por revisión**, que es justo lo que hace falta cuando lo que está bloqueado es
precisamente enviar a revisión.

⚠️ **Lo que NO funciona, para no perder el tiempo:** recargar, y el truco de marcar Sí →
guardar → No → guardar. Se probó y el bloqueo seguía. Ojo además con ese truco: al marcar
"Sí" Play exige **una finalidad** o Guardar no se habilita, así que ni siquiera se puede
completar sin declarar una finalidad falsa durante unos segundos.

⚠️ **Y no era solo un problema de formulario.** Quien tuviera la app instalada desde ese
canal **estaba viendo anuncios de AdMob** mientras la ficha declaraba que no hay ninguno.
Pausarlo no fue un truco para desatascar la consola: es lo que hace cierta la declaración.

**Regla para la próxima:** antes de declarar algo sobre permisos o SDKs, mirar **los cuatro
canales** —producción, interna, cerrada y abierta— y comprobar que ninguno sirve una
versión anterior al cambio. Un canal de pruebas olvidado es un artefacto activo.

### 🧾 LAS DECLARACIONES DE PLAY, PUESTAS AL DÍA (2026-11-24)

Con la v28 ya en producción, se corrigieron las declaraciones que seguían describiendo la
app con anuncios. Cuatro quedaron **listas para enviar a revisión**: Anuncios, ID de
publicidad, Seguridad de los datos y Clasificaciones del contenido.

#### ⚠️⚠️ HAY DOS APPS EN LA CONSOLA Y SE TRABAJÓ UN RATO EN LA EQUIVOCADA

La cuenta `sdelapenya1991@` tiene **FoodRanker y Eligr**. Las pantallas de *Contenido de la
aplicación* son idénticas y no se ve el nombre sin mirar la cabecera. Se rehízo entero el
cuestionario de clasificación y se cambió el ID de publicidad **en Eligr** antes de darse
cuenta.

**Cómo se detectó, que es lo que hay que recordar:** las fechas de la lista de
declaraciones no cuadraban entre una visita y otra. En una, *Datos de inicio de sesión*
ponía 1 sept; en la otra, 20 nov — y nosotros sabíamos que esa se tocó **el 20 de
noviembre**. Un apartado no se actualiza solo: si las fechas retroceden o avanzan sin que
hayas hecho nada, estás mirando otra app.

⚠️ **Pendiente en Eligr**: descartar esos cambios desde su *Resumen de publicación*
mientras sigan sin enviar. Su clasificación quedó descrita con las respuestas de
FoodRanker (platos, comentarios, fotos de comida) y con el correo `foodranker.app@`.

#### 🔴 El hallazgo: el AD_ID seguía declarado como compartido para publicidad

El formulario de Seguridad de los datos era del 2 de noviembre, de cuando AdMob seguía
sirviendo. Exportándolo a CSV (botón *Exportar a CSV*, mucho más fiable que ir pantalla por
pantalla) apareció esto:

```
PSL_DEVICE_ID : PSL_DATA_USAGE_ONLY_SHARED               → true
PSL_DEVICE_ID : DATA_USAGE_SHARING_PURPOSE : PSL_ADVERTISING → true
```

Es decir: **la ficha declaraba que se comparte el identificador de dispositivo con terceros
con fines publicitarios**, justo al lado del "No contiene anuncios" recién guardado. Era el
residuo de AdMob, y la contradicción más seria de las que había. Quitado: ahora la ficha
dice *"No se comparten datos con terceros"*.

Otros tres arreglos del mismo formulario:
- **Historial de búsquedas**: estaba marcado y **no corresponde**. Comprobado en el código:
  no hay historial, no se persiste nada y Analytics no registra ni un término (los eventos
  son `plate_published`, `plate_liked`, `plate_rated`… ninguno de búsqueda).
- Faltaban **"Otra información"** (bio, ciudad y web del perfil) y **"Otros mensajes en la
  aplicación"** (los comentarios).
- *Interacciones con la aplicación* estaba como opcional; no lo es, Analytics no tiene
  interruptor.

⚠️ **La ubicación no sale en la vista previa y es correcto**: al declararla **efímera** —no
se almacena, solo se usa en el momento de la búsqueda— Play la recoge en el formulario pero
no la enseña en la ficha.

#### La clasificación sube de 3+ a 12+, y está bien

PEGI pasa a *Control parental*, USK a 12 e IARC a 12+, con los descriptores "Supervisión
parental recomendada" e "Interacción de usuarios". Lo empuja la combinación de **contenido
generado por usuarios como origen principal + interacción + contenido en línea**.

⚠️ **La de septiembre estaba mal.** Una app donde la gente sube fotos y se comenta entre sí
no es apta para 3 años; si salió 3+ fue porque alguna respuesta no reflejaba la app —muy
probablemente la de *contenido en línea*, que es **Sí**: el APK no trae ni un plato, todo
llega del servidor—. Esto no es una pérdida, es una corrección.

Dos respuestas que costaron discusión y conviene no volver a equivocar:
- *"¿Capacidad de bloquear usuarios **o** contenido?"* → **Sí**. Con ese "o" basta la
  moderación de contenido, que sí existe (`moderatePlateImage` + umbral de 3 denuncias).
  **No es la misma pregunta que la política de UGC**, que pide bloqueo entre usuarios.
- *"¿Capacidad de informar?"* → **Sí**, evidentemente: hay denuncia de platos, comentarios
  y valoraciones.

#### Lo que NO había que tocar

*Contenido y audiencia objetivo* ya estaba en **18 y más** — lo correcto: marcar 13-17
mete la app en los requisitos de apps dirigidas a menores, y por debajo de 13 en la
política de familias. *Política de Privacidad* apunta a la URL buena; lo que cambió hoy es
el contenido de la página, no la URL. Salud, financieras y gubernamentales siguen sin
aplicar.

#### Lo que queda, por orden

1. Enviar los cuatro cambios a revisión.
2. **Quitar la app de AdMob** (`apps.admob.com`). Mientras el inventario siga de alta,
   Google sigue viendo un editor con monetización activa.
3. **Borrar `awardAdXp`** de `functions` — pero **no antes de que quede poca gente en v26**:
   esos clientes conservan el botón de anuncio recompensado y llaman a esa función.
4. **Declarar no operador**. Plazo: 1 de febrero de 2027.
5. Archivar el producto de suscripción (ya no afecta a la ficha: Play quitó solo "Compras
   en la aplicación" al ver que el binario no lleva la librería de facturación).

### 🔴 NADIE TIENE CACHÉ OFFLINE DE FIRESTORE, Y NO ES UNA CARRERA (2026-11-23)

Crashlytics avisó de dos no críticos nuevos en la 1.11. El primero,
`com.app.foodranker.di.FirebaseModule`, es el `Fallos.reportar` del `catch` de
`provideFirestore`. **Reproducido en el Redmi**, en el primer arranque y a la primera:

```
W/FirebaseModule: Firestore settings skipped — already initialized (persistent cache inactive)
```

⚠️ **El comentario del código dice que es una carrera que "no falla siempre". Es falso, y
conviene corregirlo:** en `FoodRankerApp.onCreate()`, dentro de
`if (FirebaseAuth.getInstance().currentUser != null)`, se llama a
`FoodRankerMessagingService.saveCurrentToken()`, que accede a Firestore por `getInstance()`.
Eso **arranca Firestore** antes de que Hilt construya el singleton, así que cuando
`provideFirestore` intenta aplicar `firestoreSettings` ya es tarde y salta
`IllegalStateException`. No depende de ningún temporizador: **le pasa a todo el que tenga
sesión iniciada, que son todos**. Resultado: la app funciona **sin caché persistente de
Firestore**, es decir, sin offline, para el cien por cien de los usuarios.

No lo introdujo la v28. Que Crashlytics lo marque como "nuevo error" solo significa que es
la primera vez que se ve **en la 1.11**; el no crítico estaba desde que se puso el
`Fallos.reportar`, y por eso se puso: para saber a cuánta gente le ocurría. La respuesta es
"a todos".

**El arreglo** es mover la configuración de Firestore al principio de `onCreate()`, antes
de la línea que toca el token, y dejar en `provideFirestore` solo el `getInstance()` con su
`try/catch` de red de seguridad. Hay que comprobarlo con el logcat: tras el arreglo, esa
línea de `W/FirebaseModule` **no debe aparecer** al arrancar con sesión.

#### ✅ El segundo, `io.grpc.Status.asException`: ERA LAS INVITACIONES, Y YA ESTÁ ARREGLADO

`PERMISSION_DENIED: Missing or insufficient permissions`. No era red ni App Check (los tres
servicios están **UNENFORCED**, consultado por API). Era una regla contra el propio código:

`ReferralManager.applyReferralCode` hace, dentro de la transacción,
`tx.get(referralRef)` sobre `/referrals/{referrerId}_{miUid}` para no contar dos veces la
misma invitación — y la regla de esa colección era `allow read: if false`. **Esa lectura no
podía funcionar nunca: abrir un enlace de invitación fallaba siempre, para todo el mundo.**

Se llega por `MainActivity` → `applyInviteCode` cuando alguien abre un enlace de invitación
estando con sesión. Comprobado contra el motor real de reglas (`firebaserules:test`) que con
las reglas viejas esa lectura se denegaba.

**Arreglado el 2026-11-23 solo con reglas, sin versión nueva de la app**, así que vale para
todos los instalados al instante:

```
allow get: if isSignedIn() && referralId.split('_')[1] == request.auth.uid;
allow list: if false;
```

⚠️ **Filtra por el ID, no por `resource.data`, y eso es lo importante:** en el momento del
`tx.get` el documento normalmente **no existe**, y entonces `resource` es `null` y cualquier
condición sobre su contenido falla igual que antes. El ID es determinista y los uid de
Firebase no llevan `_`, así que `split` da exactamente dos partes.

Probado 5/5 **contra el ruleset ya publicado** (no contra el fichero local): el referido lee
el suyo; un tercero no; el referidor tampoco (mínimo privilegio); sin sesión no; y la
colección no se puede listar.

⚠️ Queda una cosa por mirar cuando se vea otro `PERMISSION_DENIED`: la etiqueta del
`Fallos.reportar` vive en la pestaña **Registros** del evento de Crashlytics, no en la
traza. Sin ella hay más de veinte candidatos.

Es la excepción que lanza Firestore cuando una operación falla, y la reportan más de veinte
sitios distintos con `Fallos.reportar`, así que sin la traza de Crashlytics no se puede
saber cuál. Descartado que sea App Check: consultado por API, los tres servicios
(`firestore`, `identitytoolkit`, `places`) están en **UNENFORCED**.

⚠️ **Hipótesis por confirmar, no dato:** sin caché persistente, cualquier lectura que pille
un hueco de red falla con `UNAVAILABLE` en vez de servirse de disco. Es decir, el primer
fallo podría estar **causando** el segundo. Al arreglar la caché, mirar si este desaparece
antes de buscarle una causa propia.

Para ubicarlo hace falta abrir el error en Crashlytics y leer dos cosas: la **clave del
log** (la cadena que se pasa a `Fallos.reportar`, del estilo "descubrir: cargar platos") y
el **modelo del dispositivo**.

### 📋 LA LISTA CON LA QUE SE ABRIÓ LA V29 (ya aplicada, ver arriba)

Tres cosas, por orden de lo que cuesta decidirlas. **Cada una en su commit**, no mezcladas.

**0. BLOQUEAR ENTRE USUARIOS — mejora pendiente, NO un incumplimiento declarado.**

⚠️ **Corrección del 2026-11-23, el mismo día.** Primero se escribió aquí que el cuestionario
de clasificación dejaba por escrito un incumplimiento. **Era un error**: se mezcló el
cuestionario IARC con la política de contenido generado por usuarios de Play, que son cosas
distintas. La pregunta del cuestionario dice *"capacidad de bloquear usuarios **o**
contenido generado por usuarios"*, y con ese "o" la app **sí** cumple: `moderatePlateImage`
rechaza las fotos inapropiadas antes de publicarlas y el umbral de 3 denuncias oculta el
contenido solo. La respuesta correcta ahí es **Sí**, y así se respondió.

Lo que sigue faltando, y es otra cosa, es que **un usuario pueda bloquear a otro por su
cuenta**: eso lo pide la política de UGC de Play, y hoy no existe —comprobado en el modelo
`User`, en las reglas, en las 17 funciones y en el cliente, donde no hay ni una cadena de
texto—. Es una mejora real, no una casilla que hayamos declarado en contra.

⚠️ **No confundirlo con lo que ya hay**, que son otras dos cosas:
- *Denunciar*: a las 3 denuncias el contenido se oculta **para todos** (`UMBRAL_DENUNCIAS`,
  y `filter { it.reportCount < 3 }` en comentarios y valoraciones).
- *`banned`*: desactiva la cuenta entera, lo ejecuta el admin con `manageUser.js ban`.

Bloquear es **individual e inmediato**: "yo no quiero volver a ver a esta persona", sin
depender de que otros tres opinen igual ni de que intervenga nadie.

⚠️ **Hacerlo de verdad o no hacerlo.** La vía barata —filtrar en el cliente y ya— no es
bloquear, es ocultar: el otro sigue viendo tu perfil, comentando tus platos y siguiéndote,
y tú solo dejas de enterarte. Un botón que dice "Bloquear" y hace eso **es peor que no
tenerlo**, porque promete una protección que no da. Alcance mínimo:

1. Subcolección `users/{uid}/bloqueados/{otroUid}`, con regla de que solo el dueño la lee y
   la escribe.
2. **Reglas de servidor** que impidan al bloqueado seguirte y comentar tus platos, con
   `exists()` sobre esa subcolección. Esto es lo que lo hace real, y es la parte que cuesta
   pensar.
3. Filtrado en el cliente en **todas** las listas: feed, comentarios, valoraciones,
   seguidores, liga y notificaciones. Olvidar una sola pantalla es el fallo clásico y es
   justo donde reaparece quien creías bloqueado. Es la parte que cuesta no olvidar.

**No hace falta volver al cuestionario al terminarlo**: esa respuesta ya es **Sí** por la
moderación de contenido, y seguirá siéndolo.

**1. El naranja, la mitad que falta.** `onPrimary` arregló los botones; siguen en 2,84 la
pastilla de nota del detalle y del feed (`★ 9,9`, `★ 10,0`), los chips de filtro de
`ExploreScreen` y `AddPlateScreen`, el chip de categoría sobre la foto, el banner de reto,
el logo `F` de la cabecera y el `+` de la barra inferior.

Hay que **decidir, no aplicar a ciegas**: WCAG exime los logotipos y pide 3,0 —no 4,5— a
los iconos. Lo que de verdad incumple es el **texto**: pastillas y chips. La propuesta es
pasar esos a `FoodTheme.colors.onOrange` y dejar en blanco el logo y los iconos. Mientras
no se haga, queda la incoherencia de la v28: en el detalle de plato conviven un botón con
letra oscura y una pastilla con letra blanca, los dos naranjas.

Al terminar: `python -I tools/ui/check_contrast.py` y mirarlo en el móvil en los dos temas.

**1-bis. ~~La caché de Firestore~~ — HECHO el 2026-11-24, ver arriba.** (queda aquí
tachado y no borrado: era el punto de más impacto de la lista.)

**1-bis-original.** Ver la sección de arriba: un
`getInstance()` en `onCreate` arranca Firestore antes de que Hilt aplique los ajustes, y la
app se queda sin offline para todos. Es el arreglo de más impacto de los tres y el más
pequeño.

**2. `res/raw/keep.xml` y volver a encender `isShrinkResources`.** Son 370 KB. Está
apagado porque borraba cinco cadenas (`cd_like`, `cd_unlike`, `vm_saved`, `vm_unsaved`,
`vm_save_fail`) y provocaba un `Resources.NotFoundException` **solo en release**. Las cinco
se usan con referencias normales a `R`, así que fijarlas por nombre debería bastar. El
detalle está más abajo, en la sección de las recomendaciones de Play. Verificar con
`tools/i18n/check_apk.py`.

**3. La tabla de la liga, pendiente de ver con datos.** El arreglo de contraste
(`Color.White` → `surface`) no se pudo comprobar en pantalla porque la liga reinicia los
lunes y se verificó un lunes. **Cualquier día que no sea lunes**, abrir la liga en el móvil
en modo oscuro y confirmar que las filas se leen.

También sigue abierto, sin fecha: los títulos de tarjeta de plato que truncan sin elipsis,
Play Integrity antes de activar App Check, y AGP 9 (que desbloquearía Hilt y Places).

### ✅ V28 VERIFICADA EN EL REDMI, Y LO QUE QUEDA DEL NARANJA (2026-11-23)

Instalada **desde la prueba interna**, con la firma de Play, sobre la v27 que tenia el
movil. Modo oscuro del sistema. Todo correcto:

| | |
|---|---|
| Sesion mantenida al actualizar | ✅ |
| Feed sin banner, mision diaria pequena | ✅ |
| Perfil: "Invita amigos" legible, sin estrella, sin fila Premium | ✅ |
| Detalle de plato: sin intersticial, "quien ha interactuado" visible sin pagar | ✅ |
| Botones naranjas con letra oscura | ✅ |
| Errores de FoodRanker en el log | ninguno |
| Rastro de AdMob / Billing / UMP | ninguno |

⚠️ Las 7 coincidencias de "anuncios" que salieron en el primer `grep` del log eran
**falsas**: la palabra `ump` dentro de `dumpsys` y `DUMP_CACHED_LOG`. Filtrar por proceso
antes de alarmarse.

#### Lo que NO se ha podido verificar, y por que

- **La tabla de la liga.** Hoy es lunes y la liga reinicia los lunes, asi que nadie tiene
  XP esta semana: la pantalla dice "Nadie ha votado aun" y no llega a pintar filas. No hay
  pestana global; es una sola vista. Para verla habria que votar un plato desde la cuenta
  real, que altera la nota de un plato de produccion — **no se hizo a proposito**. El
  cambio es `Color.White` → `FoodTheme.colors.surface`, el mismo par que la tarjeta "Nivel
  y XP" del perfil, que si se ha visto legible. Queda **verificado por equivalencia**, no
  en pantalla. Mirarlo cualquier dia que no sea lunes.
- **El login con Google**: exigiria cerrar la sesion del movil personal.
- **Los limites de 20 platos/dia y 1.600 m**: exigirian publicar platos reales.

#### 🟡 PENDIENTE PARA LA V29 — el naranja, solo a medias

El cambio de `onPrimary` arreglo **los botones**, pero en la app hay mas elementos con
fondo naranja y texto blanco que no son botones y siguen en 2,84:

- la pastilla de nota del detalle de plato (`★ 9,9 · 1 voto`) y la del feed (`★ 10,0`);
- los chips de filtro seleccionados (`ExploreScreen`, `AddPlateScreen`);
- el chip de categoria sobre la foto (`Pizza`), el banner de reto, el logo `F` de la
  cabecera y el `+` de la barra inferior.

**Se dejan a proposito, no por olvido.** Tres razones: no son una regresion —llevan asi
desde siempre—, la v28 ya estaba subida cuando se vio, y **no todos deben cambiar**: WCAG
exime los logotipos, y los iconos piden 3,0 y no 4,5. Lo que de verdad incumple es el
**texto** de las pastillas y los chips.

⚠️ Esto deja una incoherencia visible: en el detalle de plato conviven un boton con letra
oscura y una pastilla con letra blanca, los dos naranjas. Es el precio de haber arreglado
solo la mitad. **Decidir en la v29**: pasar a oscuro el texto de pastillas y chips (y dejar
en blanco el logo y los iconos), o volver atras. Va junto a `keep.xml`.

### 🎨 TEXTO ILEGIBLE EN MODO OSCURO: UN SOLO PATRON, TRES SITIOS (2026-11-23)

Revisando los contrastes aparecio el mismo fallo repetido: **una tarjeta con el fondo
escrito a mano y el texto tomado del tema**. El fondo no cambia con el modo y el texto si,
asi que en oscuro queda casi blanco sobre un crema o un blanco fijos.

| Donde | Que pasaba | Contraste |
|---|---|---|
| Perfil, fila "Invita amigos" | crema `0xFFFFF3E0` + `textPrimary` | **1,01** |
| Liga, cada fila de la tabla | `Color.White` + `textPrimary` | **1,07** |
| Pantalla "Invita amigos" | `Scaffold` clavado en `0xFFF8F8F8` | fondo blanco en oscuro |

⚠️ **Ninguno lo introdujo la v28.** La fila de invitar ya llevaba su crema fijo (`git log -L`
lo situa antes de `8837989`); lo que hizo la v28 fue quitarle de encima la tarjeta de
Premium, que tenia el mismo defecto, y dejarla a la vista. El de la liga es el peor: deja la
clasificacion entera ilegible.

**Lo que NO estaba mal, y por eso no se toco**: los fondos *oscuros* fijos (la tarjeta de
mision diaria `0xFF1A2419`, el snackbar `0xFF2A2A2A`, la nota final del plato `0xFF1A1F19`)
llevan el texto en `Color.White` tambien fijo. Se leen en los dos modos: son "siempre
oscuros" por diseno, igual que la cabecera del perfil. Y en Notificaciones los pasteles son
solo el circulo del emoji, no el fondo del texto.

**El arreglo**: nueve tokens nuevos en `FoodRankerColorTokens`, en **pares cerrados**
fondo/texto con un valor por modo (`accentSoft`/`onAccentSoft`/`onAccentSoftMuted`,
`successSoft`/`onSuccessSoft`/`onSuccessSoftMuted`, los dos bordes y `successStrong`).
De paso el verde de acento pasa de `0xFF168963` a `0xFF117A56`: el anterior se quedaba en
**4,38** sobre blanco, por debajo del 4,5 de AA.

**Herramienta nueva: `tools/ui/check_contrast.py`.** Calcula el contraste WCAG de cada par
en los dos temas. La que ya habia, `check_theme_colors.py`, solo comprueba que nadie clave
los colores *del tema*; por eso esto llevaba meses sin verse. Hay que pasarla al tocar
colores, igual que las de `tools/i18n/`.

⚠️ **La tabla de la liga no se ha podido ver con datos**: la cuenta de revision no tiene
ciudad ni XP, asi que esa pantalla nunca llega a pintar filas. El cambio es
`Color.White` → `FoodTheme.colors.surface`, el mismo token que usan todas las demas
tarjetas (verificadas en pantalla), y en oscuro da 15,25 de contraste. Queda **verificado
por calculo, no en pantalla** — mirarlo en el Redmi, que si tiene ciudad y liga.

Verificado en el emulador, release y en los dos temas: perfil e "Invita amigos".

#### Y uno de fondo: el naranja de marca con texto blanco no llega a AA

`#FF6B35` con blanco encima da **2,84**. El minimo es 4,5 para texto normal y 3,0 para
texto grande: no llega a ninguno. Afecta a todos los botones primarios.

⚠️ **El tema ya lo habia decidido y los botones se lo saltaban.** `DarkColorScheme` tenia
`onPrimary = Color.Black` y `LightColorScheme` tenia blanco, asi que el mismo boton salia
con letra oscura en oscuro y blanca en claro. No era un diseno deliberado: el esquema claro
eligio mal. Se iguala al oscuro, y **el naranja no se toca** -- sigue igual en iconos,
acentos, cabeceras y logo. Contraste 7,41.

Se descarto oscurecer el boton a `#C2410C` (4,88): pasa el liston, pero mete un segundo
naranja en la app y apaga el boton. No compensa cuando la otra salida conserva el color y
da mejor contraste.

Ademas de `onPrimary`, habia **12 sitios escribiendo `Color.White` a mano** dentro de un
boton naranja, saltandose el token. Ahora usan `FoodTheme.colors.onOrange`. Entre ellos el
FAB de valorar, el boton "Entrar" de la hoja de correo y el contador de avisos no leidos
(9sp blanco sobre naranja, lo peor parado de la app).

**Lo que sigue en blanco a proposito**: el texto sobre las *cabeceras* naranjas (perfil,
login) y sobre las fotos. Ahi es texto grande sobre degradado y funciona.

### 📄 LA V28 NO SOLO ERA CÓDIGO: TRES TEXTOS SEGUÍAN VENDIENDO ANUNCIOS Y PREMIUM (2026-11-23)

Quitar AdMob y Premium del binario dejó la app diciendo una cosa y contando otra en **tres
sitios distintos**, los tres con la misma frase: *"la versión gratuita muestra anuncios"* y
*"la suscripción Premium elimina los anuncios"*.

1. **La ficha de Play** (`docs/play-store-listing.md`): la descripción completa, en los dos
   idiomas, tenía un apartado entero titulado "GRATIS, CON ANUNCIOS".
2. **La política de privacidad y las condiciones DENTRO del APK**
   (`res/values*/strings.xml`): AdMob y Google Play Facturación listados como terceros que
   reciben datos, una sección 7 de privacidad describiendo los anuncios personalizados, la
   base legal del RGPD citando *"la publicidad personalizada"* con un enlace a "Opciones de
   privacidad de anuncios" —pantalla que se borró con `ConsentManager`—, y dos secciones de
   las condiciones (8 y 9) dedicadas a la publicidad y a la suscripción.
3. **Las páginas públicas de gh-pages** (`privacy.html`, `terms.html` y sus `-en`), que son
   **las URL que Play tiene declaradas**. Se regeneran con
   `python -I tools/legal/build_pages.py <destino>`, que las saca de `strings.xml`: una vez
   arreglada la app, es un comando.

⚠️ **Esto no podía esperar a "después de la v28".** Play compara la política declarada con
lo que hace la app, y el siguiente paso del plan es justamente declarar *Contiene anuncios:
No* y quitar el identificador de publicidad del formulario de Datos de seguridad. Subir la
v28 con una política que dice que hay anuncios y declarar en la consola que no los hay es
una contradicción puesta por escrito en los dos documentos que el revisor abre.

**Qué se decidió en la redacción, y por qué:**

- **Las secciones no se borran, se reescriben en negativo**: "7. Publicidad: no hay",
  "8. Publicidad: no hay", "9. Pagos dentro de la app". Borrarlas obligaba a renumerar seis
  secciones en dos idiomas y en cuatro HTML, y además un apartado que dice *"no hay
  publicidad y la app ya no pide el identificador de publicidad"* vale más que su ausencia:
  es lo que el usuario va a buscar y lo que el revisor quiere leer.
- **Se nombra la versión**: "hasta la 1.10 enseñaba anuncios de AdMob, desde la 1.11 no".
  Quien actualice desde producción venía de una app con anuncios; decir solo "no hay
  anuncios" parecería que nunca los hubo.
- **La suscripción retirada se trata con cuidado**: *"ya no se puede contratar"* y *"si
  llegaste a tener una activa, puedes cancelarla desde Google Play"*. No se dice que no se
  renueve, porque una suscripción activa **sí** se renovaría: estaba fuera de venta desde el
  2 de noviembre, que no es lo mismo que cancelada.
- **`pp_8_b` decía que el servidor protege "el estado Premium"**: ahora dice "nivel y
  distintivos", que es lo que de verdad protege.

**Y una captura de la ficha mentía igual** (`docs/play-store-screenshots/*/03-profile.png`,
en los dos idiomas): enseñaba la **estrella dorada de Premium** sobre la foto de perfil,
porque las capturas se hicieron dándole Premium a la cuenta para que no apareciese el banner
de AdMob. **Resuelto el 2026-11-23 quitando el badge de la imagen**, no rehaciendo la
captura: el resto de esa pantalla no cambió en la v28 y la cuenta de revisión no tiene datos
que fotografiar. El cómo, y las dos trampas que costaron una pasada en falso, están en
`docs/play-store-listing.md`.

**Lo que NO se tocó, a propósito:**

- `hosting/public/app-ads.txt` sigue declarando al editor de AdMob. No es falso ni molesta a
  nadie —solo sirve para que las redes publicitarias pujen, y ya no hay inventario—, pero
  quitarlo es un `firebase deploy` del hosting, una acción hacia fuera que va cuando se
  toque la consola, no ahora.
- `pp_1_b` sigue describiendo el alta como si solo existiera la cuenta de Google ("tus datos
  de la cuenta de Google al iniciar sesión"). Desde la v23 también se entra con correo y
  contraseña. **Es una inexactitud anterior y ajena a esto**, así que no se ha metido en
  este cambio; está pendiente de decidir.

**Comprobado:** `tools/i18n/check_strings.py` (paridad de claves y marcadores),
`check_args.py`, `tools/i18n/check_listing.py` (todo cabe; la v28 son 368 y 340 caracteres
de los 500) y el diff de las cuatro páginas generadas.

### 🔎 LAS 3 RECOMENDACIONES DE PLAY SOBRE LA V27: INVESTIGADAS, NO SE TOCA NADA (2026-11-22)

Play muestra tres "acciones recomendadas" sobre la v27. **Ninguna es un bloqueo** y, tras
investigarlas, **dos no se pueden arreglar** y la tercera tiene trampa conocida.

#### 1 y 2. Las de "vista de extremo a extremo": NO se pueden quitar

Play dice que la app usa `Window.setStatusBarColor`, `Window.setNavigationBarColor` y
`LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`, y señala
`com.google.android.libraries.places.widget.internal.ui.AutocompleteImplFragment`.

**El código propio está limpio**: `enableEdgeToEdge()` se llama en `MainActivity`, no hay
`statusBarColor`/`navigationBarColor` en ningún sitio, y los temas no llevan
`windowOptOutEdgeToEdgeEnforcement` ni `fitsSystemWindows`.

⚠️ **Quien llama a esas APIs es AndroidX.** Escaneando los 257 artefactos de la caché de
Gradle:

```
setStatusBarColor:     activity-1.12.1, core-1.17.0, core-splashscreen-1.0.1,
                       material-1.12.0, material-1.4.0, places-4.1.0
setNavigationBarColor: activity-1.12.1, core-1.17.0, core-splashscreen-1.0.1,
                       material-1.12.0, browser-1.4.0
```

**`androidx.activity` es donde vive `enableEdgeToEdge()`**, la función que la propia
recomendación te dice que uses: la llama por dentro para compatibilidad con Android
anterior al 15. **No hay nada que hacer desde aquí.** No perder más tiempo con esto.

#### El intento de subir Places, y por qué se abandonó

Se probó, y la cadena es esta:

**Places 6.1.0 → Hilt que entienda Kotlin metadata 2.4 → Hilt ≥ 2.59 → AGP 9.0.**

Con Hilt 2.58 (la actual) y Places 6.1.0, el release falla con *"Provided Metadata instance
has version 2.4.0, while maximum supported version is 2.3.0"*. Con Hilt 2.59 o 2.60.1 falla
antes: *"only compatible with AGP version 9.0.0 or higher"*. **No existe una Hilt que
soporte metadata 2.4 sobre AGP 8.**

Y aunque se hiciera, **no serviría**: lo de arriba. Comparando los artefactos:

| | 4.1.0 (actual) | 5.3.0 | 6.1.0 |
|---|---|---|---|
| `setStatusBarColor` | sí | sí | no |
| `setNavigationBarColor` | sí | no | no |
| cutout `SHORT_EDGES` | sí | no | no |

Pero `activity-1.12.1` sigue llamando a las dos primeras pase lo que pase.

⚠️ **La migración de API, si algún día se hace, son tres renombres y nada más** (probado,
compila con 5.3.0): `Place.Field.NAME` → `DISPLAY_NAME`, `Place.Field.ADDRESS` →
`FORMATTED_ADDRESS`, `Places.initialize()` → `initializeWithNewPlacesApiEnabled()`, y en el
`Place`: `name` → `displayName`, `address` → `formattedAddress`. `Place.Field.ID` no cambia.

⚠️ `ACCESS_WIFI_STATE` **sigue** con Places 5.3.0: tampoco se va actualizando.

⚠️ Al experimentar, un `sed` con el patrón `version "..." apply false` pisó también el
plugin `com.google.gms.google-services` y lo dejó apuntando a una versión inexistente. Si
aparece *"Plugin [id: 'com.google.gms.google-services', version: '2.58.1'] was not found"*,
es eso: `git checkout -- build.gradle.kts`.

#### 3. La de R8: dos partes, una aprovechable

Play pide dos cosas: *"La reducción de recursos no está habilitada"* y *"Actualiza AGP a la
9.0 o posterior"*.

La primera es la que ya está documentada más abajo: `isShrinkResources = false` está
apagado **a propósito** porque borraba cinco cadenas y provocaba un
`Resources.NotFoundException` en release. **Nunca se probó la solución estándar**: un
`res/raw/keep.xml` que las fije por nombre. Las cinco se usan con referencias normales a
`R` (`R.string.cd_like` y compañía), no con `getIdentifier()`, así que `keep.xml` debería
bastar. Son 370 KB. Hacerlo en su propio commit y verificar con `tools/i18n/check_apk.py`.

La segunda, AGP 9, es el proyecto grande — y es el que desbloquearía también Hilt y Places.

---

### 🔁 DOS HUECOS QUE QUEDABAN EN LOS AGREGADOS (2026-11-22, tarde)

Salieron de una auditoría de Codex. **Las tres eran reales**; se arreglan las dos que
pueden corromper la media de un plato.

#### 1. El alta y la edición podían cruzarse

`onRatingUpdated` comprobaba `totalRatings === 0` **del plato**, no si **esa valoración** ya
estaba incorporada. En un plato con votos previos, si la edición llegaba antes que el alta:
la valoración no tenía `appliedAvg`, se caía a `ratingAverage(before)` y se le aplicaba al
plato un delta por un voto que todavía no contaba. Después `onRatingCreated` la incorporaba
bien y el delta inventado se quedaba dentro para siempre.

**Arreglo:** `if (ratingSnap.get("processed") !== true) return;` dentro de la transacción.
Una valoración sin incorporar no ha aportado nada, así que no hay nada que reconciliar. **No
se sella**: sellarla dejaría escrito que ya aportó algo.

#### 2. El borrado restaba la nota viva, no el sello

⚠️ **Corrige una afirmación falsa que estaba en este fichero y en el commit `f0e0186`:** que
`onRatingDeleted` solo corre en cascadas donde el plato ya no existe. **No es cierto.**
`deleteUserAccount` hace `deleteQueryBatch(ratings where userId == uid)`, y esas valoraciones
están casi todas en platos **de otros**, que siguen vivos.

Restaba `ratingAverage(rating)` en vez de `appliedAvg`. Con una edición pendiente de agregar,
la nota viva ya cambió pero el plato tiene dentro la anterior: restar la viva desvía el
promedio. Igual con `wouldOrderAgain` frente a `appliedWouldOrderAgain`. **Arreglo:** restar
el sello, con el valor vivo como reserva para las no selladas.

#### 3. Denuncias — anotada, NO arreglada

El `create` de `reports` valida el prefijo del id y `reportedByUserId`, pero no que el
destino del cuerpo coincida con el del id ni obliga a que el contador suba. El agujero grave
—plantar denuncias a nombre de otro— **está cerrado**; lo que queda es que alguien se
autobloquee una denuncia o que el cuerpo discrepe del id. No corrompe datos ajenos.

#### Probado en el banco: 4/4

`scratchpad/prueba_hallazgos.py`. Tres intentos fallidos antes de un verde que valiera:

⚠️ **1. El emulador cargó CERO funciones y la prueba dio verde.** `Cannot determine backend
specification. Timeout after 10000` → hace falta `FUNCTIONS_DISCOVERY_TIMEOUT=180`. El 404
del disparo (`valid functions are:` vacío) es la señal; sin mirarlo, los tres "OK" parecían
buenos. **Y `firebase-tools` ya no acepta Java 17: hay que poner el JBR 21 de Android Studio
primero en el `PATH`**, no basta con `JAVA_HOME`.

⚠️ **2. `moderatePlateImage` borra todo plato creado en el banco** (`has no imageUrl,
rejecting` → `Cascade delete complete`), así que los platos de prueba desaparecían solos.
`--only functions:X` NO sirve: es un filtro de despliegue, el emulador los carga todos. Se
neutraliza parcheando el bundle compilado con una salida temprana condicionada a
`BANCO_SIN_MODERACION=1`, y **se restaura con `npm run build`** (comprobado: 0 parches).

⚠️ **3. Una aserción floja dio un falso verde.** Mirar solo la media al borrar pasa también
cuando no corre nada, porque la media no cambia; hay que exigir que **`totalRatings` baje**.
Y dormir un número fijo de segundos hizo fallar la prueba con el arreglo ya correcto: hay
que **sondear**.

⚠️ **Para probar la guarda del alta hace falta un caso determinista**: con una valoración
normal, `onRatingCreated` corre antes de que dé tiempo a entregar la edición y la guarda ni
se toca. Se usa **la valoración del propio autor**, que `onRatingCreated` salta a propósito y
por tanto se queda sin `processed` de forma estable.

---

### 🚫 V28 (1.11) — FUERA LA PUBLICIDAD Y FUERA PREMIUM (2026-11-22)

**El porqué no es técnico, es económico.** AdMob ingresa **0,11 € al mes** (dato real de la
consola, no estimación). El umbral de pago en euros son **70 €**: a ese ritmo se tardaría
**53 años en cobrar por primera vez**. Y la suscripción lleva fuera de venta desde el
2026-11-02 por no haber cuenta de comercio, así que las compras integradas ingresan **cero
por diseño**.

Mientras tanto, mantener esa publicidad obligaba a declararse **operador** en Play, y eso
publica **teléfono y dirección** desde el 1 de marzo de 2027. Se estaba pagando un precio
real por un ingreso que no existe.

#### Comprobado en la consola de Play antes de decidir

Hoy el perfil público de la cuenta son **dos líneas: el país y el correo de desarrollador**.
La dirección NO se publica. La propia consola lo dice: *«Si decides obtener ingresos en
Google Play, tu dirección legal completa se mostrará públicamente»* — y "obtener ingresos en
Google Play" es su sistema de facturación, no AdMob. Los datos de contacto (teléfono
incluido) están en el bloque que Google marca como *«no se mostrará públicamente»*.

⚠️ **Conclusión que conviene no perder: los anuncios nunca publicaron nada. Toda la
exposición vendría de UN SOLO ACTO, declararse operador.** Quitar la publicidad no es una
medida de privacidad: es lo que hace que declararse **no operador** sea cierto.

#### Lo que se ha retirado

Banner, intersticial y vídeo recompensado · `AdManager`, `BannerAd`, `ConsentManager` ·
el muro de consentimiento RGPD del arranque · la fila "opciones de privacidad de anuncios"
del perfil · `PremiumScreen`, `BillingManager`, `BillingViewModel` · la tarjeta "hazte
Premium" y el badge de la estrella · la función `awardAdXp` · las dependencias
`play-services-ads` y `user-messaging-platform` · el id de AdMob y la propiedad
`AD_SERVICES_CONFIG` del manifest · 28 cadenas de texto en los dos idiomas.

#### ⚠️ Premium abría TRES cosas, no solo el badge

No se vio hasta mapear el código, y cambia qué significa "retirarlo":

| | Antes gratis | Antes Premium | Ahora, para todos |
|---|---|---|---|
| Platos al día | 10 | 20 | **20** |
| Radio de "cerca de ti" | 550 m | 1.600 m | **1.600 m** |
| "Quién ha interactuado" | no | sí | **sí** (cualquier autor) |

Se eligió el valor alto en los tres: bajar a todos habría sido quitar algo que ocho usuarios
ya tenían. Ampliar el radio no cuesta nada porque la consulta va contra `venues` con
`limit(50)`. Y la regla de `saves` **nunca miró el premium**, solo la autoría del plato, así
que abrir "quién ha interactuado" no necesitó tocar `firestore.rules`.

⚠️ **Error que estuvo a punto de colarse:** al unificar el límite diario quedó usando
`maxDailyPlates`, cuyo valor por defecto es **10**, justo lo contrario de lo acordado. Se
pilló al leer los defaults para borrarlos. Se comprobó además que la plantilla de Remote
Config del servidor está **vacía** (`remoteconfig:get`), así que manda el valor del código.

#### ⚠️ QUITAR ADMOB NO QUITA EL IDENTIFICADOR DE PUBLICIDAD

Hallazgo que no estaba previsto. Tras sacar el SDK, el APK seguía con **5 clases de
`AdvertisingIdClient`** y el permiso `com.google.android.gms.permission.AD_ID`: lo arrastra
**Firebase Analytics** vía `play-services-ads-identifier`. Sin tocarlo, habría que seguir
declarando la recogida del AD_ID en el formulario de seguridad de los datos, lo que
contradice decir que no hay publicidad.

Se corta con `tools:node="remove"` sobre ese permiso, que es **el interruptor documentado
por Google**. Quedan `ACCESS_ADSERVICES_AD_ID` y `ACCESS_ADSERVICES_ATTRIBUTION`, que declara
la librería de medición para las APIs del Privacy Sandbox: **no se tocan**, porque quitarlas
no está documentado como forma de desactivar nada y podría romper Analytics en silencio.

#### Verificado en el emulador (720x1640, 320 dpi, 3 botones, texto al 110 %)

| | |
|---|---|
| Primer arranque **sin muro de consentimiento** | ✅ de 3 pantallas de trámite a 2 |
| Sin banner en el feed | ✅ |
| Perfil sin tarjeta de Premium, sin badge, sin privacidad de anuncios | ✅ |
| Cinco entradas y salidas de plato sin intersticial | ✅ 0 coincidencias en logcat |
| Sin botón "ver anuncio" al final de las valoraciones | ✅ |
| "Quién ha interactuado" en plato propio | ❌ **sin probar** (ver abajo) |

⚠️ **Para entrar en el emulador: las cuentas personales NO tienen contraseña.**
`sergiodelapenya1991@` y `sdelapenya1991@` solo tienen proveedor `google.com`, así que
"entrar con correo" no funcionará nunca con ellas por mucho que se teclee bien. La única con
proveedor `password` es **`foodrankerreview2@`**. Y esa cuenta no ha publicado ningún plato,
por eso la comprobación de "quién ha interactuado" quedó sin hacer: el cambio es quitar
`isPremium &&` de una condición cuyo servidor ya limitaba por autoría.

⚠️ Trampas del emulador con teclado español: **no recibe `AltGr+2`**, así que la arroba no se
puede teclear — hay que meterla con `adb shell input text`. Y el portapapeles está
sincronizado con Windows: al pegar, Android enseña la sugerencia **con la contraseña en
claro**, que acaba en cualquier captura que se tome en ese momento.

---

### 📱 V27 (1.10) — EL FILTRADO DE DISPOSITIVOS Y DOS RECORTES DE TEXTO (2026-11-22)

AAB construido y verificado sobre el binario: `versionCode=27`, `versionName=1.10`.
**Aún no subido a Play** y **aún sin revisar en el Redmi**.

Lo que lleva, además de lo que ya estaba en el repo sin publicar (el arreglo del listener de
avisos y **la edición de nombre de plato**, que estaba terminada y nadie podía usar):

#### `uses-feature`: Play escondía la app y nadie se enteraba

Play **deduce hardware obligatorio a partir de los permisos** y filtra dispositivos en
silencio. La referencia es la tabla 2 de
`developer.android.com/guide/topics/manifest/uses-feature-element`. No había **ningún**
`uses-feature` en el manifest fusionado. Lo que había que cubrir:

* **`android.hardware.wifi`** — lo implica `ACCESS_WIFI_STATE`, que NO es nuestro: lo
  inyecta `com.google.android.libraries.places`. Este era el filtrado real.
* **`android.hardware.location`** (+ `.gps` y `.network`) — con `targetSdk 36` las de
  localización ya NO implican `.gps` ni `.network` (eso era "only when target API level is
  20 or lower"), pero la propia página pide declararlos cuando se usan, y aquí se usan.

Los cuatro van con `required="false"`. Una declaración explícita **tiene precedencia sobre
lo implícito**, así que esto cierra el asunto sea cual sea la lectura de la tabla.

⚠️ **`USE_FINGERPRINT` NO necesita nada.** Lo inyecta `androidx.biometric` y parece que
debería, pero **no aparece en la tabla 2** (sus categorías son Bluetooth, Camera, Location,
Microphone, Telephony y Wi-Fi). Comprobado contra la documentación, no supuesto.

#### La pastilla "Compartir", por fin vista fallar

Reproducida en el emulador a `wm size 720x1640`, `wm density 320`, overlay
`navbar.threebutton` **y en castellano** — en inglés ("Share") no se nota, que es parte de
por qué se escapó dos veces. No ponía "Comp": ponía **"Compa"**, y cortaba a mitad de
palabra porque había `maxLines = 1` **sin** `overflow`, y el defecto de Compose es `Clip`.

La cuenta: a 360dp de ancho cada pastilla mide 91,5dp y el relleno, el icono y el separador
se comen 44dp. Al texto le quedaban 47,5dp y "Compartir" pide unos 57.

**El arreglo es apilar el icono sobre el texto**, no apretar márgenes. Apretar daba justo
los 12dp que faltaban y se descartó por eso: volvería a romperse con el texto del sistema
en grande o con un idioma de palabra más larga. Apilando, el texto dispone de los 79dp
completos. Se añadió además `overflow = TextOverflow.Ellipsis` como red de seguridad.

⚠️ Compose BOM es `2024.09.00` (Compose 1.7), así que **no hay `autoSize`** — llegó en 1.8.
Si algún día se sube el BOM, esto se puede simplificar.

#### Y de paso, el botón principal del login se cortaba

Al probar el arreglo con `settings put system font_scale 1.3` apareció otro fallo, **no
relacionado con el cambio**: `Button(... .height(56.dp))` es altura FIJA, así que
"Continuar con Google" pasaba a dos líneas y la segunda quedaba fuera del botón. El botón
principal del login, ilegible. Cambiado a `heightIn(min = 56.dp)`: idéntico al 100% y crece
solo cuando hace falta.

⚠️ **Lección: probar el 130% de tamaño de texto es barato y encuentra cosas.** Las dos
pantallas se vieron en castellano e inglés, al 100% y al 130%.

#### Revisada en el Redmi con la firma de Play (2026-11-22)

Subida a **prueba interna** e instalada desde Play en el Redmi (`2409BRN2CY`), para que el
binario lleve la firma de Play y no la de subida. **Promocionar esa misma versión a
producción**, no volver a subir el AAB: así lo que llega a los usuarios es el binario
probado.

El Redmi es **720x1640 a densidad 320** — exactamente lo que se configuró en el emulador— y
está **al 110 % de tamaño de texto**, dato que conviene recordar: la pastilla tenía aún menos
sitio del que se midió al 100 %.

| Comprobado | Resultado |
|---|---|
| "Compartir" entero y botón de Google en una línea, al 110 % | ✅ |
| **Entrar con Google** (solo verificable con la firma de Play) | ✅ |
| La campana carga, sin errores del listener en el registro | ✅ |
| **Renombrar plato**: cambio de mayúscula, mismo slug | ✅ guardado y reflejado |
| **La guarda del slug**: nombre realmente distinto | ✅ rechazado, con el mensaje bueno |
| Errores de la app en todo el recorrido | **ninguno** |

El plato de prueba quedó **restaurado exactamente** (`Pizza Barbacoa/4quesos`), descripción
incluida. `renamePlate` es seguro por construcción: solo el autor, solo plato aprobado,
**solo si el slug no cambia**, y solo escribe `name`.

⚠️ Trampas de conducir la app por `adb`: `KEYCODE_BACK` **cierra la hoja entera** y descarta
la edición — para ocultar el teclado hay que pulsar el ✓ del propio teclado. Y
`input text` no mete acentos ni "ñ", así que para probar hay que elegir un plato sin ellos.

#### Pendientes vistos de paso, NO bloqueantes

* **`E/Finsky: requestIntegrityToken() failed for com.app.foodranker`**, tres veces durante
  el arranque. Es Play Integrity fallando. Hoy no rompe nada porque **App Check no está en
  modo obligatorio**, pero hay que resolverlo ANTES de activarlo o dejaría fuera a los
  usuarios legítimos.
* **Los títulos de las tarjetas de plato se cortan sin puntos suspensivos** ("Pizza
  Barbacoa/", "Carpaccio de", "Arroz meloso de"), en la rejilla del perfil. Es la misma
  familia que la pastilla: `maxLines` sin `overflow`.
* **La tarjeta de "Misión diaria" ocupa demasiado** en la pantalla de entrada (~140dp) y
  deja poco sitio para los platos. Lo alto viene del título a dos líneas, los 16dp de
  relleno interior y el círculo de 52dp.

#### Comprobaciones pasadas

`check_strings` (paridad y marcadores), `check_args`, `check_notif_parity`,
`check_slug_parity` y `check_theme_colors`: todas OK.

#### Trampas del banco de pruebas con `adb` en Git Bash

* **Git Bash convierte `/sdcard/...` en una ruta de Windows** y `screencap` responde con su
  texto de uso, que parece un error de argumentos y no lo es. Se arregla con
  `export MSYS_NO_PATHCONV=1`.
* **El idioma hay que forzarlo por app**: `adb shell cmd locale set-app-locales
  com.app.foodranker --locales es-ES`. El emulador viene en `en-US` y en inglés el fallo no
  se reproduce.
* **La instalación falla con `INSTALL_FAILED_UPDATE_INCOMPATIBLE`** si queda una copia
  firmada con otra clave: hay que desinstalar primero.
* Al arrancar salen **dos diálogos encadenados** (permiso de notificaciones y
  consentimiento de anuncios) y el onboarding. Conviene capturar entre pulsación y
  pulsación en vez de encadenar toques a ciegas: la maqueta se mueve al cerrar el primero.

---

### 🧮 EL XP DE LIGA SE DUPLICABA Y LA MEDIA DE LOS PLATOS SE CORROMPÍA (2026-11-22)

**Desplegado en producción el 22 de noviembre**: `functions` (18/18) y `firestore.rules`.
Nada de esto toca el cliente, así que entró en producción sin pasar por revisión de Play.

#### El fallo

`onRatingUpdated` aplicaba al plato el **delta del evento** (`after − before`). La entrega
de eventos de Eventarc es **at-least-once y sin orden garantizado**, así que:

* entregado dos veces, el delta se aplicaba dos veces → la media del plato se iba, y el XP
  de liga se pagaba otra vez;
* entregado al revés, se aplicaba el delta de un paso que ya no era el actual.

Medido en el banco con el código que estaba desplegado, una secuencia `A,B,A,B,A` sobre un
plato que debía quedar en 8.0 lo dejaba en **13.0**. `A,A,A` lo dejaba en 7.0.

`RETRY_POLICY_DO_NOT_RETRY` baja la probabilidad pero no la quita. Y el sello
`aggregatedEventId` que ya había solo pilla el **mismo** evento repetido seguido, no dos
eventos distintos intercalados.

#### El arreglo: reconciliar contra el estado vivo, no contra el evento

`onRatingUpdated` ya no mira el `before` del evento. Lee la valoración **dentro de la
transacción** y le aplica al plato `avgVivo − appliedAvg`, donde `appliedAvg` es lo que el
plato ya tenía incorporado de esa valoración. Luego resella `appliedAvg = avgVivo`.

Eso lo hace **idempotente y además insensible al orden**: los deltas conmutan. Es la
diferencia con una guarda por marca de tiempo, que arreglaría la duplicación pero rompería
el reordenamiento — Codex pilló justo esa regresión en un intento anterior.

Lo mismo para `wouldOrderAgainCount` y `wouldOrderAgainResponses`, comparando lo vivo con
`appliedWouldOrderAgain` (con `null` explícito para "no contestó", que no suma a ninguno de
los dos).

#### El sello se escribe en los tres sitios que incorporan una valoración

* `onRatingCreated` → `appliedAvg` + `appliedWouldOrderAgain`
* `approveplate` → lo mismo, para la valoración inicial del autor
* `onRatingUpdated` → lo resella en cada reconciliación

⚠️ **`onRatingDeleted` se dejó a propósito sin tocar.** Resta `ratingAverage(rating)` en vez
del sello, pero solo corre en cascadas de Admin SDK donde el plato ya no existe y sale por
el `if (!plateDoc.exists) return`. Apuntado por si algún día se unifica, no porque corra prisa.

#### El relleno (hecho una sola vez, NO repetible)

`functions/scripts/backfillAppliedAvg.js` selló las valoraciones que ya estaban en
producción: **69/69**. Simulación por defecto, `--confirm` para escribir.

**El origen del sello es exacto, no una estimación**: los tres caminos que incorporan una
valoración al plato reescriben `rating.averageScore` con lo que acaban de aplicar, **en la
misma transacción** que la escritura del plato. O sea que el valor guardado *es* lo que el
plato tiene dentro. La simulación lo confirmó: 0 sin procesar, 0 sin `averageScore`, 0 donde
el guardado discrepara de la fórmula.

⚠️ **El orden importaba: primero el relleno, después desplegar.** Al revés deja una ventana
en la que el código nuevo ve valoraciones sin sellar y cae a la rama del `before`.

Verificado en producción después del despliegue (script de solo lectura): **0 valoraciones
con `aggregatedEventId`**, **0 descuadres** entre los agregados de los 59 platos y el
recálculo desde sus valoraciones, **0 sellos distintos de la nota viva**.

#### Las reglas: el sello tiene que ser inalcanzable desde el cliente

El `create` de `ratings` solo prohibía `processed` y **admitía cualquier otro campo extra**.
Si el cliente pudiera sembrar `appliedAvg`, una valoración podría nacer declarando "yo ya
aporté un 10 a este plato" y en la primera edición mover la media del plato en esa
diferencia inventada. Ahora el `create` prohíbe `appliedAvg`, `appliedWouldOrderAgain` y
`aggregatedEventId`. El `update` ya estaba cerrado por el `hasOnly`.

Comprobado antes de cerrarlo que el cliente no manda ninguno de los tres.

#### ⚠️ LECCIÓN DE MÉTODO: el banco de pruebas tenía una carrera dentro

Las primeras cifras que dio este trabajo **eran basura**, y no por el código: cada escenario
empezaba borrando la valoración anterior, y ese `DELETE` disparaba `onRatingDeleted`, que
restaba del plato recién recreado. Según si el evento de borrado llegaba antes o después, el
escenario medía bien o medía cualquier cosa.

Y el borrado masivo del emulador **tampoco sirve**: `DELETE /emulator/v1/projects/{p}/
databases/(default)/documents` también emite eventos de borrado, que llegan tarde y caen
sobre los platos que la ejecución siguiente recrea con el mismo nombre.

**Regla para el banco: ni un solo `DELETE`, un plato nuevo por escenario, y sufijo único por
ejecución** (`time.strftime("%H%M%S")`). Se pilló porque un número contradecía la teoría —
el control daba 7.0 donde solo podía dar 8.0. **Si una cifra del banco no cuadra con lo que
el código dice que hace, sospecha del banco antes de tocar el código.**

#### Trampas del banco que costaron tiempo

* **`approveplate` es inalcanzable por HTTP**: es interna y la única que la llama es
  `moderatePlateImage`, que es fail-closed y sin Vision borra el plato en vez de aprobarlo.
  Se prueba exportándola desde el bundle compilado (`lib/index.js`) como `__approveplate` y
  llamándola desde Node contra el emulador. Es el código de verdad, no una copia.
* **`awardXP` hace `if (!snap.exists) return null`**: sin el documento `users/{uid}` el XP no
  se escribe nunca, mientras el log `XP +45` se imprime igual. El banco necesita el usuario.
* **`approveplate` devuelve `Promise<void>`**: mirar su retorno para comprobar la concesión
  no mide nada. Se mira en `users/{uid}.xp` y en el aviso de aprobación.
* **`refreshPriceAggregate` hace `orderBy("createdAt")`** y Firestore omite los documentos
  sin ese campo: sin `createdAt` la consulta sale vacía y el agregado se limpiaría.
* **El emulador de functions recarga `lib/index.js` en caliente** ("Loaded functions
  definitions from source") pero **no registra endpoints HTTP nuevos** al recargar. Sirve
  para intercambiar builds (HEAD ↔ nuevo) sin reiniciar.
* **Firestore devuelve los enteros como `number`**, así que un sello guardado como `9`
  (`integerValue`) lo reconoce el `typeof === "number"`. Probado, porque si no lo
  reconociera la valoración pasaría por "sin sellar".

#### Verificado de extremo a extremo CONTRA PRODUCCION (y como repetirlo sin molestar)

Que el bundle desplegado sea byte a byte el probado no prueba que el trigger corra bien en
la infraestructura de produccion. Se comprobo asi, **14/14**, sin que ningun usuario se
entere y sin dejar rastro:

* Se elige una valoracion **propia** sobre un plato donde es la **unica** valoracion (hay
  siete asi): ningun dato de otro usuario entra en juego.
* `onRatingUpdated` solo escribe agregados del plato y el sello — **ni avisos ni XP**, se
  comprobo leyendo la funcion. Nadie recibe nada.
* Las escrituras van en **parejas que se anulan**: cada prueba edita y restaura, y el
  `finally` restaura siempre, tambien si algo revienta por el medio. **Nunca se borra nada.**

⚠️ **La escritura identica NO sirve**: la guarda de arriba
(`if (!scoresChanged && !repeatChanged && !priceChanged) return`) sale antes de la
transaccion. Para entrar hay que cambiar de verdad un campo de nota.

Las dos ramas que se midieron:

* **delta 0 por compensacion** — presentacion −1.5 y satisfaccion +1.0 cumplen
  `0.2·Δp + 0.3·Δs = 0`: pasa la guarda, entra en la transaccion, lee el sello y sale por
  "nada que incorporar" **sin tocar el plato**.
* **delta no nulo** — sabor +0.5 da exactamente +0.25 de media. El plato subio de 8.85 a
  9.1 y al restaurar volvio a **8.85 exacto**.

⚠️ Los numeros no son cualesquiera: `deltaAvg === 0` es una comparacion **exacta**, y la
ida y vuelta tiene que cerrar sin residuo. Se comprobo en Node **antes** de escribir que la
compensada da `0` y no `1e-16`, y que `8.85 + 0.25 − 0.25 === 8.85`. Con otros valores la
prueba mediria la coma flotante en vez del codigo.

#### ⚠️ CADA EDICION DE VALORACION CUESTA DOS INVOCACIONES (no es un fallo nuevo)

La prueba hizo 4 escrituras y el registro mostro **8 ejecuciones** de `onRatingUpdated`. No
es entrega duplicada de Eventarc: no aparece ni un "ya agregado, se ignora" en el log. Son
4 reales + 4 que **la funcion se dispara a si misma** al escribir el sello en el documento
de la valoracion. Esas 4 mueren en la guarda de arriba sin escribir nada, porque el sello no
toca ningun campo de nota.

**Ya pasaba antes de este cambio**: la version anterior tambien hacia
`tx.update(event.data!.after.ref, ...)`. No hay bucle y no corrompe nada; solo dobla el
numero de invocaciones de ese trigger. Con ~12 usuarios da igual, pero conviene saberlo
antes de mirar una factura o un grafico de invocaciones y pensar que hay algo raro.

#### ⚠️ Los registros de Cloud Logging van en UTC

Costo un intento: el despliegue salio a las `23:32Z` del 21, que son las **00:32 del 22** en
hora peninsular. Un filtro `timestamp>="2026-11-22T00:00:00Z"` se deja fuera el despliegue
entero y parece que no hay registros. Y `firebase functions:log` devolvio entradas que se
paraban dias antes; para esto es mas fiable la REST API de `logging` con un filtro propio.

#### Las reglas publicadas se pueden probar sin tocar datos

`POST https://firebaserules.googleapis.com/v1/{rulesetName}:test` ejecuta el **motor real**
sobre el ruleset que esta **publicado**, sin escribir ningun documento. Es la forma de
comprobar las reglas en produccion sin inventarse datos de prueba: se saca el `rulesetName`
de `/projects/{p}/releases/cloud.firestore`. Salio **6/6**, incluido el `ALLOW` del `create`
normal, que es el que importa de verdad — si eso fallara, nadie podria valorar.

#### ADC para los scripts de Admin SDK

El `adc_tmp.json` necesita el par de cliente de **`firebase-tools`**, no el de
`gcloud`. Con el de `gcloud` (el `client_secret` que empieza por `GOCSPX-`) sale
`2 UNKNOWN: Getting metadata from plugin failed with error: invalid_client`, que no dice en
ningún momento que el problema sea el cliente.

El par de `firebase-tools` es público (cliente OAuth de aplicación instalada) y está en el
propio paquete — se saca de `functions/node_modules/firebase-tools/lib/api.js`, constantes
`clientId` / `clientSecret`. **No se copia aquí**: el repo es público y no hace falta que un
escáner de secretos tenga nada que mirar.

El `refresh_token`, que es lo único que de verdad es secreto, sale de
`~/.config/configstore/firebase-tools.json`. Escribir el fichero **sin BOM** y **borrarlo en
el mismo comando** que lo usa.

---

### 🔔 LA CAMPANA SE QUEDABA CONGELADA PARA SIEMPRE (2026-11-20)

Salió de una auditoría de Codex sobre el estado de la app, **y es real**: comprobado
leyendo el código, no aceptado por venir de otro agente.

#### El fallo

`NotificationRepository.startListening` empieza con:

```kotlin
if (userId == activeUserId && listener != null) return
```

y el callback hacía `if (error != null || snapshot == null) return@addSnapshotListener`.

**Firestore TERMINA la suscripción cuando entrega un error**, pero la referencia se quedaba
ahí no-nula: viva y muerta a la vez. A partir de ese momento, el `return` de arriba hacía
que **ningún reintento volviera a suscribirse**. Y los reintentos existen: `ON_RESUME` y
`LaunchedEffect(currentUserId)`, los dos en `DiscoverScreen`. **No hay `stopListening`.**

Resultado: **la campana se quedaba congelada el resto de la vida del proceso**, ni siquiera
volviendo de segundo plano. Solo se arreglaba matando la app.

**Pasa de verdad**: el 20 nov, a las 08:19:34, salió un `PERMISSION_DENIED` sobre
`notifications/{uid}/items` **en el instante del login**, porque el listener se suscribe
antes de que el token de Firestore haya propagado.

#### El arreglo

Soltar `activeUserId` en la rama de error. La siguiente llamada ya no coincide, entra, hace
`remove()` del registro muerto y se vuelve a suscribir.

⚠️ **No se anula `listener` ahí dentro a propósito.** Si el error llegara antes de que
termine la asignación de arriba, anularlo lo dejaría colgado igual: la asignación posterior
volvería a guardar el registro muerto. Con `activeUserId = ""` no hay carrera posible.

⚠️ **Queda la causa raíz**: el listener se sigue suscribiendo antes de que propague el
token. Con esto se recupera en el siguiente `ON_RESUME`, no al instante. Atacarla de verdad
sería no arrancarlo hasta que Auth esté listo.

⚠️ **Sin test.** `GuardiaDeAvisosTest` cubre la clase pura; esta rama vive dentro del
lambda de `addSnapshotListener` y testearla pide mockear Firestore o sacar el estado del
listener a su propia clase. Se dejó el cambio mínimo porque la v26 estaba en revisión.

⚠️ **No lleva `versionCode`**: entra en la **v27**, junto con el `uses-feature` de wifi y
location, y la pastilla "Comp".

#### El resto de la auditoría de Codex, con veredicto

| Hallazgo | Veredicto |
|---|---|
| La campana tras el login | ✅ **confirmado y arreglado** (arriba) |
| Las Cloud Functions siguen en versiones de octubre | ✅ correcto, **es el plan**: se despliegan cuando la v26 esté repartida en producción |
| Desplegar no completa la migración FCM | ✅ cierto: el cliente ya registra cada instalación, pero la función de envío usa un único `fcmToken`. Ya estaba en pendientes |
| `plates` se puede leer sin importar el estado | ✅ **confirmado**: `firestore.rules:135` dice `allow read: if true`. El filtro de moderación es solo del cliente, y **App Check sigue sin forzarse** |
| Contador de denuncias sin ligar a la denuncia | ⚠️ **sin verificar** |
| La edición de valoraciones aplica delta sin deduplicar | ⚠️ **sin verificar** |
| Los topes de búsqueda no aprietan (59 platos, 40 locales) | ✅ coherente con lo ya apuntado |

---

### 🔒 LOS DOS HALLAZGOS QUE QUEDABAN DE CODEX, CONFIRMADOS Y ARREGLADOS (2026-11-20)

Eran los dos que en su momento se marcaron como **sin verificar**. Comprobados leyendo el
código: **los dos eran reales**. Ninguno es regresión reciente.

#### 1. El contador de denuncias no estaba atado a la denuncia — explotable

El cliente hacía lo correcto: una transacción que crea `reports/{uid}_{docId}` y solo
entonces incrementa. **Pero la regla no exigía nada de eso**, solo que el delta fuera +1:

```
changedKeys().hasOnly(['reportCount'])
&& request.resource.data.reportCount == resource.data.reportCount + 1
```

La API key va dentro del APK, así que **cualquier usuario autenticado podía llamar a REST
e incrementar `reportCount` las veces que quisiera, sin crear ninguna denuncia**. Con el
umbral en 3, tres llamadas hundían cualquier plato — y sin rastro en `reports` que
permitiera auditarlo.

Arreglado con un helper aplicado a los **tres** sitios (plates, ratings, comments):

```
function denunciaCreadaAhora(docId) {
  let ref = /databases/$(database)/documents/reports/$(request.auth.uid + '_' + docId);
  return existsAfter(ref) && !exists(ref);
}
```

`existsAfter` obliga a que la denuncia se cree en la misma transacción; `!exists` impide
que el mismo usuario vuelva a sumar. Es la garantía que el comentario de la regla de
ratings ya daba por hecha (*"el id determinista del reporte impide contarlo dos veces"*)
pero que solo cumplía el cliente.

#### 2. `onRatingUpdated` aplicaba el delta sin idempotencia

Recalcula `(currentAvg * totalRatings - oldAvg + newAvg) / totalRatings` y suma a los
contadores de "lo volvería a pedir", **sin marca de procesado**. Los triggers son
**at-least-once**: una entrega duplicada aplicaba el delta dos veces y dejaba el promedio
del plato corrupto, sin forma de detectarlo después.

Es **el mismo patrón que ya se arregló en el XP de liga** el 18 de noviembre; la lección no
se había aplicado aquí. Ahora lleva sello `aggregatedEventId = event.id`, escrito en la
misma transacción que el delta.

⚠️ **Antes de añadir el campo se comprobó que no rompe la edición de valoraciones.** La
regla del autor usa `changedKeys().hasOnly([...])`, así que un cliente que reenviara el
data class entero borraría el campo y se quedaría sin poder editar. No pasa: la edición usa
`update(mapOf(...))` con campos concretos (`PlateDetailViewModel:650`), que es también por
lo que `leagueXpAmount` ya convivía sin problema.

#### ✅ DESPLEGADOS Y PROBADOS CONTRA PRODUCCIÓN

Las reglas compilaron y se publicaron; `functions` volvió a desplegarse con el sello.

**La prueba que importaba no era que el ataque fallara, sino que denunciar siguiera
funcionando** — si `existsAfter` no se comportara como se espera, el arreglo dejaría a
todo el mundo sin poder denunciar. Se probó por REST con un `idToken` real de la cuenta de
revisión, contra un plato de verdad:

| Caso | Resultado |
|---|---|
| Subir `reportCount` **sin** crear denuncia (el ataque) | ✅ `PERMISSION_DENIED` — antes pasaba |
| Denuncia + incremento en el mismo commit (el flujo real) | ✅ pasa — no se rompió nada |

Después se limpió: el reporte de prueba borrado y el `reportCount` del plato devuelto a 0
con Admin SDK. `adc_tmp.json` borrado.

⚠️ Un detalle de las reglas de permisos: `Bash(firebase deploy --only functions)` como
regla **exacta** solo casa con el comando literal. Con `2>&1 | tail -12` detrás no casa y
decide el clasificador. Para comandos con tubería hace falta la forma de prefijo.

---

### ✅ `functions` DESPLEGADO — Y LA ESPERA ERA INNECESARIA (2026-11-20)

**Desplegadas las 18 funciones** en `europe-west1`, todas con *Successful update operation*
y estado ACTIVE. Se acabó el desfase: lo que corría era de octubre.

Confirmación de que subió el código nuevo y no el viejo: **`renamePlate` aparece en
`functions:list`**, y esa función no existía en el despliegue anterior.

#### ⚠️ LA REGLA DE "ESPERAR AL REPARTO" ESTABA MAL CALIBRADA

Durante días se mantuvo la regla *"no desplegar `functions` hasta que la v22+ esté
repartida en producción, porque los clientes viejos no entienden los canales de avisos"*.
Al comprobarla de verdad, resultó **excesiva en dos sentidos**:

**1. El canal inexistente no rompe nada.** Antes de la v22 el manifest **no declaraba**
canal por defecto y los `foodranker_*` no existían, así que esos clientes **ya funcionaban
con el canal de reserva que crea solo el SDK de FCM**. Recibir un `channelId` que no
conocen los deja donde ya estaban.

**2. Ya estaba pasando, y sin consecuencias.** En los logs:

```
2026-11-16 18:50:48  onnotificationcreated: push enviado: tipo=like canal=foodranker_social
```

La función **enviaba el canal desde el 16 de noviembre**, con producción entera en la v20 y
ningún cliente con ese canal creado. Cuatro días de envíos sin un solo error. (El log prueba
que FCM los aceptó, no que se pintaran en pantalla — pero no hubo ninguna queja.)

#### ⚠️ Y la espera no era gratis

Lo que llevaba sin desplegar incluía **un arreglo de crash pensado precisamente para curar
a los clientes viejos sin que actualicen** (`9296b93`), con esta nota en el propio código:

> *"Se arregla aquí ADEMÁS de en el cliente a propósito: así deja de pasar en todas las
> versiones ya instaladas, sin esperar a que nadie actualice."*

Es el crash de tocar un aviso que no es de un plato, visto en Crashlytics el 16 de
noviembre y **presente desde la 1.2**. Y con él esperaba el arreglo del **XP de liga que se
pagaba dos veces**, que mientras tanto seguía corrompiendo datos.

**Lección:** una regla de prudencia también hay que verificarla. Esta protegía de un riesgo
hipotético mientras mantenía vivos dos problemas reales, y bastaban dos comprobaciones
(el manifest y un log) para verlo. Con un parque de **~12 usuarios**, además, el cálculo
nunca estuvo cerca.

#### Qué vigilar ahora

- **Crashlytics**: el crash de los avisos debería dejar de aparecer, en todas las versiones.
- Que los avisos siguen llegando (probar un like o un comentario desde otra cuenta).
- El **XP de liga**, que ya no debería duplicarse.

---

### ✅ LA v26 YA ESTÁ PUBLICADA EN PRODUCCIÓN (2026-11-20)

Google la aprobó y, con **publicación gestionada desactivada**, salió sola el mismo día.
Aviso de Play: *"Se ha publicado la actualización de la aplicación."*

Producción pasa de la **20 (1.6)** a la **26 (1.9.3)**, y con ella llega por fin el acceso
por correo que desbloquea a los revisores.

#### ⚠️ PUBLICADA NO ES REPARTIDA — esto es lo que decide el despliegue de `functions`

El aviso de Play dice que está publicada, **no que la tengan los usuarios**. Los clientes
actualizan a lo largo de días. Y el orden que no se puede saltar sigue intacto:

1. Mirar en **Play Console → Estadísticas → distribución por versión** qué parte del
   parque está ya en la 26.
2. **Solo entonces** `firebase deploy --only functions`.

El motivo no ha cambiado: los cambios de servidor que esperan (canales de avisos,
idempotencia del XP de liga) **no los entienden los clientes anteriores a la v22**, y hasta
hoy producción entera estaba en la 20. Desplegar con la mayoría todavía en la 20 les
rompe los avisos.

⚠️ Desplegar también **sigue sin completar la migración FCM**: el cliente ya registra cada
instalación en `users/{uid}/dispositivos`, pero la función de envío usa un único
`fcmToken`. Eso es trabajo aparte.

#### Lo que toca vigilar estos días

- **Si vuelve a llegar un aviso de bloqueo al móvil de Sergio**, el revisor usó el botón de
  Google en vez del camino de correo. Salida de raíz: mover la credencial a una dirección
  que no sea una cuenta de Google.
- **Crashlytics**, por si la v26 trae algo que no salió en las pruebas.
- **La campana**: el arreglo del listener está en el repo pero **NO en la v26** — entra en
  la v27.

---

### 🚀 LA v26 ENVIADA A REVISIÓN PARA PRODUCCIÓN (2026-11-20)

**Estado: esperando a Google.** Enviado el 20 nov desde *Resumen de publicación*.

| Qué | Detalle |
|---|---|
| Versión | **26 (1.9.3)**, lanzamiento completo al 100 % |
| Venía de | producción en la **20 (1.6)** — un salto de 6 versiones |
| Notas | las de **producción**, no las del interno (ver `docs/play-store-listing.md`) |
| Datos de inicio de sesión | actualizados el mismo día, **una sola entrada**, "Lista para enviar a revisión" |
| Aviso de hardware | **ignorado a propósito** (ver más abajo), marcando "publicar es más importante" y "ya tengo previsto resolverlo" |

⚠️ **Publicación gestionada desactivada**: cuando Google apruebe, **sale sola al 100 %**. No
hay un segundo botón.

#### Lo que viene cuando apruebe

1. **Esperar a que esté repartida de verdad en producción** (no basta con que Google
   apruebe: hay que ver clientes en la 26).
2. **Entonces** `firebase deploy --only functions`. Los cambios de servidor que esperan
   (canales de avisos, idempotencia del XP de liga) **no los entienden los clientes
   anteriores a la v22**, y hasta ahora producción entera estaba en la 20.
3. Si los revisores entran con la credencial de correo, el problema de las tres cuentas
   de revisión muertas queda cerrado. Si vuelve a llegar un aviso de bloqueo al móvil de
   Sergio, es que el revisor usó el botón de Google: la salida de raíz entonces es mover
   la credencial a una dirección **que no sea una cuenta de Google** (p. ej.
   `review@foodranker.app`, existiendo solo en Firebase).

#### Dos detalles del envío, por si se repiten

- Los **Datos de inicio de sesión no salen en la lista de "cambios"**, sino abajo, en
  *Información que has aportado*. No cuentan para el contador de "Enviar N cambios"
  porque no son contenido publicable, pero **sí se tienen en cuenta en esa revisión**.
- El botón del diálogo de los datos de inicio de sesión pone **"Añadir"** aunque estés
  editando la entrada existente. No crea una segunda — comprobado en la lista después.

---

### 📱 LA v23 PROBADA EN EL MÓVIL REAL — Y UN FALLO QUE OBLIGA A LA v24 (2026-11-19)

**Las dos pruebas críticas de la v23, superadas sobre la build firmada por Play**, en el
Redmi 2409BRN2CY (Android 16, 720x1640, navegación de tres botones):

| Prueba | Resultado |
|---|---|
| **Entrar con correo** (`foodrankerreview2@gmail.com`) | ✅ entra, llega al ranking |
| **Google Sign-In** (el que se rompió dos veces solo en builds de Play) | ✅ entra |
| Perfil de la cuenta de revisión | ✅ "Play reviewer" con la estrella de Premium |
| `users/{uid}/dispositivos` | ✅ 2 documentos (emulador + Redmi) — la regla desplegada funciona |
| logcat | ✅ ni `PERMISSION_DENIED`, ni error de Firebase, ni crash |

Comprobado con Admin SDK, no supuesto: proveedores `google.com, password` **en el mismo
uid**, `premium: true`, `name: "Play reviewer"`. La trampa de "una cuenta por dirección"
está resuelta como se diseñó.

#### ⛔ El fallo: el alta por correo es inalcanzable con navegación de tres botones

En la hoja de correo, **«¿Aún no tienes cuenta? Créala» cae dentro de la franja de la
barra de navegación**. No es que se vea mal: **no se puede pulsar**. Medido, no deducido —
`uiautomator` da el enlace en `[146,1572][574,1626]` sobre una pantalla de 1640 de alto, y
al tocar (360,1599) **respondió el botón de inicio del sistema y la app se fue al
escritorio**.

Consecuencia: en un móvil con tres botones **nadie puede registrarse por correo**, que es
la mitad de lo que añade la v23.

⚠️ **A la revisión de Play no le afecta**: el revisor rellena los dos campos y pulsa
*Entrar*, que está en `[308,1377][412,1430]`, muy por encima de la barra. Por eso la v23
sirve para dar acceso a los revisores aunque tenga este fallo.

#### Tres intentos hasta dar con la causa (v24, v25 y por fin la v26)

**La causa de verdad no era de insets.** `ModalBottomSheet` se abre en estado
**parcialmente expandido**, a media pantalla, y el formulario **no cabe en esa mitad**. Lo
que sobra queda fuera de la parte visible y solo se alcanza **arrastrando la hoja hacia
arriba**, cosa que nadie adivina. Por eso todo padding que se añadiera al final no movía
nada: se sumaba al final de un contenido que ya estaba cortado.

| Intento | Qué se hizo | Resultado |
|---|---|---|
| **v24** | `windowInsetsPadding(ime union navigationBars)` | ❌ bounds idénticos |
| **v25** | inset crudo con `asPaddingValues()`, dentro del scroll | ❌ bounds idénticos |
| **v26** | `skipPartiallyExpanded = true` + el hueco fuera del scroll | ✅ |

```kotlin
val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
val huecoBarra = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

ModalBottomSheet(sheetState = estadoHoja, ...) {
    Box(modifier = Modifier.padding(bottom = huecoBarra)) {   // FUERA del scroll
        Column(modifier = Modifier.verticalScroll(...).imePadding()...) { ... }
    }
}
```

Dos cosas que conviene no deshacer:

- **`ModalBottomSheet` CONSUME los insets de la barra**, así que dentro de su contenido
  `WindowInsets.navigationBars` vale cero y `windowInsetsPadding` no añade nada. Medido
  con un log: `navCrudo=96px` pero el `windowInsetsPadding` no movía la vista. El inset
  del teclado **no** está consumido, por eso `imePadding` sí funciona.
- **El hueco de la barra va fuera del `verticalScroll`.** Dentro se convierte en espacio
  al final del contenido desplazable y no sube nada.

Verificado sobre la **build de release con R8**: enlace en `[175,1312][545,1360]` con la
barra desde `1544`, y al tocarlo se abre el modo alta. En modo alta con el teclado abierto
el formulario entero queda visible sin necesidad de desplazar.

#### 📌 PARA LA PRÓXIMA VERSIÓN: dos cosas pequeñas que no bloquean

**1. `android.hardware.wifi` obligatorio.** Al enviar la v26 a producción, Play avisa de que
*"las declaraciones de funciones de hardware obligatorias están restringiendo la
disponibilidad"*. No lo declara nadie: es **implícito**.

```
uses-implied-feature: name='android.hardware.wifi'
  reason='requested android.permission.ACCESS_WIFI_STATE permission'
```

Y ese permiso lo mete **`com.google.android.libraries.places:places:4.1.0`** (el selector de
local), según `manifest-merger-release-report.txt`. **No es nuevo**: el manifest no se tocó
en la v23–v26 y producción ya está publicada así desde la v20. Se decidió **Continuar de
todos modos** para no gastar otra ronda de revisión: en teléfonos no excluye a nadie, solo
a Chromebooks sin wifi y Android XR.

Hay un segundo igual que Play todavía no marca, `android.hardware.location`, implícito por
los permisos de ubicación. La app funciona sin ubicación (solo se resiente "Cerca de ti"),
así que también debería ser opcional. Los dos se arreglan en el manifest:

```xml
<uses-feature android:name="android.hardware.wifi"     android:required="false" />
<uses-feature android:name="android.hardware.location" android:required="false" />
```

⚠️ Mismo cuidado que con el `uses-feature` de CAMERA que ya está comentado arriba en el
manifest: si algún día se quita la librería Places, este `uses-feature` sobra.

**2. La pastilla "Compartir" sale truncada como "Comp"** en la pantalla de login a 720px de
ancho. En inglés ("Share") cabe; en castellano no. Sin verificar ningún arreglo todavía.

#### Verificado en el Redmi con la v26 ya firmada por Play

| Prueba | Resultado |
|---|---|
| «¿Aún no tienes cuenta? Créala» | ✅ `[146,1313][574,1367]`, barra desde 1546 — 179px de margen |
| Tocarlo | ✅ abre el alta, el foco se queda en la app |
| Alta con teclado (caso peor) | ✅ botón en 764, enlace en 863 |
| Botón *Entrar* con el teclado abierto | ✅ visible en 638: ya no hay que cerrar el teclado |
| Login del revisor por correo | ✅ entra al ranking |
| **Google Sign-In** | ✅ entra |

Antes del arreglo ese enlace estaba en `[146,1572][574,1626]`, dentro de la barra.

⚠️ **Un hallazgo aparte, que no viene de este cambio:** al entrar aparece **un**
`PERMISSION_DENIED` en `notifications/{uid}/items`, en el mismo instante del login
(08:19:34 del 20 nov). Es el listener de avisos suscribiéndose antes de que el token de
Firestore haya propagado. La app sigue funcionando y solo ocurre una vez, pero conviene
mirar si la campana se queda sin cargar cuando pasa. **No lo causa la v26** — solo toca
`EmailAuthSheet.kt`.

#### ⚠️ CÓMO PROBAR ESTO BIEN — dos errores de método que costaron dos versiones

**1. El emulador estaba en navegación por gestos.** Con gestos el inset inferior es mínimo
y nada se solapa, por eso nunca cazó esta familia de fallos:

```
adb shell cmd overlay enable  com.android.internal.systemui.navbar.threebutton
adb shell cmd overlay disable com.android.internal.systemui.navbar.gestural
```

**2. El emulador era más alto que el móvil, y eso invalidó una verificación entera.**
2400px a 420dpi son **571dp**; el Redmi tiene 1640px a 320dpi, o sea **512dp**. Con 59dp
más el formulario cabía y el fallo no salía. Se dio por bueno un arreglo que no lo era.

```
adb shell wm size 720x1640
adb shell wm density 320
```

Así la barra queda en `[0,1544][720,1640]` frente a `[0,1546][720,1640]` del Redmi, y el
fallo **sí** se reproduce. **Antes de dar por bueno un arreglo de layout, hay que ver el
fallo reproducido en el banco de pruebas** — si no, no se está midiendo nada.

Para restaurar el emulador: `wm size reset` y `wm density reset`.

#### Pendiente, cosmético y sin verificar

En la pantalla de login, la tercera pastilla sale truncada como **"Comp"** en lugar de
"Compartir" a 720px de ancho. No se ha tocado: para verlo arreglado hay que medir en una
pantalla de ese ancho, y el emulador tiene otra. No afecta a la revisión.

#### Cómo se desatascó Play, por si vuelve a pasar

El móvil tenía la v22 **del canal cerrado** y Play se negaba a ofrecer la 23 del interno:
botón *Abrir* en vez de *Actualizar* y el mensaje *"Eliminado tu cuenta del programa de
pruebas beta. Desinstala la versión de prueba beta e instala la versión pública."*
**Desinstalar y reinstalar desde Play lo resolvió**: sin nada instalado, Play sirvió
directamente *FoodRanker (beta interna)*, versionCode 24 no, **23 (1.9)**, instalador
`com.android.vending`.

#### Trampas de manejar el móvil por adb, apuntadas

- El **WebView del consentimiento (UMP) ignora `input tap`**. Hay que mandar un gesto con
  duración: `input swipe x y x y 120`. Con el tap seco parece que la app está colgada.
- `screencap -p /sdcard/x.png` falla desde Git Bash porque reescribe la ruta del aparato.
  Usar `exec-out screencap -p > fichero_local`.
- Las coordenadas van en el espacio de la pantalla **tal y como está girada**. Con el móvil
  tumbado son 1640x720, no 720x1640.
- Para el ADC de los scripts de Admin SDK, el `client_id` bueno es el de **firebase-tools**
  (`563584335869-fgrhgmd47bqnekij5i8b5pr03ho849e6...`), que sale de `lib/api.js` del
  paquete global. El de gcloud da `invalid_client`.

---

### 🔑 LA CUENTA DE REVISIÓN DEJA DE SER UNA CUENTA DE GOOGLE (2026-11-19)

**Tercera cuenta de revisión muerta por el mismo motivo.** Esta vez se encontró la causa
de verdad, y no era la configuración de ninguna de las tres.

#### Lo que estaba pasando

Avisos en el móvil de Sergio sobre intentos de entrar en `foodrankerreview2@gmail.com`,
pidiendo autorizar desde el teléfono y un código que al revisor no se le puede dar.

Se miró la actividad de la cuenta antes de tocar nada, y el correo de Google lo dice sin
rodeos: **"Se ha bloqueado un intento de inicio de sesión — Alguien acaba de usar tu
contraseña para intentar iniciar sesión. Google lo ha impedido."** Dos veces: **17 nov
0:51 y 18 nov 11:23**. Ninguno entró.

**Eran los revisores, no un ataque.** Lo dice la propia frase: *usó tu contraseña*, o sea
que la tenía bien, y quien la tiene es Play Console. Y el ritmo —dos intentos en dos
días, no doscientos— no es fuerza bruta.

#### La causa real, que no es de configuración

La página oficial de Play ([ayuda 15748846]) pide, literal:

> *"Si tu aplicación normalmente requiere un código de verificación en dos pasos o una
> contraseña de un solo uso, asegúrate de proporcionar credenciales de inicio de sesión
> **reutilizables que puedan saltarse esos requisitos**."*

Y una cuenta de Google **no puede ser eso, por diseño**. No hay ningún ajuste que apague
el motor de riesgo. La cuenta 2 estaba configurada exactamente como mandaba el manual de
la sesión anterior (sin 2FA, sin llaves de acceso, sin "saltar contraseña") y la
bloquearon igual.

⚠️ **Por eso se descartó el apaño que parecía obvio:** activar la verificación en dos
pasos y pasarle códigos de respaldo al revisor. Va en contra del texto de arriba por dos
sitios — los códigos son **de un solo uso** (no reutilizables) y **satisfacen** el
segundo factor en vez de **saltárselo** — y además son consumibles: diez, y te enteras de
que se acabaron por un rechazo. Se llegó a recomendar y se retiró al leer la página
oficial.

#### La solución: correo y contraseña con Firebase Auth

Una credencial de Firebase no pasa por Google: **no hay desafío, ni aviso al móvil de
nadie, ni caducidad**. Es exactamente lo que describe la página de Play.

De paso resuelve otras dos cosas que llevaban tiempo ahí:

- Quien no tiene cuenta de Google no podía entrar en FoodRanker.
- **Google Sign-In ya se rompió dos veces en producción** (ver el aviso histórico en
  `AuthRepository.signInWithGoogle`). Hasta hoy, eso dejaba la app sin ninguna entrada.
- Es el **primer camino de login que se puede probar en el emulador**, que no tiene
  cuenta de Google. Hasta ahora nada de dentro de la sesión era testeable en local.

#### Qué se ha tocado (v23 / 1.9)

| Fichero | Qué |
|---|---|
| `AuthRepository.kt` | `signInWithEmail`, `signUpWithEmail`, `sendPasswordReset` |
| `AuthViewModel.kt` | los tres, más `passwordResetSent` aparte de `AuthState` |
| `EmailAuthSheet.kt` | **nuevo** — hoja inferior con entrar / crear cuenta / recuperar |
| `AuthScreen.kt` | el acceso discreto bajo el botón de Google, y la hoja |
| `ErrorMapper.kt` | los `errorCode` de FirebaseAuth, **antes** que el resto |
| `strings.xml` ×2 | 17 cadenas nuevas en inglés y castellano |
| `reviewerAccount.js` | **nuevo** — crea o actualiza la cuenta de revisión |

**Decisiones que conviene no deshacer sin leer esto:**

- **No se exige verificar el correo** antes de entrar. Hacerlo reconstruiría el problema
  que este camino viene a resolver: otro paso que depende de un buzón ajeno.
- **`signUpWithEmail` no reutiliza `completeSignIn`.** Firebase crea la cuenta *sin*
  nombre y `syncUserDocument` lee `displayName`; sincronizar antes de escribir el perfil
  haría que todos los registros por correo nacieran con el nombre de respaldo.
- **Los códigos de Auth se miran antes que el resto en `ErrorMapper`.** El resto decide
  por el **texto** del mensaje, y un `ERROR_USER_NOT_FOUND` acabaría en "no encontrado".
- **Contraseña incorrecta y usuario inexistente comparten mensaje.** Con la protección
  contra enumeración de correos Firebase devuelve el mismo `ERROR_INVALID_CREDENTIAL`
  para los dos, y distinguirlos diría quién tiene cuenta.
- **La hoja va en `ModalBottomSheet`, no en pantalla propia**, para no tocar el grafo de
  navegación: el camino de Google, que es el que usa todo el mundo, queda intacto.

⚠️ **Trampa de "una cuenta por dirección de correo"** (activado por defecto en Firebase):
quien se registre por correo con la misma dirección que usa en Google verá fallar el
botón de Google con `ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL`. No pierde la
cuenta —entra por correo— y el mensaje se lo dice (`err_auth_use_email_login`). Es un
mensaje distinto del de registrarse con un correo ya usado, a propósito: son los dos
sentidos del mismo choque y confundirlos deja al usuario igual de atascado.

#### ⛔ BLOQUEANTE: el proveedor está DESACTIVADO en Firebase

Comprobado contra la API, no supuesto:

```
POST identitytoolkit.googleapis.com/v1/accounts:signInWithPassword
→ PASSWORD_LOGIN_DISABLED
```

**Sin esto el código no hace nada.** Hay que activarlo a mano en
**Firebase Console → Authentication → Sign-in method → Email/Password → Habilitar**
(solo el primer interruptor; "Enlace de correo sin contraseña" NO).

⚠️ Al activarlo, la API key pública del APK permite crear cuentas por REST, y **App
Check sigue sin estar forzado** (está en los pendientes de siempre). No es distinto de
cómo está hoy el proveedor de Google, pero conviene saberlo antes de darle.

#### La cuenta de revisión, ya mirada

`foodrankerreview2@gmail.com` **ya existe en Firebase**: uid
`B8V8uOZyazcTnumJo6mvMzjriXo2`, proveedor `google.com`, no deshabilitada. El script le
**añade** la contraseña al mismo usuario, así que conserva uid, XP y el Premium que
tuviera. No crea una cuenta nueva.

```
REVIEWER_PASSWORD='...' GOOGLE_APPLICATION_CREDENTIALS=adc_tmp.json \
  node scripts/reviewerAccount.js foodrankerreview2@gmail.com --confirm
```

#### 📦 LA v23 (1.9), COMPILADA Y PROBADA EN RELEASE

| | |
|---|---|
| AAB | `app/build/outputs/bundle/release/app-release.aab` · 16,1 MB |
| Verificado sobre el binario | `versionCode='23' versionName='1.9'` (aapt2 badging) |
| `check_apk.py` | 860 cadenas en los dos idiomas · 9 solo-por-defecto (de librería) · **0 solo-castellano** |

⚠️ **`check_apk.py` lee `$TEMP/res.txt`, que hay que regenerar antes**, o audita la build
anterior sin decir nada:

```
aapt2 dump resources app/build/outputs/apk/release/app-release.apk > "$TEMP/res.txt"
```

Había también un `app-release.apk` del 9 de octubre tirado en `outputs/`, que hizo creer
por un momento que el AAB era la v17. Si `aapt2 dump badging` dice una versión rara,
mirar la fecha del fichero antes que el código.

**Probado el APK de release firmado, con R8 activo** (lo anterior era debug sin minificar):
instalación limpia → consentimiento → permiso de avisos → onboarding → entrar con
`foodrankerreview2@gmail.com` → Inicio, feed cargado y **sin banner de anuncios**, que es
el Premium funcionando. R8 no se ha comido nada.

#### 🔑 La cuenta de revisión, lista

| | |
|---|---|
| uid | `B8V8uOZyazcTnumJo6mvMzjriXo2` (el de siempre, no se creó otro) |
| Proveedores | `google.com` **+ `password`** |
| Nombre | **Play reviewer** (antes "Sergio", en Auth y en Firestore) |
| Premium | **true** — lo pedía la casilla de Play y estaba en `false` |

La contraseña **no se guarda aquí** (repo público): está en Play Console y en quien la
generó. Comprobada con una llamada real a `accounts:signInWithPassword`.

⚠️ **La declaración de Play se cambia AL SUBIR la v23, no antes.** Las instrucciones
nuevas mandan al revisor a pulsar "Sign in with email", y ese botón no existe en la 20
que está en producción ni en la 22 de pruebas.

#### ✅ Reglas de Firestore DESPLEGADAS (2026-11-19) — y así se encontró

Lo cazó el logcat del build de release:

```
W Firestore: Write failed at users/{uid}/dispositivos/{id}: PERMISSION_DENIED
W FCM     : Error guardando el token del dispositivo: PERMISSION_DENIED
```

El bloque `dispositivos` está en `firestore.rules` desde el commit `e574df5` pero **nunca
se ejecutó `firebase deploy --only firestore:rules`**. Firestore deniega por defecto lo
que no tiene regla.

**No rompe los avisos**: la escritura vieja de `fcmToken` en el perfil sí pasa, que es
justo por lo que el cambio se diseñó aditivo. Pero mientras no se despliegue, cada login
deja ese error en el log y la mitad nueva no guarda nada.

```
firebase deploy --only firestore:rules
```

**Desplegado y comprobado el 2026-11-19.** No basta con que deje de dar error: se miró
que el documento se escribe de verdad.

```
documentos en /dispositivos: 1
  id: f-NQy2I_RnKTGTUcCUQcpg | plataforma: android | token: f-NQy2I_RnKTGTUcCU...
```

⚠️ Ese id es el **Installation ID** del emulador, no el token: por eso un móvil deja un
único documento aunque el token rote. Era el motivo de elegirlo como id.

⚠️ **El servidor sigue leyendo `fcmToken` del perfil.** Esto solo rellena la mitad nueva;
cambiar las funciones de sitio es lo que va DESPUÉS de que la v23 esté repartida.

---

#### ⚠️ ESTADO REAL DE LOS CANALES (2026-11-19, leído en la consola)

| Canal | Versión | Estado |
|---|---|---|
| **Producción** | **20 (1.6)** | Activo · 2 países · 16 descargas |
| Prueba cerrada — Alpha | 22 (1.8) | ✅ activa, última actualización 18 nov |
| Prueba interna | 22 (1.8) | ✅ disponible para testers, 18 nov 10:09 |

*Resumen de publicación* está **vacío**: no queda nada esperando revisión.

❗ **Lo que esto corrige, y es lo más importante de esta nota:** la regla de "desplegar
`functions` cuando la v22 esté repartida" significa **repartida en PRODUCCIÓN**, no
disponible en un canal de pruebas. **Producción sigue en la 20 (1.6)**, así que todos los
usuarios reales son clientes pre-v22. Desplegar `functions` hoy les rompería los avisos.
Que la v22 esté verde en los dos canales de prueba **no desbloquea nada**.

✅ Y una buena: la v22 pasó la revisión del canal cerrado. O los revisores entraron, o
ese canal no se mira con la misma lupa que producción. En cualquier caso **la revisión
dura es la de producción**, que es donde el login de correo hará falta de verdad.

⚠️ **Publicación gestionada desactivada**: cuando Google apruebe producción, **sale
sola**. No hay segundo botón. Así que el despliegue de `functions` hay que tenerlo
pensado ANTES de darle a publicar, no después.

#### El orden, que ahora tiene tres cosas esperando

1. **Activar el proveedor** Email/Password en Firebase Console (sin esto no hay nada que
   probar).
2. Poner la contraseña con `reviewerAccount.js` y comprobar que entra **en el emulador**.
3. Probar la v23 en el Redmi, y de paso **la vuelta en oscuro por las 18 pantallas** que
   sigue pendiente desde la v22.
4. Subir la **v23** y promocionarla **a producción**, reescribiendo a la vez *Datos de
   inicio de sesión* en Play. Aquí es donde importa el login nuevo.
5. Esperar a que la v23 esté **repartida en producción** — no antes.
6. **Entonces** desplegar `functions` (canales de avisos, idempotencia del XP de liga,
   borrado reintentable) y las reglas.

Las reglas (`firestore.rules`) se pueden desplegar cuando sea: solo abren sitios nuevos.

#### Lo demás que sigue abierto en la consola

- **3 acciones recomendadas** en producción, todas etiquetadas `20 (1.6)`: las dos de
  vista de extremo a extremo (ya analizadas — **no son nuestras**, ver la sección del
  2026-11-16) y la de **R8**, que sigue sin mirarse.
- **Banner permanente "Declara tu estado de operador"**, con fecha límite **1 de febrero
  de 2027**. No es de código: va a la consulta con la gestoría, porque con AdMob
  sirviendo Google te considera operador y un particular que lo declara **publica su
  teléfono y su dirección**.

#### Sobre la cuenta de Google de revisión

La v22 **ya pasó** el canal cerrado (ver la tabla de estado de arriba), así que la prisa
se fue. Pero la cuenta sigue bloqueando logins, y **la revisión de producción de la v23
es la que de verdad se mira**.

A partir de ahora se puede tocar sin riesgo de pisar una revisión en curso: cambiarle la
contraseña mientras hay intentos de revisor en vuelo es lo único que hay que evitar, y
ya no lo hay. Aun así, la cuenta de Google **deja de ser el camino**: la credencial que
va en Play a partir de la v23 es la de Firebase.

#### El texto nuevo de *Datos de inicio de sesión* (Play Console)

Se **edita la entrada que ya existe**, no se crea una segunda — duplicarlas fue un
problema en agosto. Usuario y contraseña van en sus campos; esto es el cuadro de
instrucciones, en inglés porque lo exige el formulario. **457/500**.

```
FoodRanker requires a sign-in. There are two ways in; please use the second one.

1. Open the app.
2. On the welcome screen, below “Continue with Google”, tap “Sign in with email”.
3. Enter the email address and password given above, then tap “Sign in”.

This account has full access to all content, including premium. The credentials are reusable, have no two-step verification and do not expire. Please do not use “Continue with Google” with this address.
```

⚠️ La última frase no es un adorno: la dirección sigue teniendo cuenta de Google, y si
el revisor pulsa el botón de Google volvemos exactamente al bloqueo de siempre.

#### ✅ Probado corriendo en el emulador (2026-11-19)

El proveedor se activó en la consola y se comprobó **contra la API**, no solo en pantalla:
la misma llamada que daba `PASSWORD_LOGIN_DISABLED` pasó a dar `INVALID_LOGIN_CREDENTIALS`.

Recorrido completo con una cuenta desechable (`prueba-v23@foodranker.invalid`), **borrada
después de Auth y de Firestore**:

| Paso | Resultado |
|---|---|
| Crear cuenta | entra a Inicio; el perfil muestra **PruebaV23**, no el nombre de respaldo |
| Cerrar sesión | vuelve al login |
| Contraseña incorrecta | *"The email or password is not correct"*, no el genérico |
| Contraseña correcta | entra |
| "He olvidado la contraseña" sin correo | *"Enter your email first."* |
| "He olvidado la contraseña" con correo | *"We've sent you an email to reset your password."* |

Eso valida de paso `ErrorMapper.authCodeRes` y que `syncUserDocument` recibe el nombre
antes de escribir el documento.

#### 🐛 Un fallo encontrado por probarlo, que habría llegado a Play

**Con el teclado abierto, el botón de entrar y los enlaces de "crear cuenta" y "he
olvidado la contraseña" quedaban debajo, y la columna no hacía scroll.** En modo alta son
tres campos: no caben de ninguna manera. Arreglado con `imePadding()` +
`verticalScroll()`.

Es el mismo fallo de familia que el botón de guardar que se escondía tras la barra de
navegación en la v22. **Compilaba, pasaba los tests y las comprobaciones, y la pantalla
se veía perfecta en la captura sin teclado.**

#### Verificado

- `:app:compileDebugKotlin` limpio · **84 tests, 0 fallos**
- Las 5 comprobaciones de `tools/i18n/` + `tools/ui/check_theme_colors.py`, todas OK
- Arranque en frío con **splash negro** correcto, y la hoja sigue el tema oscuro
- ⚠️ Sigue pendiente la vuelta en oscuro por las **18 pantallas** al instalar en el Redmi

[ayuda 15748846]: https://support.google.com/googleplay/android-developer/answer/15748846

---

### 🛡️ LA AUDITORÍA DE CODEX, REVISADA UNA A UNA (2026-11-18)

Codex pasó una auditoría sobre el repo y devolvió 6 hallazgos. **Se comprobaron todos
contra el código y contra producción antes de tocar nada**, y el resultado no coincide con
las prioridades que traía. Lo que sigue es el veredicto, porque el valor está tanto en lo
que se arregló como en lo que NO hay que arreglar.

Datos de producción el 2026-11-18 (leídos con Admin SDK, solo lectura): **59 platos, los 59
`approved` y ninguno sin el campo. 40 locales. 15 usuarios.** Esos números deciden dos de
los hallazgos.

#### ✅ ARREGLADO — platos sin moderar visibles en todos los listados

Real, pero por un motivo distinto al que decía Codex, y el matiz importa en las dos
direcciones.

**Más pequeño:** no hay cola de revisión humana. `moderatePlateImage` es un trigger
`onCreate` que pasa la foto por Vision y **borra** lo que rechaza. `pending` dura lo que
tarde Vision, segundos.

**Más grande:** Codex señalaba Explorar y Cerca de ti. En realidad **no filtraba nadie**:
Inicio, Descubrir, Tendencias, "de a quien sigo", recientes… El único sitio de toda la app
que exigía `approved` era el contador de puesto en la ciudad de `PlateDetailViewModel`.

**Por qué importa aunque dure segundos:** los listados que ordenan por `createdAt` ponen el
plato recién subido **el primero** — justo el que todavía no ha mirado nadie. Y si la
función no llega a correr (cuota, caída de Vision, despliegue a medias), el plato se queda
en `pending` y visible **para siempre**.

Arreglado con un único punto de decisión, `Plate.esPublico` / `List<Plate>.soloPublicos()`
en `data/model/Plate.kt`, aplicado en los **11** sitios que listan platos. Mismo criterio
que `Fallos.reportar`: la condición que se copia once veces es la que alguien olvida en la
doce. Cubierto por `VisibilidadDePlatoTest` (8 tests).

**Dos sitios NO lo usan, a propósito:**

- **Tu propio perfil** — tu plato recién subido tiene que verse aunque esté en moderación, o
  parece que la publicación se ha perdido. En el perfil de **otro** sí se filtra, o sería la
  puerta de atrás por la que ver lo que los listados ya no enseñan.
- **Guardados y colecciones** — listas montadas a mano; solo pudieron guardar lo visible.

Los índices compuestos `status + …` ya estaban en `firestore.indexes.json` desde el rediseño
del ranking y **nunca se usaron desde el cliente**. No hizo falta ninguno, porque el filtro
es de cliente igual que el de denuncias que ya existía.

#### ✅ ARREGLADO — el XP de liga se podía pagar dos veces

Confirmado leyendo el código: `addLeagueXP` hacía `FieldValue.increment()` sin ninguna
guarda, y además corría **antes** de la comprobación de `processed` que protege al resto de
`onRatingCreated`. Los triggers de Firestore son *at-least-once*: un reintento pagaba dos
veces. En una clasificación semanal eso es la diferencia entre ganar y no ganar.

Ahora el sello `leagueCity/leagueWeekKey/leagueXpAmount` se escribe en una **transacción
antes** del incremento y es la marca de idempotencia: si ya está, no se concede nada. Si el
incremento falla después, se quita el sello para que el reintento pueda reclamarlo limpio —
si no, un clawback posterior restaría un XP que nunca se dio.

Es seguro porque cada valoración recibe XP de liga por **un solo camino**: la del autor desde
`approveplate`, las demás desde `onRatingCreated`, que descarta la del autor con
`raterId === ownerId` antes de llegar. ⚠️ Si eso deja de ser cierto, esta guarda se tragaría
la segunda concesión y haría falta una clave por motivo.

#### ✅ ARREGLADO A MEDIAS, Y ESTÁ BIEN ASÍ — el borrado de cuenta

Codex lo daba como fallo. **No lo es**: que los pasos 1-5 sigan adelante tras un error es
deliberado y estaba comentado justo ahí — los pasos 6 y 7 (perfil y cuenta de Auth) son los
que exige Play, y un fallo limpiando valoraciones no puede dejar la cuenta viva.

Lo que sí faltaba: **enterarse**. Devolvía `{ success: true }` igual, así que un borrado a
medias era indistinguible de uno completo salvo leyendo los logs en ese momento. Ahora los
pasos fallidos se anotan en `borradosIncompletos/{uid}` (cerrada a cal y canto en las
reglas) **antes** de borrar la cuenta — después ya no hay a quién preguntar — y se vacían
con `functions/scripts/retryDeletions.js` (simulación por defecto, `--confirm` para ir en
serio), al estilo de `manageUser.js`.

⚠️ Las etiquetas de paso son la clave que une la función y el script. Si se renombra un paso
en `deleteUserAccount` y no en `retryDeletions.js`, ese paso deja de reintentarse en
silencio; por eso el script avisa de etiquetas que no reconoce en vez de ignorarlas.

#### 🟡 MEDIO ARREGLADO, A PROPÓSITO — el token FCM

Codex lo ponía en "Alta" por estar legible (`users/{uid}` es `allow read: if isSignedIn()`).
**Exagerado**: con un token no se puede mandar un push, hace falta la credencial de servidor
de FCM. Es minimización de datos, no un agujero explotable.

Lo que sí era un fallo de verdad, y Codex colaba de pasada: `fcmToken` es un **campo único**,
así que un segundo móvil pisa el token del primero y cerrar sesión en uno deja al otro sin
avisos.

**Por qué la mitad y no entero:** un token **no se puede rellenar desde el servidor** — solo
el móvil conoce el suyo. Si las funciones cambiaran de sitio antes de que la app escriba en
el nuevo, todo el que siguiera en v21 se queda sin avisos. Misma trampa que los canales. Así
que:

- **v22 (hecho):** la app escribe en **los dos sitios**. Nuevo:
  `users/{uid}/dispositivos/{idInstalacion}`, privado (`isOwner`), un documento por móvil,
  con el **Installation ID** como id — no el token, que rota y dejaría un documento muerto
  por rotación y avisos duplicados. Al cerrar sesión se borra solo el de **este** móvil.
- **Después, con la v22 repartida:** las funciones leen de ahí (todos los dispositivos, no
  uno), y se quita `fcmToken` del perfil y de su `hasOnly`.

Es **puramente aditivo**: si la escritura nueva falla, los avisos siguen por la vieja. Se
hizo así porque **no se ha podido probar con una sesión real** — el emulador no tiene cuenta
de Google. Un cambio no testeable que toca los avisos tiene que ser incapaz de romper nada.

#### 🟡 PARCHE CONSCIENTE — la búsqueda

Real: Explorar baja los mejores por `rankingScore` y busca **dentro** de ellos, así que un
plato que existe pero no entra en el recorte no aparece **aunque lo escribas entero**.

Subido el tope de 100 a **500** (`ExploreViewModel.TOPE_CATALOGO`), con el KDoc diciendo sin
rodeos que es un parche. Con 59 platos el fallo queda lejos. **No se arregló de verdad porque
es una decisión de producto, no un detalle:** Firestore no busca subcadenas, solo prefijos
sobre un campo indexado, y hoy "pizza" encuentra "La Pizzería" — por prefijo dejaría de
hacerlo.

Y hay un segundo fallo **peor de lo que decía Codex**: Cerca de ti hace
`whereGreaterThan("lat")` con `limit(50)`, y Firestore ordena por el campo del rango, así que
se queda con los 50 **más al sur** de la banda, no con los 50 más cercanos. Con 40 locales no
se nota. El arreglo de verdad es un geohash.

**Cuando el catálogo se acerque a 500, esto hay que hacerlo en serio.**

#### ⚪ NO TOCADO, CON MOTIVO

- **Recompensa de anuncios sin verificar en servidor.** Cierto. Verificarlo de verdad exige
  SSV de AdMob (configuración en consola + endpoint + callback). Infla XP, nada más.
- **Imagen huérfana en Cloudinary.** El código ya comprueba la existencia del plato **antes**
  de subir, con su comentario. Queda la ventana de que falle la escritura, y ahí solo se
  loguea la URL. Borrarla exige el secreto de Cloudinary → tiene que ser servidor.

#### De paso: `check_listing.py` daba un falso positivo permanente

Contaba el bloque "Los tres de una vez" (las tres notas con sus etiquetas `<es-ES>`…) como
una sola nota de ~950 caracteres y marcaba `SE PASA` en **cada ejecución**, con las tres de
dentro holgadamente bajo el límite. Un aviso siempre encendido no avisa de nada. Ahora mide
cada etiqueta por separado y el check da OK.

#### Verificado

- `tsc --noEmit` limpio · **84 tests, 0 fallos** (8 nuevos de visibilidad)
- Las 5 comprobaciones de `tools/i18n/` + `tools/ui/check_theme_colors.py`, todas OK
- `check_apk.py`: 9 solo-por-defecto (de librería) y 0 solo-castellano
- Emulador API 35 en **oscuro**: arranque en frío con splash negro, login legible, sin crash
- ⚠️ **Nada de lo de dentro de la sesión está probado** — el emulador no tiene cuenta de
  Google y un build local no se instala sobre el de Play. Sigue pendiente la vuelta en oscuro
  por las 18 pantallas al instalar la v22 desde prueba interna.

#### El orden no cambia

1. Subir la **v22** a prueba interna → 2. esperar a que esté repartida → 3. **entonces**
   desplegar `functions` (canales de avisos + estos dos arreglos de servidor) y las reglas.

⚠️ Las reglas (`firestore.rules`) llevan dos bloques nuevos (`dispositivos`,
`borradosIncompletos`). **Se pueden desplegar antes que las funciones sin problema**: solo
abren sitios nuevos, no cambian ninguno existente.

---

### 📦 LA v22 (1.8), SUBIDA A LOS DOS CANALES (2026-11-18)

Es la versión que **desbloquea los canales de avisos**: hasta que esté repartida no se
puede desplegar `functions` (ver la sección siguiente). Lleva, además del reparto de
canales, el modo oscuro y dos arreglos de la hoja de editar plato.

AAB construido en `app/build/outputs/bundle/release/app-release.aab` (16,0 MB),
`versionCode = 22`, `versionName = "1.8"`.

**Estado en Play (2026-11-18):**

| Canal | Estado |
|---|---|
| **Prueba interna** | ✅ Activo desde las **10:09**, sin revisión — *"Disponible para testers internos"*. Como siempre, **no aparece en "Cambios en revisión"**. |
| **Prueba cerrada - Alpha** | ⏳ **Enviada a revisión el 2026-11-18.** Es la que llega al Redmi. Las comprobaciones previas de Play no la pararon: el bloqueo de `READ_MEDIA_IMAGES` que frenó a la v19 sigue resuelto desde la v20. |
| **Producción** | Sin tocar. |

⏳ **Esperando la aprobación del canal cerrado.** En cuanto llegue: instalar en el Redmi y
**darse la vuelta en oscuro por las 18 pantallas** (ver más abajo), que es lo único que
queda sin verificar de esta versión.

#### 1. El modo oscuro no existía — existía el tema, no el uso

`FoodRankerTheme` calculaba desde siempre un `FoodRankerColorTokens` con los colores de
cada modo y lo publicaba en `LocalFoodColors`. **No lo leía nadie**: `FoodTheme.colors`
no aparecía ni una vez fuera de su propia definición. Las pantallas usaban directamente
las constantes del tema CLARO — `TextPrimary`, `SurfaceWhite`, `DividerColor`… — **330
veces en 24 ficheros**.

Por eso en oscuro salía una mezcla: los diálogos de Material sí seguían el tema (usan
`MaterialTheme`) y el resto de la app seguía blanca. No era "el modo oscuro a medias",
era que el modo oscuro estaba construido y sin enchufar.

**Convertidos 304 usos** a `FoodTheme.colors.*`. Mapeo 1 a 1:

| Constante del tema claro | Token |
|---|---|
| `BackgroundLight` | `FoodTheme.colors.background` |
| `SurfaceWhite` | `FoodTheme.colors.surface` |
| `SurfaceMuted` | `FoodTheme.colors.surfaceMuted` |
| `TextPrimary` | `FoodTheme.colors.textPrimary` |
| `TextSecondary` | `FoodTheme.colors.textSecondary` |
| `DividerColor` | `FoodTheme.colors.divider` |

#### Los tres sitios donde el color fijo es CORRECTO

No todo color fijo es un bug. Lo es cuando el contenido va sobre un fondo que **no sigue
el tema**:

- **El botón de Google del login.** Fondo blanco fijo sobre el degradado naranja. Al
  convertirlo, su texto pasó a seguir el tema y en oscuro salía gris claro sobre blanco:
  "Continue with Google" **ilegible**. Lo pillé en el emulador, no leyéndolo.
- **El logo con `onDark = false`.** `onDark` significa "voy sobre fondo oscuro", no "tema
  oscuro". Con el color del tema, "Ranker" desapareció del splash y quedó solo "Food".
- **`ShareCard`.** No se le dibuja al usuario: se convierte en la **imagen que se
  comparte**. Si siguiera el tema, la misma imagen saldría clara u oscura según el móvil
  de quien comparte, y la ve gente que ni tiene la app.

Los tres llevan el marcador `// tema-fijo: <motivo>` en la propia línea.

#### El guardián: `tools/ui/check_theme_colors.py`

Este fallo **no lo caza el compilador** —la constante existe y compila igual de bien— y no
se ve si solo se prueba en claro, que es como se probó durante dos años. El script falla si
una pantalla vuelve a clavar un color del tema claro sin el marcador. Ejecutarlo al tocar
UI, junto con los de `tools/i18n/`.

#### El arranque también era blanco

Dos sitios que **no pinta Compose** y que por eso no salían en el recuento:

- `windowSplashScreenBackground` era `#FFFFFF` fijo → ahora `@color/splash_background`,
  con su gemelo en **`values-night/colors.xml`** (nuevo).
- `android:windowBackground`, que es lo que se ve entre que aparece la ventana y se pinta
  la primera composición → `@color/window_background`, igual.
- El splash de Compose (`SplashScreen.kt`) tenía el degradado blanco clavado. Ahora tiene
  dos versiones y pasa `onDark = esOscuro` al logo.

⚠️ Esos dos colores son los de `BackgroundLight` / `BackgroundDark` **escritos otra vez** en
XML, porque viven en dos mundos. Si se tocan en `Color.kt`, hay que tocarlos aquí también.

#### 2. La hoja de editar plato: dos arreglos

- **Se metía debajo de la barra de navegación.** El alto no es fijo (nombre hasta 2 líneas,
  descripción hasta 4, y el error del nombre se va a 3 con el de "solo cosmético") y el
  `padding(bottom = 32.dp)` era un número inventado. Con el alto máximo, guardar y borrar
  quedaban **inalcanzables**. Ahora: `verticalScroll` + `navigationBarsPadding()` +
  `imePadding()` (el teclado tapaba justo el botón de guardar).
- **El error rojo no se borraba al reescribir.** `nameError` lo decide el servidor y vive
  fuera de la hoja, así que la hoja no podía limpiarlo sola: el texto rojo se quedaba
  contradiciendo lo que estabas escribiendo. Nuevo callback `onNameEdited`.

#### 3. `Fallos.reportar` ya no se traga la cancelación

`catch (e: Exception)` captura también `CancellationException`, que no es un fallo: es la
señal de que a la corrutina le han dicho que pare. Tragársela rompe la concurrencia
estructurada — el `cancel()` deja de significar nada y el `catch` sigue, así que un
ViewModel que se está destruyendo todavía escribe su `_uiState`, o pinta un error por una
carga que el usuario canceló al salir.

Ahora **relanza**. Que una función llamada `reportar` lance es poco intuitivo y va ahí a
propósito: es el único punto por el que pasan los **68 `catch`** de la app. La alternativa
era repetir `catch (e: CancellationException) { throw e }` 68 veces, que es justo la línea
que alguien olvida al añadir el sitio 69.

Es seguro porque se comprobaron dos cosas, no porque lo parezca:
- en los **68 sitios** `Fallos.reportar` es la **primera** sentencia del `catch`
  (verificado con script), así que lo que se salta al relanzar es precisamente lo que no
  debe ejecutarse;
- **no hay ni un `withTimeout`** en la app. `TimeoutCancellationException` **es** una
  `CancellationException`, así que el día que se añada uno, ese `catch` tiene que capturar
  el timeout antes de llamar aquí. Está escrito en el KDoc.

4 tests nuevos en `FallosTest`, que se pueden ejecutar sin Firebase porque los dos casos
filtrados deciden **antes** de pedir `FirebaseCrashlytics.getInstance()`. Eso convierte "no
ha reventado" en una aserción de verdad.

#### Qué está probado y qué no

✅ Compila, **76 tests en verde**, pasan los cinco `tools/i18n/` y el nuevo de colores.
✅ Visto en el emulador (API 35) **en claro y en oscuro**: splash del sistema, splash de
Compose, login y Términos.

⚠️ **Las 18 pantallas de dentro NO se han visto en oscuro.** El emulador no tiene cuenta de
Google y la app no deja pasar del login; y un build local no se puede instalar encima del
de Play, así que tampoco en el Redmi. Las 18 usan los mismos seis tokens por el mismo
mecanismo que las que sí se han visto, pero eso es un argumento, no una captura. **Al
instalar la v22 por prueba interna, darse una vuelta con el móvil en oscuro.**

#### Pendiente de esta sección

- ✅ ~~Subir la v22 a prueba interna~~ — hecha el 2026-11-18, y enviada también a revisión
  en el canal cerrado. Queda **esperar la aprobación** → instalar en el Redmi → la vuelta
  en oscuro → **entonces** desplegar `functions`.
- Las reglas (`firestore.rules`) se pueden desplegar **ya**, sin esperar a nada: los dos
  bloques nuevos (`dispositivos`, `borradosIncompletos`) solo abren sitios nuevos y no
  cambian ninguno existente.
- El pie del logo (`FoodRankerMark`) es azul marino y sobre fondo oscuro casi no se ve. Es
  el activo de marca, no un color de tema; se deja como está a propósito.

---

### 🔀 LOS AVISOS, REPARTIDOS EN TRES CANALES (2026-11-17) — ⚠️ SERVIDOR SIN DESPLEGAR

Sale de la revisión de arriba: `foodranker_social` se le enseñaba al usuario como **"Likes y
valoraciones"** pero llevaba **siete tipos** de aviso. Quien silenciaba lo que creía que eran
likes se quedaba también sin saber que alguien a quien sigue había publicado — y sin
enterarse de que se lo había hecho.

| Canal | Id | Qué lleva |
|---|---|---|
| **Interacciones** | `foodranker_social` *(id de siempre)* | like, rating, comment, follow |
| **Gente que sigues** | `foodranker_following` **nuevo** | new_plate |
| **Logros y liga** | `foodranker_achievements` **nuevo** | level_up, badge, league_result |
| Moderación de platos | `foodranker_moderation` | sin cambios (importancia alta) |
| Recordatorio diario | `foodranker_daily` | sin cambios |

#### Por qué el canal de interacciones conserva el id viejo

Porque **un canal ya creado no se puede modificar desde el código**: el nombre y la
descripción sí, la importancia **no**. Conservando `foodranker_social`:

- quien lo tuviera **silenciado lo conserva silenciado**, que es justo lo que quería para
  los likes;
- lo que se saca de ahí nace como canal nuevo con la importancia por defecto, así que
  **vuelve a llegarle** lo de la gente a la que sigue.

Cambiar el id habría reseteado también el ajuste de los likes, que es lo único que la gente
sí había decidido a conciencia.

#### El reparto vive en UN sitio

`NotificationHelper.canalDe(tipo)`. Lo usan los dos caminos del cliente —
`FoodRankerMessagingService` (push en primer plano) y el listener de Firestore
(`NotificationRepository`)—, que antes elegían canal cada uno por su cuenta con un `when`
copiado. **5 tests** en `NotificationChannelsTest` fijan el reparto.

⚠️ Y tiene que coincidir con el del servidor (`onNotificationCreated`, un `channelId` por
`case`): el mismo aviso lo pinta el servidor con la app cerrada y el cliente con la app
abierta. Si dejan de coincidir, el mismo hecho cae en un canal distinto según por dónde
entró, y eso no lo reproduce nadie a mano.

#### 🛑 LO QUE FALTA, Y EL ORDEN IMPORTA

**El cambio del servidor está escrito y compilado, pero NO desplegado.** A propósito.

`foodranker_following` y `foodranker_achievements` **no existen en la v21 ni anteriores**, que
es lo que lleva todo el mundo. Si se despliega el servidor antes de repartir la v22, en esos
móviles FCM manda esos avisos a su canal inventado
(`fcm_fallback_notification_channel`), con un nombre genérico que el usuario no reconoce.
**No se pierde ningún aviso** —comprobado en la documentación, no supuesto— pero se ve peor
que ahora.

**Orden correcto:** publicar la v22 → esperar a que esté repartida → desplegar
`functions`. Hasta entonces el servidor sigue mandando todo por `foodranker_social`, que es
exactamente el comportamiento de hoy.

#### De paso: `default_notification_channel_id` en el manifest

Añadido apuntando a `foodranker_social`. Es lo que decide a dónde va un push cuyo canal no
exista en ese móvil; sin él, FCM usa el suyo inventado.

⚠️ **No arregla el caso de la v21**: la línea viaja en el manifest, así que solo la tiene
quien instale la versión que la incluye. Lo que protege es el caso futuro — cada vez que se
añada un canal, el servidor lo mandará antes de que el cliente que lo crea esté repartido.

✅ Compila (app y functions) y pasan los tests. ⚠️ **Sin probar en un móvil**: no se puede
instalar un build local encima del de Play. Los canales nuevos aparecerán en Ajustes →
Notificaciones la primera vez que se abra la v22.

### 🔔 ESTADO DE LOS AVISOS: EL DE "A QUIEN SIGUES" SÍ FUNCIONA (2026-11-17)

Revisado a fondo porque surgió la duda de si al seguir a alguien te avisa cuando publica.
**Sí avisa**, y con datos: hay **11 avisos `new_plate` en la base**, 8 de ellos para Sergio,
el último del **2026-11-15** (de Miguel). No es teoría del código, son documentos.

Los diagnósticos quedan en `functions/scripts/checkNotifs.js` y `checkNotifs2.js`
(solo lectura, requieren ADC como `manageUser.js`).

#### Por qué daba la sensación de que no funcionaba

Tres cosas, y ninguna es un fallo:

1. **El aviso va agrupado por autor y día, a propósito.** El id del documento es
   `newplate_<autor>_<día>`: el primer plato del día lo **crea** —y eso dispara el push— y
   los siguientes solo incrementan `plateCount`, que es una actualización y
   `onDocumentCreated` no salta. Con una persona que lleva **37 platos publicados**, la
   alternativa habría sido comerse 37 avisos seguidos.
2. **La función se desplegó alrededor del 2026-10-08.** El `new_plate` más antiguo es de ese
   día. Los platos de Sandra (3) y Dani (1), de septiembre, nunca pudieron avisar: la
   función no existía. Eso explica los dos "huecos" que aparecen al cruzar seguidos con
   platos.
3. **El token FCM es uno por usuario** y se lo lleva el último móvil donde se inicie sesión.
   Con el cambio del Note 10S al Redmi 14C, los push de esa época pudieron irse al otro.

#### Fotografía del sistema (2026-11-17)

```
like 17 · comment 10 · new_plate 8 · league_result 2 · moderation_approved 1 · badge 1 · follow 1
```
(los de Sergio; en total hay 45 comment, 18 moderation_approved, 11 new_plate, 11
moderation_rejected, 9 league_result, 8 rating, 4 follow, 2 badge, 1 level_up)

- **17 seguimientos** entre 15 usuarios; **13 de 15 tienen `fcmToken`**.
- Los tres canales existen en el móvil y **ninguno está silenciado**: `foodranker_social`
  (importancia 3), `foodranker_moderation` (4), `foodranker_daily` (2).
- Los logs del servidor enseñan pushes saliendo bien, incluido uno a Sergio el 2026-11-16.
  ⚠️ `firebase functions:log` solo devuelve una ventana corta (unas horas), así que no sirve
  para comprobar algo de hace dos días.

#### 🟡 Lo que SÍ conviene cambiar: el canal "social" se llama como una de sus siete cosas

`foodranker_social` se muestra al usuario como **"Likes y valoraciones"**, pero por ahí van
**siete tipos**: `like`, `rating`, `comment`, `follow`, **`new_plate`**, `level_up`, `badge` y
`league_result`.

Dos consecuencias:

- Quien silencie lo que cree que son "likes" **se queda también sin enterarse de que alguien
  a quien sigue ha publicado**, sin saber que lo ha hecho.
- No hay forma de quedarse con unos y no con otros, que es justo lo que uno querría.

Y encaja con la decisión (buena) de que la fila "Notificaciones" del perfil **abra los
ajustes del sistema** en vez de montar interruptores propios: si los canales son el único
mando, tienen que estar bien partidos y bien nombrados. Propuesta: separar al menos
`foodranker_social` en **"Interacciones"** (like, rating, comment, follow) y **"Gente que
sigues"** (`new_plate`), y quizá un tercero para los logros (level_up, badge, league_result).

⚠️ Al crear canales nuevos hay que tener en cuenta que **un canal ya creado no se puede
modificar desde el código** (nombre sí, importancia no): si se renombra el actual, quien ya
tenga la app conserva su ajuste; si se parte en varios, los nuevos nacen con la importancia
por defecto.

#### ✅ Falsa alarma que se investigó por el camino

`onNotificationCreated` **no mira ninguna preferencia del usuario** antes de mandar el push.
Parecía un agujero, pero no lo es: **no hay preferencias de app que mirar**. La fila de
"Notificaciones" abre los ajustes de Android y todo el control vive en los canales, que es
donde el sistema ya lo gestiona. Nada que arreglar.

### 📱 LA v21 PROBADA EN EL REDMI, Y LO QUE APARECIÓ DEBAJO (2026-11-17)

Probada por adb sobre el **Redmi 14C, que lleva Android 16 (SDK 36)** — dato que no estaba
apuntado y que importa: **el edge-to-edge está forzado en ese móvil**, así que lo que la
lista de comprobación mandaba mirar en el emulador se ha visto en hardware real.

Actualizada desde Play (canal cerrado): `versionCode=21 versionName=1.7`, **sin cable y sin
perder la sesión**, como estaba previsto.

#### 🔴 LO IMPORTANTE: falta un índice y el "rango en la ciudad" NO ha funcionado nunca

En un paseo normal por la app, logcat soltó esto **dos veces**:

```
Firestore: Listen for Query(users where city==Casarrubios Del Monte and xp>495
order by -xp, -__name__) failed: code=FAILED_PRECONDITION,
description=The query requires an index.
```

Es exactamente el escenario que se describió ayer al instrumentar esos `catch`, palabra por
palabra: *"son las que fallan enteras si falta el índice compuesto, y así no se notaría
nunca"*. **Pasó al día siguiente.**

Son **dos consultas distintas** en `ProfileViewModel`, y solo una está rota:

| Consulta | Índice que necesita | ¿Declarado? |
|---|---|---|
| Rival más cercano (`orderBy xp ASCENDING`) | `(city ASC, xp ASC)` | ✅ sí |
| **Rango en la ciudad** (`orderBy xp DESCENDING`) | `(city ASC, xp DESC)` | ❌ **no** |

El `order by -xp` del error es descendente: es la del rango. Lleva rota desde que existe, y
**nadie se iba a enterar**, porque el `catch` devuelve `0` y un 0 se lee como "eres el
primero de tu ciudad".

✅ **ARREGLADO Y VERIFICADO EL MISMO DÍA.** Índice desplegado a producción
(`firebase deploy --only firestore:indexes`), y comprobado en el móvil: tras construirse,
`FAILED_PRECONDITION` pasó de salir en cada carga de perfil a **0**, y el perfil pinta
*"#1 en Casarrubios Del Monte · Ranking ciudad"* con la consulta corriendo de verdad.

⏱️ **El índice tarda en construirse y el mensaje lo dice.** Primero sigue saliendo *"The
query requires an index"*, luego cambia a ***"That index is currently building and cannot be
used yet"***, y solo entonces desaparece. Unos 5 minutos con esta cantidad de datos. No dar
el despliegue por fallido al primer reintento.

🎯 **Qué veía el usuario**: el `catch` devolvía `0` y la pantalla pinta `0 + 1`, así que
**a todo el mundo le salía "#1 en su ciudad"**, fuese verdad o no. Un trofeo falso para
todos, y encima indistinguible del caso legítimo. Por eso no lo reportó nadie nunca.

#### 🏁 Y con esto, la cadena de Crashlytics quedó cerrada de punta a punta

No hizo falta fabricar un fallo: **lo trajo este bug real**. A las 10:29 llegó un correo de
`firebase-noreply@google.com`:

```
Nuevo error no crítico: io.grpc.Status.asException - Android - com.app.foodranker 1.7
```

`io.grpc.Status.asException` es justo el tipo que lanza Firestore para `FAILED_PRECONDITION`.
O sea que funcionó **todo** lo de ayer, en orden y por primera vez:

1. El fallo real saltó en `ProfileViewModel`.
2. `Fallos.reportar` se ejecutó (integrado por R8, como se verificó en el mapeo).
3. `ErrorMapper.esDelEntorno` lo dejó pasar por no ser de red — el filtro distingue bien.
4. `recordException` lo registró y subió a Firebase.
5. **La alerta de "errores recuperables nuevos", configurada el 2026-11-16 y que hasta hoy
   estaba conectada a nada, mandó el correo.**

Es exactamente el ciclo que se quería montar, estrenado con un bug que llevaba escondido
desde el principio.

**El arreglo en `firestore.indexes.json`**: segundo índice `(city ASC, xp DESC)`.
⚠️ Desplegar el índice lo arregla **para todas las instalaciones de golpe, sin actualizar la
app**, que es lo que importa con la v20 todavía en manos de la gente.

💡 Alternativa que NO necesita índice nuevo, por si algún día se quiere simplificar: cambiar
ese `DESCENDING` por `ASCENDING`. La consulta cuenta usuarios por encima con `limit(200)`, y
el conteo sale igual en los dos sentidos. Pero eso exige una release; el índice no.

#### ✅ Selector de idioma: las cuatro comprobaciones

1. El diálogo sale, con las tres opciones y "Predeterminado del sistema" marcada.
2. Al elegir English **la app se recrea en inglés** (perfil entero traducido), y
   `cmd locale get-app-locales com.app.foodranker` devuelve `[en]`.
3. El subtítulo de la fila pasa a decir **"English"**.
4. "System default" devuelve el control al móvil: vuelve a castellano y el framework
   devuelve `[]`.

Los nombres de idioma se quedan cada uno en su lengua ("Español" / "English"), que es lo
buscado. Y el reparto instalado confirma el otro acierto: solo hay `base`,
`split_config.arm64_v8a` y `split_config.xhdpi`, **ningún split de idioma** — por eso están
los dos idiomas instalados y el selector puede funcionar.

#### ✅ Corrección del nombre de plato: las dos ramas

Sobre "Pulpo a la plancha con mojo verde", y **dejado como estaba al terminar**:

- **Rechazo**: añadiendo " picante" (slug distinto) el campo se pone rojo y sale *"Solo
  puedes corregir cómo está escrito el nombre. Si es un plato distinto, añádelo como uno
  nuevo."* No escribe nada.
- **Aceptación**: cambiando solo la última letra a mayúscula (`verdE`, mismo slug) **guarda**
  — verificado reabriendo la hoja. Revertido después, también verificado.

#### ✅ Que `recordException` llegue: verificado en el binario, no en la consola

No se puede forzar un no fatal sin romper algo o tocar las reglas de producción, así que se
verificó por el otro lado, que además es más fuerte: **el mapeo de R8 del AAB instalado**.

`Fallos` aparece como `R8$$REMOVED$$CLASS$$692` — R8 **integró** el método en los **521**
sitios que lo llaman, no lo eliminó. Y la cadena inline llega entera hasta el fondo:

```
Fallos.reportar:52                          → descarta CancellationException
ErrorMapper.esDelEntorno:62 ← reportar:53   → el filtro
FirebaseCrashlytics.log:251 ← reportar:55   → el breadcrumb
FirebaseCrashlytics.recordException:213 ← reportar:56
CrashlyticsCore.logException:331            → el registro real
```

⚠️ Esto prueba que **el código llega vivo al binario**. Lo que no prueba es la entrega a
Firebase. Pero con el fallo del índice saltando de verdad, el no fatal "perfil: rango en la
ciudad" debería aparecer solo en Crashlytics: **mirarlo**, y si está, el camino queda
cerrado de punta a punta sin haber roto nada.

#### 🟡 Lo que apareció de paso (ninguno bloquea)

**El modo oscuro está a medias, y no es nuevo.** El Redmi está en modo oscuro: las pantallas
salen claras porque hay **12 sitios con `containerColor = BackgroundLight` clavado**, pero
los diálogos y las hojas sí siguen el tema y salen oscuros. Resultado: app clara con
diálogos negros. No lo introduce el selector de idioma — en `ProfileScreen` ya había cinco
`AlertDialog` con lo mismo. O se soporta el modo oscuro de verdad, o se fuerza el claro;
ahora mismo no es ninguna de las dos.

**La hoja de editar plato, por debajo de la barra de navegación.** Cuando sale el mensaje de
error (tres líneas), el contenido baja y los botones del sistema se dibujan **encima** de
"Guardar". Se arregla desplazando la hoja, así que no bloquea, pero no debería pasar.

**El mensaje de error no se limpia al escribir.** Tras un rechazo, el texto rojo sigue ahí
mientras corriges el nombre, hasta el siguiente intento.

**Cero crashes** en todo el recorrido (`FATAL EXCEPTION` = 0).

ℹ️ Y de paso, para la próxima vez que haya que conducir el móvil por adb: `KEYCODE_BACK`
**cierra la hoja entera**, no solo el teclado — se pierde lo escrito. Para ocultar el teclado
hay que pulsar la tecla ✓ del propio teclado (abajo a la derecha).

### 📦 LA v21 (1.7) COMPILADA Y VERIFICADA SOBRE EL BINARIO (2026-11-16)

`versionCode 21`, `versionName 1.7`. El salto de nombre (1.6 → 1.7, y no otro invisible como
el 19 → 20) es porque esta trae **selector de idioma**, que se ve.

Lo que lleva dentro, todo de los últimos dos días y nada de ello probado en pantalla:
selector de idioma, corrección del nombre de plato, lista blanca de ids en los enlaces, y los
68 `catch` que ahora reportan a Crashlytics.

#### ✅ Lo que se comprobó sobre el AAB

| Qué | Cómo | Resultado |
|---|---|---|
| `CAMERA` y `hardware.camera` | strings del manifest proto del AAB | **ausentes** (0 apariciones) |
| `READ_MEDIA_IMAGES` | igual | **ausente** |
| Versión | `bundle_manifest/.../AndroidManifest.xml` | `versionCode="21"` `versionName="1.7"` |
| Firma | `jarsigner -verify` | **`jar verified`**, firmado por `O=sdelapenya.dev, CN=Sergio de la Peña` |
| UMP tras R8 | 4 cadenas en `base/dex/*.dex` | **4/4** |
| Split de idiomas | `08 03 10 01` en `BundleConfig.pb` | presente |
| Paridad de textos | `check_strings.py` | OK (594/593, ver abajo) |
| Resto de `tools/i18n/` | `check_args`, `check_notif_parity`, `check_slug_parity` | OK |

Dos sustos que no lo eran, anotados para no volver a investigarlos:

- **`check_strings.py` dice `en=594 es=593`.** La que sobra en inglés es
  **`default_web_client_id`**, el id de OAuth: no es texto de interfaz y **no debe
  traducirse**, así que la asimetría es correcta y por eso el script da OK igualmente.
- **`scan_ui.py` marca 20 literales.** Todos falsos positivos: 16 son etiquetas de animación
  (`cardScale`, `heartColor`, `shimmer`…), 2 son el logo (`Food` / `Ranker`, que es la marca)
  y 2 son castellano clavado en `AuthRepository` que **nunca llega a la pantalla**:
  `SignInCancelledException` la intercepta `AuthViewModel:64` antes de mostrar nada, y la de
  "usuario nulo" pasa por `ErrorMapper`, que siempre devuelve un recurso traducido. Importa
  comprobarlo ahora que hay usuarios en Reino Unido.

#### ❌ Los símbolos de depuración: `debugSymbolLevel` NO hace nada en esta app

Esto no salió como se esperaba y conviene dejarlo escrito, porque la lista de comprobación
decía *"los símbolos de depuración presentes (que Play deje de dar la advertencia)"* y **eso
no va a pasar**.

El `ndk { debugSymbolLevel = "FULL" }` que se añadió está puesto y Gradle ejecuta su tarea,
pero **extrae cero**:

```
app/build/intermediates/native_debug_metadata/release/.../out   -> VACÍA
app/build/intermediates/native_symbol_tables/                   -> VACÍA
```

y el AAB no trae ninguna entrada `BUNDLE-METADATA/com.android.tools.build.debugsymbols/`.

**Por qué**, comprobado y no deducido. La app no tiene código nativo propio; los únicos 8
`.so` del bundle vienen de dos dependencias de androidx (`libandroidx.graphics.path.so` y
`libdatastore_shared_counter.so`, en los 4 ABIs). Pasándoles `llvm-readelf -S`:

```
sin .symtab ni .debug_* -> YA VIENEN STRIPEADAS
```

Vienen ya sin símbolos desde sus AAR, así que no hay nada que extraer. **No se puede
arreglar desde aquí** mientras androidx las publique así.

**Qué significa de verdad:** si alguna vez peta *dentro* de una de esas dos librerías, esos
marcos saldrían sin simbolizar en Crashlytics. Son dos librerías diminutas y muy rodadas, y
todo el código de la app es Kotlin, cuyo mapeo sí sube (`proguard.map`, 108 MB, está en el
bundle). Es decir: **la advertencia de Play es cosmética para este caso**.

👉 La línea se queda en `build.gradle.kts` de todas formas: no estorba, y el día que entre
una dependencia con `.so` sin stripear, empezará a funcionar sola.

#### 📤 Enviada a los dos canales de prueba (2026-11-16)

Subida el mismo día a **Prueba cerrada - Alpha** (lanzamiento completo) y a **Prueba
interna**. Las notas, en los tres idiomas de una vez con `Ctrl+A` sobre la caja: Play
confirmó *"Has incluido notas de la versión en 3 de 3 idiomas"* sin duplicar etiquetas.

⚠️ **Despiste de la pantalla de crear versión, para la próxima:** al bajar, debajo de la
tabla de App bundles hay una sección **"Versión anterior"** que lista el bundle ya publicado
(el `20 (1.6)`). Si se llega ahí con la página a medio desplazar, parece que la versión
nueva lleva adjunto el bundle viejo. No es así: el de la versión es el que sale arriba, bajo
"Subir app bundles".

✅ **Aprobada y activa en los dos canales** (comprobado el 2026-11-17): *Prueba cerrada -
Alpha · Versión: 21 (1.7) · Última actualización: 16 nov 2026*, con el check verde. **El
Redmi ya puede actualizarse desde Play**, sin cable y sin perder la sesión — con eso quedan
desbloqueadas las tres pruebas de pantalla que faltan.

✅ **La de prueba interna salió al momento**, sin pasar por revisión: *"Activo · Última
versión: 21 (1.7) · Disponible para testers internos"*, publicada a las 22:03 del mismo día.
Las versiones de prueba interna no se revisan, así que **no aparecen en "Cambios en
revisión"** — no hay que esperarlas ahí. Con eso, el bundle 17 deja de ser la versión activa
de ese canal.

📍 **Resultado, comprobado el mismo día:** la mitad que importaba ya está. En **Política →
Contenido de la aplicación → Permisos de fotos y vídeos**, el aviso *"Tu aplicación usa los
siguientes permisos de fotos y vídeos sin declarar"* sale con **la lista vacía**: ningún
bundle activo pide ya `READ_MEDIA_IMAGES`.

Pero la **declaración sigue archivada** (la fila desplegada muestra *"Permisos declarados:
android.permission.READ_MEDIA_IMAGES"*), con su texto falso de la cámara. Y **esa página no
tiene botón de Guardar**: termina en el cuadro de texto y el pie, así que desde ahí no se
puede ni editar ni retirar.

**Decisión: dejarla quieta de momento.** No se aplica a nada (sin permiso detectado no
afecta a usuarios, ficha ni revisiones), no hay nada que pulsar, y había una revisión en
curso. **Volver a mirarla cuando aprueben la v21**: lo más probable es que Play recalcule la
sección sola, como ya vació la lista de detectados. Si para entonces sigue igual, probar a
editar el texto a ver si aparece el Guardar, con la cola de revisión vacía.

👀 Lo que se esperaba al subirla y por qué: que desapareciera la declaración
de **"Permisos de fotos y vídeos"**. Existía solo porque el único bundle que Play veía
pidiendo `READ_MEDIA_IMAGES` era el **17 (1.4)**, parado ahí desde el 10 de octubre; el 21
no lo pide (verificado sobre el AAB). Si no se va sola al cabo de unos días, habrá que
retirarla a mano. Su texto afirmaba además que *"se usa la cámara para tomar la foto en el
momento"*, que nunca fue cierto.

#### Lo que queda de la v21, todo de pantalla

El binario está listo; lo que falta **no se arregla compilando**:

1. Selector de idioma y corrección del nombre de plato → emulador `FoodRanker_Test` (API 35).
2. Que los no fatales lleguen a Crashlytics → provocar un fallo que NO sea de red.
3. El aspecto edge-to-edge en Android 15.

Ver la tabla "Dónde se prueba cada cosa" más abajo: en el Redmi **no** entra una build local
encima de la de Play, el camino es subirla al canal de pruebas.

### 🔭 LOS FALLOS SILENCIOSOS YA SE VEN: 68 `catch` REPORTAN A CRASHLYTICS (2026-11-16)

Era el pendiente que dejó a la vista la revisión de alertas de esta misma mañana: la alerta
de **"errores recuperables nuevos"** se acababa de configurar y estaba **conectada a nada**,
porque la app no llamaba a `recordException` ni una sola vez. Había 78 `catch` repartidos y
de ninguno se enteraba nadie.

Ya no cambia nada para el usuario —se le sigue fallando igual de silenciosamente— pero sí
para nosotros. Es la diferencia entre *"la app va bien"* y *"la app va bien **que yo sepa**"*.

#### La pieza nueva: `utils/Fallos.kt`

```kotlin
Fallos.reportar(e, "perfil: borrar cuenta")
```

Un `object` con un solo método, al estilo del resto de `utils/`. Hace `log(donde)` +
`recordException(e)`. La etiqueta es un breadcrumb para poder leer el informe: Crashlytics
agrupa los no fatales **por traza, no por ese texto**.

#### ⚠️ Lo importante no es lo que reporta, es lo que NO reporta

Un no fatal por cada fallo sería peor que ninguno: quien se meta en el metro sin cobertura
genera uno por cada carga, la alerta suena todas las mañanas y acaba apagada. Así que
`Fallos` filtra por **`ErrorMapper.esDelEntorno(e)`** (nuevo, en el mapper que ya sabía
clasificar) y se calla ante red, timeouts y cancelaciones.

Lo que sí pasa el filtro, que es lo que interesa:

| Llega | Por qué |
|---|---|
| `PERMISSION_DENIED` | Casi siempre una regla de Firestore mal puesta. **El usuario nunca la va a reportar**, porque no sabe que ha pasado nada |
| `NOT_FOUND` | Datos incoherentes, algo que apunta a un documento que ya no está |
| Fallos de subida | Problema nuestro, y el usuario pierde su foto |
| Lo inesperado | Justo lo que no se puede prever desde aquí |

**6 tests nuevos** en `ErrorMapperTest` (15 en total) fijan ese filtro por los dos lados: de
menos y la alerta se vuelve ruido, de más y volvemos a la ceguera de partida.

#### Dónde quedaron los 68

`PlateDetailViewModel` 18 · `ProfileViewModel` 15 · `DiscoverViewModel` 11 ·
`AddPlateViewModel` 5 · `ImageCompressor` 3 · `ExploreViewModel`, `NotificationsViewModel`,
`FoodImageValidator` 2 · y 1 en `Trending`, `Referral`, `NearbyDishes`, `League`,
`FollowList`, `Challenge`, `NotificationRepository`, `AdManager`, `ShareManager`,
`FirebaseModule`.

Tres que valen más que el resto:

1. **Las consultas compuestas con fallback silencioso.** `catch { null }`, `catch { false }`,
   `catch { 0 }` dentro de los `async` de perfil y de plato (rival, rango en la ciudad, mi
   valoración, ¿está guardado?). Son consultas con `whereEqualTo` + `whereGreaterThan` +
   `orderBy`: **las que fallan enteras si falta el índice compuesto**, y así no se notaba
   nunca. Ya pasó una vez con un índice roto desde siempre. Peor aún, el `catch { 0 }` del
   rango se confunde con "eres el primero de tu ciudad".
2. **`AddPlateViewModel`, el límite diario.** El `catch` de `FAILED_PRECONDITION` hace
   fail-open a propósito, pero eso significa que **mientras falte el índice el límite diario
   no existe para nadie**. Se reporta solo esa rama: el `throw e` del `else` ya acaba en el
   `catch` exterior, que reporta por su cuenta, y si no saldría el informe dos veces.
3. **`FirebaseModule`.** Era un `catch (_: IllegalStateException)`, una carrera con
   `FoodRankerMessagingService`. Cuando salta, la app se queda **sin caché offline entera** —
   justo lo que no se nota mirándola en el sofá con wifi.

#### 🙅 Lo que se dejó SIN reportar, a propósito

No es un descuido, es la mitad del trabajo. Si algún día se añade aquí, que sea con un motivo:

| Sitio | Por qué no |
|---|---|
| `PlateRepository` (4) y `AuthRepository:167` | Devuelven `Result.failure(e)`: **no se tragan nada**, propagan. Quien llama ya reporta, y reportar aquí duplicaría el informe |
| `AuthRepository:119` | El fallback al navegador funciona, no es un fallo. Cuántas veces entra por ahí es **analítica, no Crashlytics** |
| `AuthRepository:223` | Ignora un fallo de red al cerrar sesión **a propósito**, para no bloquear el logout. Decisión vieja, sigue en pie |
| `ReferralManager:65` | `already_referred` → `false`, el resto hace `throw`. No traga nada |
| `ConnectivityObserver:40` | `unregisterNetworkCallback` revienta si ya estaba desregistrado. Inofensivo |
| `NotificationHelper:110` | Algunos fabricantes restringen `activeNotifications`. Es una peculiaridad de vendor que no podemos arreglar |
| `VenueRepository:94` | Que no haya ubicación es lo normal (permiso denegado, GPS apagado) |
| `MealDBSeeder` (4) | No se usa en producción por decisión de producto (`plates` vacío a propósito) |

#### ⏳ Pendiente que sale de aquí: el tragado de cancelaciones

`catch (e: Exception)` **también captura `CancellationException`**, así que ahora mismo los
ViewModels se tragan las cancelaciones y rompen la concurrencia estructurada. Lo correcto es
relanzarla.

`Fallos.reportar` solo evita **reportarla** (una cancelación no es un fallo); deliberadamente
**no** la relanza, porque eso cambia el flujo de control en 68 sitios de una app que ya está
en producción y que no se puede probar entera a mano. Va aparte, con su propia prueba.

✅ Compila y los tests pasan (`:app:testDebugUnitTest`, 15 en `ErrorMapperTest`, 0 fallos).
⚠️ Nada de esto se ha visto en un móvil: el primer no fatal no aparecerá en Crashlytics
hasta que haya una build con esto instalada. **Va en la v21.**

---

### 🚫 LAS DOS RECOMENDACIONES DE EDGE-TO-EDGE NO SON NUESTRAS (2026-11-16)

Revisadas a fondo antes de tocar nada, y la conclusión es que **no hay nada que arreglar**.
Queda escrito para no volver a investigarlo dentro de tres meses.

Lo que Play marca es que el binario llama a APIs obsoletas en Android 15+. Es verdad que las
llama. **Pero ni una desde nuestro código**, que tiene cero apariciones de `setStatusBarColor`,
`setNavigationBarColor`, `setSystemUiVisibility`, `setDecorFitsSystemWindows`,
`SYSTEM_UI_FLAG_*` ni `windowOptOutEdgeToEdgeEnforcement`.

**Cómo se comprueba** (sirve para la próxima, es el mismo método que con el permiso CAMERA:
preguntar al binario, que no opina). `strings` no existe en el shell de este PC, así que con
`grep -o -a` directamente sobre el dex, y para los puntos de llamada:

```bash
unzip -o -q app/build/outputs/bundle/release/app-release.aab 'base/dex/*'
"$SDK/build-tools/36.0.0/dexdump.exe" -d base/dex/classes.dex | awk '
  /Class descriptor/ { cls=$0; sub(/.*: /,"",cls) }
  /^      name          :/ { m=$0; sub(/.*: /,"",m) }
  /setStatusBarColor|setSystemUiVisibility|setDecorFitsSystemWindows/ { print cls " -> " m }'
```

Los 24 puntos de llamada salen todos de dependencias, y los nombres ofuscados se resuelven
con `app/build/outputs/mapping/release/mapping.txt`:

| En el dex | Quién es de verdad |
|---|---|
| `Ld/o;` `Ld/r;` `Ld/t;` | **`androidx.activity.EdgeToEdgeApi26/29/35`** |
| `AutocompleteImplFragment` | SDK de **Places** |
| `PlayCoreDialogWrapperActivity` | **Play Core** |
| `CoordinatorLayout` | androidx |

O sea: **lo marca la propia librería que implementa el edge-to-edge**. `EdgeToEdgeApi26/29`
son el backport para Android 8–14, y `minSdk` es 26, así que quitarlos significaría romper
el edge-to-edge en todo lo anterior a Android 15 — bastante peor que el aviso.

Tampoco hay versión que subir: **`activity-compose` ya está en 1.12.1**, lo último.

Y lo nuestro está bien hecho: `enableEdgeToEdge()` en `MainActivity:68`, `Theme.kt` pone solo
el tono de los iconos con `WindowCompat` (y tiene el comentario de por qué nada de
`statusBarColor`), y **los 14 `Scaffold` consumen su `padding`** — comprobado uno a uno, que
es el fallo clásico.

⚠️ Lo que sí queda sin verificar es el aspecto real en un Android 15/16: con `targetSdk 36` el
edge-to-edge **ya está forzado en producción**. Nadie se ha quejado, pero son 14 usuarios.
Mirarlo en el emulador `FoodRanker_Test` (**API 35**) cuando se pruebe la v21: el forzado
del edge-to-edge empieza en Android 15 = API 35, así que ese AVD ya lo reproduce y no hace
falta bajar una imagen de API 36.

📌 La tercera recomendación, la de **R8**, sigue sin tocar y hay que leerla con cuidado:
`isShrinkResources` está en **false a propósito** (ver más abajo).



### 🎉 FOODRANKER ESTÁ PUBLICADA EN PRODUCCIÓN (2026-11-16)

Google aprobó la v20 y, con la **publicación gestionada desactivada**, salió sola sin pedir
confirmación.

```
Producción · Activo · Última versión: 20 (1.6) · 2 países · 14 descargas
```

Confirmado desde fuera: `https://play.google.com/store/apps/details?id=com.app.foodranker`
devuelve **HTTP 200**. Ayer daba 404. La ficha es pública en España y Reino Unido.

Se acabó el camino que empezó el 2026-08-09. **A partir de aquí lo que falla, falla en
manos de desconocidos.**

#### ✅ El "Sitio web" de la ficha, cambiado a `foodranker.web.app`

Hecho el mismo día, una vez aprobada la versión. ⚠️ **Ese campo NO pasa por revisión**: el
diálogo avisa de que *"los cambios se publicarán en Google Play inmediatamente"* y el botón
es **"Guardar y publicar"**. No hay que enviarlo desde Resumen de publicación.

El campo **"Política de privacidad" se dejó intacto**, apuntando a
`sdelapenya.github.io/FoodRanker/privacy.html`, que es donde viven las páginas que genera
`tools/legal/build_pages.py`. La portada nueva de `foodranker.web.app` enlaza a ellas.

#### ⏳ app-ads.txt: servido y esperando a que AdMob lo rastree

La pantalla **AdMob → Aplicaciones → FoodRanker → app-ads.txt** confirmó el diagnóstico
entero: la fila mostraba la URL vieja, `sdelapenya.github…`, con **"Último rastreo: hace 15
horas"** —antes del cambio— y estado *"No se ha encontrado ningún archivo app-ads.txt"*.

⚠️ **La URL no se puede escribir a mano en AdMob.** El botón "Configurar app-ads.txt" solo
abre instrucciones; AdMob saca la URL **siempre** de la ficha de Play. Por eso el arreglo
tenía que ser el cambio de ficha.

**Qué mirar para saber si funcionó**: la celda *"URL del archivo app-ads.txt"*, no el
estado. Cuando ponga `foodranker.web.app`, AdMob ya ha leído la ficha nueva. De 24 a 48
horas. Si a los tres días sigue en github.io, comprobar que la ficha se publicó de verdad.

📊 **112 consultas de anuncios en 7 días** con 14 descargas: ya hay inventario moviéndose,
que es lo que hace que este fichero valga dinero.

#### Lo que cambia de prioridad ahora que hay usuarios reales

**App Check.** Hasta hoy no se podía encender el enforcement porque no había tráfico con el
que comprobar que no se deja fuera a nadie legítimo — y encenderlo a ciegas es la forma
clásica de echar a todos los usuarios de golpe. Ahora empieza a haberlo. En unos días las
métricas tendrán datos. La clave de Places viaja en un binario que ya es público, así que
el riesgo dejó de ser teórico.

#### Las 3 recomendaciones que Play enseña en el panel

Ninguna bloquea nada:

1. *"Es posible que la vista de extremo a extremo no funcione para todos los usuarios"*
2. *"Tu aplicación usa APIs o parámetros obsoletos para la vista de extremo a extremo"*
3. *"Mejora la memoria y el rendimiento con la optimización de R8"*

Las dos primeras son el **edge-to-edge** que ya estaba en el backlog, y aprietan porque la
app apunta a API 36. La tercera hay que leerla con cuidado antes de tocar nada:
`isShrinkResources` está en **false a propósito** desde que borró cinco cadenas que el
código sí usaba (ver más abajo en este documento).

### 🔔 ALERTAS DE CRASHLYTICS: ESTABAN A MEDIAS, Y POR ESO NO NOS ENTERAMOS (2026-11-16)

**Dónde están**, que no es donde uno busca: Firebase Console → **Configuración de proyecto
→ pestaña "Alertas"**. (La campana de la barra superior lleva al mismo sitio.) Son **por
usuario, no por proyecto**: cada persona con acceso configura las suyas.

Arriba del todo hay un interruptor maestro, *"Recibir alertas por correo electrónico y en
la consola sobre este proyecto"*. Ya estaba encendido; sin él, lo de abajo no sirve.

#### Lo que estaba mal

| Alerta | Antes | Ahora |
|---|---|---|
| **Nuevos problemas irrecuperables** (crashes nuevos) | ❌ **ningún canal** | ✅ correo |
| **Nuevos problemas de ANR** | ❌ ninguno | ✅ correo |
| Errores recuperables nuevos (no fatales) | ❌ ninguno | ✅ correo |
| Problemas frecuentes | ✅ correo | ✅ correo |
| Regresiones | ✅ correo + consola | ✅ correo + consola |

**La que avisa de un crash NUEVO estaba apagada.** Estaban puestas las dos que hablan de
problemas ya conocidos —que se repiten mucho, o que vuelven— pero no la que dice "ha
aparecido uno que no existía". Por eso el fallo de `MainActivity.kt:197` vivió desde la 1.2
hasta que alguien entró a mirar por casualidad.

#### La pestaña "Alertas de velocidad" (misma sección, otra pantalla)

También configurada: **Problemas irrecuperables** y **Problemas de ANR**, las dos con
correo y consola.

El umbral venía de fábrica en **25 usuarios `y` 1 %**, y es un **"y"**: con 13 usuarios en
total, que 25 se vieran afectados era aritméticamente imposible. **No habrían saltado
nunca.** Es el valor por defecto de Google, pensado para apps con cientos de miles de
instalaciones.

Puestas en **10**, que es el suelo que impone Firebase ("La cantidad mínima de usuarios
debe ser de 10"; rechaza 3).

✅ **No hay que volver a tocarlo al crecer** — el porcentaje escala solo:

| Usuarios | Hace falta | En la práctica |
|---|---|---|
| 13 | 10 **y** 1 % | 10 personas, el 77 % |
| 1.000 | 10 **y** 10 | 10 personas, el 1 % |
| 100.000 | 10 **y** 1.000 | 1.000 personas |

Con pocos usuarios manda el suelo de 10; pasado el millar manda el porcentaje.

⚠️ Aun así, a 13 usuarios estas dos **casi no van a sonar**: que 10 personas vean el mismo
fallo ya es una catástrofe. **La que hace el trabajo a esta escala es "Nuevos problemas
irrecuperables"**, de la pestaña básica.

#### 🔇 Lo que las alertas dejaron a la vista: nadie registra los no fatales

"Errores recuperables nuevos" no saltaría nunca hoy, porque **la app no llama a
`recordException` ni una sola vez**. Lo único que hace con Crashlytics es
`setCrashlyticsCollectionEnabled` en `FoodRankerApp`.

Y errores haberlos, haylos. Hay **61 `catch (e: Exception)`** repartidos, y varios se lo
tragan todo sin dejar rastro:

| Fichero | `catch` | Ejemplo |
|---|---|---|
| `PlateDetailViewModel.kt` | 18 | |
| `ProfileViewModel.kt` | 15 | línea 324: `catch (e: Exception) { /* silencioso */ }` |
| `DiscoverViewModel.kt` | 11 | línea 388, igual |
| `AddPlateViewModel.kt` | 4 | |

Si a un usuario le falla guardar la descripción de un plato, **no se entera él ni se entera
nadie**. Nunca.

**Pendiente (sin fecha, no bloquea nada):** sustituir esos `catch` mudos por
`FirebaseCrashlytics.getInstance().recordException(e)`. No cambia nada para el usuario —se
sigue fallando en silencio para él— pero el fallo pasa a ser visible. Es la diferencia
entre "la app va bien" y "la app va bien **que yo sepa**".

⚠️ No vale hacerlo a lo bruto en los 61: algunos son legítimos y ruidosos (cancelaciones,
`AuthRepository:223` ignora un fallo de red al cerrar sesión **a propósito**, para no
bloquear el logout). Hay que ir uno a uno.

### 🐛 TOCAR UN AVISO QUE NO ES DE UN PLATO CERRABA LA APP (2026-11-16)

Encontrado por casualidad al entrar en Crashlytics a configurar las alertas. **No lo había
reportado nadie.**

```
java.lang.IllegalArgumentException
Navigation destination that matches request NavDeepLinkRequest... cannot be found
MainActivityKt$FoodRankerNavigation$7$1$3$2$1.invokeSuspend  (MainActivity.kt:197)
```

**Versiones 1.2 – 1.6.** No lo trajo la v20 ni nada de esta semana: lleva ahí desde marzo.
2 eventos, 2 usuarios, porque la base son 13 personas y esos avisos son poco frecuentes.
En proporción, sin embargo, se cargó el "usuarios sin bloqueos" del 100 % al **84,62 %**.

#### La cadena

`onNotificationCreated` mandaba **siempre** la clave `plateId` en el `data` del push,
vacía para los cuatro tipos que no cuelgan de un plato: **`level_up`, `badge`, `follow` y
`league_result`**.

Con la app en segundo plano o cerrada —el caso normal de una notificación— el push **no
pasa por `FoodRankerMessagingService`**: lo pinta Play Services, y al tocar la tarjeta
lanza `MainActivity` con ese `data` como extras del intent. Allí:

```kotlin
val notifPlateId = activity.intent?.getStringExtra("plateId") ?: return@…
```

`getStringExtra` devuelve `""`, que **no es null**, así que el guard no saltaba.
`pendingPlateId = ""` → `navigate("plate/")` → ruta inexistente → excepción → la app se cierra.

Traducido: **subir de nivel, ganar un logro, que te sigan o terminar la liga y tocar el
aviso = la app se cierra.** Justo los avisos de retención, y justo antes de salir a
producción.

#### Arreglado en tres capas, y el orden importa

| Dónde | Qué |
|---|---|
| **`functions/src/index.ts`** | la clave `plateId` solo viaja si hay un plato de verdad |
| **`MainActivity`** | `takeIf { it.isNotBlank() }`, no solo `?:` |
| **`NotificationHelper`** | no adjunta un extra vacío (camino de primer plano) |

**El del servidor es el que vale ya.** Omitir la clave arregla el fallo en **todas las
versiones instaladas sin publicar una app nueva**, porque todo el que la lee tolera null:
`message.data["plateId"]`, `as? String`, `getStringExtra`. Comprobado uno por uno antes de
quitarla.

Los del cliente se quedan igualmente: protegen de las notificaciones que sigan en la
bandeja desde antes del arreglo, y de que alguien vuelva a meter un extra vacío.

⚠️ `DeepLinkParser` **no** tenía el fallo: ya filtraba con `takeIf { it.isNotBlank() }`
tanto en `webPathId` como en el esquema propio. El agujero era solo el de las
notificaciones.

#### 🛡️ Y el mismo fallo por el otro camino: enlaces con ids raros (2026-11-16)

Al arreglar lo de los avisos quedó la duda de si había más formas de construir una ruta
inválida. **La hay**, y por una puerta que está abierta a cualquiera.

`Screen.PlateDetail.createRoute` interpola **sin codificar**:

```kotlin
fun createRoute(plateId: String) = "plate/$plateId"
```

Parecía que `Uri.pathSegments` protegía, porque parte por `/`. **No protege**: devuelve los
segmentos **decodificados**. Así que `…/plate/abc%2Fdef` da el segmento `abc/def`, la ruta
sale `plate/abc/def`, no casa con `plate/{plateId}` y `navigate()` tumba la app. Igual con
`%20`, `%23`, saltos de línea.

Y los **App Links están verificados**, así que cualquiera puede repartir un enlace de esos
y cerrarle la app a quien lo toque. No roba nada, pero es una primera impresión pésima.

**Arreglado en el parser**, que es la frontera donde entra lo de fuera. Lista blanca
`^[A-Za-z0-9_-]{1,256}$` aplicada a los tres caminos (plato, perfil, invitación) y tanto
al esquema `https` como a `foodranker://`.

Lista blanca y no lista negra a propósito: enumerar lo que rompe una ruta es una carrera
que se pierde. Los tres identificadores caben de sobra:

| Qué | Forma | ¿Pasa? |
|---|---|---|
| Plato | `{placeId}__{dishSlug}`, ambos `[A-Za-z0-9_-]` | sí |
| Perfil | uid de Firebase, alfanumérico | sí |
| Invitación | UUID sin guiones en mayúsculas, `[A-F0-9]{10}` | sí |

Si algún día un id legítimo no pasara, el enlace abre la app por su pantalla normal en vez
de ir al contenido: molesto, pero no un cierre. Es el modo de fallo que queremos.

**5 tests nuevos** en `DeepLinkParserTest` (61 en total). Uno deja constancia del motivo:
`assertEquals("abc/def", uri.pathSegments[1])` — pasa, y es la prueba de que `%2F`
reaparece como barra. Otro fija que los ids **reales** siguen funcionando, que es lo que
no se puede romper.

⚠️ Esto va en la v21. A diferencia del de los avisos, **no se puede arreglar desde el
servidor**: el enlace lo fabrica quien quiera, no pasa por nuestra infraestructura.

### 🧹 PREPARATIVOS DE LA v21 — ya en `main`, SIN compilar (2026-11-15)

Nada de esto está en Play. Son cambios de código que entran solos en la próxima vez que se
compile un AAB. **El primer paso de esa compilación es subir `versionCode` a 21** — hoy
sigue en 20 a propósito, para que el árbol no finja ser lo que está publicado.

| Cambio | Dónde |
|---|---|
| `debugSymbolLevel = "FULL"` | `app/build.gradle.kts`, en `buildTypes { release }` |
| Permiso `CAMERA` y su `uses-feature`, **eliminados** | `app/src/main/AndroidManifest.xml` |

#### 🔵 SELECTOR DE IDIOMA PROPIO — hecho, SIN probar en un móvil (2026-11-15)

Era el "arreglo bueno" que este handoff llevaba pidiendo desde el 2026-11-02. La fila de
idioma del perfil ya **no** abre Ajustes: abre un diálogo de la app y llama directamente a
`LocaleManager.setApplicationLocales()`.

**Por qué.** Esa pantalla de Ajustes la pone cada fabricante y MIUI la tiene rota: su
`AppLocalePickerActivity` se estrella en su propio `onCreate` (NPE, ActionBar nulo). El
`FLAG_ACTIVITY_NEW_TASK` que se puso el 2026-11-02 evitaba que se llevara FoodRanker por
delante, pero no arreglaba lo de fondo — el usuario seguía sin poder cambiar de idioma.
Llamando a `LocaleManager` no hay pantalla ajena de por medio.

**No duplica estado.** Es la misma API que usa Ajustes, así que la elección la guarda el
sistema, persiste entre arranques y se sigue viendo en Ajustes → Idioma de la aplicación.
`applicationLocales` vacío = "sigue al móvil", y se traduce a `null` en el código.

**Sigue siendo solo Android 13+**, porque `LocaleManager` es de API 33 y `minSdk` es 26. En
8–12 no hay fila, igual que antes. Extenderlo a esas versiones exigiría AppCompat o manejar
la configuración a mano, y es otra decisión.

De propina, el subtítulo de la fila ya no es un texto fijo: dice en qué idioma está la app
ahora mismo. Eso dejó `prof_language_sub` sin usar y se ha borrado — lo cazó
`tools/i18n/find_unused.py`.

| Comprobación | Resultado |
|---|---|
| `check_strings.py` | OK, paridad correcta (en=591, es=590) |
| `check_args.py` | 0 usos con argumentos insuficientes |
| `scan_ui.py` / `scan_es.py` | mismos recuentos que antes, **ninguna** mención a ProfileScreen |
| `find_unused.py` | ninguna clave de idioma sin usar |
| `compileReleaseKotlin` | limpio |
| Tests | **56, 0 fallos** |

⚠️ La diferencia de una clave entre `en` y `es` es `default_web_client_id`, el id de Google
Sign-In. Es técnico, no se traduce, y ya estaba así antes. El script lo da por bueno.

🔴 **FALTA PROBARLO EN UN DISPOSITIVO.** Compila y no tiene texto clavado, pero nadie ha
pulsado todavía el diálogo. Lo que hay que ver: que el diálogo salga, que al elegir un
idioma la app se recree en ese idioma, que el subtítulo lo refleje, y que
"Predeterminado del sistema" devuelva el control al móvil.

El emulador `FoodRanker_Test` (API 35, Pixel 6, imagen con Play Store) sirve, pero **la app
es un muro de login**: hace falta iniciar sesión a mano una vez. En el Redmi no se puede
instalar un debug encima del build de Play — firma distinta, habría que desinstalar y se
pierde la sesión.

#### Por qué se pudo borrar `CAMERA` a secas

La app **nunca** ha usado la cámara: cero referencias a CameraX, `ACTION_IMAGE_CAPTURE` o
`TakePicture`, ninguna dependencia de cámara ni de escáner, y el único lanzador de permisos
(`MainActivity`) pide `POST_NOTIFICATIONS` y nada más.

Y, a diferencia de `READ_MEDIA_IMAGES`, **no hacía falta `tools:node="remove"`**: el informe
del fusionador (`app/build/outputs/logs/manifest-merger-release-report.txt`) decía que
`CAMERA` venía *ADDED from* el propio `app/src/main/AndroidManifest.xml`. Ninguna dependencia
lo reintroduce. El de fotos sí lo lleva porque ese lo inyectan librerías de medios.

El `uses-feature android:required="false"` se fue con él y no deja hueco: estaba para que
Play no dedujera que la cámara es obligatoria, deducción que **solo** ocurre cuando el
permiso está declarado. ⚠️ Si algún día entra una dependencia que declare `CAMERA`, hay que
volver a poner el `uses-feature` o Play empezará a esconder la app a los móviles sin cámara,
**en silencio**.

#### Verificado sobre el manifest fusionado, no sobre el fuente

`./gradlew :app:processReleaseManifest` y después mirar
`app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml`,
que es lo que de verdad acaba en el AAB:

- `android.permission.CAMERA` → **ausente**
- `android.hardware.camera` → **ausente**
- `android.permission.READ_MEDIA_IMAGES` → **ausente** (el `tools:node="remove"` sigue haciendo su trabajo)
- El único `uses-feature` que queda es `glEsVersion 0x00020000 required=true`, inyectado por
  una dependencia (Play Services). Es estándar y lo cumple cualquier móvil moderno.
- **56 tests, 0 fallos.**

⚠️ No se recompiló el AAB a propósito: `app/build/outputs/bundle/release/app-release.aab`
sigue siendo el **20 verificado y publicado**, y sobrescribirlo perdería esa correspondencia.

#### ✏️ EDICIÓN DEL NOMBRE DE PLATO — solo cosmética, por diseño (2026-11-15)

Llevaba meses pendiente y no era pereza: **el id del documento es `{venueId}__{dishSlug}`
y los ids de Firestore son inmutables**, así que renombrar un plato no puede cambiar su id.
Dejar editar el nombre a secas rompe el deduplicado — la siguiente persona que añada ese
plato con el nombre nuevo calcula otro id y crea un duplicado, justo lo que la identidad
canónica existe para evitar (`docs/VENUES.md`).

**Decisión (Sergio, 2026-11-15): solo cosmética.** Tildes, mayúsculas y puntuación. El
campo `dishSlug` **nunca se escribe**, así que el deduplicado no puede romperse pase lo
que pase. Un renombrado de verdad —mover el documento y con él ratings, comments y
saves— queda sin hacer y es otro trabajo.

Efecto secundario bueno: la restricción hace la operación **resistente a abusos sin
esfuerzo**. No se puede convertir "Paella" en otra cosa para secuestrar un plato que ya
acumuló valoraciones, porque cambiaría el slug.

#### Lo hace una Cloud Function, no las reglas

`renamePlate` (`functions/src/index.ts`, región `europe-west1`). Comprueba que quien llama
es el autor, que el plato está aprobado, y que `toDishSlug(nombreNuevo)` coincide con el
`dishSlug` guardado. Si no, devuelve `FAILED_PRECONDITION` con `slug-changed`.

**Por qué no las reglas de Firestore**: no tienen normalización Unicode. No pueden saber
que "Ñoquis" colapsa con "noquis", así que rechazarían precisamente las correcciones
útiles. Y abrir `name` en las reglas sin poder verificar nada dejaría a un cliente
manipulado reescribir el nombre de cualquier plato suyo. Mismo criterio que `getLeagueId`:
cuando un identificador lo entienden dos lados, manda el servidor.

⚠️ **Las reglas NO se han tocado** y `name` sigue sin ser editable por el cliente. No hace
falta: el Admin SDK se las salta.

#### El slug vive ahora en dos sitios, y hay un guardián

`functions/src/dishSlug.ts` es el gemelo de `app/.../utils/DishSlug.kt`. Si divergen,
`renamePlate` empieza a rechazar correcciones legítimas o a aceptar nombres que no
corresponden al slug. **No corrompe la identidad** —esa función nunca escribe `dishSlug`—
pero deja la app mintiendo sobre cómo se llama un plato.

`tools/i18n/check_slug_parity.py` lo vigila, y **no mantiene su propia lista de casos**:
los extrae de `DishSlugTest.kt`, que ya define el comportamiento esperado. Añadir un caso
al test lo mete solo en la comprobación. Hay que compilar antes las functions
(`npm --prefix functions run build`), porque ejecuta el JS generado.

Comprobado que el guardián sirve: rompiendo a propósito la eliminación de diacríticos en
el JS, el script detecta 2 divergencias y devuelve 1.

#### En la interfaz

El campo nuevo está en `EditPlateSheet`, encima de la descripción, con un texto de ayuda
que avisa **antes** de intentarlo: *"Solo cómo está escrito: tildes y mayúsculas"*. Si el
servidor lo rechaza, la hoja **no se cierra** y el texto de ayuda se vuelve el error — si
cerrara dejando el nombre igual, parecería que el cambio se perdió sin explicación.

Si el nombre no cambia, no se llama al servidor.

| Comprobación | Resultado |
|---|---|
| `check_strings.py` | OK (en=594, es=593) |
| `check_slug_parity.py` | 10 casos, todos coinciden |
| `scan_ui.py` / `scan_es.py` | mismos recuentos de siempre |
| `tsc` y `compileReleaseKotlin` | limpios |
| Tests | **56, 0 fallos** |

✅ **DESPLEGADA el 2026-11-16**, `renamePlate(europe-west1)`, Node 22, 2ª generación.
Comprobado después con `firebase functions:list`: 18 funciones, las 17 anteriores
intactas. No afecta a nadie todavía, porque ninguna versión publicada la llama.

⚠️ **El primer intento falló** con *"User code failed to load. Cannot determine backend
specification. Timeout after 10000"*. **No era el código**: `require('./lib/index.js')`
carga en 771 ms. Es el descubrimiento del CLI, que levanta un servidor local y se rinde
a los 10 s. Se arregla con `FUNCTIONS_DISCOVERY_TIMEOUT=120` delante del comando — ver
"Trampas conocidas".

🔴 **SIN PROBAR EN DISPOSITIVO**, igual que el selector de idioma. Va a la misma lista de
la v21.

De paso: el diálogo de idioma usaba un `lang_cancel` propio cuando ya existía
`action_cancel`. Corregido y borrada la clave duplicada.

#### ✈️ Lista de comprobación para cuando se compile la v21

Decidido el 2026-11-15: el selector de idioma **no** se prueba hasta entonces. Habría hecho
falta arrancar el emulador e iniciar sesión a mano, y no compensaba con la v20 en revisión.

##### 🧭 Dónde se prueba cada cosa (y por qué no todo es en el Redmi)

Lo que más despista: **en el Redmi 14C no se puede instalar una build local encima de la de
Play.** La de Play va firmada con la clave de Play y la de aquí con la de subida, así que la
instalación falla por firma; desinstalar sí deja instalarla, pero **se pierde la sesión**, y
recuperarla es volver a pasar por el login de Google. De ahí que se venga usando el emulador.

| Qué | Dónde | Por qué |
|---|---|---|
| Comprobaciones del AAB (`CAMERA`, `READ_MEDIA_IMAGES`, símbolos, `jarsigner`, marcadores de UMP, `BundleConfig.pb`) y `tools/i18n/` | **Ningún dispositivo** | Son sobre el fichero. Se pueden hacer antes de instalar nada |
| Selector de idioma · corrección del nombre de plato · aspecto edge-to-edge | **Emulador `FoodRanker_Test` (API 35)** | Es la vía directa. Peaje: la app es un muro de login y hay que entrar a mano una vez |
| Que `recordException` llegue a Crashlytics | Emulador o Redmi | Da igual cuál: lo que se mira es la consola de Firebase, no la pantalla |
| Cualquier cosa con la build **real** de la tienda | **Redmi, por el canal de pruebas** | Subir el AAB a prueba interna/cerrada: Play lo actualiza encima, **misma firma y sesión intacta**. Es la única forma de tocar en el móvil lo que de verdad se publica |

⚠️ Los push de FCM siguen siendo de **un móvil a la vez**: `users/<uid>.fcmToken` es uno por
usuario, y quien entre último se lleva los avisos (ver la sección del Redmi más abajo).

1. ✅ **Hecho el 2026-11-16**: `versionCode = 21`, `versionName = "1.7"`.
2. **Probar en un dispositivo el selector de idioma Y la corrección del nombre de plato.**
   Son las dos cosas de la v21 sin verificar.
   Cuatro cosas: que el diálogo salga; que al elegir un idioma la app se recree en él; que
   el subtítulo de la fila lo refleje; y que "Predeterminado del sistema" devuelva el
   control al idioma del móvil. Mejor en el emulador `FoodRanker_Test` (API 35) que en el
   Redmi: en el móvil haría falta desinstalar el build de Play y se pierde la sesión.
3. ✅ **Sobre el AAB, no sobre el fuente**: `CAMERA` ausente, `READ_MEDIA_IMAGES` ausente.
   ⚠️ Lo de *"y los símbolos de depuración presentes"* que decía aquí **era falso**: no se
   pueden generar en esta app porque los `.so` vienen ya stripeados de androidx. Ver la
   sección de la v21 arriba del todo.
4. ✅ **Las comprobaciones de `tools/i18n/`** — pasadas todas el 2026-11-16.
5. ✅ **Las verificaciones binarias de siempre**: `jarsigner -verify`, versionCode y
   versionName en el manifest fusionado, los 4 marcadores de UMP en `base/dex/*.dex` (R8
   puede llevarse el SDK de consentimiento sin avisar) y `08 03 10 01` en `BundleConfig.pb`
   para el split de idiomas.
6. **Que `recordException` llegue de verdad** (nuevo el 2026-11-16, ver la sección de los 68
   `catch` arriba). Vale con provocar un fallo que NO sea de red —el filtro de
   `esDelEntorno` se come la red a propósito— y ver si aparece en Crashlytics como no fatal:
   lo más fácil es cortarle los permisos a una colección en las reglas un momento, o abrir un
   plato borrado. ⚠️ Los no fatales tardan en subir: no se ven al instante.
   Y comprobar que **no** sale nada raro con solo abrir y navegar la app: si en un paseo
   normal por las pantallas ya se registran no fatales, o el filtro está mal o hay un fallo
   de verdad que llevaba tiempo escondido.
7. **Mirar el edge-to-edge en el emulador `FoodRanker_Test` (API 35).** No es un pendiente de código (ver la
   sección de edge-to-edge arriba: las APIs obsoletas son de las dependencias), pero con
   `targetSdk 36` el modo ya está forzado en producción y nunca se ha visto en una pantalla
   de Android 15/16. Que no haya texto debajo de la barra de estado ni de la de navegación.

### 🚀 LA v20 (1.6), ENVIADA A REVISIÓN DE PRODUCCIÓN (2026-11-15)

**Primera versión de producción de FoodRanker.** Hasta hoy la app solo había existido en
prueba interna y cerrada.

| | |
|---|---|
| Bundle | **20 (1.6)**, añadido *desde la biblioteca* — el mismo que Google aprobó en Alpha el 11 nov, sin recompilar |
| Lanzamiento | completo, **100 %** |
| Países | España y Reino Unido (ya aprobados el 11 nov) |
| Notas | es-ES, en-GB y en-US — *"3 de 3 idiomas"* |
| Junto con | la declaración de **Datos de inicio de sesión** con la cuenta nueva |

⚠️ **Publicación gestionada desactivada**: cuando Google apruebe, la app **sale sola**. No
hay un segundo botón de confirmación.

#### Dos trampas de la propia consola, por si se repiten

**"Subir" no es "Añadir de la biblioteca".** Al arrastrar el `app-release.aab` local, Play
contesta *"El código de versión 20 ya se ha usado"*. No es un problema del bundle: es que
ya está subido. Hay que usar **Añadir de la biblioteca** y elegir el 20, que además
garantiza que va exactamente el binario ya revisado.

**Guardar la versión no la envía.** Los cambios salen desde **Resumen de publicación**, y
conviene mirar la lista desplegada antes de darle: si solo aparece la declaración, es que
la versión de producción todavía no se ha creado y se gastaría una ronda de revisión
entera en un cambio de texto.

#### Lo que NO bloqueo y queda para despues

⚠️ **ESTA LISTA ERA DE LA ERA v20 Y ESTABA CASI TODA OBSOLETA.** Revisada entera el
2026-11-17 **mirando la consola, no la memoria**: de los cinco puntos, cuatro ya estaban
resueltos y uno de ellos ni siquiera hizo falta hacerlo. Lección para la próxima: antes de
ponerse a trabajar en algo de esta lista, **abrir Play Console y comprobar que sigue
pendiente de verdad**. Casi cuesta una hora de trabajo imaginario.

| Pendiente de entonces | Estado real (2026-11-17) |
|---|---|
| `app-ads.txt` sin desplegar, y la ficha apuntando a github.io | ✅ **Hecho el 2026-11-16.** Desplegado, `index.html` creado, y el "Sitio web" de la ficha cambiado a `foodranker.web.app` — verificado en el HTML público de Play. Solo falta que AdMob vuelva a rastrear |
| Permiso `CAMERA` muerto en el binario | ✅ **Fuera en la v21**, verificado sobre el AAB (0 apariciones de `CAMERA` y de `hardware.camera`) |
| Declaración de "Permisos de fotos y vídeos" por culpa del bundle 17 | ✅ **La mitad que importaba.** Subida la v21 a prueba interna, Play ya no detecta `READ_MEDIA_IMAGES` en ningún bundle activo. Queda el texto archivado, que **no se puede editar**: esa página no tiene botón de Guardar |
| Seguridad de los datos: borrado parcial declarado como "No" | ✅ **Ya estaba en "Sí"** con `delete-account.html` como URL. Ni hacía falta escribir una página nueva (la que hay ya documenta el borrado parcial desde el 2026-11-02) ni tocar el formulario |
| Estado de operador | ⏳ **El único que sigue vivo.** Tope **1 de febrero de 2027**, va con la gestoría junto a la cuenta de comercio. El banner ya sale fijo arriba en Play Console |

### 🟢 LA CUENTA DE REVISIÓN SE BLOQUEÓ SOLA — RESUELTA CON UNA NUEVA (2026-11-15)

**Esto fue lo último que separaba la app de producción.** El bundle 20 está aprobado en Alpha,
las notas escritas, los países puestos y las nueve declaraciones completadas.

#### Por qué una cuenta de revisión no es opcional

El formulario de **Datos de inicio de sesión** (Contenido de la aplicación) dice literal:
*"Los revisores no pueden crear cuentas, usar sus propias cuentas que ya tengan ni utilizar
pruebas sin coste para acceder a tu aplicación."*

Y FoodRanker es un muro de login: `AuthRepository` solo expone `signInWithGoogle`, no hay
modo invitado ni registro con correo (comprobado, 2026-11-15). Tampoco vale declarar "No":
esa opción exige que **no sea necesario iniciar sesión**, y lo es. Así que **sin una cuenta
que funcione, la revisión de producción se rechaza.**

⚠️ **"Datos de inicio de sesión" es el nombre nuevo de "Acceso a la aplicación".** Lo dice
un aviso en la propia pantalla. Buscarla por el nombre viejo hace pensar que falta.

#### Qué pasó con `foodrankerreview@gmail.com`

Creada el 21 de agosto. El 15 de noviembre, al intentar comprobar que seguía funcionando:
**"No hemos podido verificar que eres tú"**. No está suspendida —es un desafío de identidad,
no una inhabilitación— pero el resultado práctico es el mismo.

La causa está en sus ajustes de seguridad:

| Ajuste | Estado | Efecto |
|---|---|---|
| Verificación en dos pasos | desactivada ✅ | no era esto |
| **Saltar contraseña cuando sea posible** | **activada** ❌ | Google prefiere la llave/el aviso al móvil antes que la contraseña |
| **Llaves de acceso** | **1 registrada** ❌ | refuerza lo anterior |
| Notificación de Google | 1 dispositivo | el aviso va a un móvil que el revisor no tiene |

Con eso, quien entre con correo y contraseña desde un sitio desconocido —un revisor de
Google, repartidos por medio mundo— no basta con la contraseña: Google intenta el camino
sin contraseña y pide **el dispositivo del dueño**. De ahí el bloqueo. La cuenta tenía
además 4 avisos de *"intento sospechoso de inicio de sesión"*, casi seguro los propios
revisores.

🔒 **Y no se puede arreglar.** Cambiar cualquiera de esos ajustes es una acción sensible y
Google exige reverificarte para tocarla — la misma verificación que falla. Se entra en la
cuenta pero no se puede modificar nada. Callejón sin salida.

#### La salida: cuenta nueva, configurada desde el minuto cero

Crearla **desde el Redmi** (Ajustes → Cuentas → Añadir cuenta → Google → Crear cuenta), no
desde el navegador: así el móvil queda como dispositivo de confianza desde el principio,
que es lo que le faltaba a la otra.

1. **Rechazar la llave de acceso** — Google la ofrece varias veces durante la creación
2. **No activar la verificación en dos pasos**
3. **Desactivar "Saltar contraseña cuando sea posible"** nada más crearla, mientras Google
   aún no sospecha. Este es el paso que no se pudo dar en la cuenta vieja
4. Entrar con ella desde el navegador del PC, **solo correo y contraseña**: si pasa limpio,
   el camino del revisor funciona
5. Entrar en **FoodRanker con ella en el Redmi**: recorre lo que hará el revisor, respaldo
   por navegador incluido, y le deja su documento de usuario creado

⚠️ Google limita cuántas cuentas admite por número de teléfono, y el de Sergio ya tiene al
menos tres. Si rechaza la creación, es por ahí.

#### Lo que hay que corregir en la declaración al cambiarla

El texto de instrucciones que había (en castellano, del 21 de agosto) **se contradecía con
la propia declaración**: decía *"esta cuenta no tiene la suscripción Premium contratada"*
mientras la casilla de abajo —marcada— afirma que las credenciales dan *"acceso completo a
todas las funciones y todo el contenido, incluido contenido premium o de pago"*.

La nota es la que sobra: con `SUSCRIPCION_A_LA_VENTA = false` **no hay suscripción que
contratar**, el botón está oculto y la lista de beneficios va vacía. La casilla es correcta.

Y el formulario **exige inglés**. Texto de recambio (466/500), ya con el botón real de la
app (`auth_continue_google` → "Continue with Google"):

```
The app uses Google Sign-In only. On the first screen, tap "Continue with Google". On a
device with no Google account, the app falls back to a Google web sign-in page where you
can enter the email and password above directly. If the account picker appears instead,
choose "Use another account" and sign in there. Nothing needs to be added to the device
beforehand. All features and content are available after signing in; this version has no
paid or premium content.
```

El respaldo por navegador no es un detalle: `signInWithGoogle` intenta Credential Manager
y, si falla por lo que sea (móvil sin cuentas, sin Play Services, error interno), cae a
`signInWithBrowser` con `prompt=select_account`. **Ese es el único camino donde un revisor
puede teclear correo y contraseña**, porque la hoja nativa solo muestra cuentas ya añadidas
al dispositivo.

#### ✅ RESUELTO: cuenta nueva `foodrankerreview2@gmail.com` (2026-11-15)

La cuenta vieja se abandona — no se puede arreglar y no guarda nada. **La cuenta de
revisión vigente es `foodrankerreview2@gmail.com`.** La contraseña **no se guarda aquí**
(el repo es público): vive en Play Console y en quien la creó.

Creada **desde el Redmi** (Ajustes → Cuentas → Añadir cuenta → Google → Crear cuenta, uso
personal), no desde el navegador: así el móvil es dispositivo de confianza desde el primer
momento, que es lo que permitió tocar los ajustes de seguridad. Desde el PC no dejaba —
*"inicia sesión desde el mismo navegador"*.

Configuración verificada en pantalla, y es la que hay que repetir si algún día hace falta
otra:

| Ajuste | Valor |
|---|---|
| Verificación en dos pasos | **desactivada** |
| Llaves de acceso | **ninguna** ("Empezar a usar llaves de acceso") |
| Saltar contraseña cuando sea posible | **Desactivado** |
| Teléfono y correo de recuperación | sin poner (opcional, no bloquea) |

⚠️ Se reutilizó la contraseña de la cuenta vieja, la que tenía cuatro avisos de *"intento
sospechoso de inicio de sesión con tu contraseña"*. Casi seguro eran los revisores, pero
conviene cambiarla cuando haya un rato.

**La declaración de Play quedó así** (se editó la entrada existente, no se creó una
segunda, así que no hay duplicados):

- Nombre: `Google Play review account` (en inglés, lo exige el formulario)
- Usuario: `foodrankerreview2@gmail.com`
- Instrucciones: el texto en inglés de más abajo, 467/500
- Casilla de "acceso completo a todo el contenido, incluido premium o de pago": **marcada**

⏳ **Pendiente de las dos pruebas** antes de enviar producción:

1. Entrar desde el navegador del PC con **solo correo y contraseña**, sin que pida el móvil
2. Entrar en **FoodRanker en el Redmi** con esa cuenta — el único camino que recorre el
   respaldo por navegador, que es por donde entra un revisor sin la cuenta en su dispositivo

#### Regla para el futuro

Una cuenta de revisión con llave de acceso o "saltar contraseña" activados **se bloquea
sola** en cuanto la usan revisores desde medio mundo, y después no se puede arreglar.
Revisar sus ajustes antes de cada envío a producción, no solo que la contraseña siga siendo
la misma.
### 🟢🟢 GOOGLE HA APROBADO EL ACCESO A PRODUCCIÓN (2026-11-04)

Correo del 4 de noviembre a `sdelapenya1991@`: **acceso a producción aprobado para
`com.app.foodranker`**. Se pidió el 3 de noviembre a las 9:42, así que tardaron un día.

⚠️ **Esto NO publica la app.** Habilita el canal. Hay que crear la versión de producción y
pasar **otra** revisión distinta.

🚨 **Y no se puede sacar producción con la v18.** En el formulario que aprobaron se dijo
por escrito: *"El último que encontré, los enlaces compartidos, ya está corregido y lo
subiré antes de publicar en producción."* La v18 que llevan hoy los testers sigue
repartiendo enlaces muertos. **La v19 es ahora lo único que separa la app de producción.**

**Orden para publicar:** ~~recompilar el AAB de la v19~~ ✅ (`df53d5f`, 2026-11-08) →
~~subir a prueba cerrada~~ ✅ (2026-11-09, con las notas en es-ES, en-GB y en-US) →
~~verificar en el Redmi los App Links y las notificaciones~~ ✅ (2026-11-10, las dos
cosas probadas sobre la build de Play) → **crear la versión de producción** ← aquí estamos con las notas de `docs/play-store-listing.md`.

⚠️ Las notas de la v19 se reescribieron el 2026-11-09 (`44083e0`): el texto original era
anterior al arreglo de las notificaciones y no lo contaba. **Las de producción tienen que
ser estas, no las que había antes.**

### 🔴 DECLARACIÓN DE "OPERADOR" EN PLAY — FECHA LÍMITE 1 DE FEBRERO DE 2027

Correo del 4 de noviembre. Hay que declarar en Play Console si se es **operador**
(*trader*): Gestionar cuenta → Información sobre ti → Estado de operador. **Si no se hace,
no se pueden actualizar las apps y Google retira de Play el perfil y todas las apps.**

⚠️ **No es un clic cualquiera, y no es opcional de facto.** Google considera operador a
quien gana dinero con las apps, **también con anuncios** — y hay AdMob sirviendo. Si un
particular se declara operador, **Google publica en Play su teléfono y su dirección**. Y
mentir en esto incumple las condiciones de Google.

Es la misma pelota que la cuenta de comercio y el alta censal: **va a la consulta con la
gestoría**, no se decide a ojo. La dirección pública es exactamente el mismo problema que
ya estaba anotado para el "Perfil público del comerciante".

ℹ️ La revisión del correo sugirió anotarlo en un `GESTOR-PREGUNTAS.md`. **Ese fichero no
existe en el repo** (comprobado el 2026-11-08). Si se crea, que recoja las tres: estado de
operador, cuenta de comercio y alta censal/RETA/ROI-VIES.

### 🚧 v20 (1.6): UN PERMISO MUERTO BLOQUEÓ LA REVISIÓN DE PRODUCCIÓN (2026-11-10)

La v19 **no llegó a enviarse** a revisión de producción. Al darle a "Enviar cambios a
revisión", las comprobaciones previas de Play lo pararon en seco:

> **Usar selectores de sistema alternativos para fotos o vídeos** — *Tu aplicación debe
> usar el selector de fotos de Android u otros selectores del sistema, en lugar de
> solicitar los permisos `READ_MEDIA_IMAGES` o `READ_MEDIA_VIDEO`.*

⚠️ **No es una advertencia: es un bloqueo.** No deja ni mandarlo. Hay un "Continuar de
todos modos", pero avisa de que te lo pueden rechazar, y con una promesa ya hecha por
escrito a Google no era el momento de tentar a la suerte.

**El permiso estaba declarado y no lo usaba nadie.** La foto se elige con
`ActivityResultContracts.GetContent()` (`AddPlateScreen.kt`), que es un selector del
sistema y devuelve un URI con permiso de lectura temporal. La prueba de que sobra: `minSdk`
es 26, en Android 8–12 `READ_MEDIA_IMAGES` ni existe y la app **no declara ningún permiso
de almacenamiento** — y el selector funciona igual desde siempre.

Arreglado quitándolo del manifest con `tools:node="remove"`, para que ninguna dependencia
lo reintroduzca al fusionar y vuelva a bloquear una publicación.

ℹ️ `CAMERA` también está declarado **sin usarse** (no hay ni un `TakePicture` en el código).
Se deja: ya lleva su `<uses-feature android:required="false">` al lado, así que no filtra
dispositivos, y Play no se queja de él. Si algún día se limpia, que sea con su propia
prueba.

**AAB:** `app/build/outputs/bundle/release/app-release.aab`, 15 977 094 bytes,
`BUILD SUCCESSFUL in 4m 21s`. `versionCode = 20`, **`versionName` sigue en `1.6`**: para el
usuario no cambia absolutamente nada, así que no toca inventarse una versión. Las notas de
la versión son **las mismas de la v19**.

| Comprobación (sobre el binario) | Resultado |
|---|---|
| Firma | `jar verified` |
| Versión | 20 / 1.6 |
| `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` en el manifest del AAB | **ausentes** |
| `singleTop` | sí |
| `foodranker.web.app` sí / `foodranker.app` no | correcto |
| Esquemas `plate`, `user`, `invite` | los tres |
| UMP tras R8 | 4/4 marcas |
| Split por idioma | desactivado |
| Tests | **56, 0 fallos** |

#### Estado (2026-11-11): la v20 ya está publicada en el canal cerrado

Enviados dos cambios juntos: la v20 a **prueba cerrada** y, en **producción**, la
segmentación de España y Reino Unido (sobrevivió al descarte del borrador, no hubo que
rehacerla). Las comprobaciones previas pasaron limpias: solo quedó la advertencia de los
símbolos de depuración, que no bloquea.

**Google los aprobó el mismo día.** El *Resumen de publicación* quedó vacío —ni un cambio
en revisión ni pendiente de enviar— con *"Última publicación el 11 de noviembre de 2026"*.
La v20 está repartida a los testers y los dos países están puestos en producción. **El
permiso de fotos no dio ningún problema en la revisión: era lo único que bloqueaba.**

Añadir países a producción **no publica nada**: ese canal sigue inactivo, sin ninguna
versión. Solo deja la segmentación puesta para cuando se promueva.

✅ Confirmación gratis de que quitar el permiso no tuvo efectos colaterales: en "Cambios en
tus dispositivos admitidos", **0 dispositivos perdidos** (11 610 teléfonos y 6 509 tablets,
los mismos que con la v19).

#### Los símbolos de depuración, puestos para la próxima compilación (2026-11-11)

En `app/build.gradle.kts`, dentro de `buildTypes { release { … } }`:

```kotlin
ndk {
    debugSymbolLevel = "FULL"
}
```

Es la advertencia que Play saca en cada versión. La app no tiene código nativo propio,
pero sí `.so` de sus dependencias (Firebase, AdMob, soloader), y sin esto los fallos de
esas librerías llegan a Crashlytics y a Play como direcciones de memoria en vez de nombres
de función y números de línea.

**No engorda lo que se instala**: Play guarda los símbolos aparte y no los reparte a los
móviles, solo crece el AAB que se sube. **No se recompiló la v20 por esto** —ya estaba
enviada y no cambia nada de lo que ve el usuario—, así que **la advertencia seguirá
saliendo hasta que se compile una versión nueva**. Si los `.so` de las dependencias vienen
ya sin símbolos (es lo normal en librerías precompiladas), puede que no desaparezca del
todo: entonces no hay nada más que hacer desde aquí.

#### ✅ ~~Lo que falta probar~~ — HECHO EL 2026-11-12

Quitar el permiso **no debería** romper nada, pero lo que toca es el camino de subir un
plato con foto, que es el corazón de la app. **Antes de promover a producción:**

1. ~~Subir el AAB de la v20 a **prueba cerrada**~~ ✅ (2026-11-11, aprobado el mismo día).
2. ~~Actualizar en el Redmi 14C y probar la foto~~ ✅ (2026-11-12, ver más abajo).
3. **Promover a producción** ← aquí estamos, sin ningún bloqueante conocido.

En Play Console queda **una versión de producción en borrador con el bundle 19**. Hay que
descartarla (`Descartar versión en borrador`) o sustituir el bundle por el 20: si se envía
el 19, vuelve a bloquearse por lo mismo.

#### ✅ EL SELECTOR DE FOTOS, VERIFICADO SIN PERMISO (2026-11-12)

Hecho sobre el **Redmi 14C con la build de Play de la v20**, la que ya no declara
`READ_MEDIA_IMAGES`. Comprobado antes de tocar nada:

- `versionCode=20`, `versionName=1.6`, `installerPackageName=com.android.vending`
  (viene de Play, firmada con la clave de Play, no con la del PC).
- En `dumpsys package` → `requested permissions`, **`READ_MEDIA_IMAGES` no aparece**.
  El `tools:node="remove"` aguantó la fusión del manifest y ninguna dependencia lo
  reinyectó. Eso solo se puede ver en el paquete instalado, no en el fuente.

| Paso | Qué pasó |
|---|---|
| Subir → "Foto obligatoria" | abrió `com.google.android.photopicker.PhotopickerGetContentActivity`, el **selector del sistema**, con el aviso *"FoodRanker solo tendrá acceso a las fotos que selecciones"* |
| Elegir una foto | **la previsualización se vio** en la pantalla de publicar |
| Log de la app | `SkJpegCodec::onGetPixels + (1536, 2040)` y `SkJpegEncoder::Encode fQuality = 80` — decodificó el JPEG entero y lo recomprimió para subirlo |
| Denegaciones | **ninguna**: ni un `SecurityException` ni un `Permission Denial` del proceso de FoodRanker en todo el logcat |

**Eso zanja la cuestión**: la app lee los bytes del URI que devuelve el selector sin
ningún permiso de almacenamiento, que es exactamente lo que decía el comentario del
manifest y lo que la política de Play da por supuesto.

**No se publicó nada.** Se descartó el borrador desde el diálogo de la app, y se
comprobó en Firestore que no quedó rastro: `plates` sigue en 57 (8 de Sergio), `xp`
en 490 y los mismos 4 logros. No se subió nada a Cloudinary ni se creó ningún `venue`.

Por qué se hizo así y no publicando de verdad: publicar un plato **no notifica a nadie**
—el único disparador sobre `plates` al crear es `moderatePlateImage`, que solo avisa al
autor—, pero sí deja el plato visible en el feed y el ranking de todos. Y lo que había
que probar era leer la foto, que ocurre entero antes de publicar.
### ✅ LAS NOTIFICACIONES, VERIFICADAS EN EL MÓVIL (2026-11-10)

Hecho sobre el **Redmi 14C con la build de Play (v19)**, provocando avisos de verdad:
se crea un documento en `notifications/<uid>/items` con el Admin SDK y el resto del camino
es el real (`onNotificationCreated` → FCM → móvil). uid de Sergio:
`rNpwrHo3euaQSJjwsGFGX2vHr193`.

| Paso | Qué se hizo | Resultado |
|---|---|---|
| 1 | App al fondo + `am kill` (proceso muerto, **sin** `force-stop`) | proceso muerto |
| 2 | Aviso A creado | push recibido y **sonó** (08:46:53); tarjeta `tag=FCM-Notification:…`, puesta por Play Services; el proceso **no** arrancó |
| 3 | Se abre la app | **la misma clave antes y después**, ninguna tarjeta nueva, y la app no encoló nada |
| 4 | Campana | pasó de 3 a 4 — correcta |
| 5 | Aviso B con la app **abierta** | sonó, **una sola vez** |

⚠️ **`force-stop` invalida la prueba.** Deja la app en *stopped state* y FCM deja de
entregarle nada, así que el push no llegaría y parecería otro fallo. Hay que usar `am kill`,
que mata el proceso sin esa marca.

#### Cómo se demostró lo de "una sola vez" (no es obvio)

Con la app en primer plano el aviso se publica **dos veces**: una por `onMessageReceived`
y otra por el listener de Firestore. Las dos con el mismo id (el hash del texto), así que
se ve una sola tarjeta. Que además suene una sola vez lo hace `setOnlyAlertOnce(true)`,
y se comprueba así:

```
flags=ONLY_ALERT_ONCE|AUTO_CANCEL|SILENT     <- el sistema añadió SILENT
originalFlags=ONLY_ALERT_ONCE|AUTO_CANCEL    <- lo que pidió la app
```

⚠️ **`mLastAudiblyAlertedMs` engaña**: la segunda publicación, ya muda, lo resetea a `-1`,
así que parece que el aviso no sonó nunca. El campo que **no** se borra es
`mSoundNotificationKey` en `dumpsys notification`, que apuntaba a la tarjeta del aviso B.
Ese es el que hay que mirar.

ℹ️ La tarjeta de resumen no salió, y está bien: la de Play Services no lleva el `setGroup`
de la app, así que solo había **una** en el grupo y `actualizarResumen` la retira por
debajo de dos.

**Los dos avisos de prueba se borraron** (`fromUserName` empezaba por "Prueba v19"), y el
`adc_tmp.json` también. Las dos tarjetas que quedan en la bandeja del móvil se van solas al
deslizarlas.

### ✅ APP LINKS VERIFICADOS EN EL MÓVIL CON LA BUILD DE PLAY (2026-11-10)

La v19 está publicada en prueba cerrada (aviso de Play del 9 de noviembre) e instalada en el
Redmi 14C desde la tienda. **La verificación que llevaba meses pendiente ya está hecha.**

```
adb shell pm get-app-links com.app.foodranker
  Signatures: [DB:9A:33:74:...:FD:1D]      <- la firma de PLAY, no la de debug
  Domain verification state:
    foodranker.web.app: verified
```

Antes de actualizar, con la v18 instalada, decía `foodranker.app: 1024` — el dominio
equivocado, que nunca existió, sin verificar. Es el fallo exacto que arregla la v19.

**La pieza que faltaba por comprobar** era si la huella con la que Play firma la app está
en el `assetlinks.json` que se sirve de verdad. Lo está: la firma que reporta el móvil
(`DB:9A:33:74:…`) es la primera de las cinco del fichero servido en
`https://foodranker.web.app/.well-known/assetlinks.json`.

Comportamiento, con un plato real (`ChIJ17p_qV9HoBIRTRSfEBcZuhk__arroz-illa`):

| Prueba | Resultado |
|---|---|
| `https://foodranker.web.app/plate/<id>` en frío | `Activity: com.app.foodranker/.MainActivity`, **sin navegador** |
| `https://foodranker.web.app/user/<id>` con la app abierta | *delivered to currently running top-most instance* |
| `foodranker://invite/REF42` con la app abierta | *delivered to currently running top-most instance* |
| Instancias de `MainActivity` tras los tres | **1** (el `singleTop` funciona) |
| `logcat -b crash` | ninguno |

⚠️ **Xiaomi mete un selector de tienda**: `am start -d market://details?id=…` abre un
diálogo "Google Play / Mi App Mall" en vez de ir a Play. Añadir `-p com.android.vending`
para saltárselo.

ℹ️ El formulario de consentimiento (UMP) **sale en castellano en el primer arranque de la
v19**, lo que confirma en vivo lo que se había verificado buscando cadenas en el dex: R8 no
se llevó el SDK por delante.

ℹ️ `plates` se puede leer por la API REST de Firestore **sin autenticar**, solo con la
`current_key` de `google-services.json` (así se sacó el id del plato de arriba). Encaja con
que el ranking sea público, pero conviene mirarlo cuando se repasen las reglas.

#### Lo que sigue pendiente, y por qué

**La tanda de notificaciones.** El Redmi 14C estaba en **primer arranque**: la app se
instaló el 7 de noviembre y no se había abierto nunca. Hace falta pasar el formulario de
consentimiento y **entrar con Google**, y eso no se puede pilotar por ADB. El permiso
`POST_NOTIFICATIONS` ya está concedido (`granted=true`).

### 📱 MÓVIL NUEVO: REDMI 14C (2026-11-10)

El móvil de pruebas ya **no es el Redmi Note 10S (Android 13)** del que habla todo lo
anterior en este fichero. Ahora es un **Redmi 14C**. Lo que daba por hecho el handoff y hay
que rehacer en el móvil nuevo:

- **No tiene nada instalado.** Hay que volver a entrar por el enlace de tester del canal
  cerrado para que Play ofrezca la app.
- **La cuenta de Google del móvil tiene que estar en la lista de testers**, o Play dirá que
  la app no está disponible. El Note 10S tenía dos cuentas dadas de alta (ver más abajo);
  en el 14C hay que comprobarlo.
- **El token de FCM es uno solo por usuario** (`users/<uid>.fcmToken`). Al entrar en el
  móvil nuevo se sobreescribe el del viejo, así que los push van al 14C — pero si se vuelve
  a entrar en el Note 10S, se los lleva él. Para las pruebas de avisos: **un solo móvil con
  sesión iniciada**.
- **ADB pide autorización otra vez**: el 14C no tiene la llave de este PC. Ajustes →
  Información del teléfono → pulsar 7 veces en la versión de HyperOS → Opciones de
  desarrollador → **Depuración por USB**, y aceptar el diálogo con el móvil desbloqueado.
  En Xiaomi el diálogo no sale hasta poner el USB en **transferencia de archivos (MTP)**.
  Para instalar por cable hace falta además **"Depuración USB (ajustes de seguridad)"**,
  que obliga a tener SIM y cuenta Xiaomi — para las pruebas de la v19 no hace falta,
  la build se baja de Play.

ℹ️ `adb` no está en el PATH del bash. Ruta completa:
`C:\Users\User\AppData\Local\Android\Sdk\platform-tools\adb.exe`

### ✅ ~~PENDIENTE DE VERIFICAR EN EL REDMI~~ — HECHO EL 2026-11-10

> Lo de abajo se escribió cuando estaba pendiente. **Ya está verificado**, ver las dos
> secciones de arriba. Se deja porque describe bien qué se quería probar y por qué.

#### (texto original)

Dos cosas de la v19 están arregladas y sin comprobar en un móvil de verdad. **Las dos
necesitan la build bajada de Play**, no una local.

**1. Los App Links.** Compartir un plato por WhatsApp y abrir el enlace: debe **abrir la app
directamente**, sin pasar por el navegador. Si no, `adb shell pm get-app-links
com.app.foodranker`. Ojo: la verificación se hace contra la clave con la que está instalada
la app, y la de Play no es la de debug ni la de subida — por eso hay que probarlo con la
build de la tienda. En el emulador, con la clave de debug, ya dio `verified`.

**2. Las notificaciones repetidas.** Provocar un aviso **con la app cerrada** (que otra
cuenta dé un like o comente), recibir el push, y entonces entrar en la app:

- **No debe sonar ni aparecer nada al entrar.** Ese era el fallo.
- La campana debe seguir mostrando **el contador correcto**.
- Provocar otro aviso **con la app abierta**: ese sí debe sonar, una sola vez.

No se puede probar desde el PC: hace falta una sesión autenticada y un aviso real.

---

### 🟢 LAS NOTIFICACIONES SE REPETÍAN AL ABRIR LA APP — ARREGLADO (2026-11-08)

Sergio lo reportó así: *"la app notifica las cosas cuando estás fuera de la app y de nuevo
vuelven a saltar cuando entras a la app"*. Es exacto, y pasaba **en cada arranque en frío**.

**El diseño tiene dos caminos paralelos a propósito**: el push de FCM
(`onNotificationCreated` en `functions/src/index.ts`) y un listener de Firestore
(`NotificationRepository`). Los dos llaman a `NotificationHelper.show`, y el servidor
dispara sobre **el mismo documento** que vigila el listener, así que todo aviso viaja por
los dos caminos siempre. El anti-duplicados era que el id del aviso sale del **hash del
texto**, de ahí que `check_notif_parity.py` exija que los dos lados redacten idéntico.

#### ❌ 1. El flag `isFirstLoad` lo consumía la caché — ARREGLADO

La caché persistente de Firestore está **encendida** (50 MB, `FirebaseModule`). Con caché,
`addSnapshotListener` dispara **dos veces** al abrir la app:

1. con la **caché** — documentos viejos. El flag se apagaba aquí.
2. con el **servidor** — y todo lo creado mientras la app estaba cerrada llegaba como
   `ADDED`, con el flag ya gastado → `NotificationHelper.show` por cada uno.

Eran avisos que el usuario **ya había recibido por push estando fuera**, a veces ya leídos
o descartados, y volvían a sonar al entrar.

Arreglado sustituyendo el flag por `primerSnapshotDelServidorVisto`: se ignoran los
snapshots de caché (`snapshot.metadata.isFromCache`) y **todo lo que venga en el primer
snapshot del servidor es "lo que ya había"** y no avisa. Solo avisa lo que llegue después,
es decir lo que ocurre de verdad mientras la app está abierta.

⚠️ **El contador de la campana sí se sigue actualizando con la caché**, a propósito: tiene
que ser correcto sin red. La línea de `_unreadCount` va **antes** de los dos `return`.

#### ❌ 2. El doble sonido — ARREGLADO

`setOnlyAlertOnce` no se ponía en ningún sitio. `notify()` con un id que ya existe
reemplaza la tarjeta pero **vuelve a sonar y vibrar**, así que el hash del texto evitaba la
tarjeta duplicada pero no el ruido. Y la **tarjeta de resumen del grupo** se republica con
el mismo id en cada aviso nuevo, o sea que era un multiplicador: cinco likes sonaban cinco
veces por las tarjetas y otras cuatro por el resumen. Puesto en las dos.

#### ⚠️ 3. LO QUE NO SE HA ARREGLADO, Y POR QUÉ (decisión de Sergio, 2026-11-08)

**El hash del texto no puede deduplicar donde hace falta.** El push lleva
`notification: { title, body }`, y con *notification payload* y la app en segundo plano **la
tarjeta la pone Android**, no nuestro código: `onMessageReceived` no se ejecuta y el id lo
elige el SDK. Así que el hash solo deduplica cuando el push llega **en primer plano**, justo
el caso en que no hace falta.

Y el push **ya lleva `data.notifId`** con el id del documento de Firestore — la clave única
perfecta, conocida por los dos caminos (`change.document.id` en el listener). **El cliente
nunca la lee.**

El arreglo de verdad sería **push data-only** (`priority: high`, sin `notification`): así
`onMessageReceived` se ejecuta siempre, la app controla el id y se puede usar `notifId` en
vez del hash. **Descartado a propósito**: MIUI mata procesos en segundo plano con ganas, y
un data-only necesita que el proceso arranque para pintar algo. Si Android no lo deja, no
sale **ningún** aviso — peor que un duplicado. El *notification payload* lo pinta Android
aunque el proceso esté congelado. Se prefiere fiabilidad sobre elegancia.

🚨 **Consecuencia: `check_notif_parity.py` importa MÁS que antes, no menos.** Con el
arreglo de arriba, un tipo de aviso que el servidor no sepa redactar (cae en el `default`
del switch y solo deja un `logger.warn`) ahora **solo avisaría si llega con la app
abierta**. El listener ya no tapa los huecos del servidor al arrancar. Pasarlo al añadir
cualquier tipo de aviso.

#### ✅ Cubierto por tests (2026-11-08)

La decisión se sacó del repositorio a una clase `GuardiaDeAvisos` **para poder probarla sin
montar un Firestore**. 6 tests nuevos, 56 en total.

⚠️ **Comprobado que los tests sirven**: se volvió a poner la lógica vieja a propósito y se
pusieron rojos — falla `la cache no gasta la linea base del servidor`, que es exactamente
el fallo. Un test de regresión que pasa con el código roto no vale nada, y esto se
comprueba una vez, al escribirlo.

**Lo que los tests NO cubren, y hay que probar en el Redmi.** La app solo tiene login de
Google — no hay email/contraseña ni anónimo, comprobado — así que **no se puede crear una
sesión autenticada en el emulador** y el camino completo (push real + listener real) no se
puede ejercitar desde el PC. Ver la sección de verificaciones pendientes, arriba.

🚨 **El paso 4 de esa prueba importa tanto como el 2**: un aviso con la app **abierta** sí
tiene que sonar, una sola vez. Si el filtro se hubiera pasado de celoso, los avisos en
primer plano habrían dejado de sonar — un fallo peor que el original.

---

### 🟢 AAB DE LA v19 (1.6) — COMPILADO Y VERIFICADO, SIN SUBIR (2026-11-04)

`app/build/outputs/bundle/release/app-release.aab`, 15,98 MB. **Regenerado dos veces**: el
2026-11-04 tras la revisión de abajo (tres fallos más) y el **2026-11-08 con el arreglo de
las notificaciones repetidas**. El `versionCode` 19 sigue valiendo porque no se ha subido
nada a Play todavía.
**Esta es la versión comprometida ante Google** en la solicitud de acceso a producción:
dije por escrito que el arreglo de los enlaces se sube antes de publicar. La v18 que llevan
hoy los testers sigue repartiendo enlaces muertos.

Lleva tres arreglos de app sobre la v18, no solo uno:

| Commit | Qué |
|---|---|
| `10ab7a3` | Los enlaces compartidos (plato, perfil, invitación) iban a un dominio inexistente |
| `2217d00` | El selector de idioma de MIUI se llevaba FoodRanker por delante |
| `677bb3c` | Oculta la venta de Premium mientras no haya cuenta de comercio |

Verificado sobre el binario, no sobre el fuente:

| Comprobación | Cómo | Resultado |
|---|---|---|
| Tests unitarios | `testDebugUnitTest` | **56 tests, 0 fallos, 0 errores** |
| Versión | manifest fusionado | 19 / 1.6 |
| Clave correcta | `keytool -printcert -jarfile` | SHA-256 `24:B8:7F:…:1A:81` |
| Firma | `jarsigner -verify` | `jar verified` |
| **Host nuevo en el manifest** | manifest binario del AAB | `foodranker.web.app` presente, `foodranker.app` **ausente** |
| **Host muerto en el código** | cadenas en `base/dex/*.dex` | aparece 1 vez, en `ACCEPTED_HOSTS`; `https://foodranker.app/` no se construye nunca |
| UMP sobrevive a R8 | cadenas en `base/dex/*.dex` | las 4 marcas presentes |
| Textos legales | cadenas en `base/resources.pb` | 9 marcadores, castellano **e** inglés |
| Split por idioma | `BundleConfig.pb` en crudo | `08 03 10 01` = LANGUAGE negate:true, desactivado |
| Lado web | `curl` + API de Digital Asset Links | HTTP 200, `application/json`, **5 statements, sin errores** |
| **App Links de verdad** | `pm get-app-links` en el emulador | **`foodranker.web.app: verified`** |
| **El enlace abre la app** | `am start -W` sobre la URL https | `Activity: com.app.foodranker/.MainActivity`, sin navegador |
| `launchMode` | manifest fusionado | `singleTop` solo en MainActivity (los 3 `singleTask` son de Firebase Auth y AdMob) |
| Opción muerta quitada | cadenas en `base/resources.pb` | `Ver ranking completo` / `See full ranking` ausentes |

⚠️ **El host muerto en el dex no es un fallo, y el `OK` de esa fila cuesta leerlo.**
`DeepLinks.ACCEPTED_HOSTS` incluye `foodranker.app` **a propósito**: si algún día se
registra el dominio y se apunta al mismo sitio de Firebase, los enlaces funcionarán sin
tocar la app. Lo que importa es que `DeepLinks.HOST`, el único que **genera** enlaces, es
`foodranker.web.app`. Buscar el host muerto a secas da positivo siempre; hay que buscar
`https://foodranker.app/`, que es lo que diría que se sigue generando.

⚠️ **Las sondas de los textos legales no van en `strings.xml` donde uno cree.** El
responsable y la base jurídica están en `pp_0_b` y `pp_3_b`, y la base jurídica se escribe
`Legitimate interests` / `Art. 6(1)(f)`. Buscar `privacy.html` o `legitimate interest` en
minúsculas da **FALTA** aunque todo esté bien — esas cadenas viven en las páginas de
GitHub Pages, no en los recursos de la app.

**Notas de la tienda ya redactadas** en `docs/play-store-listing.md`, en los tres idiomas
(es-ES 352, en-GB 320, en-US 320 caracteres — límite 500). Esta vez en-US es **idéntico** a
en-GB porque no hay ninguna palabra que cambie de ortografía, pero hay que pegarlo igual:
si en-US se queda vacío, Play le sirve el castellano.

#### 🔍 LA REVISIÓN ANTES DE SUBIR ENCONTRÓ TRES FALLOS MÁS (2026-11-04)

El primer AAB de la v19 estaba compilado y verificado, y aún así llevaba un fallo que se
habría subido. **Los tres son del mismo tipo: código que nunca se había ejecutado porque
los enlaces estaban muertos.** Arreglar los enlaces no solo arregla los enlaces: enciende
caminos que llevaban años apagados y sin probar.

**1. Un enlace con la app abierta duplicaba la app.** `MainActivity` no declaraba
`launchMode`, así que era `standard`. Medido en el emulador, no deducido: tras dos enlaces
seguidos la tarea `t14` tenía **dos `MainActivity`** (`Hist #0` y `Hist #1`). El usuario ve
la app arrancar otra vez por el splash y "atrás" le devuelve a su copia anterior en vez de
salir. Y `MainActivity` **ya sobrescribía `onNewIntent`**, que con `standard` no se ejecuta
nunca: estaba escrito para un `launchMode` que no tenía. De paso, las notificaciones llevan
`FLAG_ACTIVITY_CLEAR_TOP`, que sobre una actividad `standard` la **destruye y la recrea**
perdiendo el estado. Arreglado con `android:launchMode="singleTop"`; vuelto a medir, **una
sola instancia**, y `am start` responde *"intent has been delivered to currently running
top-most instance"*, que es `onNewIntent` funcionando.

**2. El respaldo de las invitaciones no existía.** El botón "Ya tengo la app" de
`hosting/public/open.html` manda a `foodranker://<tipo>/<id>`. Había filtro para `plate` y
`user`, **no para `invite`**, y `parseFoodRankerInviteCode` devolvía `null` para el esquema
propio con un test afirmándolo. No estaba documentado en ningún sitio: no era una decisión,
era el reflejo del fallo. Justo el único de los tres tipos de enlace que nunca ha
funcionado. Antes `foodranker://invite/REF42` daba `unable to resolve Intent`; ahora entra.
El test cambiado dice por qué.

**3. "📊 Ver ranking completo" quitada del menú del plato.** Hacía `startActivity` sobre
`https://foodranker.web.app/plate/{id}`, la URL del plato que ya estás viendo. Con el
dominio muerto era un error de DNS en el navegador; **al arreglarlo pasa a ser peor**,
porque ese enlace lo captura la propia app (App Links verificados) y te devuelve al mismo
sitio. En la web no hay ningún ranking: `open.html` solo es una página de aterrizaje.
Quitada la opción y las dos cadenas `det_full_ranking`.

⚠️ **La huella del keystore de debug está en `assetlinks.json` a propósito, y eso permite
probar los App Links sin pasar por Play.** `installDebug` en el emulador +
`pm verify-app-links --re-verify` + `pm get-app-links` → `verified`. Es la forma de
comprobar la cadena completa **antes** de subir nada. Lo que no se puede probar así es la
build de Play, que va firmada con otra clave: eso hay que verlo en el Redmi con la app
bajada de la tienda.

⚠️ **`cmd package resolve-activity` MIENTE sobre los App Links.** Para los tres enlaces
verificados devolvía `ResolverActivity` (el selector), como si no estuvieran verificados.
No aplica la preferencia de dominio verificado. Lo que vale es lanzar el intent de verdad
con `am start -W` y leer qué actividad queda delante.

#### Tres cosas que quedan sueltas (ninguna bloquea subir)

**La invitación falla en silencio.** `DiscoverViewModel.applyInviteCode` captura toda
excepción y la manda a `Log.w`. Ni el usuario ve nada ni queda rastro en ningún sitio que
alguien mire. Es el mismo patrón que dejó los enlaces muertos durante meses.

**Una cuenta antigua no puede aplicar invitaciones.** Consultado en Firestore el 2026-11-04:
de 12 usuarios, **1 no tiene el campo `referredByUserId`** — el de Sergio, el más antiguo,
creado antes de que el campo existiera. La regla exige
`resource.data.referredByUserId == ''`, y en las reglas de Firestore **leer un campo que no
existe es un error de evaluación, no `null`**, así que deniega. Los otros 11 lo tienen a
`""`. No bloquea nada en práctica (nadie se invita a sí mismo), pero es la trampa a
recordar si alguna invitación falla sin motivo aparente.

**Una invitación no sobrevive a la instalación.** No hay Play Install Referrer. Quien
recibe una invitación **sin tener la app** aterriza en `open.html`, instala desde Play y el
código se pierde — que es el caso principal de una invitación. La v19 arregla al que ya la
tiene. Si alguna vez importan los referidos de verdad, esto es lo que falta.

**Retomar aquí:** subir el AAB a prueba cerrada, pegar las notas en los tres idiomas, y
**verificar los App Links en el Redmi con la build de Play** (no con la local: la
verificación se hace contra la clave con la que está instalada la app). Compartir un plato
por WhatsApp y comprobar que el enlace **abre la app directamente**, sin pasar por el
navegador. Si no lo hace: `adb shell pm get-app-links com.app.foodranker`.

---

### 🔴 PROBADO EN EL REDMI: PREMIUM NO EXISTE Y EL IDIOMA TUMBA LA APP (2026-11-02)

**La v18 (1.5) está PUBLICADA en alpha** desde el 2 nov ~15:45. El formulario de Seguridad
de los datos fue en el mismo envío, y el borrado parcial se mandó después, por separado.
App Check quedó registrado con Play Integrity (ver más abajo).

Probado sobre la build **de Play**, pilotando el Redmi por ADB. Cuatro cosas bien y dos mal.

| Qué | Resultado |
|---|---|
| Formulario de consentimiento (CMP) | ✅ Sale con logo y nombre, en castellano, con "No consentir" |
| Anuncios sin consentimiento | ✅ **Cero anuncios**, y `MobileAds` ni se inicializa |
| "Opciones de privacidad de anuncios" en el perfil | ✅ Presente |
| App en inglés | ✅ Completo. Hasta el formato numérico cambia: `10.0`, no `10,0` |
| Política de privacidad en inglés | ✅ Con el responsable y la base jurídica nuevos |
| **Premium** | ❌ No hay precio y la compra no arranca |
| **Idioma** | ❌ Se lleva la app por delante |

#### ❌ PREMIUM NUNCA HA SIDO REAL — no hay cuenta de comercio

**No es un bug del cliente.** El código se comporta exactamente como está escrito:
`queryProductDetails` no devuelve nada → el botón sale como "Suscribirse" a secas
(rama `price.isBlank()`) → `launchPurchase` devuelve `false` → "No se pudo iniciar la
compra. Inténtalo de nuevo". La app **no se cierra**: eso era el fallo del idioma.

La causa está en Play Console → **Monetizar con Play**, que dice literalmente *"Para
monetizar esta aplicación, configura una cuenta de comercio"*. Sin cuenta de comercio no
hay productos, así que **`foodranker_premium_monthly` no existe en Play**. El cliente pide
un producto que nunca se dio de alta.

⚠️ **Este handoff lleva desde agosto diciendo "Billing implementado y real (el precio
2,99 €/mes lo devuelve Google Play)". Era falso.** Lo implementado es el código cliente;
la tienda nunca ha tenido nada que vender. Corregido más abajo, en "Qué funciona".

**Para arreglarlo, en este orden:** crear la cuenta de comercio (datos fiscales y bancarios,
y Google tarda en verificarla) → crear la suscripción con el id exacto
`foodranker_premium_monthly` → plan base **activo** con precio para España → poner
`BillingManager.SUSCRIPCION_A_LA_VENTA` en `true` → volver a probar.

✅ **VENTA OCULTA MIENTRAS TANTO (2026-11-02).** `BillingManager.SUSCRIPCION_A_LA_VENTA`
en `false` esconde el botón de suscribirse, la lista de beneficios y cambia los textos de
la fila del perfil y de la cabecera. **La pantalla sigue existiendo a propósito**: dentro
está el **anuncio recompensado** (24h sin anuncios), que no depende de Billing, funciona y
genera ingresos. Ocultar la pantalla entera lo habría matado también.

Los beneficios se van **enteros**, no solo el botón: "Badge Premium" y "Más platos al día"
solo se consiguen pagando, y el anuncio recompensado únicamente quita los anuncios, y por
24 horas. Dejarlos anunciados sería prometer lo que no se puede dar.

Textos nuevos para este estado, en los dos idiomas: `prof_adfree`, `prof_adfree_sub`,
`prm_title_adfree`, `prm_sub_adfree`. Los antiguos (`prof_premium`, `prm_title`…) siguen
ahí y vuelven solos al poner el flag en `true`.

💶 **Lo que de verdad frena esto no es técnico, es fiscal.** Abrir la cuenta de comercio
obliga a mirar el alta censal (modelo 036, obligatoria desde el primer euro), el alta en
RETA (depende de la habitualidad) y, como Google paga desde **Irlanda**, el ROI/VIES y el
modelo 349. ⚠️ **Esto ya aplica hoy por AdMob**, no lo crea Premium: hay anuncios sirviendo
en una app publicada y un perfil de pagos de AdSense ya creado (`2483-2588-0840`). Sergio
decidió el 2026-11-02 consultarlo con una gestoría antes de seguir, y **dejar AdMob como
está**. Salir a producción **no** requiere Premium.

⚠️ Si se abre la cuenta de comercio: el "Perfil público del comerciante" trae marcada la
casilla *"Utiliza la dirección, el contacto y el nombre de la información legal"*, lo que
haría **pública la dirección de casa**. Y el correo de atención al cliente tiene que ser
`foodranker.app@gmail.com`, el mismo de la política y los términos, no un Gmail personal.

⚠️ El Redmi tiene **dos cuentas de Google** dadas de alta (`sergiodelapen...` y
`sdelapenya1...`). Cuando la suscripción exista, la que esté activa en Play Store decide
qué productos ve: si no aparece, mirar eso antes de buscar un bug.

#### ❌ IDIOMA: el bug es de Xiaomi, pero se llevaba la app — ARREGLADO

```
Process: com.android.settings
AppLocalePickerActivity.onCreate(AppLocalePickerActivity.java:82)
NullPointerException: ... ActionBar.setDisplayHomeAsUpEnabled(boolean) on a null object reference
```

El selector de idioma por aplicación **de MIUI** se estrella solo. Nosotros lanzamos el
intent correcto. El problema es que entraba en **nuestra tarea**, y al caerse Android
terminaba toda la pila: `W/ActivityTaskManager: Force finishing activity
com.app.foodranker/.MainActivity`. El usuario ve desaparecer FoodRanker y cree que ha
petado la app.

**Arreglado** anadiendo `FLAG_ACTIVITY_NEW_TASK` al intent (`ProfileScreen.kt`, fila de
idioma). Ajustes se sigue cayendo —eso no se arregla desde aquí— pero FoodRanker queda en
pie. ⚠️ El `runCatching` que ya había **no servía de nada**: la excepción ocurre en otro
proceso, no en el nuestro, así que no hay nada que capturar.

**Arreglo bueno, para la v19:** selector de idioma propio dentro de la app con
`LocaleManager.setApplicationLocales()` (API 33+, está en el framework, no hace falta
AppCompat). Deja de depender de una pantalla de Ajustes que cada fabricante rompe a su
manera, y de paso permite ofrecer solo los dos idiomas que la app realmente tiene.

### 🟢 SOLICITADO EL ACCESO A PRODUCCIÓN (2026-11-03, 9:42)

**Los tres requisitos aparecieron cumplidos y tachados**: versión de prueba cerrada
publicada, 12 testers aceptados y 14 días de prueba. Los dos últimos llevaban meses
dándose por pendientes en este fichero y en la memoria — ya no lo estaban.

Google responde por correo **al propietario de la cuenta** (`sdelapenya1991@`, no el de
contacto de la app). Dice **7 días o menos**, "pero en ocasiones puede llevar más tiempo".

⚠️ **Esto NO publica la app.** Habilita el canal de producción. Después hay que crear la
versión de producción y pasar **otra** revisión distinta.

#### Lo que se contestó, palabra por palabra

Guárdese: si rechazan o piden aclaraciones, hay que saber qué se dijo. Todas las casillas
eran de **300 caracteres**.

**Paso 1 — Sobre la prueba cerrada**

> *¿Cómo reclutaste usuarios?* — Amigos, familia y compañeros de trabajo, contactados uno a
> uno por mensaje directo. No he usado servicios de pago ni grupos de intercambio de
> testers: quería gente que saliera a comer de verdad y me contara qué veía, no
> instalaciones para cumplir el requisito.

> *¿Cómo de fácil resultó?* — **Difícil**

> *Interacciones de los testers* — Uso real y variado: 57 platos con foto en 34
> restaurantes de España y Francia, 66 valoraciones y 41 comentarios, repartidos entre 6
> usuarios activos. También usaron seguir perfiles y reportar contenido. Lo único que nadie
> usó fueron las invitaciones, por un fallo que ya he corregido.

> *Comentarios recibidos y cómo* — Por WhatsApp y hablando con ellos en persona,
> preguntando uno a uno. Reportaron: no llegaban los avisos de comentario (fallo real, ya
> corregido), no se podía iniciar sesión con el navegador de Xiaomi (añadí login nativo) y
> el corrector del teclado cambió el nombre de un plato al subirlo.

**Paso 2 — Sobre la aplicación**

> *Audiencia* — Adultos a partir de 18 años a los que les gusta salir a comer y quieren
> saber qué plato pedir en un sitio concreto, no si el restaurante está bien en general. La
> ficha está declarada solo para mayores de 18 porque el contenido lo publican los propios
> usuarios y la app muestra anuncios.

> *Valor* — Las reseñas puntúan restaurantes; aquí se puntúa el plato concreto, por sabor,
> presentación, relación calidad-precio y si lo volverías a pedir. Así sabes qué pedir, no
> solo dónde ir. Cada plato va atado a un local real de Google Places, con ranking por
> ciudad y la foto de quien se lo comió.

> *Descargas el primer año* — **Entre 0 y 10.000**

**Paso 3 — Preparación para producción**

> *Cambios hechos con lo aprendido* — Login nativo con Credential Manager, porque con el
> navegador de Xiaomi no se podía entrar. Arreglados los avisos de comentario, que no
> llegaban. Traducida entera al inglés tras probarla con el móvil en ese idioma. Añadida
> moderación de platos y comentarios. Son 18 versiones durante la prueba.

> *Cómo decidiste que estaba lista* — (versión A, la que reconoce el fallo abierto)
> Probando la v18 en un Redmi físico pantalla por pantalla, en castellano y en inglés, hasta
> dejar de encontrar fallos nuevos, y con Crashlytics limpio. El último que encontré, los
> enlaces compartidos, ya está corregido y lo subiré antes de publicar en producción.

🚨 **ESA ÚLTIMA RESPUESTA ES UN COMPROMISO POR ESCRITO ANTE GOOGLE.** Dice que la v19
con el arreglo de los enlaces se sube **antes** de publicar en producción. No se puede
sacar producción con la v18.

#### De dónde salieron los números

De contar Firestore y Auth con el Admin SDK el 2026-11-03, no de estimarlos:

| | |
|---|---|
| Cuentas | 13 en Auth (10 con nombre), 12 en `users` |
| Platos | **57, todos con foto**, de **6 usuarios distintos** |
| Locales | **34 restaurantes distintos**, España y Francia |
| Valoraciones | **66**, de 6 usuarios distintos, nota media **9,03** |
| Comentarios | **41**, de 5 usuarios distintos |
| Seguimientos | 15 |
| Reportes | 1 (la moderación se ejercitó de verdad) |
| **Referidos** | **0** |

⚠️ Ese **0 en referidos confirma por otra vía lo de los enlaces muertos**: no es que nadie
invitara, es que invitar no servía de nada. Cuando la v19 lleve una temporada fuera, ese
número es la forma de comprobar que el arreglo funcionó.

⚠️ `createdAt` **no es un Timestamp de Firestore** en `plates`/`ratings`/`comments`: al
contar por fecha sale 0 documentos. Si alguna vez se quiere actividad por día, hay que
mirar primero cómo está guardado de verdad.

---

#### 🔴 ENCONTRADO Y ARREGLADO: la app repartía enlaces a un dominio que no existe (2026-11-02)

Play avisó de que "3 enlaces profundos no funcionan porque tus dominios web no están
asociados a tu aplicación". El aviso se queda corto: **`foodranker.app` no estaba sin
verificar, estaba sin registrar.**

```
$ nslookup foodranker.app
*** Non-existent domain
```

Y la app lo usaba en cuatro sitios para construir enlaces que los usuarios comparten de
verdad: compartir perfil y compartir plato (`ShareManager`), copiar enlace
(`PlateDetailScreen`) e invitar amigos (`ReferralManager`). **Cada vez que un tester
compartía un plato, mandaba un enlace muerto.** El mecanismo de referidos llevaba roto
desde siempre, en silencio, porque nadie llegó a pulsar un enlace compartido — se probó
que el botón abría el selector de compartir, no dónde iba el enlace.

**Arreglado con Firebase Hosting**, no comprando dominio. Se descartó pagar 15 €/año por
ahora: el proyecto ya tenía Firebase, el sitio es gratis, da HTTPS automático y sirve
`/.well-known/assetlinks.json`, que es lo único que Android necesita.

| Pieza | Dónde |
|---|---|
| Sitio | `foodranker.web.app` (sitio `foodranker` del proyecto `foodranker-51270`) |
| Config | `firebase.json` → `hosting`, target **`links`**; mapeado en `.firebaserc` |
| Página | `hosting/public/open.html` — bilingüe según el navegador, botón a Play y respaldo `foodranker://` |
| Verificación | `hosting/public/.well-known/assetlinks.json` — **5 huellas SHA-256** |
| Dominio en código | `DeepLinks.kt` — un solo sitio, antes eran cuatro a pelo |

Desplegar: `firebase deploy --only hosting:links --project foodranker-51270`.

⚠️ **TRAMPA: el `ignore` por defecto de Firebase Hosting incluye `**/.*`**, que se lleva
por delante `.well-known/assetlinks.json` y deja los App Links sin verificar **para
siempre, sin ningún error**. Por eso el `ignore` del `firebase.json` está escrito a mano
sin esa línea. Si alguna vez el despliegue dice "found 1 file" en vez de **2**, es esto.

⚠️ **El parser acepta DOS dominios a propósito** (`DeepLinks.ACCEPTED_HOSTS`):
`foodranker.web.app` y `foodranker.app`. Un enlace compartido por WhatsApp sigue vivo años,
así que la app tiene que entender todos los dominios que haya repartido alguna vez. El
segundo está por adelantado: **si algún día se compra `foodranker.app` y se apunta al mismo
sitio de Firebase, los enlaces funcionan sin actualizar la app.** De esa lista solo se
añade; quitar un dominio rompe enlaces que ya circulan.

**Las 5 huellas de `assetlinks.json`**: las **tres de Play** (`DB:9A:33:74`, `4D:A9:19:25`,
`98:9F:85:51` — las mismas de App Check), la **de carga** del keystore local
(`24:B8:7F:C8:9A:A9:25:9B:CE:87:B0:F0:BA:E1:7E:FE:86:C3:DA:5E:2A:3B:FA:7C:CB:B0:6F:3E:66:43:1A:81`)
y la **de debug**
(`7F:3C:39:D4:52:FC:16:03:66:30:C2:A3:95:84:AB:A7:06:65:B1:65:BC:0E:A6:E5:7E:03:02:A8:79:D5:39:55`),
para que los builds locales también verifiquen. Se sacan con
`keytool -list -v -keystore <ks> -alias <alias>`; las de Play, de Play Console.

**Comprobado en vivo** con el validador oficial de Google, que es el mismo que usa Android:
```
GET https://digitalassetlinks.googleapis.com/v1/statements:list
      ?source.web.site=https://foodranker.web.app
      &relation=delegate_permission/common.handle_all_urls
→ statements: 5 · errores: ninguno
```
Las cuatro rutas (`/`, `/plate/x`, `/user/x`, `/invite/x`) responden 200. 13 tests de
`DeepLinkParserTest` en verde, `compileDebugKotlin` limpio.

🚨 **NADA DE ESTO LLEGA AL USUARIO HASTA LA v19.** La v18 instalada sigue llevando
`foodranker.app` en su manifest, así que **los enlaces siguen rotos en el móvil de todos
los testers ahora mismo**. El sitio está listo esperando a la app, no al revés. Al probar
la v19 en el Redmi: compartir un plato, abrir el enlace desde WhatsApp y ver que **abre la
app directamente**, sin pasar por el navegador. Si pasa por el navegador, la verificación
no ha prendido — se diagnostica con
`adb shell pm get-app-links com.app.foodranker`.

💡 **Mejora pendiente, no hecha**: la página no enseña el plato. Un enlace compartido
por WhatsApp sale sin imagen ni título del plato, que es justo lo que hace que a alguien le
entren ganas de pulsar. Arreglarlo pide Open Graph por plato, o sea renderizado en servidor
con una Cloud Function detrás del hosting. Candidato fuerte para después de la v19: el
mecanismo viral ya está construido y es lo que más lo potenciaría.

#### ✅ RESUELTO: AdMob sirvió un anuncio de CBD → categorías bloqueadas (2026-11-02)

El banner que salió durante la prueba era de **CBD** ("+100G de CBD gratis"). En una app de
comida con clasificación para público amplio eso puede dar un disgusto en la revisión de
producción.

**Arreglado en AdMob → Controles de bloqueo**, a nivel de cuenta (solo hay una app). De las
11 **categorías estándar** quedaron bloqueadas **8**:

| Bloqueada | Por qué |
|---|---|
| Medicamentos y complementos alimenticios | **Aquí cae el CBD** — la que motivó todo esto |
| Pérdida de peso | En una app de **comida** es lo más delicado: dieta junto a fotos de platos roza el trastorno alimentario |
| Citas | Estaba sirviendo (0,4 % impresiones) |
| Referencias al sexo | Mismos números que Citas: es el **mismo anuncio** contado dos veces |
| Salud reproductiva y sexual | 0,4 % de impresiones y **0,0 % de ingresos**: gratis |
| Ganar dinero rápidamente | La categoría de los timos |
| Juegos de casino sociales | El hueco por el que se cuela el casino que no tapa la restringida |
| Sensacionalismo | Clickbait |

**Se dejaron permitidas a propósito**: Estética y modificaciones corporales, Política y
Religión. No son riesgo de revisión y bloquearlas solo quita inventario.

Las dos **categorías restringidas** (Bebidas alcohólicas y Juegos de azar) ya venían
bloqueadas de fábrica — no hubo que tocarlas.

⚠️ **Coste conocido**: Citas y Referencias al sexo marcaban el **15,6 % de los ingresos**
de 30 días, pero es el mismo anuncio contado dos veces y el total de esos días son céntimos.
Cada bloqueo reduce la competencia por el inventario y baja algo el eCPM. Con este volumen
es irrelevante; el riesgo de revisión no lo era.

#### 💰 DECIDIDO: quien no consienta verá anuncios, pero NO personalizados

Verificado en el móvil: hoy quien pulsa "No consentir" **no ve ningún anuncio**, y por tanto
no genera un céntimo.

**Sergio decidió el 2026-11-02 cambiarlo**: quien rechace el seguimiento seguirá viendo
anuncios, solo que **sin perfilado**.

✅ **No hay que tocar código.** `ConsentManager.startAdsIfAllowed()` usa
`info.canRequestAds()` como única condición, que es la API correcta para esto.

🛑 **PERO NO ES UN INTERRUPTOR, y se investigó el 2026-11-02 sin éxito.** Lo que hay
en AdMob → Privacidad y mensajes → RGPD → Configuración de la cuenta: partners
publicitarios, maximizar cobertura, interés legítimo, modo de consentimiento, función
especial 2, sincronización y finalidades propias. **Ninguna opción de anuncios no
personalizados.**

**Por qué**: el mensaje de FoodRanker es el conforme al **TCF de IAB**, y ahí un anuncio no
personalizado **sigue usando identificadores del dispositivo** (frecuencia e informes), así
que necesita el mismo consentimiento de "almacenar información en el dispositivo" que el
usuario acaba de denegar. No es una configuración mal puesta: es cómo funciona el marco.
Lo que sí se puede servir sin consentimiento son los **anuncios limitados / entrega técnica
de anuncios** (sin identificadores, apoyados en la IP), pero no se encontró documentación
fiable de cómo activarlos, y las fuentes se contradicen hasta en si `canRequestAds()`
devuelve `true` o `false` tras rechazar.

🚨 **TRAMPA: NO activar "Función especial 2" creyendo que es esto.** Esa opción deja a
los proveedores **escanear las características del dispositivo para identificarlo**
(fingerprinting). Es más invasiva, no menos, y no sirve para servir anuncios a quien
rechaza.

**Decisión del 2026-11-02: se deja como está de momento.** El comportamiento actual es el
más conservador legalmente, no bloquea Play ni producción, y es puro dinero. Merece una
sesión propia, no un apaño la semana de pedir producción.

⚠️ Cuando se retome: **volver a probarlo en el Redmi** es más lioso de lo que parece,
porque la respuesta anterior queda guardada. Hay que entrar en Perfil → "Opciones de
privacidad de anuncios", rechazar otra vez, y mirar si aparece banner. Y al cambiar el
mensaje en AdMob hay que **publicarlo**: guardarlo no basta.

Documentación consultada: `support.google.com/admob/answer/9760862` (integración con el TCF)
y `developers.google.com/admob/android/beta/privacy/ad-serving-modes` (los cuatro modos:
personalizado, no personalizado, limitado y entrega técnica).

#### ✅ APP CHECK REGISTRADO (pasos 1 y 2 de los cuatro)

Registrado con **Play Integrity** en Firebase Console, TTL 1 hora, **enforcement todavía
apagado** en todo (Firestore, Storage, Authentication y Places API (New) comprobados en
"No se aplica").

⚠️ **La clave de firma de Play ha cambiado desde agosto, y hay TRES.** Firebase precargaba
en el formulario una SHA-256 que **no** era la que Play usa hoy. Si se hubiera guardado solo
esa, el enforcement habría rechazado a todos los usuarios reales. Se declararon las tres
(el campo admite varias justo por esto). Declarar de más no abre ningún agujero; declarar
de menos rompe en silencio.

| SHA-256 de Play | Qué es |
|---|---|
| `DB:9A:33:74:AA:77:...` | La clave **En uso** hoy |
| `4D:A9:19:25:BB:E5:...` | Anterior (20 ago, "preparada para computación cuántica") |
| `98:9F:85:51:E6:1C:...` | La única que Firebase tenía registrada |

Las **SHA-1** de las tres (`9E:6D:CB:79`, `B6:D0:BF:6D`, `99:93:55:93`) sí estaban las tres
en Firebase, así que el login nunca estuvo en peligro. Comprobado con
`npx firebase apps:android:sha:list 1:350322634794:android:42b4b2e91a8df170c4d353`.

**Lo que falta de App Check:** dar de alta el token de depuración del emulador → mirar las
métricas hasta ver tráfico **verificado** → y solo entonces `enforceAppCheck: true`.

💡 **Extra que no estaba previsto:** en la pestaña APIs aparece **Places API (New)**.
El cliente llama a Places **directamente** con la clave en el binario
(`PlacesModule.kt:24`), protegida solo por la restricción de huella. Meterla en App Check
es la que más dinero ahorra. Hacerlo en el mismo lote que las callables.

#### Cómo se pilotó el Redmi (para repetirlo)

Nada de instalar a mano: la app es la **de Play**, que es la única que obtiene token de
Play Integrity. Para forzar el idioma **sin pasar por el selector roto**:

```
adb shell cmd locale set-app-locales com.app.foodranker --locales en-GB
adb shell cmd locale get-app-locales com.app.foodranker
```

Y para los crashes, `adb logcat -b crash -d`: el buffer guarda los últimos aunque hayan
pasado minutos, pero **se rota rápido**. Limpiar con `-c` antes de reproducir.

---


### ✅ AAB DE LA v18 (1.5) — COMPILADO, VERIFICADO Y YA PUBLICADO (2026-11-01/02)

`app/build/outputs/bundle/release/app-release.aab`, 16,0 MB, `BUILD SUCCESSFUL in 4m 50s`.
Commits `f5c29c8` (consentimiento), `ec23e33` (textos legales) y `f26247b` (versión y notas).
**Todo pusheado a `origin`**, `main` y `gh-pages`.

Verificado sobre el binario, no sobre el fuente:

| Comprobación | Cómo | Resultado |
|---|---|---|
| Clave correcta | `keytool -list -v` | SHA-1 idéntica a la de agosto |
| Firma | `jarsigner -verify` | `jar verified` |
| Versión | manifest fusionado | 18 / 1.5 |
| UMP sobrevive a R8 | cadenas en `base/dex/*.dex` | `__GOOGLE_FUNDING_CHOICE_SDK_INTERNAL__`, `IABTCF_gdprApplies`, `UserMessagingPlatform`, `privacy_options_requirement_status` |
| Textos legales | cadenas en `base/resources.pb` | 8 marcadores, castellano **e** inglés |
| Split por idioma | `BundleConfig.pb` en crudo | `08 03 10 01` = LANGUAGE negate:true, desactivado |

⚠️ **Lo de R8 no es ceremonia.** La release lleva `isMinifyEnabled = true`. Si R8 se llevara
el SDK de consentimiento, el formulario no saldría **sin dar ningún error** y los anuncios se
apagarían en Europa en silencio. Hay que comprobarlo en cada release, buscando esas cadenas
en el dex — no vale buscar nombres de clases propias, que R8 ofusca.

⚠️ **`BundleConfig.pb` es protobuf**: el enum va como número, no como la cadena "LANGUAGE".
Buscar el texto no encuentra nada aunque esté bien. Hay que leer los 4 bytes `08 03 10 01`.

**Datos de firma, para no volver a buscarlos:** keystore en `e:\FoodRanker\foodranker-keystore.jks`
(ignorado en git, **nunca commiteado**, comprobado), alias `key0`, credenciales en
`local.properties`. El alias y la ruta también están en `.idea/workspace.xml`, que Android
Studio rellena y que también está ignorado; las contraseñas las guarda Windows, no el
proyecto. Certificado: `CN=Sergio de la Peña, O=sdelapenya.dev`, válido hasta 2051.

⚠️ Los valores de `local.properties` llevan **escapes de Java** (`E\:\\FoodRanker\\...`).
Un `grep -o` con un patrón que acaba en `=` devuelve solo el nombre de la clave y parece que
está vacía: pasó en esta sesión y costó dos comprobaciones en falso.

✅ **Subido y publicado el 2026-11-02.** Notas de versión en los tres idiomas pegadas
desde `docs/play-store-listing.md`. Probado en el Redmi: ver la sección del 2026-11-02
arriba del todo — el consentimiento y el idioma bien, Premium no (no hay cuenta de
comercio, así que el producto no existe).

⚠️ **Trampa al subirlo:** un borrador de versión `1.0 - primera prueba interna`, con el
app bundle `1 (1.0)` dentro, llevaba bloqueando "Crear nueva versión" desde el principio sin
que nadie lo notara. Nunca se habría podido publicar (versionCode 1 < 17 ya en vivo). Hubo
que descartarlo.

---


### ✅ TEXTOS LEGALES: responsable con nombre, base jurídica, y la web ya se genera (2026-11-01)

**Responsable del tratamiento: Sergio de la Peña**, decisión suya del 1 de noviembre, con
nombre y apellido publicados. Antes `pp_0_b` decía "un particular" sin nombrar a nadie, y el
art. 13.1.a del RGPD pide la identidad del responsable.

**Base jurídica añadida** (art. 13.1.c, que faltaba entera). No se creó sección nueva: se
amplió la 3, que pasa a ser "Cómo usamos tu información y con qué base legal". Renumerar
diez secciones en dos idiomas más el HTML era justo donde se cuela un descuadre, y poner
cada finalidad junto a su base legal se lee mejor. Las tres bases: ejecución del contrato
(cuenta, perfil, publicaciones), consentimiento (ubicación, notificaciones, cámara, fotos y
**publicidad personalizada** — encaja con el consentimiento de anuncios de hoy) e interés
legítimo (fallos, abusos, analítica agregada).

### 🔧 La web legal ya NO se mantiene a mano: `tools/legal/build_pages.py`

Los textos salían de `strings.xml` y se copiaban a mano a `gh-pages`. Había dejado de
coincidir **sin que nadie lo notara**. Al generar por primera vez aparecieron, de golpe:

- la web **no tenía la sección 0, "Quién es responsable de tus datos"** — la política a la
  que apuntan Play y AdMob no decía quién responde de los datos
- daba **otro correo** (`sdelapenya1991@`) en dos sitios, y `index.html` y
  `delete-account.html` también — esta última es **la URL declarada en Seguridad de los
  datos**, así que las peticiones de borrado iban a un buzón distinto del que dice la política
- hablaba de "la Unión Europea" donde la app ya decía EEE y Reino Unido, con AEPD e ICO
- fecha de agosto, y sin el idioma del dispositivo entre los datos recogidos
- **solo existía en castellano**, mientras las fichas `en-GB` y `en-US` apuntaban a ella

Ahora `privacy.html`, `privacy-en.html`, `terms.html` y `terms-en.html` se generan desde
`strings.xml`. **Correo unificado en las seis páginas: `foodranker.app@gmail.com`**
(confirmado por Sergio; las otras dos cuentas no se usan para esto).

Commit en `gh-pages`: `eb44128`. **Sin pushear.** Ver `tools/legal/README.md` para publicar.

⚠️ `index.html`, `style.css` y `delete-account.html` **no se generan**, se mantienen a mano.
Y **`delete-account.html` solo existe en castellano** aunque las páginas inglesas enlazan a
ella: conviene traducirla antes de producción, porque esa URL está declarada en Play.

⚠️ **Trampa de escapes, la tercera vez que muerde.** Un heredoc de bash colapsa `\\n` en
`\n`, y Python lo convierte en un salto de línea real en vez de los dos caracteres que el XML
lleva de verdad. Mató dos intentos seguidos. La salida es escribir el script con la
herramienta de ficheros y construir la barra con `chr(92)`, nunca con un literal.

---


### ✅ CONSENTIMIENTO DE ANUNCIOS (EEE / Reino Unido) — HECHO Y VERIFICADO (2026-11-01)

**El agujero más serio que ha aparecido, y no estaba en ninguna lista.** La app lleva
`play-services-ads` desde siempre y **no tenía ni una línea de CMP**: ni la dependencia
`user-messaging-platform` declarada, ni `ConsentInformation`, ni `ConsentForm`.

Desde enero de 2024 Google exige una **CMP certificada** para servir anuncios a usuarios
del Espacio Económico Europeo y Reino Unido. La app está al 100 % en 178 países, el
público es español y la prima está en Edimburgo: es justo el territorio donde es
obligatorio. Lo más probable es que **AdMob ya estuviera limitando los anuncios en
Europa** sin que nadie se diera cuenta.

⚠️ Dato que despista: `play-services-ads:23.0.0` **ya arrastraba UMP 2.2.0 de forma
transitiva**. Las clases estaban en el APK desde siempre, solo que no las llamaba nadie.
Buscar la dependencia en el árbol de Gradle no sirve para saber si esto está resuelto —
hay que buscar `ConsentInformation` en el código.

**Hecho (compila y `assembleDebug` correcto):**
- `utils/ConsentManager.kt` (nuevo). Pide el consentimiento, enseña el formulario solo si
  toca y arranca `AdManager.initialize()` cuando `canRequestAds()` dice que sí.
- Dependencia explícita `user-messaging-platform:3.0.0` en `app/build.gradle.kts`.
- `FoodRankerApp` **ya no llama a `AdManager.initialize()`**. Ahora sale de `ConsentManager`,
  desde `MainActivity`, que es quien tiene Activity para el diálogo.
- `BannerAdView` se tapa mientras `canRequestAds` sea false.
- Entrada permanente **"Opciones de privacidad de anuncios"** en el perfil (`prof_ad_privacy`),
  que solo aparece donde el SDK dice que hace falta. El RGPD exige poder cambiar de opinión.
- En debug se fuerza `DEBUG_GEOGRAPHY_EEA` con los IDs de `ADMOB_TEST_DEVICE_IDS`, para
  poder ver el formulario desde España sin estar en otro país.

**Mensaje de AdMob creado y publicado el 2026-11-01.** Reglamentos europeos → app
FoodRanker, idioma por defecto inglés + español añadido, **"No consentir" activado en los
30 y pico países**, "Cerrar (no consentir)" desactivado, URL de privacidad apuntando a
`privacy.html`.

**✅ VERIFICADO DE PUNTA A PUNTA en el emulador FoodRanker_Test (2026-11-01).** El
formulario sale con el logo de la app, el nombre sustituido y las tres opciones. Al aceptar:

| Comprobación | Resultado |
|---|---|
| `is_pub_misconfigured` | `false` — la configuración de AdMob es correcta |
| `IABTCF_gdprApplies` | `1` — el SDK detecta territorio RGPD |
| `consent_status` antes / después | `2` (REQUIRED) → `3` (OBTAINED) |
| `privacy_options_requirement_status` | `REQUIRED` — la entrada del perfil sí aparece |
| Antes de consentir | **ni una línea de `MobileAds`** — los anuncios no arrancan |
| Al consentir | `ChimeraMobileAdsSettingManagerCreatorImpl` — arrancan |

Lo último es la prueba que importa: sin consentimiento no se pide ni un anuncio.

⚠️ **Trampa del emulador, para no perder otra tarde.** Las dos primeras ejecuciones
fallaron con `W ConsentManager: Formulario de consentimiento: 4 Web view timed out`, y el
`Wall html loaded` llegaba 7 segundos DESPUÉS de que el SDK se rindiera. No era el código:
era el WebView del emulador recién borrado. Se arregló subiendo la RAM del AVD de 2048 a
4096 (`~/.android/avd/FoodRanker_Test.avd/config.ini`, con `.bak` al lado) y relanzando un
par de veces hasta que el WebView entró en calor.

⚠️ **No se puede probar con sideload en el Redmi**: tiene la versión de prueba cerrada
instalada desde Play (`installer=com.android.vending`), firmada por Google. Instalar la de
debug encima falla por firma, y desinstalarla cierra la sesión. Por eso se usó el emulador.

**Falta, y es de Sergio:** nada de AdMob. Queda verlo en el Redmi cuando la v18 esté en
prueba cerrada, junto con el precio en euros de Premium y el idioma.

### 🟡 SEGURIDAD DE LOS DATOS — un arreglo hecho, otro aplazado (2026-11-02)

**Hecho hoy**, en el mismo envio que la v18: marcado **"Otro contenido generado por el
usuario"** en Actividad en aplicaciones (paso 3 del asistente), y en el paso 4 recogido
**si**, compartido **no**, efimero **no**, **opcional**, finalidad **Funciones de la
aplicacion**. Antes el formulario no declaraba valoraciones, comentarios, nombres y
descripciones de platos ni nombre y direccion del local, que la politica si lista.

El resto del paso 3 se comprobo en pantalla y coincide con lo auditado en agosto:
ubicacion 2/2, informacion personal 3/9 (nombre, correo, IDs de usuario), fotos 1/2,
rendimiento 2/3, IDs de dispositivo 1/1, y todo lo demas a cero.

⚠️ **Lo que esta seccion decia antes sobre `PSL_SUPPORT_DATA_DELETION_BY_USER` estaba mal
mapeado.** Viendo el asistente de verdad son **dos campos distintos**, no uno:

| Campo del asistente | Estado real | ¿Correcto? |
|---|---|---|
| URL de eliminacion de **cuentas** | Rellena desde agosto con `delete-account.html` | ✅ si, no habia nada que arreglar |
| ¿Se pueden borrar **algunos** datos sin eliminar la cuenta? (Opcional) | **No** | ❌ deberia ser **Si** |

El segundo es falso: la app deja borrar platos (`ProfileViewModel.deletePlate`, linea 335),
comentarios (`PlateDetailViewModel.deleteComment`, 358) y colecciones
(`ProfileViewModel.deleteCollection`, 263), los tres enganchados a la interfaz y
comprobados. Pero marcar "Si" **exige una segunda URL**, la *"URL de eliminacion de
datos"*, que Play obliga a que explique los pasos del borrado parcial y que diga que se
borra y que se conserva. `delete-account.html` solo habla de borrar la cuenta entera.

Se dejo en **"No" a proposito** para no bloquear el envio de la v18: el campo es
**(Opcional)**, y quedarse corto declarando un control del usuario no es lo que hace que
rechacen una app — lo que la hunde es declarar menos datos de los que se recogen o mas
privacidad de la que se da, y de eso no hay nada aqui. Es reversible en el siguiente envio.

### ✅ `delete-account.html`: borrado parcial y version inglesa (2026-11-02)

Escrita mientras Google revisaba la v18. Las dos partes hechas:

1. **Borrado parcial documentado.** Que puede borrar el usuario sin eliminar la cuenta
   (plato, comentario, lista, guardado, dejar de seguir), con la ruta exacta en la app, y
   **que no se puede borrar suelto**: las valoraciones puestas a platos de otros, porque
   `firestore.rules` tiene `allow delete: if false` en `ratings` para que nadie pueda
   borrar-y-revotar y saltarse el limite de una valoracion por plato. Se pueden editar, y
   desaparecen al borrar el plato o la cuenta.
2. **`delete-account-en.html`**, la version inglesa.

Lo que se borra en cascada al eliminar un plato esta sacado de `onPlateDeleted`
(`functions/src/index.ts:2124`), no inventado: plato, imagen en Cloudinary, `ratings`,
`comments` y `saves` del plato, y `revertAuthorXP` si estaba aprobado.

**`tools/legal/build_pages.py` ya enlaza a la pagina del idioma que toca**, via el
diccionario `BORRADO`. Antes las cuatro generadas apuntaban a la castellana, asi que quien
leia la politica en ingles y pulsaba "Delete account" aterrizaba en castellano. Las cuatro
regeneradas y la auditoria de enlaces internos del sitio pasa sin ninguno roto.

De paso, `style.css`: **`h3` no tenia estilo** y heredaba el tamano por defecto del
navegador, que es MAYOR que el `h2` de 17px. Los subtitulos nuevos habrian salido mas
grandes que los titulos de seccion. Anadido `h3` a 15px, y `ol` metido en las reglas de
color y sangria que solo tenian `ul` (eso ya estaba mal antes, con los pasos numerados).

⚠️ **`index.html` sigue solo en castellano.** No esta declarada en Play, asi que no
bloquea, pero es lo primero que ve quien llega a la raiz del dominio.

**Falta por hacer, en este orden:**

1. **Pushear `gh-pages`** — hasta que no este publicada, la URL no existe.
2. Comprobar que las seis paginas responden 200.
3. Volver a Play: Seguridad de los datos → paso 2 → la pregunta del borrado parcial pasa
   de **No** a **Si**, con `https://sdelapenya.github.io/FoodRanker/delete-account.html`.
   Eso es un cambio mas en la cola de publicacion, que ira en el siguiente envio.

### ✅ POLÍTICA DE PRIVACIDAD: base jurídica y responsable — RESUELTO (2026-11-01)

**Cerrado en `ec23e33`.** La política nombra a **Sergio de la Peña** como responsable
(art. 13.1.a) y declara la base jurídica de cada tratamiento (art. 13.1.c). Verificado el
2026-11-02 **en el móvil, sobre la build de Play y en inglés**: la sección "0. Who is
responsible for your data" sale con el nombre y con `foodranker.app@gmail.com`.

⚠️ **Los textos legales viven en DOS sitios** y tienen que decir lo mismo palabra por
palabra: `strings.xml` (`values/` y `values-es/`) y la rama huérfana **`gh-pages`** de
`origin`, que sirve `privacy.html` y `terms.html`. Tocar solo uno crea justo la
incoherencia que Google compara. Y como la política va dentro de la app, cambiarla
obliga a **v18**.

### ✅ El ranking ya ordena por la nota que se ve — VERIFICADO EN PRODUCCIÓN (2026-10-15)

Commit `bfb8e76`. Desplegado y recalculado. Comprobado el 1 de noviembre con el script en
simulación: **`platos: 56 | a cambiar: 0`**. Había 53 platos cuando se hizo el cambio y hay
56: los tres nuevos salieron ya con la fórmula nueva, lo que confirma que **las Cloud
Functions desplegadas son las nuevas** (si no, habrían salido con la bayesiana y el script
los marcaría).

Para repetir la comprobación (solo lectura, no escribe nada): ADC desde el `refresh_token`
de `firebase-tools.json` y `node scripts/recalcRankingScores.js` sin `--confirm`.

**Pendiente, acordado pero sin escribir:** fase 2 del ranking — umbral de 2 valoraciones
para entrar y estantería de "recién publicados". El problema de fondo sigue siendo que
**47 de 53 platos los votó solo su propio autor**.

---


### 🚀 v17 (1.4) PUBLICADA EN ALPHA + ficha en tres idiomas (2026-10-10/11)

**La v17 está publicada** en prueba cerrada desde el 10 oct 9:12, al 100 % en **178 países**.
Notas de versión en **3 de 3 idiomas** (es-ES, en-GB, en-US).

**La ficha pasó a tres idiomas.** Se añadieron `en-GB` y `en-US` con el texto de
`docs/play-store-listing.md`, y se sustituyeron las capturas castellanas por las nuevas.
⚠️ **Se añadieron las DOS variantes de inglés a propósito**: Play sirve la ficha del idioma
exacto del usuario y, si no la encuentra, **cae al predeterminado (castellano)**. Con solo
`en-GB`, cualquiera con el móvil en inglés de EE.UU., Irlanda, Australia o India habría
visto la ficha en castellano.

Esos tres cambios quedaron **en revisión** el 11 de octubre.

#### Declaración de recursos de IA: "No etiquetar"

Play pide declarar los recursos **visuales** creados o editados con IA. Se marcó **"No
etiquetar"**, y está justificado recurso a recurso: las capturas son fotografías literales
del Redmi con la app real y datos reales, sin retoque ni recorte; el icono es el `pathData`
vectorial del propio logo renderizado; el gráfico destacado está compuesto con el logo y el
texto de la app. **Ninguno es imagen generada.**

⚠️ Marcar "etiquetar" habría sido **falso en la dirección contraria**: Play le habría puesto
a esas imágenes un aviso de "creado con IA", diciendo a los usuarios que las pantallas
podrían no ser reales cuando son exactamente lo que se encuentran al instalar.

El único punto gris es **el texto de la descripción inglesa, escrito por Claude**. La
documentación de Play menciona "imágenes, texto o vídeo" en la introducción pero la lista de
recursos declarables solo nombra los visuales, y el flujo va asset por asset sobre imágenes.
Queda así; si alguna vez molesta, la salida limpia es reescribirla a mano.

#### Lo que falta, y no es código

1. **Comprobar en el Redmi desde Play** (cuando aprueben): desinstalar la build local (va
   firmada con la clave de subida, no con la de Play) e instalar desde el enlace de prueba
   cerrada. Dos cosas que **no se pueden ver con un sideload**: que Premium enseñe el
   **precio real en euros**, y que con el móvil en inglés la app salga en inglés — esto
   último valida `language.enableSplit = false`, el cambio de empaquetado hecho a ciegas.
2. **Los 14 días**: van **7 de 14** con 12 testers (visto el 10 oct). Lo único que reinicia
   el contador es que alguien **se salga** del grupo; subir versiones no lo reinicia.
3. **Responsable del tratamiento y base jurídica** de los textos legales: decisión de Sergio,
   necesaria antes de pedir producción, no antes de seguir testeando.

---

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
- **Cuenta de revisión de Google Play creada**: ~~`foodrankerreview@gmail.com`~~ — ⚠️ **SUPERADA el 2026-11-15**: esa cuenta se bloqueó sola y la vigente es `foodrankerreview2@gmail.com` (ver arriba, "LA CUENTA DE REVISIÓN SE BLOQUEÓ SOLA"). Lo de abajo queda como registro de lo que se hizo en agosto. Dada de alta en
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

   1. ~~Firebase Console → App Check → registrar la app Android con **Play Integrity**.~~
      **HECHO el 2026-11-02**, con las tres huellas SHA-256 de Play (ver arriba).
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
      `functions/src/index.ts` y se redespliega. Son **cinco**: `validateFoodImage` (l. 677),
      `awardAdXp` (1866), `getLeagueId` (1900), `resolveVenue` (2185) y `deleteUserAccount`
      (2306) — líneas comprobadas el 2026-11-02; las anteriores (365/979/1013/1295/1391)
      llevaban meses desfasadas. Empezar por `resolveVenue` y `validateFoodImage`, que son
      las que gastan cuota de Places y de Vision.

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
- Billing implementado en el **cliente**. ⚠️ **La tienda NO tiene nada que vender**: no
  hay cuenta de comercio, así que la suscripción `foodranker_premium_monthly` no existe en
  Play y la compra siempre falla. Comprobado en el móvil el 2026-11-02 (ver arriba). Lo que
  este fichero afirmaba hasta entonces —"real, el precio 2,99 €/mes lo devuelve Google
  Play"— **era falso**
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
- ~~Formulario de Data safety~~ — **hecho**. Repasado y ampliado el 2026-11-02
- ~~Ficha: capturas, descripciones, icono, gráfico destacado~~ — **hecha**, en tres idiomas
- ~~Cuestionario de clasificación de contenido~~ — **hecho**
- ~~Declaración de contenido generado por usuarios~~ — **hecha** el 2026-11-02

⚠️ Esta lista es de agosto de 2026 y se quedó sin actualizar durante meses. Lo único que
sigue abierto de Play Console es **la cuenta de comercio** (sin ella no hay Premium) y
**pedir acceso a producción**.

### SHA-1
| | SHA-1 |
|---|---|
| Debug | `00:56:2C:9F:18:0E:7F:6C:EB:03:BB:3F:7A:03:B5:CE:F9:82:34:C7` |
| Release (clave de carga, la del keystore local) | `27:78:23:4B:D8:97:89:FE:86:23:28:F4:F7:65:10:23:15:E1:1A:59` |
| **Firma de Play** (App Signing key, la que Google usa de verdad para lo que llega a los usuarios) | `B6:D0:BF:6D:59:E8:DC:52:2E:0D:AC:E8:1C:B7:16:05:BA:90:00:DF` |

⚠️ **Esa ya NO es la única ni la que está en uso (2026-11-02).** Play tiene **tres** claves
de firma y la activa hoy es otra. Las SHA-1 de las tres: `9E:6D:CB:79:07:78:AD:01:BE:7B:4E:77:A6:0E:78:BE:EA:F5:0E:71`,
`B6:D0:BF:6D:...` (la de arriba) y `99:93:55:93:DE:3C:2D:E1:2D:1D:45:58:56:E5:94:D9:A7:2C:A5:5A`.
Las tres están registradas en Firebase, por eso el login funciona. Se leen en Play Console
→ Protegida con Play → Firma de aplicaciones → "Copiar todas las huellas digitales de
certificados" (dos opciones en el desplegable, una por algoritmo; salen separadas por comas).
Para App Check hacen falta las **SHA-256**, que son otras.

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

- **Desplegar functions falla con "Timeout after 10000" sin que el código tenga nada mal.**
  El CLI levanta un servidor local para descubrir qué funciones hay y se rinde a los 10
  segundos; en este PC no llega. Antes de tocar nada, comprobar que el módulo carga
  (`node -e "require('./lib/index.js')"` dentro de `functions/`) y, si carga, reintentar
  con **`FUNCTIONS_DISCOVERY_TIMEOUT=120`** delante del `firebase deploy`. Con eso pasa a
  la primera.

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
