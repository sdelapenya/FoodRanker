# -*- coding: utf-8 -*-
"""Comprueba que la app y el servidor redactan los avisos EXACTAMENTE igual.

Un aviso puede llegar por dos caminos: el push que redacta Cloud Functions y el
escuchador de Firestore que redacta la app. El id de la notificacion sale de
hashCode() del propio texto (NotificationHelper.show), justo para que el segundo
reemplace al primero en vez de apilarse. Si una coma cambia en un lado, el hash
cambia y el usuario ve DOS avisos del mismo hecho.

No vale con comparar plantillas: una usa {from} y la otra %1$s. Se rellenan las
dos con los mismos valores de ejemplo y se comparan los resultados.

    python -I tools/i18n/check_notif_parity.py
"""
import io, os, re, sys
import xml.etree.ElementTree as ET

EN = "app/src/main/res/values/strings.xml"
ES = "app/src/main/res/values-es/strings.xml"
TS = "functions/src/index.ts"

# Valores de ejemplo. El numero del puesto va con dos cifras para que no cuele
# una diferencia de formato entre %d y una concatenacion.
V = {"from": "Dani", "plate": "Tortilla de patatas", "score": "8,5",
     "text": "estaba buenisima", "level": "Top Chef", "badge": "Globetrotter",
     "medal": "\U0001f947", "pos": 12}

# clave del cliente -> (clave del servidor, marcadores en el orden de Android)
MAPA = {
    "notif_like_t":          ("like_t", []),
    "notif_like_b":          ("like_b", ["from", "plate"]),
    "notif_rating_t":        ("rating_t", []),
    "notif_rating_b":        ("rating_b", ["from", "plate", "score"]),
    "notif_comment_t":       ("comment_t", []),
    "notif_comment_b":       ("comment_b", ["from", "plate", "text"]),
    "notif_comment_b_plain": ("comment_b_plain", ["from", "plate"]),
    "notif_follow_t":        ("follow_t", []),
    "notif_follow_b":        ("follow_b", ["from"]),
    "notif_newplate_t":      ("newplate_t", []),
    "notif_newplate_b":      ("newplate_b", ["from", "plate"]),
    "notif_approved_t":      ("approved_t", []),
    "notif_approved_b":      ("approved_b", ["plate"]),
    "notif_rejected_t":      ("rejected_t", []),
    "notif_rejected_b":      ("rejected_b", ["plate"]),
    "notif_level_t":         ("level_t", []),
    "notif_level_b":         ("level_b", ["level"]),
    "notif_badge_t":         ("badge_t", []),
    "notif_badge_b":         ("badge_b", ["badge"]),
    "notif_league_t":        ("league_t", ["medal", "pos"]),
    "notif_league_b_win":    ("league_b_win", []),
    "notif_league_b_other":  ("league_b_other", ["pos"]),
}

BS = chr(92)


def recursos(ruta):
    """Las cadenas, deshaciendo los escapes de Android."""
    out = {}
    for n in ET.parse(ruta).getroot():
        if n.tag != "string":
            continue
        t = "".join(n.itertext())
        t = t.replace(BS + '"', '"').replace(BS + "'", "'").replace(BS + "n", "\n")
        out[n.get("name")] = t
    return out


def servidor():
    """Las tablas TEXTOS de index.ts, por idioma."""
    s = io.open(TS, encoding="utf-8").read()
    bloque = s[s.index("const TEXTOS"):s.index("const NOMBRES_I18N")]
    tablas, idioma = {}, None
    for linea in bloque.split("\n"):
        m = re.match(r'^\s*(es|en):\s*\{', linea)
        if m:
            idioma = m.group(1)
            tablas[idioma] = {}
            continue
        m = re.match(r'^\s*(\w+):\s*([\'"])(.*?)\2,\s*$', linea)
        if m and idioma:
            tablas[idioma][m.group(1)] = m.group(3)
    return tablas


def rellenar_android(t, orden):
    for i, nombre in enumerate(orden, 1):
        v = str(V[nombre])
        t = t.replace("%%%d$s" % i, v).replace("%%%d$d" % i, v)
    return t


def rellenar_servidor(t):
    for k, v in V.items():
        t = t.replace("{%s}" % k, str(v))
    return t


srv = servidor()
faltan = [i for i in ("es", "en") if i not in srv]
if faltan:
    print("no encuentro las tablas del servidor para: %s" % faltan)
    sys.exit(1)

problemas = 0
for ruta, idioma in ((ES, "es"), (EN, "en")):
    res = recursos(ruta)
    for clave, (clave_srv, orden) in sorted(MAPA.items()):
        if clave not in res:
            print("[%s] falta el recurso %s" % (idioma, clave))
            problemas += 1
            continue
        if clave_srv not in srv[idioma]:
            print("[%s] falta %s en TEXTOS del servidor" % (idioma, clave_srv))
            problemas += 1
            continue
        a = rellenar_android(res[clave], orden)
        b = rellenar_servidor(srv[idioma][clave_srv])
        if a != b:
            problemas += 1
            print("[%s] %s != %s" % (idioma, clave, clave_srv))
            print("    app:      %r" % a)
            print("    servidor: %r" % b)

print()
if problemas:
    print("avisos que se verian DUPLICADOS: %d" % problemas)
    sys.exit(1)
print("OK: los %d avisos se redactan igual en los dos lados, en ambos idiomas"
      % len(MAPA))
