# Ficha de Google Play

La ficha vivía solo dentro de Play Console, y solo en castellano: alguien en Reino Unido
que se encuentre la app la veía descrita en un idioma que no habla. Aquí está el texto de
los dos idiomas, para poder revisarlo y cambiarlo sin entrar en la consola.

**Cómo se mete**: Play Console → Crecimiento → Presencia en Play Store → Ficha principal
de Play Store → selector de idioma arriba → **Añadir traducciones** → `en-GB`. El
castellano (`es-ES`) es el idioma por defecto de la ficha y ya está puesto; compara antes
de sobrescribirlo, que lo de abajo es una reescritura, no una copia de lo que hay.

Límites de Play: nombre 30 caracteres, descripción breve 80, descripción completa 4000.
Las cuentas están comprobadas (`tools/i18n/check_listing.py`).

---

## Castellano (es-ES)

### Nombre

```
FoodRanker
```

### Descripción breve

```
Descubre, puntúa y comparte los mejores platos del mundo, plato a plato.
```

### Descripción completa

```
En FoodRanker no se puntúan restaurantes: se puntúan platos.

Porque en un sitio de cinco estrellas te puede llegar una pasta mediocre, y en el bar de
abajo hacer la mejor tortilla de la ciudad. Lo que quieres saber no es si el local está
bien, es qué pedir cuando te sientes.

QUÉ PUEDES HACER

• Publicar un plato con su foto, el local donde lo comiste y lo que te pareció.
• Puntuarlo por sabor, presentación y disfrute, y apuntar lo que pagaste.
• Ver el ranking de los mejores platos del mundo, de tu ciudad o de la gente a la que
  sigues.
• Entrar en un sitio y mirar "Qué pido aquí": los platos que otros ya han valorado ahí.
• Guardar los que te apetezcan para cuando toque, y ordenarlos en listas.
• Seguir a quien come como tú, comentar y descubrir por dónde anda.

LAS NOTAS SON DE QUIEN SE LO HA COMIDO

Para puntuar un plato hay que confirmar que lo has probado. No hay notas de oídas ni de
haber pasado por delante. Y cada foto se revisa automáticamente antes de publicarse: si
no es comida, no entra.

CUANTO MÁS APORTES, MÁS LEJOS LLEGAS

Ganas XP por publicar, puntuar y comentar, y subes por seis niveles, de Novato Foodie a
Leyenda Foodie. Hay seis logros que desbloquear, un reto cada semana y una liga semanal:
global y de tu ciudad. El podio se lleva XP y quien gana se lleva el distintivo.

GRATIS Y SIN ANUNCIOS

FoodRanker es gratis, no enseña anuncios y no hay nada que comprar dentro: ni suscripción,
ni funciones de pago, ni límites que se quiten pagando. Todo lo que hace la app lo hace
igual para todo el mundo.

Disponible en castellano e inglés.

Los platos se comen mejor contados. Empieza por el último que te sorprendió.
```

---

## Inglés (en-GB)

### Nombre

```
FoodRanker
```

### Descripción breve

```
Discover, rate and share the best dishes in the world, one plate at a time.
```

### Descripción completa

```
FoodRanker does not rate restaurants. It rates dishes.

Because a five-star place can still bring you a forgettable pasta, and the café on your
corner might do the best omelette in town. What you actually want to know is not whether
the place is any good — it is what to order once you sit down.

WHAT YOU CAN DO

• Post a dish with its photo, the place you ate it and what you made of it.
• Rate it on taste, presentation and enjoyment, and note what you paid.
• Browse the best dishes in the world, in your city, or from the people you follow.
• Walk into somewhere and check "What should I order here": the dishes other people have
  already rated at that exact place.
• Save the ones you fancy for later and sort them into lists.
• Follow people who eat like you do, leave comments and see what they are finding.

SCORES COME FROM PEOPLE WHO ATE THE DISH

Before you can rate something, you confirm you have actually tried it. No scores from
hearsay or from walking past. And every photo is checked automatically before it goes
live: if it is not food, it does not get in.

THE MORE YOU PUT IN, THE FURTHER YOU GET

You earn XP for posting, rating and commenting, and climb six levels from Rookie Foodie
to Foodie Legend. There are six achievements to unlock, a new challenge every week, and a
weekly league — both global and for your own city. The podium takes XP, and the winner
takes the badge.

FREE, AND NO ADS

FoodRanker is free, it shows no ads and there is nothing to buy inside it: no subscription,
no paid features, no limits you can pay to lift. Everything the app does, it does the same
for everybody.

Available in English and Spanish.

Food is better when it gets talked about. Start with the last dish that surprised you.
```

