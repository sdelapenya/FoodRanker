# -*- coding: utf-8 -*-
"""Comprueba que cada uso pase tantos argumentos como marcadores tiene la cadena.

El compilador no lo mira: una cadena con %1$s llamada sin argumentos lanza
MissingFormatArgumentException en cuanto se pinta esa pantalla.
"""
import io, os, re, sys
import xml.etree.ElementTree as ET

BS = chr(92)
EN = "app/src/main/res/values/strings.xml"
MARCA = re.compile(r'%(\d+)\$[a-z]')

raiz = ET.parse(EN).getroot()
nmarcas = {}
for n in raiz:
    nombre = n.get("name")
    if n.tag == "string":
        t = "".join(n.itertext())
        idx = [int(m.group(1)) for m in MARCA.finditer(t)]
        nmarcas[("string", nombre)] = max(idx) if idx else 0
    elif n.tag == "plurals":
        idx = []
        for i in n:
            idx += [int(m.group(1)) for m in MARCA.finditer("".join(i.itertext()))]
        nmarcas[("plurals", nombre)] = max(idx) if idx else 0

LLAMADA = re.compile(
    r'\b(?:getString|stringResource|getQuantityString|pluralStringResource)\s*\(')


def argumentos(s, i):
    """Devuelve la lista de argumentos de la llamada que abre en s[i] == '('."""
    prof, ini, args, en_txt, escapa = 0, i + 1, [], False, False
    j = i
    while j < len(s):
        c = s[j]
        if escapa:
            escapa = False
        elif c == BS:
            escapa = True
        elif en_txt:
            if c == '"':
                en_txt = False
        elif c == '"':
            en_txt = True
        elif c in "([{":
            prof += 1
        elif c in ")]}":
            prof -= 1
            if prof == 0:
                args.append(s[ini:j].strip())
                return [a for a in args if a], j
        elif c == "," and prof == 1:
            args.append(s[ini:j].strip())
            ini = j + 1
        j += 1
    return None, j


problemas = 0
for root, _, fs in os.walk("app/src/main/java/com/app/foodranker"):
    for f in fs:
        if not f.endswith(".kt"):
            continue
        p = os.path.join(root, f).replace(BS, "/")
        corto = p.replace("app/src/main/java/com/app/foodranker/", "")
        s = io.open(p, encoding="utf-8").read()
        for m in LLAMADA.finditer(s):
            args, fin = argumentos(s, m.end() - 1)
            if args is None:
                continue
            # Busca el R.string./R.plurals. en los argumentos
            ref = None
            for a in args:
                g = re.search(r'R\.(string|plurals)\.([A-Za-z0-9_]+)', a)
                if g:
                    ref = (g.group(1), g.group(2))
                    pos = args.index(a)
                    break
            if ref is None or ref not in nmarcas:
                continue
            necesita = nmarcas[ref]
            # argumentos de formato = los que van despues del id (y de la cantidad
            # en el caso de los plurales)
            extra = len(args) - pos - 1
            if ref[0] == "plurals":
                extra -= 1  # la cantidad no es un argumento de formato
            if extra < necesita:
                linea = s.count("\n", 0, m.start()) + 1
                print("%s:%d  R.%s.%s necesita %d argumento(s), recibe %d"
                      % (corto, linea, ref[0], ref[1], necesita, max(extra, 0)))
                problemas += 1

print("usos con argumentos insuficientes: %d" % problemas)
sys.exit(1 if problemas else 0)
