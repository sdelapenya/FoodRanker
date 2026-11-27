# -*- coding: utf-8 -*-
"""Prueba las reglas de bloqueo entre usuarios contra el motor REAL de Firestore.

No es el simulador de la consola ni una lectura a ojo: manda `firestore.rules` a
`firebaserules.projects.test`, el mismo evaluador que corre en produccion.
**No escribe nada en ninguna base de datos.**

## Por que hace falta

El bloqueo es SIMETRICO: ni A ve a B, ni B puede seguir, comentar o valorar a A. Eso no se
puede verificar leyendo las reglas, porque depende de dos `exists()` cruzados y de un
`get()` al plato. Y es justo la parte que, si se rompe, deja un boton que promete una
proteccion que no da -- que es peor que no tenerlo.

## ⚠️ El caso que NO se puede quitar: "publicar un plato"

La app crea el plato y su valoracion inicial EN EL MISMO LOTE. La primera version de
estas reglas buscaba al autor con `get()`, que lee el estado ANTERIOR al lote -- donde el
plato aun no existe. Un `get()` sobre un documento inexistente no devuelve un null
esquivable: revienta la evaluacion y deniega el lote entero. O sea, nadie habria podido
publicar un plato. Los once casos de simetria pasaban igual, porque ninguno probaba una
creacion de valoracion con el plato ausente.

Por eso estan aqui los casos de `existsAfter`: si alguien vuelve a poner `get()`, fallan.

## El truco: `functionMocks`

Las reglas consultan documentos que en un test no existen. `functionMocks` fija lo que
devuelve cada `exists()` y cada `get()` por ruta exacta, asi que se puede montar el caso
"B bloqueo a A pero A no a B" sin crear ni un dato.

⚠️ Los mocks van por ruta EXACTA. Si alguien cambia donde vive la lista
(`users/{uid}/bloqueados/{otro}`), estos tests dejan de medir lo que creen y hay que
actualizar las rutas de aqui abajo.

## Uso

    python -I tools/rules/check_bloqueo.py

Usa las credenciales de firebase-tools (`~/.config/configstore/firebase-tools.json`).
Sale 0 si pasan todos los casos, 1 si falla alguno.
"""
import io
import json
import os
import sys
import urllib.parse
import urllib.request

RAIZ = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
PROYECTO = "foodranker-51270"
# Cliente publico de firebase-tools: no es un secreto, va en su codigo fuente.
CLIENT_ID = "563584335869-fgrhgmd47bqnekij5i8b5pr03ho849e6.apps.googleusercontent.com"
CLIENT_SECRET = "j9iVZfS8kkCEFUPaAeJV0sAi"

B = "/databases/(default)/documents"
A, BU = "userA", "userB"
RUTA_A_B = B + "/users/userA/bloqueados/userB"   # A bloqueo a B
RUTA_B_A = B + "/users/userB/bloqueados/userA"   # B bloqueo a A
PLATO = B + "/plates/p1"
RUTA_A_A = B + "/users/userA/bloqueados/userA"   # nadie se bloquea a si mismo


def token():
    ruta = os.path.expanduser("~/.config/configstore/firebase-tools.json")
    if not os.path.exists(ruta):
        print("No hay credenciales de firebase-tools. Haz `firebase login` primero.")
        sys.exit(2)
    cfg = json.load(io.open(ruta, encoding="utf-8-sig"))
    datos = urllib.parse.urlencode({
        "client_id": CLIENT_ID, "client_secret": CLIENT_SECRET,
        "refresh_token": cfg["tokens"]["refresh_token"], "grant_type": "refresh_token",
    }).encode()
    return json.load(urllib.request.urlopen("https://oauth2.googleapis.com/token", datos))["access_token"]


def auth(uid):
    return {"uid": uid, "token": {"sub": uid, "firebase": {"sign_in_provider": "password"}}}


def existe(ruta, valor):
    return {"function": "exists", "args": [{"exactValue": ruta}], "result": {"value": valor}}


def existe_luego(ruta, valor):
    return {"function": "existsAfter", "args": [{"exactValue": ruta}], "result": {"value": valor}}


def trae(ruta, datos):
    """El autor del plato DESPUES del lote: la regla usa getAfter, no get. Ver la cabecera."""
    return {"function": "getAfter", "args": [{"exactValue": ruta}],
            "result": {"value": {"data": datos}}}


def plato_de(uid):
    """El plato YA existia antes de la escritura: `get` y `getAfter` ven lo mismo."""
    datos = {"addedByUserId": uid}
    return [existe_luego(PLATO, True), trae(PLATO, datos),
            {"function": "get", "args": [{"exactValue": PLATO}],
             "result": {"value": {"data": datos}}}]