---

## Novedades de esta versión ("What's new")

Límite de Play: 500 caracteres por idioma. Un texto por cada uno; si solo pones el del
idioma por defecto, a quien tenga el móvil en inglés le llega en castellano.

⚠️ La caja de Play **ya trae las etiquetas** `<es-ES>`, `<en-GB>` y `<en-US>`. Hay dos
formas de rellenarla y la diferencia importa, porque mezclarlas es lo que da el error de
etiquetas duplicadas:

- **Idioma por idioma**: pegar el texto **dentro** de cada etiqueta, sin las etiquetas.
- **De una vez** (más cómodo): clic en la caja → **`Ctrl+A`** → pegar el bloque completo
  **con** las etiquetas, reemplazando todo lo que había. Por eso cada versión de aquí abajo
  tiene también su bloque entero listo para esto.

Lo que **no** se puede hacer es pegar un bloque con etiquetas dentro de una etiqueta que ya
está: ahí es donde se duplican.

---

## v28 (1.11) — fuera los anuncios y fuera Premium

Lo que el usuario nota de la v28. El motivo de verdad (0,11 €/mes de AdMob, el umbral de
pago en 70 €, y que mantener la publicidad obligaba a declararse operador en Play) está en
`docs/HANDOFF.md`; en las notas no pinta nada.

⚠️ **Se dice qué pasa con lo que abría Premium**, y no solo que desaparece. Premium abría
tres cosas: 20 platos al día en vez de 10, un radio de 1.600 m en vez de 550 para los
platos cercanos, y "quién ha interactuado". Las tres se quedan en el valor alto para todo
el mundo. Anunciar la retirada sin esto sonaría a recorte, y es lo contrario.

⚠️ **No se menciona el identificador de publicidad** (el permiso `AD_ID` cortado con
`tools:node="remove"`), ni los dos huecos de los agregados de valoraciones: lo primero no
se ve, y lo segundo es de servidor y ya lo tiene todo el mundo sin actualizar.

**Bloque entero, para pegar de una vez:**

```
<es-ES>
Fuera los anuncios: FoodRanker ya no enseña ninguno.

Y fuera la suscripción Premium, pero sin perder nada por el camino: lo que abría pasa a estar para todo el mundo. 20 platos al día, los platos cercanos en un radio más amplio, y ver quién ha interactuado con los tuyos.

La tarjeta de la misión diaria ocupa menos, así que en la pantalla de inicio caben más platos.
</es-ES>
<en-GB>
The ads are gone: FoodRanker no longer shows any.

And so is the Premium subscription, with nothing lost along the way: what it unlocked is now there for everyone. 20 dishes a day, nearby dishes over a wider radius, and seeing who has interacted with yours.

The daily mission card takes up less room, so more dishes fit on the home screen.
</en-GB>
<en-US>
The ads are gone: FoodRanker no longer shows any.

And so is the Premium subscription, with nothing lost along the way: what it unlocked is now there for everyone. 20 dishes a day, nearby dishes over a wider radius, and seeing who has interacted with yours.

The daily mission card takes up less room, so more dishes fit on the home screen.
</en-US>
```

---

## v27 (1.10) — PUBLICADA: renombrar platos, la campana y los textos cortados

Lo que se pegó al promocionar la v27 a producción el 2026-11-22. Queda aquí por tenerlo
todo en el mismo sitio.

⚠️ **La primera línea no es de la v27.** Renombrar un plato estaba hecho desde antes pero
sin publicar, así que para producción es nuevo y es lo único de la lista que el usuario
puede *usar*; lo demás son arreglos. Por eso va suelto arriba y el resto en viñetas.

⚠️ **"disponible en dispositivos donde antes no aparecía"** es el `uses-feature`: Play
deducía hardware obligatorio de los permisos y escondía la app. Quien lea eso no tiene por
qué saber qué es un `uses-feature`, pero sí le dice por qué un amigo suyo no la encontraba.

