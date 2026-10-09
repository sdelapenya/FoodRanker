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

GRATIS, CON ANUNCIOS

FoodRanker es gratis. La suscripción Premium es opcional: quita los anuncios, añade un
distintivo a tu perfil y te deja publicar el doble de platos al día. Nada de lo que hace
la app se queda detrás de la suscripción.

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

FREE, WITH ADS

FoodRanker is free. The Premium subscription is optional: it removes the ads, adds a badge
to your profile and lets you post twice as many dishes a day. Nothing the app does is
locked behind it.

Available in English and Spanish.

Food is better when it gets talked about. Start with the last dish that surprised you.
```

---

## Novedades de esta versión ("What's new")

Límite de Play: 500 caracteres por idioma. Un texto por cada uno; si solo pones el del
idioma por defecto, a quien tenga el móvil en inglés le llega en castellano.

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

## ⚠️ Las capturas siguen en castellano

`docs/play-store-screenshots/` tiene tres capturas (ranking, ficha de plato, perfil) con la
interfaz en castellano, y son las que Play enseña **en los dos idiomas**: la consola permite
subir capturas distintas por idioma, pero si no las hay reutiliza las del idioma por
defecto. Alguien en Reino Unido lee la descripción en inglés y luego ve las pantallas en
castellano.

**Esto no se puede automatizar aquí**: las tres pantallas necesitan sesión iniciada con
Google y contenido real (platos publicados, valoraciones, perfil con nivel), y el login de
Google no se puede hacer desde la línea de comandos. Hay que repetirlas a mano:

1. Poner la app en inglés (Ajustes de Android → Aplicaciones → FoodRanker → Idioma, o
   `adb shell cmd locale set-app-locales com.app.foodranker --locales en-GB`).
2. Repetir las mismas tres pantallas, para que las dos fichas se parezcan.
3. Guardarlas en `docs/play-store-screenshots/en/` y subirlas en Play Console con el
   selector de idioma en `en-GB`.

Mientras no estén, la ficha inglesa funciona igual: Play no obliga a tener capturas por
idioma. Pero la primera impresión queda a medias.
