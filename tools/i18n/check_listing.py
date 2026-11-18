# -*- coding: utf-8 -*-
"""Comprueba que la ficha de Play cabe en los limites de la consola.

Play rechaza el texto al pegarlo, asi que es mejor saberlo antes. Lee los
bloques ``` de docs/play-store-listing.md en el orden en que estan.
"""
import io, re, sys

DOC = "docs/play-store-listing.md"
LIMITES = {"Nombre": 30, "Descripción breve": 80, "Descripción completa": 4000}
LIMITE_NOVEDADES = 500

s = io.open(DOC, encoding="utf-8").read()

# (titulo de la seccion mas cercana, contenido del bloque)
bloques = []
for m in re.finditer(r'```\n(.*?)```', s, re.S):
    antes = s[:m.start()]
    cab = re.findall(r'^#{2,4} (.+)$', antes, re.M)
    idioma = [c for c in cab if "(" in c and ")" in c]
    bloques.append((cab[-1] if cab else "?",
                    idioma[-1] if idioma else "?",
                    m.group(1).rstrip("\n")))

# Los bloques "Los tres de una vez" llevan las tres notas dentro de <es-ES>, <en-GB> y
# <en-US>, que es como Play pide pegarlas de golpe. El limite de 500 es POR IDIOMA, no
# del bloque entero: medir el bloque daba ~950 y marcaba "SE PASA" en cada ejecucion,
# con las tres notas de dentro holgadamente por debajo. Un aviso que siempre esta
# encendido no avisa de nada, asi que aqui se mide cada etiqueta por separado.
ETIQUETA = re.compile(r'<(\w\w-\w\w)>\n(.*?)</\1>', re.S)


def partes(campo, idioma, texto):
    """Cada trozo que hay que medir por separado, como (idioma, texto)."""
    tramos = ETIQUETA.findall(texto)
    if not tramos:
        return [(idioma, texto)]
    return [("%s %s" % (idioma, tag), cuerpo.strip("\n")) for tag, cuerpo in tramos]


problemas = 0
print("%-22s %-18s %6s %6s" % ("campo", "idioma", "chars", "limite"))
for campo, idioma, texto in bloques:
    limite = LIMITES.get(campo)
    if limite is None:
        # Las novedades van bajo encabezados de idioma, no de campo.
        limite = LIMITE_NOVEDADES
        campo_m = "Novedades"
    else:
        campo_m = campo
    for idioma_m, trozo in partes(campo, idioma, texto):
        n = len(trozo)
        marca = "" if n <= limite else "  <<< SE PASA"
        if n > limite:
            problemas += 1
        print("%-22s %-18s %6d %6d%s" % (campo_m, idioma_m, n, limite, marca))

print()
if problemas:
    print("campos que se pasan del limite: %d" % problemas)
    sys.exit(1)
print("OK: todo cabe")