**Bloque entero, para pegar de una vez:**

```
<es-ES>
Ya puedes cambiar el nombre de un plato que hayas publicado, desde tu perfil.

Además:
• La campana de avisos ya no se queda en blanco si falla la conexión al entrar.
• Arreglados textos que se cortaban en pantallas estrechas y con la letra del sistema en grande.
• La app vuelve a estar disponible en dispositivos donde antes no aparecía.
</es-ES>
<en-GB>
You can now rename a dish you've posted, from your profile.

Also:
• The notifications bell no longer stays blank if the connection fails when you sign in.
• Fixed text being cut off on narrow screens and at large system font sizes.
• The app is now available on devices where it didn't show up before.
</en-GB>
<en-US>
You can now rename a dish you've posted, from your profile.

Also:
• The notifications bell no longer stays blank if the connection fails when you sign in.
• Fixed text being cut off on narrow screens and at large system font sizes.
• The app is now available on devices where it didn't show up before.
</en-US>
```

---

## v26 (1.9.3) — PRODUCCIÓN: el salto de la 1.6 a la 1.9.3

⚠️ **Estas NO son las notas del canal interno.** Producción estaba en la **20 (1.6)**, así
que al promocionar la v26 sus usuarios reciben de golpe lo de la v21, la v22 y la v23. Las
notas del interno ("la hoja de correo se abría a medias") **no valen aquí**: quien viene de
la 1.6 nunca tuvo esa pantalla y le llega ya arreglada.

**Qué se deja fuera a propósito:**

- Los arreglos de la **v24, v25 y v26**: corrigen versiones que nunca salieron de los
  canales de prueba.
- El botón de guardar tras la barra y el aviso en rojo (v22), y el enlace mal formado
  (v21). Caben por carácter, pero tapan las tres cosas grandes.
- Los **canales de avisos** (v22), con el criterio de siempre: no reparten hasta que se
  despliegue `functions`, y eso va **después** de que la v26 esté repartida en producción.

Longitudes: **es-ES 433** y **en 404** caracteres, de 500.

**Bloque entero, para pegar de una vez:**

```
<es-ES>
FoodRanker ya sigue el modo oscuro del móvil: antes se quedaba en blanco aunque tuvieras el sistema en oscuro.

Ya puedes entrar con tu correo y una contraseña, sin necesidad de una cuenta de Google. Si entrabas con Google, sigue funcionando igual que siempre.

También puedes elegir el idioma de la app desde tu perfil, en castellano o en inglés (necesita Android 13 o posterior), y corregir el nombre de un plato que añadieras mal.
</es-ES>
<en-GB>
FoodRanker now follows your phone's dark mode: it used to stay white even with your system set to dark.

You can now sign in with your email and a password, with no Google account needed. If you signed in with Google, it still works just the same.

You can also choose the app's language from your profile, Spanish or English (needs Android 13 or later), and correct the name of a dish you added wrongly.
</en-GB>
<en-US>
FoodRanker now follows your phone's dark mode: it used to stay white even with your system set to dark.

You can now sign in with your email and a password, with no Google account needed. If you signed in with Google, it still works just the same.

You can also choose the app's language from your profile, Spanish or English (needs Android 13 or later), and correct the name of a dish you added wrongly.
</en-US>
```

---

## v26 (1.9.3) — interno: la hoja de correo se abría a medias

Tres versiones subidas al interno con el mismo fallo sin arreglar (v24 y v25 no lo
tocaban: ver `docs/HANDOFF.md`). La v26 es la que lo arregla de verdad, **comprobada en el
Redmi**, no en el emulador.

El texto no menciona la barra de navegación ni los insets: al usuario le importa que el
botón de crear cuenta ahora se puede pulsar.

**Bloque entero, para pegar de una vez:**

```
<es-ES>
Arreglado: la pantalla para entrar o registrarse con correo se abría a medias y el botón de crear cuenta quedaba fuera de alcance.
</es-ES>
<en-GB>
Fixed: the email sign-in screen opened half-way and the create account button was out of reach.
</en-GB>
<en-US>
Fixed: the email sign-in screen opened half-way and the create account button was out of reach.
</en-US>
```

---

## v23 (1.9) — entrar con correo y contraseña