def plato_del_lote(uid):
    """El plato se esta creando EN ESTA MISMA escritura.

    `getAfter` lo ve; `get`, que mira el estado anterior, se encuentra un documento que no
    existe. Ese mock de `undefined` es el que reproduce el fallo real: si alguien vuelve a
    poner `get()` en la regla, este caso revienta con el mismo "service call error" que
    habria dejado a todo el mundo sin poder publicar un plato.
    """
    return [existe_luego(PLATO, True), trae(PLATO, {"addedByUserId": uid}),
            {"function": "get", "args": [{"exactValue": PLATO}],
             "result": {"undefined": {}}}]


def caso(nombre, peticion, mocks, espera, previo=None):
    """`previo` es el documento tal y como esta ANTES de la peticion (para los update)."""
    c = {"expectation": espera, "pathEncoding": "URL_ENCODED",
         "request": peticion, "functionMocks": mocks}
    if previo is not None:
        c["resource"] = {"data": previo}
    return (nombre, c)


# Una valoracion valida de A sobre el plato p1. Los ejes van en rango para que lo unico
# que pueda hacer fallar el caso sea el bloqueo y no otra validacion.
VALORACION = {"userId": A, "plateId": "p1", "flavorScore": 8,
              "presentationScore": 8, "satisfactionScore": 8, "averageScore": 8}
RUTA_VAL = B + "/ratings/p1_userA"


def plato(autor, likes, quienes):
    return {"addedByUserId": autor, "likes": likes, "likedByUsers": quienes}


CASOS = [
    # ── seguir ────────────────────────────────────────────────────────────────
    caso("A sigue a B, sin bloqueo",
         {"path": B + "/follows/userA_userB", "method": "create", "auth": auth(A),
          "resource": {"data": {"followerId": A, "followingId": BU}}},
         [existe(RUTA_A_B, False), existe(RUTA_B_A, False)], "ALLOW"),
    caso("A sigue a B habiendole bloqueado",
         {"path": B + "/follows/userA_userB", "method": "create", "auth": auth(A),
          "resource": {"data": {"followerId": A, "followingId": BU}}},
         [existe(RUTA_A_B, True), existe(RUTA_B_A, False)], "DENY"),
    # El que de verdad importa: la simetria. Sin esto, el bloqueado sigue entrando.
    caso("A sigue a B, pero B bloqueo a A  (SIMETRIA)",
         {"path": B + "/follows/userA_userB", "method": "create", "auth": auth(A),
          "resource": {"data": {"followerId": A, "followingId": BU}}},
         [existe(RUTA_A_B, False), existe(RUTA_B_A, True)], "DENY"),
    # ── comentar ──────────────────────────────────────────────────────────────
    caso("A comenta un plato de B, sin bloqueo",
         {"path": B + "/comments/c1", "method": "create", "auth": auth(A),
          "resource": {"data": {"userId": A, "plateId": "p1"}}},
         plato_de(BU) + [existe(RUTA_A_B, False), existe(RUTA_B_A, False)], "ALLOW"),
    caso("A comenta un plato de B, que le bloqueo  (SIMETRIA)",
         {"path": B + "/comments/c1", "method": "create", "auth": auth(A),
          "resource": {"data": {"userId": A, "plateId": "p1"}}},
         plato_de(BU) + [existe(RUTA_A_B, False), existe(RUTA_B_A, True)], "DENY"),
    # ── la lista es privada ───────────────────────────────────────────────────
    caso("A lee SU lista de bloqueados",
         {"path": RUTA_A_B, "method": "get", "auth": auth(A)}, [], "ALLOW"),
    caso("B intenta leer la lista de A",
         {"path": RUTA_A_B, "method": "get", "auth": auth(BU)}, [], "DENY"),
    caso("A escribe en SU lista",
         {"path": RUTA_A_B, "method": "create", "auth": auth(A),
          "resource": {"data": {"creadoEn": 1}}}, [], "ALLOW"),
    caso("B intenta escribir en la lista de A",
         {"path": RUTA_A_B, "method": "create", "auth": auth(BU),
          "resource": {"data": {"creadoEn": 1}}}, [], "DENY"),
    # ── avisos ────────────────────────────────────────────────────────────────
    # Un 'like' dispara push: sin esta regla el bloqueo se saltaria por el buzon.
    caso("A da like a B, que le bloqueo  (el aviso)",
         {"path": B + "/notifications/userB/items/n1", "method": "create", "auth": auth(A),
          "resource": {"data": {"type": "like", "fromUserId": A, "createdAt": 1, "isRead": False}}},
         [existe(RUTA_A_B, False), existe(RUTA_B_A, True)], "DENY"),
    caso("A da like a B sin bloqueo  (el aviso)",
         {"path": B + "/notifications/userB/items/n1", "method": "create", "auth": auth(A),
          "resource": {"data": {"type": "like", "fromUserId": A, "createdAt": 1, "isRead": False}}},
         [existe(RUTA_A_B, False), existe(RUTA_B_A, False)], "ALLOW"),
    # ── valorar ───────────────────────────────────────────────────────────────
    # ⚠️ EL CASO QUE FALTABA. La app crea el plato y esta valoracion en el MISMO lote, asi
    # que al evaluar la regla el plato aun no existe en el estado anterior. Con `get()`
    # esto denegaba, y nadie podia publicar nada. Ver la cabecera.
    caso("A publica su plato y lo valora EN EL MISMO LOTE",
         {"path": RUTA_VAL, "method": "create", "auth": auth(A),
          "resource": {"data": VALORACION}},
         plato_del_lote(A) + [existe(RUTA_A_A, False)], "ALLOW"),
    caso("A valora un plato de B, sin bloqueo",
         {"path": RUTA_VAL, "method": "create", "auth": auth(A),
          "resource": {"data": VALORACION}},
         plato_de(BU) + [existe(RUTA_A_B, False), existe(RUTA_B_A, False)], "ALLOW"),
    caso("A valora un plato de B, que le bloqueo  (SIMETRIA)",
         {"path": RUTA_VAL, "method": "create", "auth": auth(A),
          "resource": {"data": VALORACION}},
         plato_de(BU) + [existe(RUTA_A_B, False), existe(RUTA_B_A, True)], "DENY"),
    caso("A valora un plato de B, al que bloqueo",
         {"path": RUTA_VAL, "method": "create", "auth": auth(A),
          "resource": {"data": VALORACION}},
         plato_de(BU) + [existe(RUTA_A_B, True)], "DENY"),
    # Editar una valoracion anterior al bloqueo: bajarla a un 1 hace el mismo dano que
    # crearla con un 1, asi que el update tiene que mirar el bloqueo tambien.
    caso("A edita su valoracion del plato de B, sin bloqueo",
         {"path": RUTA_VAL, "method": "update", "auth": auth(A),
          "resource": {"data": dict(VALORACION, flavorScore=9, averageScore=9)}},
         plato_de(BU) + [existe(RUTA_A_B, False), existe(RUTA_B_A, False)], "ALLOW",
         previo=VALORACION),
    caso("A edita su valoracion del plato de B, que le bloqueo",
         {"path": RUTA_VAL, "method": "update", "auth": auth(A),
          "resource": {"data": dict(VALORACION, flavorScore=1, averageScore=1)}},
         plato_de(BU) + [existe(RUTA_A_B, False), existe(RUTA_B_A, True)], "DENY",
         previo=VALORACION),
    # ── me gusta ──────────────────────────────────────────────────────────────
    # Denegar solo el aviso dejaba el like puesto, y el uid del bloqueado seguia
    # apareciendo en "quien ha interactuado".
    caso("A da like al plato de B, sin bloqueo",
         {"path": PLATO, "method": "update", "auth": auth(A),
          "resource": {"data": plato(BU, 1, [A])}},
         [existe(RUTA_A_B, False), existe(RUTA_B_A, False)], "ALLOW",
         previo=plato(BU, 0, [])),
    caso("A da like al plato de B, que le bloqueo  (SIMETRIA)",
         {"path": PLATO, "method": "update", "auth": auth(A),
          "resource": {"data": plato(BU, 1, [A])}},
         [existe(RUTA_A_B, False), existe(RUTA_B_A, True)], "DENY",
         previo=plato(BU, 0, [])),
    # Quitar un like tuyo SIEMPRE debe poderse: si no, uno anterior al bloqueo se
    # quedaria pegado para siempre.
    caso("A quita su like aunque B le haya bloqueado",
         {"path": PLATO, "method": "update", "auth": auth(A),
          "resource": {"data": plato(BU, 0, [])}},
         [existe(RUTA_A_B, False), existe(RUTA_B_A, True)], "ALLOW",
         previo=plato(BU, 1, [A])),
]


