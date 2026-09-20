# RATINGS — rediseño del sistema de valoración

**Estado (2026-09-20): SERVIDOR YA EN PRODUCCIÓN.** Desplegadas las reglas y las Cloud
Functions (incluidas las dos nuevas, `onRatingDeleted` y `refreshGlobalStats`), y **aplicada
la migración**: 53 documentos actualizados + `stats/global`. Verificado después contra
producción, solo lectura: 8/8 comprobaciones, todo coherente.

**Queda pendiente:**
- ⚠️ **Los índices NO se han desplegado** — el comando lo bloqueó el clasificador de seguridad
  (es el que puede borrar índices). Hay que ejecutarlo a mano:
  `npx firebase deploy --only firestore:indexes --project foodranker-51270`.
  Mientras no esté: la app en producción (v12) ordena por `averageScore` y **no se ve
  afectada**; lo único que falla es el job nocturno `refreshGlobalStats`, que necesita el
  índice `status + totalRatings`. No es grave — `stats/global` ya quedó escrito por la
  migración, así que solo deja de refrescarse.
- **La app v13 no está publicada** y no debe publicarse hasta tener los índices: sus consultas
  ordenan por `rankingScore`.
Auditoría del sistema anterior y sus problemas: ver `docs/HANDOFF.md`, sección
"🔍 EN CURSO: auditoría del sistema de valoraciones".

El ranking es el núcleo de FoodRanker: si la nota de un plato no significa algo defendible,
no hay producto. Este documento fija qué se valora, cómo se calcula y en qué orden se
implementa.

---

## 1. Decisiones tomadas

### 1.1 Valorar es haber comido

**Solo puede valorar quien declara haber probado el plato.** Antes de abrir el formulario se
declara "Lo he probado". Quien no lo ha probado **da like**, que pasa a significar "me
apetece" — el mecanismo ya existe (`Plate.likes`/`likedByUsers`) y hoy compite con el voto
por el mismo gesto.

**Por qué**: hoy no hay ninguna comprobación (`PlateDetailViewModel.submitRating` solo mira
que no hayas votado ya), así que cualquiera puede puntuar un plato desde una foto. Con Sabor
y Presentación el absurdo no se ve; con Satisfacción se ve a la legua — es imposible saber si
te quedaste con hambre mirando una pantalla. Limitar solo ese eje sería peor: dejaría a gente
que no lo ha probado moviendo la nota igual, y crearía dos tipos de voto no comparables.

Con esto, cada señal recupera su significado: **el like es deseo, la nota es experiencia**, y
la nota del plato pasa a significar *lo que opinan los que se lo comieron*.

**Verificación por GPS: marca, nunca bloquea.** Si el usuario está o ha estado en el local
(ya hay permiso de ubicación y Places por "Qué pido aquí"), su voto se marca como verificado
y se muestra el distintivo. Es el modelo de "compra verificada" de Amazon. **No se exige**:
bloquear por GPS se carga los votos legítimos de quien valora al llegar a casa, que serán la
mayoría.

La declaración sola no garantiza nada, y se acepta: la gente no suele mentir cuando se le
pregunta de frente, y quien fuera a mentir lo haría con cualquier sistema que no sea un ticket.

### 1.2 Tres ejes, con pesos desiguales

| Eje | Peso | Escala |
|-----|------|--------|
| **Sabor** | **50 %** | 1–10, paso 0,5 |
| **Presentación** | **20 %** | 1–10, paso 0,5 |
| **Satisfacción** | **30 %** | 1–10, paso 0,5 |

**Sale `valueScore` (Precio/Calidad), entra `satisfactionScore`.**

**Por qué se va Precio/Calidad**: era una opinión *sobre un hecho que la app desconocía*, y
por eso ambiguo hasta para quien lo rellenaba — un 8 ahí no distingue "es barato" de "es caro
pero lo vale". Ese eje ambiguo arrastraba un tercio entero de la nota. El precio pasa a ser un
dato objetivo (§1.3) y la síntesis calidad-precio la da el binario de §1.4.