Lo que el usuario nota de la v23.

⚠️ **No se menciona el motivo de verdad**, que es que la revisión de Play no podía
entrar con una cuenta de Google (ver `docs/HANDOFF.md`). A quien usa la app eso no le dice
nada; lo que le importa es que hay otra forma de entrar.

### Castellano (es-ES) — v23

```
Ya puedes entrar en FoodRanker con tu correo y una contraseña, sin necesidad de una cuenta de Google. Si ya entrabas con Google, no tienes que hacer nada: sigue funcionando igual que siempre.

¿Se te olvida la contraseña? Desde la misma pantalla te mandamos un correo para poner una nueva.
```
*(289 caracteres)*

### Inglés (en-GB) — v23

```
You can now sign in to FoodRanker with your email and a password — no Google account needed. If you already sign in with Google, there is nothing to do: it keeps working exactly as before.

Forgotten your password? From the same screen we’ll email you a link to set a new one.
```
*(276 caracteres)*

### Inglés (en-US) — v23

**Idéntico** al de en-GB. Hay que pegarlo igual: si en-US se queda vacío, Play le sirve el
del idioma por defecto, que es el castellano.

```
You can now sign in to FoodRanker with your email and a password — no Google account needed. If you already sign in with Google, there is nothing to do: it keeps working exactly as before.

Forgotten your password? From the same screen we’ll email you a link to set a new one.
```
*(276 caracteres)*

### Los tres de una vez — v23

Para el camino de `Ctrl+A` + pegar encima, reemplazando todo lo que haya en la caja:

```
<es-ES>
Ya puedes entrar en FoodRanker con tu correo y una contraseña, sin necesidad de una cuenta de Google. Si ya entrabas con Google, no tienes que hacer nada: sigue funcionando igual que siempre.

¿Se te olvida la contraseña? Desde la misma pantalla te mandamos un correo para poner una nueva.
</es-ES>
<en-GB>
You can now sign in to FoodRanker with your email and a password — no Google account needed. If you already sign in with Google, there is nothing to do: it keeps working exactly as before.

Forgotten your password? From the same screen we’ll email you a link to set a new one.
</en-GB>
<en-US>
You can now sign in to FoodRanker with your email and a password — no Google account needed. If you already sign in with Google, there is nothing to do: it keeps working exactly as before.

Forgotten your password? From the same screen we’ll email you a link to set a new one.
</en-US>
```

---

## v22 (1.8) — modo oscuro de verdad y la hoja de editar plato

Lo que el usuario nota de la v22.

⚠️ **No se mencionan los canales de avisos**, aunque van en esta versión. Los canales
nuevos aparecen en los ajustes del móvil en cuanto se instala la v22, pero el reparto de
verdad no empieza hasta que se despliegue `functions`, y eso es **después** de que la v22
esté repartida. Anunciarlo ahora sería prometer algo que todavía no hace. Mismo criterio
que con el arreglo de los avisos en la v21: lo que se hace del lado del servidor no se
anuncia en las notas de una versión.

Tampoco se menciona que `Fallos.reportar` ya no se traga las cancelaciones: es invisible
para quien usa la app.

### Castellano (es-ES) — v22

```
FoodRanker ya sigue el modo oscuro del móvil. Antes la app se quedaba en blanco aunque tuvieras el sistema en oscuro; ahora cambia entera, desde el arranque.

Y al editar un plato tuyo: el botón de guardar ya no se esconde detrás de la barra de navegación, y el aviso en rojo desaparece en cuanto corriges el nombre.
```
*(316 caracteres)*

### Inglés (en-GB) — v22

```
FoodRanker now follows your phone's dark mode. The app used to stay white even with your system set to dark; now the whole thing changes, right from startup.

And when editing one of your dishes: the save button no longer hides behind the navigation bar, and the red warning clears as soon as you fix the name.
```
*(310 caracteres)*

### Inglés (en-US) — v22

**Idéntico** al de en-GB otra vez. Hay que pegarlo igual: si en-US se queda vacío, Play le
sirve el del idioma por defecto, que es el castellano.

```
FoodRanker now follows your phone's dark mode. The app used to stay white even with your system set to dark; now the whole thing changes, right from startup.

And when editing one of your dishes: the save button no longer hides behind the navigation bar, and the red warning clears as soon as you fix the name.
```
*(310 caracteres)*

