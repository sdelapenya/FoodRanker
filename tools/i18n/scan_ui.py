# -*- coding: utf-8 -*-
"""Segunda pasada: literales en posicion de texto visible de Compose.

El primer escaner filtraba por palabras castellanas y se dejaba fuera las de una
sola palabra ("Buscando...", "Guardados"). Aqui el criterio no es el idioma sino
el sitio: lo que va en un Text(), un title=, un label=, etc. deberia salir
siempre de los recursos, en cualquier idioma.
"""
import io, os, re

BS = chr(92)
LIT = '"((?:[^"' + BS + BS + ']|' + BS + BS + '.)*)"'
LETRAS = re.compile(r'[a-zA-Z]{3,}')

PATRONES = [
    re.compile(r'\bText\(\s*' + LIT),
    re.compile(r'\b(?:title|subtitle|message|label|placeholder|contentDescription|'
               r'text|supportingText|headline|body|hint|descripcion|titulo)\s*=\s*' + LIT),
    re.compile(r'\bToast\.makeText\([^,]+,\s*' + LIT),
    re.compile(r'\bState\.Error\(\s*' + LIT),
]

# Marca, dominios, patrones de fecha y claves tecnicas: no son texto traducible.
BLANCA = {
    "FoodRanker", "EAT. RATE. REPEAT.", "🍽️ FoodRanker", "Bio",
    "thefork.de", "thefork.es", "dd MMM", "text/plain", "image/png",
}

hits = []
for root, _, fs in os.walk("app/src/main/java/com/app/foodranker"):
    for f in fs:
        if not f.endswith(".kt") or f in ("SeedData.kt", "MealDBSeeder.kt"):
            continue
        p = os.path.join(root, f).replace(BS, "/")
        corto = p.replace("app/src/main/java/com/app/foodranker/", "")
        for i, linea in enumerate(io.open(p, encoding="utf-8"), 1):
            s = linea.strip()
            if s.startswith("//") or s.startswith("*") or s.startswith("/*"):
                continue
            l2 = re.sub(r'//.*$', '', linea)
            for pat in PATRONES:
                for m in pat.finditer(l2):
                    t = m.group(1)
                    if t in BLANCA or not LETRAS.search(t):
                        continue
                    # Solo interpolacion o formato: no hay texto que traducir.
                    nucleo = re.sub(r'\$\{[^}]*\}|\$[a-zA-Z_.]+|%[-0-9.]*[a-z]', '', t).strip()
                    if not LETRAS.search(nucleo) or nucleo in BLANCA:
                        continue
                    hits.append((corto, i, t))

SAL = os.environ.get("TEMP", ".") + "/scan_ui.txt"
vistos = set()
with io.open(SAL, "w", encoding="utf-8", newline="\n") as f:
    for p, i, t in hits:
        if (p, i, t) in vistos:
            continue
        vistos.add((p, i, t))
        f.write("%s:%d  %s\n" % (p, i, t[:95]))
print("posiciones de UI con literal: %d -> %s" % (len(vistos), SAL))
