# -*- coding: utf-8 -*-
"""Comprueba los dos strings.xml: paridad de claves y de marcadores de formato.

Un %1$s en un idioma y %1$d en el otro no falla al compilar: revienta en tiempo
de ejecucion y solo en ese idioma.
"""
import io, re, sys
import xml.etree.ElementTree as ET

EN = "app/src/main/res/values/strings.xml"
ES = "app/src/main/res/values-es/strings.xml"
MARCA = re.compile(r'%(\d+)\$([a-z])|%([a-z])')

NO_TRADUCIBLE = {"default_web_client_id"}


def leer(ruta):
    raiz = ET.parse(ruta).getroot()
    simples, plurales = {}, {}
    for n in raiz:
        nombre = n.get("name")
        if n.tag == "string":
            simples[nombre] = "".join(n.itertext())
        elif n.tag == "plurals":
            plurales[nombre] = {
                i.get("quantity"): "".join(i.itertext()) for i in n
            }
    return simples, plurales


def marcas(t):
    out = []
    for m in MARCA.finditer(t):
        out.append((m.group(1) or "?", m.group(2) or m.group(3)))
    return sorted(out)


en_s, en_p = leer(EN)
es_s, es_p = leer(ES)
problemas = []

solo_en = set(en_s) - set(es_s) - NO_TRADUCIBLE
solo_es = set(es_s) - set(en_s)
for k in sorted(solo_en):
    problemas.append("sin traducir al castellano: %s" % k)
for k in sorted(solo_es):
    problemas.append("sobra en castellano (no esta en values/): %s" % k)

for k in sorted(set(en_s) & set(es_s)):
    a, b = marcas(en_s[k]), marcas(es_s[k])
    if a != b:
        problemas.append("marcadores distintos en %s: en=%s es=%s" % (k, a, b))

solo_en_p = set(en_p) - set(es_p)
solo_es_p = set(es_p) - set(en_p)
for k in sorted(solo_en_p):
    problemas.append("plural sin traducir: %s" % k)
for k in sorted(solo_es_p):
    problemas.append("plural que sobra en castellano: %s" % k)
for k in sorted(set(en_p) & set(es_p)):
    for q in ("one", "other"):
        if q not in en_p[k] or q not in es_p[k]:
            problemas.append("plural %s sin cantidad '%s'" % (k, q))
            continue
        a, b = marcas(en_p[k][q]), marcas(es_p[k][q])
        if a != b:
            problemas.append("plural %s[%s]: en=%s es=%s" % (k, q, a, b))

# Apostrofe recto sin escapar: aapt lo rechaza, pero mejor avisar aqui. Ojo,
# ElementTree devuelve el backslash tal cual, asi que \' no cuenta.
SUELTO = re.compile(r"(?<!" + "\\" * 2 + r")'")
for ruta, d in ((EN, en_s), (ES, es_s)):
    for k, v in sorted(d.items()):
        if SUELTO.search(v):
            problemas.append("apostrofe sin escapar en %s (%s)" % (k, ruta.split("/")[-2]))


# Android RECORTA los espacios del principio y el final de un <string>, salvo que
# el valor vaya entre comillas dobles. Confiar en ellos deja textos pegados:
# "Termina en2d 10:23" en vez de "Termina en 2d 10:23" (visto en el Redmi).
CRUDO = re.compile(r'<string name="([^"]+)">(.*?)</string>', re.S)
for ruta in (EN, ES):
    bruto = io.open(ruta, encoding="utf-8").read()
    for m in CRUDO.finditer(bruto):
        nombre, val = m.group(1), m.group(2)
        if val.startswith('"') and val.endswith('"'):
            continue
        if val != val.strip():
            problemas.append(
                "espacios que aapt se come en %s (%s): %r"
                % (nombre, ruta.split("/")[-2], val))

print("claves: en=%d es=%d | plurales: en=%d es=%d"
      % (len(en_s), len(es_s), len(en_p), len(es_p)))
if problemas:
    print("\n".join(problemas))
    print("PROBLEMAS: %d" % len(problemas))
    sys.exit(1)
print("OK: paridad de claves y marcadores correcta")
