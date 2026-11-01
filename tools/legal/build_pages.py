# -*- coding: utf-8 -*-
"""Genera las páginas legales de gh-pages a partir de strings.xml.

Por qué existe. Los textos legales viven en dos sitios —la app y la web pública a la
que apuntan Play, AdMob y la propia ficha— y tienen que decir lo mismo. Mantenerlos a
mano no funcionó: cuando se escribió esto, la web llevaba meses sin la sección "Quién
es responsable de tus datos", ponía una fecha distinta, daba otro correo de contacto y
hablaba de la Unión Europea donde la app ya hablaba del EEE y el Reino Unido. Nadie lo
vio porque no había forma de verlo.

Ahora la app es la única fuente y la web se genera. Si los textos cambian, se regenera
y se vuelve a publicar; no hay un segundo sitio donde olvidarse de algo.

    python -I tools/legal/build_pages.py <directorio-de-salida>

Sin argumento escribe en tools/legal/out/. Genera cuatro páginas: privacidad y términos
en castellano (privacy.html, terms.html — los nombres que ya están publicados y a los
que apuntan Play y AdMob, no se pueden cambiar) y en inglés (privacy-en.html,
terms-en.html).

NO genera index.html, style.css ni delete-account.html: esas no salen de strings.xml.
"""
import io
import os
import re
import sys
import html
import xml.etree.ElementTree as ET

RAIZ = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

IDIOMAS = {
    "es": os.path.join(RAIZ, "app/src/main/res/values-es/strings.xml"),
    "en": os.path.join(RAIZ, "app/src/main/res/values/strings.xml"),
}

# Los nombres en castellano son los que ya están publicados: Play, AdMob y la ficha
# apuntan a ellos. Cambiarlos rompería enlaces que no controlamos.
PAGINAS = {
    ("privacy", "es"): "privacy.html",
    ("privacy", "en"): "privacy-en.html",
    ("terms", "es"): "terms.html",
    ("terms", "en"): "terms-en.html",
}

DOCS = {
    "privacy": {"prefijo": "pp_", "titulo": "pp_title", "fecha": "pp_updated"},
    "terms": {"prefijo": "tos_", "titulo": "tos_screen_title", "fecha": "tos_updated"},
}

TEXTOS = {
    "es": {
        "nav_privacy": "Privacidad",
        "nav_terms": "Términos",
        "nav_delete": "Eliminar cuenta",
        "otro_idioma": "English",
        "desc_privacy": "Política de Privacidad de la aplicación FoodRanker.",
        "desc_terms": "Términos de Servicio de la aplicación FoodRanker.",
        "pie_privacy": "Política de Privacidad",
        "pie_terms": "Términos de Servicio",
    },
    "en": {
        "nav_privacy": "Privacy",
        "nav_terms": "Terms",
        "nav_delete": "Delete account",
        "otro_idioma": "Castellano",
        "desc_privacy": "Privacy Policy for the FoodRanker app.",
        "desc_terms": "Terms of Service for the FoodRanker app.",
        "pie_privacy": "Privacy Policy",
        "pie_terms": "Terms of Service",
    },
}

AVISO = ("<!-- GENERADO por tools/legal/build_pages.py desde strings.xml. "
         "No editar a mano: el siguiente build lo sobrescribe. -->")

# El último tramo no puede acabar en punto: si no, "escríbenos a foo@bar.com." se
# lleva el punto de la frase dentro del mailto:.
RE_CORREO = re.compile(r"[\w.+-]+@[\w-]+(?:\.[\w-]+)+")


def carga(ruta):
    """Devuelve {clave: texto} con los escapes de Android ya resueltos."""
    raiz = ET.parse(ruta).getroot()
    fuera = {}
    for nodo in raiz.findall("string"):
        if nodo.text is None:
            continue
        fuera[nodo.get("name")] = desescapa(nodo.text)
    return fuera


def desescapa(s):
    """Escapes de Android: \\n es salto de línea, y \\' \\" \\\\ son el carácter suelto."""
    salida = []
    i = 0
    while i < len(s):
        c = s[i]
        if c == chr(92) and i + 1 < len(s):
            sig = s[i + 1]
            if sig == "n":
                salida.append("\n")
            elif sig == "t":
                salida.append("\t")
            else:
                salida.append(sig)
            i += 2
        else:
            salida.append(c)
            i += 1
    return "".join(salida)


def enlaza_correos(texto_escapado):
    def sub(m):
        c = m.group(0)
        return '<a href="mailto:%s">%s</a>' % (c, c)
    return RE_CORREO.sub(sub, texto_escapado)