**Por qué Satisfacción y no "cantidad"**: es el único eje donde *más no es mejor* — una ración
desmesurada que no te acabas no merece nota alta, así que una escala de "escasa" a "excesiva"
tendría el óptimo en el centro y **no sería sumable** a una media donde 10 = mejor. Formulado
como **"¿te quedas satisfecho?"** (1 = te quedas con hambre, 10 = sales perfectamente), el 10
vuelve a ser el óptimo, es promediable, y captura la queja real en restauración española. La
ración excesiva deja de penalizarse: caso raro y mucho menos dañino que el contrario.

**Por qué la presentación baja de 33 % a 20 %**: con el tercio actual se castiga
sistemáticamente a la cocina de cuchara y al guiso feo pero brutal, y se premia el plato de
diseño mediocre.

```
nota = 0.50 · sabor + 0.20 · presentación + 0.30 · satisfacción
```

### 1.3 El precio es un dato, no una opinión

**Euros exactos**, no tramos. **Obligatorio al subir el plato, opcional al valorar.**

**Por qué euros y no €/€€/€€€**: la diferencia no es de precisión, es de qué puedes construir
después. Con tramos no puedes comparar dos platos del mismo tramo, ni calcular calidad-precio,
ni decir "está 4 € por encima de la media de tu ciudad"; envejecen con la inflación; y **eso ya
lo tiene Google Maps a nivel de local**. Con euros exactos tienes un dato agregable, y el
conjunto *cuánto cuesta realmente cada plato en cada sitio* es el activo que ninguna otra app
tiene, porque Google pregunta por locales, no por platos.

**Por qué obligatorio solo al subir**: quien sube acaba de comer y tiene el ticket o la carta
delante — máxima calidad del dato. A quien vota después se le muestra el precio ya conocido
para **confirmarlo de un toque o corregirlo** si ha cambiado; volver a pedírselo en frío es
fricción para un dato que ya está, y produce números inventados.

Se pregunta algo inequívoco: **el precio que pagaste por este plato** (no el menú, no la mesa).
Se aceptan decimales sin exigirlos.

**Los precios falsos tienen tres causas y tres remedios distintos:**

- **Error honesto** (120 en vez de 12) → **mediana**, nunca media: un valor absurdo entre
  cuatro correctos no la mueve un céntimo. Más validación de rango en el campo.
- **Precio caducado** → se guarda **con fecha** y se prioriza lo reciente.
- **Manipulación** (un hostelero abaratando su plato) → **un solo reporte nunca manda**:
  mediana de varios, más "reportar precio incorrecto".

**No hay forma de garantizar precios reales, y no hace falta** — esto no es contabilidad, es un
orden de magnitud fiable. La clave es que **la interfaz no prometa más de lo que sabe**: con un
reporte, "1 persona pagó 12 €" es verdad y "Precio: 12 €" es mentira; con cinco, ya se puede
decir "suele costar unos 12 €".

### 1.4 "¿Lo volverías a pedir?" — el modelo Rotten Tomatoes

Binario Sí/No al final del formulario. Se agrega como **porcentaje**: "el 87 % lo volvería a
pedir".

**Qué aporta**: una media responde *cuánto* le gustó a la gente; un porcentaje responde *a qué
proporción* le gustó. La segunda suele ser más útil para decidir, y además:

- **Es legible sin explicación** — "87 %" se entiende al instante; "7,2" no (¿bueno comparado
  con qué?).
- **Anula el problema de calibración** — hay quien nunca baja del 8 y quien reserva el 9 para
  su abuela; en una media esos dos se contradicen aunque opinen lo mismo, en un binario solo
  cuenta si cruzaron el umbral.
- **Es una decisión, no un gusto abstracto** — es literalmente la pregunta que te haces delante
  de una carta.

**Su debilidad conocida, asumida a propósito**: pierde la intensidad. Un plato correcto que
nadie odia puede salir mejor parado que uno excepcional y polarizante (el caso clásico de RT:
todos un 6 → 100 %; mitad dieces y mitad treses → 50 %). **Por eso el porcentaje no sustituye a
la nota, la acompaña** — igual que RT enseña el Tomatómetro y la media al lado. La nota dice
cuánto de bueno es; el porcentaje, cuánta gente lo repetiría.