### Los tres de una vez — v22

Para el camino de `Ctrl+A` + pegar encima, reemplazando todo lo que haya en la caja:

```
<es-ES>
FoodRanker ya sigue el modo oscuro del móvil. Antes la app se quedaba en blanco aunque tuvieras el sistema en oscuro; ahora cambia entera, desde el arranque.

Y al editar un plato tuyo: el botón de guardar ya no se esconde detrás de la barra de navegación, y el aviso en rojo desaparece en cuanto corriges el nombre.
</es-ES>
<en-GB>
FoodRanker now follows your phone's dark mode. The app used to stay white even with your system set to dark; now the whole thing changes, right from startup.

And when editing one of your dishes: the save button no longer hides behind the navigation bar, and the red warning clears as soon as you fix the name.
</en-GB>
<en-US>
FoodRanker now follows your phone's dark mode. The app used to stay white even with your system set to dark; now the whole thing changes, right from startup.

And when editing one of your dishes: the save button no longer hides behind the navigation bar, and the red warning clears as soon as you fix the name.
</en-US>
```

---

## v21 (1.7) — selector de idioma, nombres de plato y enlaces raros

Lo que el usuario nota de la v21. **No se menciona** el arreglo de los avisos que cerraban
la app: ese se hizo del lado del servidor el 2026-11-16 y ya lo tiene todo el mundo sin
actualizar nada, así que anunciarlo ahora confundiría. Tampoco se menciona que ahora se
registran los fallos en Crashlytics, que es invisible para quien usa la app.

⚠️ El selector de idioma **solo existe en Android 13 o posterior** (`LocaleManager` es de
API 33). Las notas lo dicen a propósito: sin ese paréntesis, quien tenga Android 12 lo
buscaría en su perfil y no lo encontraría.

### Castellano (es-ES) — v21

```
Ya puedes elegir el idioma de FoodRanker desde tu perfil, en castellano o en inglés, sin tocar el idioma del móvil. (Necesita Android 13 o posterior.)

Si te equivocaste al escribir el nombre de un plato que añadiste, ahora puedes corregirlo.

Y un enlace mal formado ya no cierra la app al abrirlo.
```
*(299 caracteres)*

### Inglés (en-GB) — v21

```
You can now choose FoodRanker's language from your profile, Spanish or English, without changing your phone's language. (Needs Android 13 or later.)

If you mistyped the name of a dish you added, you can now correct it.

And a malformed link no longer closes the app when you open it.
```
*(284 caracteres)*

### Inglés (en-US) — v21

Otra vez **idéntico** al de en-GB: no hay ninguna palabra que se escriba distinto. Hay que
pegarlo igual, porque si en-US se queda vacío Play le sirve el del idioma por defecto, que
es el castellano.

```
You can now choose FoodRanker's language from your profile, Spanish or English, without changing your phone's language. (Needs Android 13 or later.)

If you mistyped the name of a dish you added, you can now correct it.

And a malformed link no longer closes the app when you open it.
```
*(284 caracteres)*

### Los tres de una vez — v21

Para el camino de `Ctrl+A` + pegar encima, reemplazando todo lo que haya en la caja:

```
<es-ES>
Ya puedes elegir el idioma de FoodRanker desde tu perfil, en castellano o en inglés, sin tocar el idioma del móvil. (Necesita Android 13 o posterior.)

Si te equivocaste al escribir el nombre de un plato que añadiste, ahora puedes corregirlo.

Y un enlace mal formado ya no cierra la app al abrirlo.
</es-ES>
<en-GB>
You can now choose FoodRanker's language from your profile, Spanish or English, without changing your phone's language. (Needs Android 13 or later.)

If you mistyped the name of a dish you added, you can now correct it.

And a malformed link no longer closes the app when you open it.
</en-GB>
<en-US>
You can now choose FoodRanker's language from your profile, Spanish or English, without changing your phone's language. (Needs Android 13 or later.)

If you mistyped the name of a dish you added, you can now correct it.

And a malformed link no longer closes the app when you open it.
</en-US>
```

---

## v19 y v20 (1.6) — los enlaces compartidos, y el idioma en Xiaomi

