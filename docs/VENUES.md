# Identidad canónica plato + local

**Estado:** propuesta, sin implementar
**Fecha:** 2026-08-09
**Decisión tomada:** Google Places como *resolutor*, colección propia `venues` como *almacén*

---

## El problema

`Plate` guarda `restaurantName` y `name` como texto libre y cada publicación crea un
documento nuevo. Nada impide que "Carbonara" en "Da Luigi" exista ocho veces, una por
usuario que la publique. Cada copia acumula sus propias valoraciones, así que el ranking
nunca converge y la pregunta que la app quiere responder — *¿qué pido en este sitio?* —
no llega a tener respuesta.

Son dos problemas distintos y se resuelven de forma diferente:

- **El local**: existe una fuente canónica externa (Google Places, `place_id` estable).
- **El plato dentro del local**: no existe fuente canónica. Lo resuelve la UX, no el algoritmo.

## Momento

Producción está a **cero platos**. Este cambio no necesita migración, ni backfill, ni
convivencia de dos modelos. Con usuarios reales el mismo cambio es un proyecto incómodo.

---

## Modelo de datos

### Nueva colección `venues/{placeId}`

El id del documento **es** el `place_id` de Google.

```
{
  id: string,           // place_id (== doc id)
  name: string,         // nombre canónico según Places
  address: string,
  city: string,         // como la devuelve Places
  cityNormalized: string, // lowercase sin acentos — para liga y ranking por ciudad
  country: string,
  lat: number,
  lng: number,
  geohash: string,      // para "cerca" por distancia (fase 2)
  plateCount: number,   // denormalizado
  createdAt: number
}
```

### Cambios en `plates`

Campos nuevos:

- `venueId: string` — el `place_id`
- `dishSlug: string` — nombre del plato normalizado

**Id de documento determinista: `{placeId}__{dishSlug}`**

Esto es lo que hace que el deduplicado lo imponga Firestore y no la buena voluntad del
usuario: dos personas añadiendo "Carbonara" en el mismo local escriben en el mismo
documento, quieran o no.

Se conservan `restaurantName`, `city`, `country`, `latitude` y `longitude` — ahora
rellenos desde el venue — para no reescribir las pantallas actuales de golpe.

### Normalización del slug

```
minúsculas -> quitar acentos (NFD + descartar diacríticos) -> quitar puntuación
-> colapsar espacios en "-" -> trim
```

**No** se quitan artículos ni palabras vacías: "la carbonara" y "carbonara" son
técnicamente distintos y fusionarlos automáticamente puede unir platos que no lo son.

**Limitación asumida**: el slug solo detecta duplicados exactos tras normalizar.
"Carbonara" y "Spaghetti carbonara" siguen siendo dos platos. El slug es la red de
seguridad; **quien evita los casi-duplicados es la UI de sugerencias**, mostrando lo que
ya existe en ese local antes de dejar crear nada.

---

## Flujo nuevo de AddPlate

### Paso 1 — ¿Dónde estás?

- **Camino rápido**: botón "Usar mi ubicación" → Places *Nearby Search* (radio ~100 m) →
  lista de locales cercanos. Es la vía natural: quien fotografía un plato está
  físicamente en el restaurante, así que el local es uno de los cinco más próximos.
  Una sola llamada, sin sesión de tecleo.
- **Alternativa**: buscar por texto (*Autocomplete*), para quien no dé permiso de
  ubicación o publique después.

Al elegir local se resuelve el venue (ver "Resolución de venues" abajo).

### Paso 2 — ¿Qué plato?

Consulta `plates where venueId == placeId` y muestra los ya registrados con foto y nota.

- Toca uno existente → va directo a **valorarlo**. No crea nada.
- "Añadir plato nuevo" → nombre, foto, categoría, descripción.

Este es el cambio de producto que importa: la app pasa de *"publico mi plato"* a
*"valoro un plato de aquí"*. El segundo usuario ya no crea contenido, aporta un voto
sobre algo que existe — mucha menos fricción, y es lo que hace converger los rankings.

### Paso 3 — Tu valoración

Sin cambios respecto a hoy.

---

## Resolución de venues: por Cloud Function, no desde el cliente

El cliente **no escribe** `venues` directamente. Nueva callable `resolveVenue(placeId)`:

1. Recibe un `place_id`.
2. Llama a *Place Details* con una clave de **servidor** (no viaja en el APK).
3. Escribe `venues/{placeId}` con `merge`.
4. Devuelve el venue al cliente.

**Por qué**: el documento de venue es la identidad canónica que ven todos los usuarios.
Si el cliente pudiera escribirlo, cualquiera podría renombrar "Bar Manolo" a algo
ofensivo y afectaría a todo el mundo. Resolviéndolo en servidor, el nombre y la dirección
siempre vienen de Google. Coste: una llamada extra por **local nuevo**, no por plato.

La clave del cliente queda limitada a *Autocomplete* y *Nearby Search*, que solo
devuelven candidatos.

---

## Clave de API de Places

Va en `local.properties` → `BuildConfig`, igual que el resto de config local. Es
extraíble del APK — como cualquier clave de cliente — así que la protección es:

1. **Restringirla a apps Android**: package `com.app.foodranker` + los SHA-1 de debug,
   de release y **de Play App Signing** (los tres).
2. **Restringirla solo a la Places API**.
3. **Poner un tope de cuota diario** en Cloud Console. Esta es la protección real contra
   una clave filtrada: acota el gasto máximo pase lo que pase.
4. Alertas de facturación.

La clave de **servidor** para `resolveVenue` no se restringe por app, sino que vive en la
configuración de la función y nunca sale del backend.

---

## Reglas de Firestore

- `venues`: lectura para autenticados; **escritura denegada al cliente** (solo Admin SDK
  desde `resolveVenue`).
- `plates`: la regla actual tiene una **lista blanca explícita de campos**
  (`changedKeys().hasOnly([...])` y el `hasOnly` del create). Hay que añadir `venueId` y
  `dishSlug` o toda creación de platos empezará a fallar.
- Conviene validar en la regla que `venueId` no esté vacío al crear.

---

## Fases

**Fase 1 — el núcleo**
Colección `venues`, callable `resolveVenue`, selector de local en AddPlate (nearby + texto),
sugerencia de platos del local, ids deterministas, reglas actualizadas.

**Fase 2 — "Cerca" de verdad**
Hoy "Cerca" filtra por la ciudad que el usuario escribe en su perfil; las coordenadas del
plato nunca se rellenan. Con venues resueltos hay lat/lng, así que se puede pasar a
distancia real con geohash.

**Fase 3 — varias fotos por plato**
En fase 1, el plato se queda con la foto del primero que lo publicó. Quien lo valora
después no aporta foto. Es una simplificación deliberada, no un olvido.

---

## Lo que hay que decidir/hacer fuera del código

- Crear la clave de Places y restringirla (pasos arriba).
- Volver a añadir el permiso de localización al manifest — se quitó por no usarse — y
  declararlo en el formulario de **Data safety** de Play.

## Riesgos asumidos

- El slug lo calcula el cliente, así que un cliente manipulado podría escribir un id que
  no corresponda a su `dishSlug` y crear un duplicado. Impacto bajo (ya podía crear
  duplicados) y se puede endurecer más adelante desde la CF.
- Dependencia de un proveedor externo para dar de alta locales nuevos. Mitigado porque
  los datos quedan en `venues`: si algún día se deja Places, lo ya guardado sigue siendo
  válido y solo deja de poder resolverse *nuevos* locales.