**Dos cautelas**:
- Con pocos votos el porcentaje es grotesco (1 voto = 100 % o 0 %) → **el porcentaje solo se
  muestra a partir de 5 votos**; por debajo se enseña la fracción ("4 de 5 lo repetirían").
  Ver §5.
- **No entra en el ranking** de momento: solo se muestra y, como mucho, desempata. Si entrase
  en la fórmula de orden desde el principio, no habría quien explicara por qué un plato está
  donde está.

### 1.5 Ranking: media bayesiana

El ranking deja de ordenar por `averageScore` y pasa a ordenar por un campo nuevo,
`rankingScore`, calculado con media bayesiana (el modelo de IMDb):

```
rankingScore = (v / (v + m)) · R  +  (m / (v + m)) · C

  v = totalRatings del plato
  R = averageScore del plato (la nota ponderada de §1.2)
  m = votos mínimos para "confiar" en la nota  → arrancar en 5
  C = media global de todos los platos aprobados
```

**Por qué**: hoy un plato con 1 voto de 10,0 va por delante de uno con 40 votos de 9,7, y ese
primer voto es **siempre el del propio autor** — cualquiera puede publicar, auto-puntuarse
10/10/10 y encabezar el ranking de su ciudad. Con 9 usuarios no se nota; con 200 el ranking
pierde toda credibilidad.

**`averageScore` no se toca**: sigue siendo lo que se muestra al usuario. `rankingScore` solo
ordena. Separar "la nota" de "el orden" es lo que permite que la nota siga siendo legible.

**Esto resuelve de paso el auto-voto del autor**: en cuanto un voto solo no puede encabezar
nada, auto-puntuarse un 10 deja de servir. **Por eso el voto del autor no recibe trato
especial** — sigue contando como uno más.

**Y por eso se filtra en vez de ponderar** (§1.1): la tentación es dar más peso al voto
verificado por GPS y menos al de quien votó desde el sofá. No se hace. Ponderar es opaco (nadie
entiende por qué un plato está donde está), crea usuarios de segunda que dejan de votar cuando
lo descubren, y añade un parámetro que hay que justificar para siempre. Con "solo vota quien lo
ha probado", **todos los votos valen igual** y la regla cabe en una frase.

`C` necesita vivir en algún sitio: documento `stats/global` recalculado por una función
programada (con ~30 ratings en producción, a diario sobra).

---

## 2. Modelo de datos

### `ratings/{plateId}_{userId}`

| Campo | Cambio | Notas |
|-------|--------|-------|
| `flavorScore` | = | 1–10 |
| `presentationScore` | = | 1–10 |
| `satisfactionScore` | **nuevo** | 1–10 — "¿te quedas satisfecho?" |
| `valueScore` | **deja de escribirse** | se conserva en documentos antiguos, ya no se usa |
| `wouldOrderAgain` | **nuevo** | `boolean` |
| `pricePaidCents` | **nuevo** | `int?` en céntimos (enteros, nunca coma flotante para dinero) |
| `verifiedAtVenue` | **nuevo** | `boolean` — GPS coincidía con el local |
| `averageScore` | fórmula nueva | ahora ponderada 50/20/30 |
| `userName`, `userPhotoUrl`, `comment`, `createdAt` | = | |
| `processed`, `league*` | = | solo servidor |

### `plates/{venueId}__{dishSlug}`

| Campo | Cambio | Notas |
|-------|--------|-------|
| `averageScore` | = | lo que se **muestra** |
| `totalRatings` | = | |
| `rankingScore` | **nuevo** | lo que **ordena** (§1.5) |
| `wouldOrderAgainCount` | **nuevo** | numerador del porcentaje (los que dijeron que sí) |
| `wouldOrderAgainResponses` | **nuevo** | **denominador**: cuántos contestaron. No vale `totalRatings` — las valoraciones antiguas no contestaron y hundirían el % |
| `priceMedianCents` | **nuevo** | mediana de los precios reportados |
| `priceReportCount` | **nuevo** | cuántos lo han reportado — gobierna qué se enseña (§1.3) |
| `priceUpdatedAt` | **nuevo** | para "visto en {fecha}" |