def cuerpo_a_html(cuerpo):
    """Bloques separados por línea en blanco; las líneas que empiezan por viñeta
    se agrupan en una lista. Es exactamente como lo pinta la app."""
    trozos = []
    for bloque in cuerpo.split("\n\n"):
        lineas = [l.strip() for l in bloque.split("\n") if l.strip()]
        if not lineas:
            continue
        if all(l.startswith("•") for l in lineas):
            items = ["      <li>%s</li>" % enlaza_correos(html.escape(l.lstrip("• ").strip()))
                     for l in lineas]
            trozos.append("    <ul>\n" + "\n".join(items) + "\n    </ul>")
        else:
            for l in lineas:
                trozos.append("    <p>%s</p>" % enlaza_correos(html.escape(l)))
    return "\n".join(trozos)


def secciones(claves, prefijo):
    """Descubre las secciones existentes en vez de fijar cuántas hay, para que añadir
    una a strings.xml no exija tocar este fichero."""
    numeros = sorted(
        int(m.group(1))
        for k in claves
        for m in [re.match(r"^" + prefijo + r"(\d+)_t$", k)]
        if m
    )
    fuera = []
    for n in numeros:
        t = claves.get("%s%d_t" % (prefijo, n))
        b = claves.get("%s%d_b" % (prefijo, n))
        if t is None or b is None:
            raise SystemExit("falta %s%d_t o _b" % (prefijo, n))
        fuera.append((t, b))
    return fuera


def construye(doc, idioma, claves):
    cfg = DOCS[doc]
    t = TEXTOS[idioma]
    titulo = claves[cfg["titulo"]]
    fecha = claves[cfg["fecha"]]

    otro = "en" if idioma == "es" else "es"
    href_privacy = PAGINAS[("privacy", idioma)]
    href_terms = PAGINAS[("terms", idioma)]
    href_otro = PAGINAS[(doc, otro)]

    partes = []
    for st, sb in secciones(claves, cfg["prefijo"]):
        partes.append("  <section>\n    <h2>%s</h2>\n%s\n  </section>"
                      % (html.escape(st), cuerpo_a_html(sb)))

    if doc == "privacy":
        pie_href, pie_txt, desc = href_terms, t["pie_terms"], t["desc_privacy"]
    else:
        pie_href, pie_txt, desc = href_privacy, t["pie_privacy"], t["desc_terms"]

    return """<!DOCTYPE html>
{aviso}
<html lang="{idioma}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{titulo} &mdash; FoodRanker</title>
<meta name="description" content="{desc}">
<link rel="stylesheet" href="style.css">
</head>
<body>
<header class="topbar">
  <div class="wrap">
    <a class="brand" href="index.html">Food<span>Ranker</span></a>
    <nav>
      <a href="{href_privacy}"{cur_p}>{nav_privacy}</a>
      <a href="{href_terms}"{cur_t}>{nav_terms}</a>
      <a href="delete-account.html">{nav_delete}</a>
      <a href="{href_otro}">{otro_idioma}</a>
    </nav>
  </div>
</header>

<main>
  <h1>{titulo}</h1>
  <p class="updated">{fecha}</p>

{secciones}

  <footer>
    <p>&copy; 2026 FoodRanker &middot; <a href="{pie_href}">{pie_txt}</a></p>
  </footer>
</main>
</body>
</html>
""".format(
        aviso=AVISO,
        idioma=idioma,
        titulo=html.escape(titulo),
        desc=html.escape(desc),
        fecha=html.escape(fecha),
        href_privacy=href_privacy,
        href_terms=href_terms,
        href_otro=href_otro,
        cur_p=' aria-current="page"' if doc == "privacy" else "",
        cur_t=' aria-current="page"' if doc == "terms" else "",
        nav_privacy=t["nav_privacy"],
        nav_terms=t["nav_terms"],
        nav_delete=t["nav_delete"],
        otro_idioma=t["otro_idioma"],
        secciones="\n\n".join(partes),
        pie_href=pie_href,
        pie_txt=pie_txt,
    )


def main():
    destino = sys.argv[1] if len(sys.argv) > 1 else os.path.join(RAIZ, "tools/legal/out")
    if not os.path.isdir(destino):
        os.makedirs(destino)

    cargados = {k: carga(v) for k, v in IDIOMAS.items()}

    for (doc, idioma), nombre in sorted(PAGINAS.items()):
        pagina = construye(doc, idioma, cargados[idioma])
        ruta = os.path.join(destino, nombre)
        with io.open(ruta, "w", encoding="utf-8", newline="\n") as f:
            f.write(pagina)
        print("%-18s %d secciones" % (nombre, pagina.count("<section>")))

    print("")
    print("escrito en: %s" % destino)


if __name__ == "__main__":
    main()
