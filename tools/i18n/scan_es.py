# -*- coding: utf-8 -*-
"""Busca literales de texto visible sin traducir en el codigo Kotlin."""
import io, os, re

BS = chr(92)
LIT = re.compile('"((?:[^"' + BS + BS + ']|' + BS + BS + '.)*)"')
LETRAS = re.compile(r'[a-zA-ZáéíóúñÁÉÍÓÚÑ]{3,}')
PAL = re.compile(
    r'\b(de|la|el|los|las|un|una|que|con|por|para|tu|tus|mi|mis|sin|aun|'
    r'aún|más|este|esta|hay|ya|sé|no|en|al|del|se|es|son|ha|'
    r'han|te|le|lo|su|sus|y)\b', re.I)

# Una palabra sola que parece texto: empieza por mayuscula y sigue en minusculas,
# con acentos y signos finales permitidos. No encajan ni UNCHECKED_CAST ni
# MissingPermission ni cardScale.
PALABRA_SUELTA = re.compile(r'^[A-ZÁÉÍÓÚÑ][a-záéíóúñ]{2,}[.!?…]*$')

# Codigo muerto: la siembra de MealDB se descarto como decision de producto.
IGNORAR = ("SeedData.kt", "MealDBSeeder.kt")

logs, vis = [], []
for root, _, fs in os.walk("app/src/main/java/com/app/foodranker"):
    for f in fs:
        if not f.endswith(".kt") or f in IGNORAR:
            continue
        p = os.path.join(root, f).replace(BS, "/")
        corto = p.replace("app/src/main/java/com/app/foodranker/", "")
        for i, linea in enumerate(io.open(p, encoding="utf-8"), 1):
            s = linea.strip()
            if s.startswith("//") or s.startswith("*") or s.startswith("/*"):
                continue
            l2 = re.sub(r'//.*$', '', linea)
            for m in LIT.finditer(l2):
                t = m.group(1)
                if not LETRAS.search(t):
                    continue
                nucleo = re.sub(r'\$\{[^}]*\}|\$[a-zA-Z_]+', '', t).strip()
                if not LETRAS.search(nucleo):
                    continue
                if " " not in nucleo and not PAL.search(nucleo):
                    # Una palabra suelta: casi siempre es un identificador, pero por
                    # aqui se colaron "Seguidores", "Siguiendo" y "Buscando...". Se
                    # queda si parece texto y no codigo: inicial mayuscula y el resto
                    # minusculas. Asi caen fuera UNCHECKED_CAST, MissingPermission,
                    # cardScale y las rutas, que son lo que de verdad abunda.
                    if not PALABRA_SUELTA.match(nucleo):
                        continue
                destino = logs if re.search(r'(Log\.[a-z]|println|Timber)', l2) else vis
                destino.append((corto, i, t))

SAL = os.environ.get("TEMP", ".") + "/scan_es.txt"
with io.open(SAL, "w", encoding="utf-8", newline="\n") as f:
    f.write("===== VISIBLE (%d) =====\n" % len(vis))
    for p, i, t in vis:
        f.write("%s:%d  %s\n" % (p, i, t[:95]))
    f.write("\n===== SOLO LOGS (%d) =====\n" % len(logs))
    for p, i, t in logs:
        f.write("%s:%d  %s\n" % (p, i, t[:95]))
print("visible=%d logs=%d -> %s" % (len(vis), len(logs), SAL))