Todos los campos nuevos del plato los escribe **solo Cloud Functions vía Admin SDK**, igual que
`averageScore`/`totalRatings` hoy. `firestore.rules:131` ya lo deja explícito; hay que extender
el comentario y asegurarse de que el bloque `allow update` de `plates` (rules:100-132) **no**
los admite desde el cliente.

---

## 3. Migración de los datos existentes

En producción hay ~30 valoraciones reales y 9 usuarios (fase de prueba cerrada), así que la
migración es barata — pero hay que hacerla, porque la fórmula de la nota cambia.

**`satisfactionScore` no existe en los ratings antiguos y NO se puede rellenar con
`valueScore`**: significan cosas distintas (precio/calidad vs. saciedad), copiarlo sería
inventar datos. Para esos documentos se **renormalizan los pesos sobre los ejes disponibles**:

```
nota_antigua = (0.50 · sabor + 0.20 · presentación) / 0.70
```

Matemáticamente correcto y no inventa nada. Después se recalculan `averageScore` y
`rankingScore` de todos los platos aprobados.

**El precio queda vacío** en los platos ya existentes hasta que alguien lo aporte → hace falta
una entrada "añadir precio" para platos ya publicados, no solo en el alta.

Script nuevo en `functions/scripts/` siguiendo el patrón ya establecido (simulación por
defecto, `--confirm` para escribir de verdad), como `backfillUserNames.js`.

---

## 4. Plan de implementación

### ✅ Fase 0 — HECHA (2026-09-19)

Corregir la carga de valoraciones del detalle: `PlateDetailViewModel.kt:110-113` hace
`whereEqualTo("plateId").limit(50)` **sin `orderBy`**, así que con más de 50 votos Firestore
devuelve 50 arbitrarias y `hasUserRated` (`:189`) puede dar falso negativo — el usuario vería
el botón de valorar y al pulsarlo recibiría "Ya valoraste este plato". **El índice ya existe**
(`firestore.indexes.json:69-76`, `plateId+createdAt`). No depende de nada de este rediseño.

**Cómo se arregló, que fue más de lo previsto**: el `orderBy` **por sí solo no bastaba**. Con
más de 50 votos, el del propio usuario puede seguir quedando fuera de la página y
`hasUserRated` daría falso negativo igual. Así que además se lee el rating propio **aparte, por
su id determinista** (`ratings/{plateId}_{uid}`) y se fusiona en la lista si no venía. Lectura
puntual, barata y siempre correcta.

### ✅ Fase 1 — IMPLEMENTADA EN LOCAL, SIN DESPLEGAR (2026-09-19)

Todo esto va junto porque toca las mismas pantallas y **el cambio de `orderBy` exige versión
nueva de la app de todas formas**: separarlo obligaría a tocar el mismo formulario dos veces.

#### ⚠️ Convivencia de formatos y orden de despliegue — leer antes de empezar

Descubierto al empezar a implementar (2026-09-19). **No hay un estado intermedio coherente en
el cliente**: o escribe el formato viejo o el nuevo, así que el modelo, los ViewModels y las
dos pantallas se cambian en un solo bloque o el proyecto ni compila.

Y hay dos restricciones duras que condicionan el orden:

1. **Las reglas actuales rechazarían los ratings nuevos.** `firestore.rules:149-150` exige
   `valueScore >= 1 && <= 10` en **cada `create`**. En cuanto el cliente deje de enviarlo,
   Firestore deniega **todas** las valoraciones. Las reglas nuevas tienen que estar
   desplegadas **antes** de que salga la app.
