package com.app.foodranker.data.model

/**
 * El filtro de bloqueo, en un solo sitio.
 *
 * Existe por lo mismo que [soloPublicos] y que `Fallos.reportar`: la línea que se copia
 * en once sitios es la que alguien olvida en el doce. Aquí el doce duele más de lo
 * normal, porque "una pantalla que se olvidó del filtro" significa enseñarle a alguien
 * justo al usuario del que pidió no saber nada nunca más.
 *
 * Y por eso, además de esta función, hay un comprobador: `tools/check_bloqueo_cliente.py`
 * recorre las conversiones a modelo y falla si alguna no pasa por aquí. Que no se olvide
 * una pantalla no puede depender de que me acuerde.
 *
 * ## Qué es `ocultos`
 *
 * La unión de las dos direcciones —a quién bloqueé y quién me bloqueó— tal y como la
 * sirve `BloqueoRepository.ocultos`. A la hora de pintar, la dirección da igual.
 *
 * ## Lo que NO se filtra: la liga
 *
 * La liga es la única lista de personas que se deja entera, y es deliberado. `LeagueScreen`
 * pinta el puesto con `index + 1` sobre lo que recibe, así que quitar una fila **renumera
 * a los de abajo**: dos usuarios verían clasificaciones distintas y la que se comenta
 * fuera de la app sería mentira para alguno. Llevar el puesto real aparte dejaría un hueco
 * visible, pero el podio (`take(3)`) ascendería a quien no toca.
 *
 * Una clasificación es un objeto compartido, no un muro personal. El bloqueo impide
 * interactuar y esconde lo que esa persona publica; su puesto en un marcador común no es
 * algo que te haya enviado a ti.
 *
 * ## Por qué un overload por modelo y no una interfaz común
 *
 * Lo natural sería una interfaz `ConAutor { val autorUid: String }`. No se puede: estos
 * data class los mapea Firestore, y una propiedad calculada se le escapa al escribir y
 * acabaría como un campo de más en el documento. Varias colecciones tienen un `hasOnly`
 * en las reglas, así que ese campo de más no sería un detalle estético: rompería la
 * escritura entera. Con `@get:Exclude` se evitaría, pero es una anotación fácil de
 * olvidar en el modelo siguiente, y el fallo sería silencioso hasta que alguien no
 * pudiera publicar. Un overload por modelo no tiene esa trampa.
 */

/** Platos de usuarios con bloqueo de por medio, fuera. */
fun List<Plate>.sinBloqueados(ocultos: Set<String>): List<Plate> =
    if (ocultos.isEmpty()) this else filter { it.addedByUserId !in ocultos }

/** Valoraciones de usuarios con bloqueo de por medio, fuera. */
@JvmName("sinBloqueadosRatings")
fun List<Rating>.sinBloqueados(ocultos: Set<String>): List<Rating> =
    if (ocultos.isEmpty()) this else filter { it.userId !in ocultos }

/** Comentarios de usuarios con bloqueo de por medio, fuera. */
@JvmName("sinBloqueadosComments")
fun List<Comment>.sinBloqueados(ocultos: Set<String>): List<Comment> =
    if (ocultos.isEmpty()) this else filter { it.userId !in ocultos }

/**
 * Usuarios con bloqueo de por medio, fuera: listas de seguidores y seguidos, búsqueda
 * de gente, "quién ha interactuado".
 */
@JvmName("sinBloqueadosUsers")
fun List<User>.sinBloqueados(ocultos: Set<String>): List<User> =
    if (ocultos.isEmpty()) this else filter { it.id !in ocultos }

/**
 * Avisos de usuarios con bloqueo de por medio, fuera.
 *
 * Las reglas ya impiden que un bloqueado cree uno nuevo, y el trigger filtra los de
 * plato nuevo. Esto es para los que ya estaban en la campana **antes** del bloqueo: sin
 * esto, bloquear a alguien te dejaba sus avisos antiguos ahí, con su nombre y su foto.
 */
@JvmName("sinBloqueadosNotificaciones")
fun List<FoodNotification>.sinBloqueados(ocultos: Set<String>): List<FoodNotification> =
    if (ocultos.isEmpty()) this else filter { it.fromUserId !in ocultos }
