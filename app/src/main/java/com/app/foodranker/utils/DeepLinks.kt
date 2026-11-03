package com.app.foodranker.utils

/**
 * El dominio de los enlaces que la app reparte, en un solo sitio.
 *
 * Antes estaba escrito a pelo en cuatro ficheros, y los cuatro apuntaban a
 * `foodranker.app`, **un dominio que no existe** (NXDOMAIN, comprobado el 2026-11-02).
 * Cada vez que alguien compartía un plato o invitaba a un amigo mandaba un enlace muerto:
 * el que lo recibía veía un error del navegador. El mecanismo de referidos llevaba roto
 * desde siempre, en silencio, porque nadie llegó a pulsar un enlace compartido.
 *
 * Ahora es [HOST], servido por Firebase Hosting (sitio `foodranker` del proyecto
 * `foodranker-51270`, ver `firebase.json` y `hosting/public/`). Gratis, con HTTPS y
 * capaz de servir `/.well-known/assetlinks.json`, que es lo que Android necesita para
 * verificar los App Links del manifest.
 */
object DeepLinks {

    /** El dominio que se usa al **generar** enlaces nuevos. */
    const val HOST = "foodranker.web.app"

    /**
     * Los dominios que se **aceptan** al abrir un enlace.
     *
     * Son más que [HOST] a propósito. Un enlace compartido por WhatsApp sigue vivo años,
     * así que la app tiene que entender todos los dominios que haya repartido alguna vez,
     * no solo el actual. `foodranker.app` está aquí por adelantado: si algún día se
     * registra y se apunta al mismo sitio de Firebase, los enlaces funcionarán sin
     * necesidad de actualizar la app. Quitar un dominio de esta lista rompe enlaces que
     * ya están circulando — solo se añade.
     */
    val ACCEPTED_HOSTS = setOf("foodranker.web.app", "foodranker.app")

    fun plate(plateId: String)  = "https://$HOST/plate/$plateId"
    fun user(userId: String)    = "https://$HOST/user/$userId"
    fun invite(code: String)    = "https://$HOST/invite/$code"
}