2. **Los dos formatos van a convivir semanas.** Quien siga en v12 mantiene el formato antiguo
   hasta que actualice, y en Play no se puede forzar. Por tanto, **durante la transición**:
   - las reglas deben aceptar los dos formatos — `satisfactionScore`/`wouldOrderAgain` **no**
     pueden ser obligatorios todavía, y `valueScore` debe seguir permitido;
   - `onRatingCreated`/`onRatingUpdated` deben tratar el rating sin `satisfactionScore` con la
     renormalización de §3, exactamente igual que los históricos.

   Endurecer las reglas (exigir los campos nuevos, prohibir `valueScore`) es un paso posterior,
   cuando el parque de instalaciones haya rotado.

**Orden de despliegue, sin atajos:** reglas permisivas con ambos formatos → Cloud Functions →
migración de los datos existentes → versión nueva de la app → (semanas después) endurecer
reglas.

#### Estado real de cada pieza (todo en local, nada desplegado)

| Pieza | Estado |
|-------|--------|
| `firestore.rules` | ✅ Validadas contra el emulador: 18 comprobaciones |
| `functions/src/index.ts` | ✅ Compila (`npm run build`), sin desplegar |
| `functions/scripts/migrateRatings.js` | ✅ **Ejecutada de verdad contra el emulador** con datos de los dos formatos: 14 comprobaciones |
| `firestore.indexes.json` | ✅ Índices de `rankingScore` añadidos **sin quitar ninguno** |
| Cliente (modelos, VMs, UI) | ✅ Compila (`compileDebugKotlin`) |
| Verificación en dispositivo | ❌ **Sin hacer** — la UI no se ha visto funcionando de verdad |

**Cómo se validaron las reglas** (repetible): el emulador exige **Java 21+** y el JDK del PATH
es el 17, pero Android Studio trae uno válido. Desde `e:\FoodRanker`:

```bash
export PATH="/c/Program Files/Android/Android Studio/jbr/bin:$PATH"
npx firebase emulators:exec --only firestore --project foodranker-rulestest "node <ruta>/test.mjs"
```

El test usa `@firebase/rules-unit-testing` y cubre lo que de verdad da miedo: que el formato
viejo (v12) y el nuevo se acepten **los dos**, que no se puedan sembrar agregados desde el
cliente, y que los rangos de precio/escala rechacen lo que deben. El script vive en el
scratchpad de la sesión (no persiste) — si hace falta repetirlo, se reescribe.

**Sobre `verifiedAtVenue`**: lo escribe el cliente, así que un cliente manipulado podría
marcarse como verificado. Se acepta a propósito: es un distintivo visual que **no pondera
nada** (ni nota ni ranking), y verificarlo de verdad en servidor exigiría mandarle la
ubicación, que es justo lo que se quiere evitar.
✅ **Implementado y probado (2026-09-20).** `VenueRepository.isAtVenue(lat, lng)` compara la
posición actual con la del local usando `GeoUtils.haversineMeters` y el radio de
`Rating.VENUE_RADIUS_METERS` (200 m). Lo usan `PlateDetailViewModel.submitRating` (al valorar)
y `AddPlateViewModel.submitPlate` (al publicar, donde el autor suele estar allí). La UI pinta
**"📍 en el local"** junto al nombre en la lista de valoraciones.

**No pide el permiso a propósito**: si no está concedido, `isAtVenue` devuelve false y el voto
sale sin verificar. Pedirlo justo al valorar metería fricción en la acción más valiosa de la
app, y el distintivo no vale tanto. Vive en `VenueRepository` (no duplicado en cada ViewModel)
porque allí ya estaban `currentLocation()` y el `Context`.

**Cómo se probó, porque el emulador lo pone difícil**: el GPS del AVD se quedó congelado en
40.4168,-3.7038 y no hubo forma de moverlo (`adb emu geo fix` y la consola por telnet
devuelven OK pero no actualizan nada). Como esa posición está a **433,7 m** del local de
pruebas, el primer intento dio `false` — que era **lo correcto**. Para probar el camino
positivo se subió el radio a 500 m temporalmente, se repitió el voto (`verifiedAtVenue: true`
y distintivo en pantalla) y se restauró a 200. Los dos caminos quedan verificados.

✅ **Check-in de 24 h implementado (2026-09-20).** Sin él, la verificación solo pillaba a quien
valoraba con el plato delante — la minoría. Lo normal es salir del restaurante y escribir la
valoración luego, y entonces el GPS ya no coincide.