def main():
    tok = token()
    fuente = io.open(os.path.join(RAIZ, "firestore.rules"), encoding="utf-8").read()
    cuerpo = {"source": {"files": [{"name": "firestore.rules", "content": fuente}]},
              "testSuite": {"testCases": [c for _, c in CASOS]}}
    peticion = urllib.request.Request(
        "https://firebaserules.googleapis.com/v1/projects/%s:test" % PROYECTO,
        data=json.dumps(cuerpo).encode(),
        headers={"Authorization": "Bearer " + tok, "Content-Type": "application/json",
                 "x-goog-user-project": PROYECTO})
    res = json.load(urllib.request.urlopen(peticion))

    mal = 0
    for (nombre, c), t in zip(CASOS, res.get("testResults", [])):
        esperado = "permite" if c["expectation"] == "ALLOW" else "deniega"
        ok = t.get("state") == "SUCCESS"
        if not ok:
            mal += 1
        print("  %-4s %-46s (esperado: %s)" % ("OK" if ok else "MAL", nombre, esperado))
        if not ok and t.get("debugMessages"):
            for m in t["debugMessages"][:3]:
                print("         %s" % str(m)[:120])
    print("\n  %d de %d" % (len(CASOS) - mal, len(CASOS)))
    return 1 if mal else 0


if __name__ == "__main__":
    sys.exit(main())
