#!/usr/bin/env python3
"""Vigila que las pantallas no vuelvan a clavar los colores del tema claro.

## Que problema resuelve

El tema oscuro de FoodRanker existia desde el principio: `FoodRankerTheme` calcula un
`FoodRankerColorTokens` con los colores de cada modo y lo publica en `LocalFoodColors`.
Lo que no existia era nadie que lo leyera. Hasta el 2026-11-17 las pantallas usaban
directamente las constantes del tema CLARO (`TextPrimary`, `SurfaceWhite`, ...), 330
veces en 24 ficheros, y `FoodTheme.colors` no aparecia ni una vez fuera de su propia
definicion. Resultado: en modo oscuro los dialogos de Material salian oscuros (esos si
usan `MaterialTheme`) y el resto de la app seguia blanca. Una mezcla, no un tema.

Se convirtieron 304 usos a `FoodTheme.colors.*`. Esto esta aqui para que no vuelvan: la
constante sigue existiendo y compila igual de bien en los dos sitios, asi que el fallo no
lo caza el compilador ni se nota si solo se prueba en claro -- que es como se probo
durante dos anos.

## Uso

    python tools/ui/check_theme_colors.py

Sale con codigo 1 si encuentra algo. Pensado para ejecutarlo al tocar la UI, junto con
los de tools/i18n/.
"""
import os
import re
import sys

RAIZ = os.path.join("app", "src", "main", "java", "com", "app", "foodranker", "ui")

# Constantes del tema claro que NO deben aparecer en una pantalla, y con que sustituirlas.
PROHIBIDAS = {
    "BackgroundLight": "FoodTheme.colors.background",
    "SurfaceWhite": "FoodTheme.colors.surface",
    "SurfaceMuted": "FoodTheme.colors.surfaceMuted",
    "TextPrimary": "FoodTheme.colors.textPrimary",
    "TextSecondary": "FoodTheme.colors.textSecondary",
    "DividerColor": "FoodTheme.colors.divider",
}
TOKEN = re.compile(r"\b(" + "|".join(PROHIBIDAS) + r")\b")

# Lo unico exento por fichero: donde se definen los dos temas.
EXENTOS = {"theme": "es la definicion del tema"}

# Un color fijo puede ser CORRECTO, pero solo sabiendolo. Para esos casos se pone el
# marcador en la propia linea, con el motivo detras:
#
#     contentColor = TextPrimary // tema-fijo: boton blanco sobre degradado
#
# Va en la linea y no en una lista aparte a proposito: una lista de excepciones por
# numero de linea se desfasa con el primer commit que mueva el fichero, y encima deja la
# razon lejos de la linea que la necesita. Hoy hay tres sitios asi, y los tres son el
# mismo caso: contenido sobre un fondo que NO sigue el tema (el degradado naranja del
# login, y la tarjeta que se convierte en imagen para compartir).
MARCADOR = "tema-fijo:"


def exento(ruta: str) -> str:
    partes = ruta.replace("\\", "/").split("/")
    for clave, motivo in EXENTOS.items():
        if clave in partes:
            return motivo
    return ""


def main() -> int:
    if not os.path.isdir(RAIZ):
        print(f"no encuentro {RAIZ} -- ejecutalo desde la raiz del repo")
        return 2

    fallos = []
    exentos_vistos = {}
    for base, _, ficheros in os.walk(RAIZ):
        for f in ficheros:
            if not f.endswith(".kt"):
                continue
            ruta = os.path.join(base, f)
            motivo = exento(ruta)
            with open(ruta, encoding="utf-8") as fh:
                for n, linea in enumerate(fh, 1):
                    if linea.lstrip().startswith(("import ", "//", "*", "/*")):
                        continue
                    m = TOKEN.search(linea)
                    if not m:
                        continue
                    if MARCADOR in linea:
                        continue
                    if motivo:
                        exentos_vistos[ruta] = motivo
                        continue
                    fallos.append((ruta, n, m.group(1), linea.strip()))

    for ruta, motivo in sorted(exentos_vistos.items()):
        print(f"  (exento: {os.path.basename(ruta)} -- {motivo})")

    if not fallos:
        print("OK: ninguna pantalla clava los colores del tema claro.")
        return 0

    print(f"\n{len(fallos)} usos del tema claro clavados en la UI:\n")
    for ruta, n, tok in ((a, b, c) for a, b, c, _ in fallos):
        print(f"  {ruta}:{n}  {tok}  ->  {PROHIBIDAS[tok]}")
    print(
        "\nEsto compila, pero en modo oscuro se ve mal. Usa FoodTheme.colors.*,\n"
        "que ya resuelve el color segun el tema activo.\n"
        "Si el color fijo es correcto (contenido sobre un fondo que no sigue el\n"
        f"tema), marcalo en la propia linea:  // {MARCADOR} <motivo>"
    )
    return 1


if __name__ == "__main__":
    sys.exit(main())