`VenueCheckInStore` (`utils/`) anota por qué locales ha pasado el usuario y `isAtVenue` lo
consulta **antes** de mirar el GPS:

1. ¿Check-in por ese local en las últimas 24 h? → verificado, **sin encender el GPS**.
2. Si no, ¿está allí ahora mismo (≤ 200 m)?

**Coste: cero.** Los check-ins se registran en `NearbyDishesViewModel` con lo que "Qué pido
aquí" **ya tiene resuelto** — esa pantalla ya pide la ubicación y ya consulta `venues` en
Firestore. Ni una llamada más a Places, a Firestore ni al GPS; de hecho ahorra, porque el
camino 1 evita la lectura de ubicación. Solo se anotan los locales a ≤ 200 m, no todos los que
salen en pantalla (que llegan a 550 m, o 1.600 m con Premium).

**Almacenamiento local a propósito** (`SharedPreferences`): el dato solo alimenta un distintivo
que no pondera nada, y guardarlo en el servidor sería registrar por dónde se mueve la gente.
Se pierde al cambiar de móvil y no pasa nada. Los caducados se purgan al registrar nuevos.

**Cómo se probó**, ya que el GPS del emulador está clavado a 433,7 m del local: (1) radio a
500 m → se abre "Qué pido aquí" y se comprueba que el check-in queda escrito en
`venue_checkins.xml`; (2) radio de vuelta a **200 m** y se reinstala conservando datos, con lo
que **el GPS ya no puede verificar**; (3) se vota → `verifiedAtVenue: true`. Como por GPS era
imposible, la verificación solo pudo venir del check-in.

#### 🐛 El bug que solo apareció en el móvil: `valueScore: 0` (2026-09-20)

**Con las reglas ya desplegadas, NADIE podía valorar.** Al pulsar "Publicar" en el emulador
Android, logcat soltaba:

```
Write failed at ratings/<plateId>_<uid>: PERMISSION_DENIED
```

**Causa**: un data class de Kotlin **serializa TODOS sus campos**. `Rating.valueScore` es
`Float = 0f` no nulable, así que el cliente nuevo lo sigue mandando con un **0** aunque el eje
esté retirado. La regla pedía que, si `valueScore` venía, estuviese en 1..10 — y 0 no lo está,
así que rechazaba **todas** las valoraciones.

**Arreglo**: `legacyScoreOk()`, que acepta ausente, nulo, **0** ("no aplica") o 1..10.

**Por qué no lo cazaron las pruebas del emulador de reglas**: los documentos del test se
construían a mano, poniendo solo los campos del formato nuevo. Es decir, probaban *lo que yo
creía que mandaba el cliente*, no lo que manda de verdad. El test ya incluye un caso con el
documento **exacto** que serializa el data class, `valueScore: 0` incluido.

**Regla para la próxima**: al validar en reglas un campo que el cliente serializa desde un data
class, hay que contar con su **valor por defecto** (0, "", false), no solo con su ausencia. Y
un test de reglas debe replicar el documento real, campo por campo.

#### ⚠️ Trampa de índices: la DIRECCIÓN importa (2026-09-20)

Al probar contra producción las consultas reales de la v13, todas las de `orderBy` explícito
funcionaron, pero falló la del **puesto en la ciudad**
(`PlateDetailViewModel`: `status == approved AND city == X AND rankingScore > N`).

Motivo: esa consulta **no lleva `orderBy`**, y en ese caso Firestore ordena implícitamente por
el campo del rango en **ASCENDENTE**. El índice se había creado con `rankingScore DESCENDING`,
que sirve para los rankings pero **no** para esto — y un índice compuesto solo se puede
recorrer invertido si se invierten *todos* sus campos, cosa que aquí no encaja.

Solución: un índice aparte con `status ASC, city ASC, rankingScore ASC`. Los dos conviven.

