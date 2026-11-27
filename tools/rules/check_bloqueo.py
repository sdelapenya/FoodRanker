# -*- coding: utf-8 -*-
"""Prueba las reglas de bloqueo entre usuarios contra el motor REAL de Firestore.

No es el simulador de la consola ni una lectura a ojo: manda `firestore.rules` a
`firebaserules.projects.test`, el mismo evaluador que corre en produccion, y comprueba
once casos. **No escribe nada en ninguna base de datos.**

## Por que hace falta

El bloqueo es SIMETRICO: ni A ve a B, ni B puede seguir, comentar o valorar a A. Eso no se
puede verificar leyendo las reglas, porque depende de dos `exists()` cruzados y de un
`get()` al plato. Y es justo la parte que, si se rompe, deja un boton que promete una
proteccion que no da -- que es peor que no tenerlo.

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
Sale 0 si pasan los once casos, 1 si falla alguno.
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


def trae(ruta, datos):
    return {"function": "get", "args": [{"exactValue": ruta}], "result": {"value": {"data": datos}}}


def caso(nombre, peticion, mocks, espera):
    return (nombre, {"expectation": espera, "pathEncoding": "URL_ENCODED",
                     "request": peticion, "functionMocks": mocks})


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
         [trae(PLATO, {"addedByUserId": BU}), existe(RUTA_A_B, False), existe(RUTA_B_A, False)], "ALLOW"),
    caso("A comenta un plato de B, que le bloqueo  (SIMETRIA)",
         {"path": B + "/comments/c1", "method": "create", "auth": auth(A),
          "resource": {"data": {"userId": A, "plateId": "p1"}}},
         [trae(PLATO, {"addedByUserId": BU}), existe(RUTA_A_B, False), existe(RUTA_B_A, True)], "DENY"),
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
