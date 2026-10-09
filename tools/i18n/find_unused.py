# -*- coding: utf-8 -*-
"""Lista las cadenas que no referencia nadie.

No se fia de la lista de lint: busca cada clave en TODAS las formas en que se
puede usar (R.string.x, R.plurals.x, @string/x, @plurals/x) por todo el fuente,
los recursos y el manifest.

    python -I tools/i18n/find_unused.py          # solo lista
    python -I tools/i18n/find_unused.py --borrar # las quita de los dos strings.xml
"""
import io, os, re, sys
import xml.etree.ElementTree as ET

BS = chr(92)
EN = "app/src/main/res/values/strings.xml"
ES = "app/src/main/res/values-es/strings.xml"
RAIZ = "app/src/main"

# Las lee el SDK por nombre en tiempo de ejecucion, no desde nuestro codigo.
INTOCABLES = {
    "default_web_client_id", "gcm_defaultSenderId", "google_api_key",
    "google_app_id", "google_crash_reporting_api_key", "google_storage_bucket",
    "project_id", "firebase_database_url", "app_name",
}


def claves(ruta):
    out = []
    for n in ET.parse(ruta).getroot():
        if n.tag in ("string", "plurals"):
            out.append((n.tag, n.get("name")))
    return out


def texto_del_proyecto():
    """Todo el fuente, los recursos y el manifest, menos los propios strings.xml."""
    trozos = []
    for root, _, fs in os.walk(RAIZ):
        for f in fs:
            if not f.endswith((".kt", ".java", ".xml")):
                continue
            p = os.path.join(root, f).replace(BS, "/")
            if p.endswith(("values/strings.xml", "values-es/strings.xml")):
                continue
            try:
                trozos.append(io.open(p, encoding="utf-8", errors="replace").read())
            except OSError:
                pass
    return "\n".join(trozos)


def limpiar_comentarios(s):
    """Quita los comentarios de seccion que se han quedado sin cadenas debajo.

    Por lineas y no con una expresion: en el fichero los comentarios van
    separados por lineas en blanco, y un lookahead se complica de mas. Un
    comentario sobra si la siguiente linea con algo es otro comentario o el
    cierre de <resources>.
    """
    lineas = s.split("\n")
    fuera = set()
    for i, l in enumerate(lineas):
        if not re.match(r'^\s*<!--.*-->\s*$', l):
            continue
        for j in range(i + 1, len(lineas)):
            sig = lineas[j].strip()
            if not sig:
                continue
            if sig.startswith("<!--") or sig.startswith("</resources>"):
                fuera.add(i)
            break
    if not fuera:
        return s
    # Se quita el comentario y la linea en blanco que lo seguia, si la habia.
    for i in sorted(fuera, reverse=True):
        fin = i + 1
        while fin < len(lineas) and not lineas[fin].strip():
            fin += 1
        del lineas[i:fin]
    return limpiar_comentarios("\n".join(lineas))


todo = texto_del_proyecto()
# Tambien dentro de los propios strings.xml puede haber referencias cruzadas.
for r in (EN, ES):
    todo += "\n" + re.sub(r'<string name="[^"]*">', "", io.open(r, encoding="utf-8").read())

sin_usar = []
for tipo, nombre in claves(EN):
    if nombre in INTOCABLES:
        continue
    pat = re.compile(r'(?:R\.%s\.%s|@%s/%s)\b' % (
        "plurals" if tipo == "plurals" else "string", re.escape(nombre),
        "plurals" if tipo == "plurals" else "string", re.escape(nombre)))
    if not pat.search(todo):
        sin_usar.append((tipo, nombre))

print("cadenas definidas: %d" % len(claves(EN)))
print("sin ninguna referencia: %d" % len(sin_usar))
for tipo, nombre in sin_usar:
    print("   %s/%s" % (tipo, nombre))

if "--borrar" not in sys.argv:
    print("\n(solo listado; pasa --borrar para quitarlas)")
    sys.exit(0)

nombres = {n for _, n in sin_usar}
for ruta in (EN, ES):
    s = io.open(ruta, encoding="utf-8").read()
    fuera = 0
    for tipo, nombre in sin_usar:
        if tipo == "string":
            pat = re.compile(r'[ \t]*<string name="%s">.*?</string>\n' % re.escape(nombre), re.S)
        else:
            pat = re.compile(r'[ \t]*<plurals name="%s">.*?</plurals>\n' % re.escape(nombre), re.S)
        s, n = pat.subn("", s)
        fuera += n
    s = limpiar_comentarios(s)
    with io.open(ruta + ".tmp", "w", encoding="utf-8", newline="\n") as f:
        f.write(s)
    os.replace(ruta + ".tmp", ruta)
    print("%s: quitadas %d" % (ruta.split("/")[-2], fuera))