**Regla para la próxima**: una consulta con `where(campo >)` y sin `orderBy` necesita ese campo
**ascendente** en el índice. Probablemente esto lleva roto desde siempre con `averageScore`
(nunca hubo un `status+city+averageScore`), que es por lo que el badge "#N en tu ciudad" no se
veía nunca: la consulta tiene un `catch` que devuelve 0 en silencio.

#### Hallazgos de la revisión del diff (2026-09-19)

Dos correcciones que salieron al repasar el código, ya aplicadas:

1. **La nota guardada en el rating no se reescribía.** El servidor recalcula la nota para el
   plato pero el documento del rating conservaba la que mandó el cliente — y un cliente
   antiguo la calcula con la fórmula vieja. La lista de valoraciones habría enseñado un número
   distinto del que ese voto aporta. Ahora `onRatingCreated` y `approveplate` la reescriben.
2. **Precio fantasma.** Si se borraba (o se vaciaba) la única valoración que aportaba precio,
   `refreshPriceAggregate` salía sin tocar nada y el plato seguía enseñando el precio de algo
   que ya no existía. Ahora limpia el agregado cuando no queda ningún precio.

Y dos efectos que conviene conocer, ninguno negativo:

- **Los platos pendientes desaparecen del ranking, y está bien.** La migración no les escribe
  `rankingScore` (no están aprobados), y una consulta ordenada por ese campo excluye a quien no
  lo tiene. Antes, con `averageScore = 0`, aparecían al final de la lista pese a no haber
  pasado la moderación de Vision API.
- **El "puesto en la ciudad" puede empezar a funcionar.** Esa consulta necesita un índice
  `status + city + <campo de orden>` que **no está en `firestore.indexes.json`**, así que hoy
  lo más probable es que falle en silencio (tiene un `catch` que devuelve 0) y el badge nunca
  salga. El índice nuevo sí se ha añadido, así que puede aparecer un "#N en Toledo" en la
  tarjeta de compartir que antes no se veía. No es un fallo: es algo roto desde antes que se
  arregla de paso.

**Sobre el alcance real de la bayesiana, comprobado con números**: no invierte cualquier
diferencia, solo la que no está respaldada por votos. Con `m = 5`, 40 votos de 9,5 **ganan** a
un único 10 (verificado). Pero 2 votos de 7,8 **no** ganan a un único 10, y es correcto: dos
votos tampoco son mucha evidencia. Si se quisiera penalizar más agresivamente a los platos con
poquísimos votos, la palanca es subir `m`.

**Servidor / datos:**
1. `firestore.rules` — validar los campos nuevos en `create`/`update` de `ratings`
   (rules:138-164), quitar `valueScore` de las validaciones obligatorias, impedir que el
   cliente escriba los campos nuevos de `plates`.
2. `functions/src/index.ts` — `onRatingCreated` (:571-679) y `onRatingUpdated` (:764-804) con
   la fórmula ponderada; agregación de `wouldOrderAgainCount`, mediana de precio y
   `rankingScore`; `approveplate` (:417-486) siembra con la fórmula nueva; función programada
   para `stats/global`.
3. **`onRatingDeleted`** — no existe hoy, y por eso borrar ratings con el Admin SDK deja la
   media obsoleta (hueco ya documentado en `wipe-content`). Con más agregados que mantener,
   toca cerrarlo aquí.
4. Script de migración (§3).
5. `firestore.indexes.json` — duplicar con `rankingScore` los 4 índices que hoy usan
   `averageScore` (`:11-51`). ⚠️ **Cuidado con la mina ya documentada**: desplegar índices borra
   los que no estén en el fichero.

**Cliente:**
6. `Rating.kt` / `Plate.kt` — campos nuevos y `computeAverage()` ponderada.
7. `RatingBottomSheet` (`PlateDetailScreen.kt:1125-1234`) — tercer slider, binario, precio
   confirmable, y la declaración "Lo he probado" como paso previo.
8. `AddPlateScreen.kt:552-575` y `AddPlateViewModel.kt:305-355` — mismos ejes + precio
   obligatorio. **Quien sube rellena exactamente lo mismo que quien vota**, salvo el precio: si
   el autor rellenara campos distintos, su voto no sería comparable con el resto.
