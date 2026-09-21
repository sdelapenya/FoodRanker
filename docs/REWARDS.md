# REWARDS — XP, badges y liga

**Estado:** diagnóstico hecho y **arreglado lo que mentía** (2026-09-21). El sistema de
recompensas *nuevo* está **aparcado a propósito**: ver §4.

---

## 1. Cómo funciona hoy

**XP global** (`users/{uid}.xp`, acumulativo para siempre) y **XP de liga**
(`leagues/{scope}_{semana}/entries/{uid}.xp`, semanal) son **contadores distintos**.

| Acción | XP global | XP de liga |
|--------|-----------|------------|
| Publicar plato aprobado | 30 + 15 | 45 |
| Valorar el plato de otro | 15 | 15 |
| Recibir una valoración | 10 | — |
| Comentar (1 vez por plato) | 5 | — |
| Ver anuncio (24 h de espera) | 50 | — |
| Referidos | 100 / 50 | — |
| Retos | `xpReward` | — |

**Niveles**: 6 umbrales (0/200/600/1500/4000/10000), duplicados en `getLevel()`
(`functions/src/index.ts`) y `RewardManager.LEVELS`. El cliente **siempre recalcula** desde
`xp`; el campo `users.level` es informativo.

**Badges**: 5, en `RewardManager.ALL_BADGES` y concedidos por `checkAndAwardBadges`.
`first_plate`, `globetrotter` (3 países), `popular` (50 likes), `critic` (10 valoraciones),
`top10`. **Nunca se revocan**, y un badge con id desconocido no se pinta — así que uno nuevo
exige añadirlo también a `ALL_BADGES` del cliente.

**Liga**: semanal (ISO `YYYY-Www`), reinicia el lunes. **No hay cierre**: al cambiar la semana
simplemente se escribe en otro documento y el anterior queda huérfano. Nadie lo lee, no hay
histórico ni ganador.

---

## 2. El diagnóstico (por qué no engancha)

1. **El XP no desbloquea nada.** Subes de nivel y no pasa nada: es un contador de vanidad.
2. **La economía premiaba lo contrario de lo que necesita la app** (ver §3).
3. **Los badges son casi todos de "primera vez"**: los consigues y se acaba el juego.
4. **La liga prometía un premio inexistente** (ver §3).
5. **No hay ciclo corto**: la misión diaria llevaba muerta desde antes del rediseño de
   valoraciones (el código que la incrementaba no lo llamaba nadie).

---

## 3. Arreglado el 2026-09-21 — "lo que mentía"

**La economía, de 11× a 3×.** Publicar daba 50 y valorar 5: subir un plato valía **once
valoraciones**, justo cuando el problema real es que 22 de 27 platos tienen **un único voto**.
Ahora `XP_PLATE_WITH_PHOTO = 30` y `XP_GIVE_RATING = 15` (publicar 45 contra valorar 15).
Publicar sigue valiendo más porque cuesta más, pero ya no aplasta al resto.

**La promesa falsa del badge.** `LeagueScreen` decía *"· Top 3 gana badge 🏅"* y el ticker de
Discover *"Top 3 de la semana gana badge especial"*. **Ese badge no existe** ni hay nada que
cierre la semana: la app prometía un premio que no entregaba. Lo notó un tester. Sustituido por
algo cierto ("Reinicia cada lunes") hasta que el cierre de liga esté hecho.

**La liga por ciudad no funciona con este volumen.** Los 9 usuarios están repartidos entre
Madrid, Toledo, Alcázar, Fuenlabrada y Casarrubios: cada "liga" tenía una o dos personas — la
semana W39 tenía **exactamente una**. Y quien no había puesto ciudad **no participaba y perdía
su XP de liga en silencio** (le pasaba a una tester real). Ahora hay una constante
`LEAGUE_SCOPE` en `functions/src/index.ts`: en `"global"` compite todo el mundo junto.
**Para volver a la liga local basta con ponerla en `"city"` y redesplegar** — el cliente
pregunta el id al servidor (`getLeagueId`), así que no hace falta versión nueva de la app.
⚠️ El campo `leagueCity` que se estampa en cada rating guarda **el prefijo del documento de
liga**, no la ciudad; los clawbacks lo usan para reconstruir el id. Los ratings antiguos llevan
la ciudad, que entonces era el prefijo, así que se revierten bien igual.

**`XP_RECEIVE_LIKE` borrado.** Estaba definido en `RewardManager` y **ninguna Cloud Function lo
usaba**: recibir likes no daba nada, pese a que la constante sugería lo contrario.

---

## 4. Aparcado hasta que la app crezca

Con 9 usuarios, el cuello de botella no es la motivación: es que no hay gente ni votos
suficientes para que un sistema de recompensas se note. Estas ideas son buenas, pero **el
momento es el que no cuadra** — no construirlas todavía.

**Pasaporte gastronómico.** Sellos por cocina, provincia o tipo de local. Coleccionismo que no
se agota (siempre falta un sello) y que es propio de una app de comida, no gamificación
genérica. Ya existe `globetrotter` como semilla.

**Descubridor.** Premiar a quien publica un plato que *luego otros* votan alto, en vez de a
quien publica mucho. Convierte "subir fotos" en "encontrar joyas", que es lo que la app dice
ser. Escala bien.

**Catador con criterio.** Reconocer a quien valora y cuya nota *resiste* cuando llegan más
votos. ⚠️ Hoy es **inaplicable**: con 22 platos de un solo voto no hay con qué comparar.
Necesita escala. Y hay que diseñarlo con cuidado para no castigar al que opina distinto.

**Cierre de liga semanal.** Función programada que al pasar la semana marque ganador, conceda
badge (con contador, porque los badges no se revocan y ganar 5 veces daría el mismo icono),
avise al ganador y archive el resultado para un histórico/palmarés. Es lo que haría que ganar
signifique algo. Hoy ganar no deja ni rastro.

**Que el nivel desbloqueé algo.** La idea es buena pero requiere construir funciones nuevas:
hoy no hay casi nada que "desbloquear" en la app.

### ⚠️ Dos avisos para cuando se retome

**El ritmo de esta app NO es diario.** Se come fuera una o dos veces por semana. Una misión
diaria del tipo "valora 3 platos hoy" es **incompatible** con la regla de que solo valora quien
ha probado el plato: solo se podría cumplir valorando cosas que no has comido, que es justo el
agujero que cerró el rediseño. La capa corta debe ser **semanal**.

**Las recompensas deben premiar comer y contarlo, no abrir la app.** Si el sistema empuja a
valorar por sumar puntos, se carga la credibilidad del ranking, que es el producto.