⚠️ **Las mismas notas valen para la v20.** El `versionCode` 20 solo quita un permiso
que sobraba (`READ_MEDIA_IMAGES`, ver `docs/HANDOFF.md`), no cambia nada que el usuario
note, y el `versionName` sigue siendo `1.6`. No reescribir esto para la v20: pegar lo
mismo.

### Castellano (es-ES) — v19

```
Los enlaces que compartías de un plato, un perfil o una invitación no llevaban a ningún sitio. Ya funcionan: quien reciba uno abre la app directamente en lo que le mandaste, y si no la tiene instalada, una página le explica qué es FoodRanker.

Los avisos que ya habías recibido con la app cerrada volvían a sonar al entrar en ella. Arreglado.

Y el atajo para cambiar el idioma de la app ya no se lleva FoodRanker por delante en algunos móviles Xiaomi.
```
*(452 caracteres)*

### Inglés (en-GB) — v19

```
Links you shared to a dish, a profile or an invite led nowhere. They work now: whoever gets one opens the app straight on what you sent them, and if they do not have it installed, a page explains what FoodRanker is.

Notifications you had already received with the app closed went off again when you opened it. Fixed.

Also, the shortcut for changing the app language no longer takes FoodRanker down on some Xiaomi phones.
```
*(422 caracteres)*

### Inglés (en-US) — v19

El texto es **idéntico** al de en-GB: esta vez no hay ninguna palabra que se escriba
distinto. Hay que pegarlo igual, porque si en-US se queda vacío Play le sirve el del
idioma por defecto, que es el castellano.

```
Links you shared to a dish, a profile or an invite led nowhere. They work now: whoever gets one opens the app straight on what you sent them, and if they do not have it installed, a page explains what FoodRanker is.

Notifications you had already received with the app closed went off again when you opened it. Fixed.

Also, the shortcut for changing the app language no longer takes FoodRanker down on some Xiaomi phones.
```
*(422 caracteres)*

---

## v18 (1.5) — consentimiento de anuncios y textos legales

### Castellano (es-ES) — v18

```
Ahora FoodRanker te pregunta antes de usar tus datos para anuncios personalizados, como exige la normativa europea. Decides tú, y puedes cambiar de opinión cuando quieras desde Perfil → Opciones de privacidad de anuncios.

La política de privacidad dice además quién responde de tus datos y con qué base legal se tratan, y ya se puede leer en inglés y en castellano.
```
*(366 caracteres)*

### Inglés (en-GB) — v18

```
FoodRanker now asks before using your data for personalised ads, as European rules require. It is your choice, and you can change your mind at any time under Profile → Ad privacy options.

The privacy policy now also says who is responsible for your data and the legal basis for processing it, and you can read it in English as well as Spanish.
```
*(344 caracteres)*

### Inglés (en-US) — v18

```
FoodRanker now asks before using your data for personalized ads, as European rules require. It is your choice, and you can change your mind at any time under Profile → Ad privacy options.

The privacy policy now also says who is responsible for your data and the legal basis for processing it, and you can read it in English as well as Spanish.
```
*(344 caracteres)*

---

## v17 (1.4) — histórico

### Castellano (es-ES)

```
FoodRanker ya está entera en inglés y en castellano, avisos incluidos: llegan en el
idioma de cada uno. La app sigue el idioma del móvil, y en Android 13 o posterior puedes
cambiárselo solo a ella desde Perfil → Idioma.

Los precios se ven en la moneda del país del local, no en la de tu móvil, y el enlace de
reservas lleva a ese sitio y no al buscador español.

Y el aviso de que alguien te ha comentado un plato vuelve a llegar: salía en la campana
pero no avisaba.
```

### Inglés (en-GB) — novedades

```
FoodRanker is now fully in English as well as Spanish — notifications included, so they
reach you in your own language. The app follows your phone's language, and on Android 13
or later you can set a different one just for it under Profile → Language.

Prices show in the currency of the venue's country, not your phone's, and the booking link
takes you to that place instead of the Spanish search page.

Comment notifications are fixed too: they showed in the bell but never notified you.
```

---

## Capturas