9. Pantallas que ordenan: cambiar `orderBy("averageScore")` por `rankingScore` en
   `DiscoverViewModel.kt:127/181/240`, `PlateRepository.kt:20/39`, `ExploreViewModel.kt:155`,
   `TrendingViewModel.kt:50/61`, y el puesto en la ciudad de `PlateDetailViewModel.kt:161-171`.
10. Mostrar el `%` de repetición junto a la nota (a partir de 5 votos) y el precio con su
    matiz de confianza (§1.3).
11. **El like pasa a significar "me apetece"** — revisar su texto/icono y el bloqueo del voto
    a quien no declara haberlo probado.

### Fase 2 — pendiente de decidir, no bloquea

- **Valoración rápida en el feed: el código muerto YA SE BORRÓ (2026-09-19).**
  `DiscoverViewModel.submitRating`/`submitRatingAnalytics` no los llamaba nadie, y mantenerlos
  obligaba a duplicar en ellos todo el modelo nuevo (satisfacción, binario, precio) para algo
  que no se ejecuta. Se fueron también `ratingFeedback` y `clearRatingFeedback`, que se
  quedaban sin quien los escribiera. Git lo conserva si algún día se quiere recuperar.
  **Efecto colateral a decidir**: la **misión diaria de votos** llamaba a `incrementVote()`
  desde ahí dentro, así que ya no avanza — pero es que **tampoco avanzaba antes**, porque esa
  función nunca se ejecutaba. `DailyMissionManager` se deja intacto. Hay que decidir si la
  misión diaria se reengancha al flujo real de valoración (el del detalle) o se retira.
- Deriva de coma flotante de `onRatingUpdated` y job de reconciliación.
- `ratings` es de **lectura pública sin auth** (`firestore.rules:140`) mientras `comments` sí
  exige `isSignedIn()` (:168) — inconsistente.

---

## 5. Decisiones cerradas (2026-09-19)

**`m` (votos mínimos de la bayesiana) = 5.** En constante compartida cliente/servidor, junto a
las `XP_*` que ya se duplican en `functions/src/index.ts:38-44` y `RewardManager.kt:6-11`.
Importa entender qué hace y qué no: **entre platos con el mismo número de votos, la bayesiana
conserva el orden exacto** — solo cambia el orden entre platos con distinto `v`. Así que un `m`
alto no "aplana" el ranking, solo impide que un plato con 1 voto adelante a uno con 20.
Revisar cuando la mediana de votos por plato pase de ~10; con el volumen de hoy, 5 es agresivo
a propósito, que es justo lo que hace falta contra el auto-voto.

**Umbral del `%` de repetición = 5 votos, pero por debajo se enseña la fracción.** Ocultarlo
del todo mataría la función en el lanzamiento: con ~30 ratings en producción, casi ningún plato
llegaría al umbral. Por debajo de 5 se muestra **"4 de 5 lo repetirían"** — con pocos datos, la
fracción es honesta y legible, mientras que un "80 %" sacado de 5 votos aparenta una precisión
que no existe. A partir de 5, porcentaje.

**GPS: verificación en el momento de valorar, radio 200 m, sin tracking en segundo plano.**
200 m cubre el error de GPS urbano más el tamaño del local sin abrir la mano de más. **No se
implementa seguimiento en segundo plano** — es coste de batería, permiso nuevo y un problema de
privacidad, para un distintivo que no bloquea nada. Con eso, quien valora desde casa
simplemente no sale verificado, y es correcto.
Como refuerzo gratis: **"Qué pido aquí" ya pide GPS en el local**, así que esa consulta puede
dejar un check-in implícito que valide el voto durante **24 h** — cubre el caso normal de
valorar al llegar a casa sin añadir ni un permiso.

**`valueScore`: se conserva, no se muestra, no se borra.** Borrarlo no aporta nada y perdería
la posibilidad de reconstruir si la fórmula nueva resulta estar mal calibrada. Mostrarlo en las
valoraciones antiguas confundiría — sería un eje que ya no existe y que nadie puede comparar
con los votos nuevos.