`docs/play-store-screenshots/es/` y `docs/play-store-screenshots/en/`, tres de cada
(ranking, ficha de plato, perfil), 1080x2400, hechas en el Redmi el 2026-10-09 sobre la
v17 y con datos reales. En Play Console se suben con el selector de idioma en `es-ES` y en
`en-GB`; si solo se suben las de un idioma, Play reutiliza esas para los dos.

### Si algún día se rehacen, estas tres cosas (leer ANTES de disparar)

1. **Bajar un poco menos en el perfil.** La de ahora corta a media etiqueta — se lee
   "Platos publicados" partido y "Likes / dados" medio tapado por la barra de navegación.
   Dejar que el bloque de estadísticas entre entero o cortar por un hueco, no por un texto.
2. **Esperar a que cargue la ficha de plato**: ~14 s. La primera tanda cogió el esqueleto
   de carga, y se nota en el peso (85 KB contra 1,2 MB).
3. **Ya no hace falta el truco de Premium.** Se daba para que no saliera el banner de
   AdMob; desde la v28 no hay anuncios ni Premium, así que vale cualquier cuenta. Pero hace
   falta una **con datos**: la de revisión tiene 0 platos y 0 XP.

Las anteriores estaban **obsoletas**, no solo en castellano: enseñaban "Top semana" y
"Reset semanal del ranking", dos textos que se corrigieron por no ser verdad, y platos de
prueba que ya no existen (`Bar Casa Benito`, Toledo). Se han sustituido.

### La estrella de Premium: quitada de `03-profile.png` (2026-11-23)

Las capturas se hicieron dándole Premium a la cuenta
(`node scripts/manageUser.js grant-premium <uid>`) para que no saliera el banner de AdMob.
El efecto secundario fue que **`03-profile.png`, en los dos idiomas, enseñaba la estrella
dorada de Premium** sobre la foto de perfil: un distintivo que desde la v28 no existe.

**Se quitó de la imagen en vez de rehacer la captura**, porque el resto de esa pantalla no
ha cambiado en la v28 (la fila de Premium estaba más abajo, fuera del encuadre) y rehacerla
exigía el Redmi con la cuenta real: la de revisión tiene 0 platos y 0 XP y habría salido un
perfil vacío. La edición hace que la captura sea **más** fiel a la app, no menos.

**Cómo se hizo, por si hay que repetirlo.** El avatar está centrado en la imagen (centro
x=540 de 1080), así que la zona simétrica del badge tiene el mismo anillo y el mismo fondo.
Comprobado antes de tocar nada: los píxeles del fondo a un lado y a otro **coinciden
exactamente**. Entonces:

- **fondo naranja** (fuera del aro): espejo horizontal, que es reconstrucción exacta;
- **aro exterior** (radio 149-166 desde el centro del avatar): el espejo NO vale, porque el
  aro tiene un degradado circular y traía el tono del lado contrario — dejaba un escalón de
  `(255,134,90)` a `(255,233,189)`. Se reconstruye **interpolando en ángulo** entre los dos
  extremos sanos del arco;
- **anillo blanco** (142-149): es blanco puro, no hizo falta nada.

⚠️ **Dos trampas que costaron una pasada en falso:** el detector de "píxeles dorados" se
tragó el antialias del texto *Sergio De La Peña* de debajo y dio un badge del doble de
tamaño (radio 73 en vez de 38), así que el primer intento se comió medio nombre y lo dejó
escrito del revés. El badge real es centro (661, 594) radio 38, y el texto empieza en
y=677. Hay que **medir el badge excluyendo lo casi blanco** y poner un tope por abajo.

Las de ranking y ficha de plato no llevan la estrella: esa solo la pinta la cabecera del
perfil, y ninguna de las dos enseña un avatar. Pueden quedarse. La de ranking sí lleva la
tarjeta de misión diaria **en su tamaño viejo y con el texto viejo** ("Misión diaria: mueve
el ranking · 0 de 3 votos hechos"); no es falso, pero envejece.

⚠️ Detalle menor que sigue ahí: `03-profile.png` corta a media etiqueta ("Platos
publicados" / "Likes dados") contra la barra de navegación. No se arregla editando; si
algún día se rehacen las tres, bajar un poco menos.

⚠️ **Esperar a que cargue.** La primera tanda cogió el esqueleto de carga de la ficha de
plato (se nota en el peso: 85 KB contra 1,2 MB). Para esa pantalla hacen falta ~14 s.
