# -*- coding: utf-8 -*-
"""Busca texto ilegible: pares fondo/texto que no llegan al contraste minimo.

Por que existe. `check_theme_colors.py` comprueba que las pantallas no clavan los
colores DEL TEMA (que nadie escriba 0xFFF8F9FA en vez de FoodTheme.colors.background).
Eso deja fuera el fallo que de verdad se ve: una tarjeta con un fondo fijo claro
-- un crema, un verde palido -- y encima un `Text` que si usa el token del tema.
En claro se lee; en oscuro el token se vuelve casi blanco y el texto desaparece
sobre el crema. La tarjeta "Invita amigos" del perfil llevaba meses asi.

Que saca, en dos bloques:

  1. TABLA DE TOKENS. Todas las combinaciones de texto y fondo del tema, en claro
     y en oscuro. Es exacta: no depende de adivinar nada, solo de Color.kt.

  2. FONDO FIJO + TEXTO DEL TEMA. El patron de arriba. Un fondo escrito a mano no
     cambia con el tema y el texto si, asi que uno de los dos modos va a fallar
     casi siempre. Estos son los que hay que mirar.

    python -I tools/ui/check_contrast.py [--todo]

Umbrales WCAG 2.1 AA: 4.5 para texto normal, 3.0 para texto grande (>=18sp, o
>=14sp en negrita) y para iconos.

Limites. Es analisis estatico: empareja cada Text con el ultimo fondo declarado
en el mismo composable y a menos de VENTANA lineas, asi que puede emparejar mal.
El texto sobre una FOTO no se puede evaluar y se ignora a proposito. Lo que sale
son candidatos a mirar en pantalla, no una sentencia.
"""
import io
import os
import re
import sys
import itertools

RAIZ = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
FUENTES = os.path.join(RAIZ, "app/src/main/java/com/app/foodranker")
VENTANA = 25          # lineas que un fondo declarado sigue valiendo

# Tokens del tema con su valor en (claro, oscuro). Salen de ui/theme/Color.kt y el
# script avisa si alli cambian.
TOKENS = {
    "background":    ("FFF8F9FA", "FF0D0D0D"),
    "surface":       ("FFFFFFFF", "FF1C1C1E"),
    "surfaceMuted":  ("FFEEF0F4", "FF2C2C2E"),
    "textPrimary":   ("FF1A1A2E", "FFF2F2F7"),
    "textSecondary": ("FF6B7280", "FF9098A6"),
    "divider":       ("FFE5E7EB", "FF2C2C2E"),
    # Pares de acento suave: fondo de tarjeta destacada y su texto.
    "accentSoft":         ("FFFFF3E0", "FF3A2A1C"),
    "accentSoftBorder":   ("FFFFCC80", "FF5C4327"),
    "onAccentSoft":       ("FF5D3A00", "FFF3DCC4"),
    "onAccentSoftMuted":  ("FF7A5020", "FFD2B394"),
    "successSoft":        ("FFEAF7EE", "FF17291F"),
    "successSoftBorder":  ("FFB9DEC6", "FF2F4F3C"),
    "onSuccessSoft":      ("FF153B2A", "FFD5EEE0"),
    "onSuccessSoftMuted": ("FF516D5E", "FF9EC6B0"),
    "successStrong":      ("FF117A56", "FF3FD69B"),
}
FIJOS_TEMA = {
    "OrangePrimary": "FFFF6B35", "OrangeLight": "FFFF8E53", "OrangeDark": "FFE85520",
    "StarYellow": "FFFFC107", "SuccessGreen": "FF10B981", "ErrorRed": "FFEF4444",
    "Color.White": "FFFFFFFF", "Color.Black": "FF000000",
}
FONDOS_TOKEN = ["background", "surface", "surfaceMuted"]
TEXTOS_TOKEN = ["textPrimary", "textSecondary"]
# Los acentos suaves solo tienen sentido con SU texto, no con cualquiera: medirlos
# contra background daria ruido. Se comprueban como pares cerrados.
PARES_ACENTO = [
    ("onAccentSoft", "accentSoft"), ("onAccentSoftMuted", "accentSoft"),
    ("onSuccessSoft", "successSoft"), ("onSuccessSoftMuted", "successSoft"),
    ("successStrong", "successSoft"), ("successStrong", "surface"),
    ("textPrimary", "surface"), ("textSecondary", "surface"),
]

AA_NORMAL, AA_GRANDE = 4.5, 3.0


def canal(c):
    c = c / 255.0
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def luminancia(h):
    return (0.2126 * canal(int(h[2:4], 16)) + 0.7152 * canal(int(h[4:6], 16))
            + 0.0722 * canal(int(h[6:8], 16)))


def contraste(a, b):
    la, lb = luminancia(a), luminancia(b)
    if la < lb:
        la, lb = lb, la
    return (la + 0.05) / (lb + 0.05)


def mezcla(frente, fondo, alpha):
    out = "FF"
    for i in (2, 4, 6):
        f, d = int(frente[i:i+2], 16), int(fondo[i:i+2], 16)
        out += "%02X" % int(round(f * alpha + d * (1 - alpha)))
    return out


LITERAL = re.compile(r"Color\(0x([0-9A-Fa-f]{8})\)")
TOKEN = re.compile(r"FoodTheme\.colors\.(\w+)")
NOMBRADO = re.compile(r"\b(OrangePrimary|OrangeLight|OrangeDark|StarYellow|SuccessGreen|ErrorRed|Color\.White|Color\.Black)\b")
ALPHA = re.compile(r"\.copy\(\s*alpha\s*=\s*([0-9.]+)f?\s*\)")
# Fondo de verdad. `contentColor` NO entra: es el color del contenido, no del fondo.
FONDO = re.compile(r"(containerColor\s*=|Surface\(\s*color\s*=|\.background\()")
PINTA_TEXTO = re.compile(r"\b(color|tint)\s*=")
GRANDE = re.compile(r"fontSize\s*=\s*(\d+)\.sp")
NEGRITA = re.compile(r"FontWeight\.(Bold|SemiBold|ExtraBold|Black)")
NUEVO_BLOQUE = re.compile(r"^\s*(@Composable|(private\s+)?fun\s+\w+)")


def resuelve(txt, modo):
    """(hex, etiqueta, es_token) del primer color de `txt`, o None."""
    m = LITERAL.search(txt)
    if m:
        return m.group(1).upper(), "0x" + m.group(1).upper(), False
    m = TOKEN.search(txt)
    if m and m.group(1) in TOKENS:
        return TOKENS[m.group(1)][modo].upper(), m.group(1), True
    m = NOMBRADO.search(txt)
    if m:
        return FIJOS_TEMA[m.group(1)].upper(), m.group(1), False
    return None


def analiza(ruta, modo):
    lineas = io.open(ruta, encoding="utf-8", errors="replace").read().split("\n")
    fondo = None   # (hex, linea, etiqueta, es_token)
    salida = []
    for n, linea in enumerate(lineas, 1):
        if NUEVO_BLOQUE.match(linea):
            fondo = None
        if linea.strip().startswith("//"):
            continue
        if fondo and n - fondo[1] > VENTANA:
            fondo = None
        if FONDO.search(linea):
            r = resuelve(linea, modo)
            if r:
                hexa, etq, es_tok = r
                a = ALPHA.search(linea)
                if a:
                    hexa = mezcla(hexa, TOKENS["background"][modo].upper(), float(a.group(1)))
                    etq += " @%s" % a.group(1)
                fondo = (hexa, n, etq, es_tok)
            continue
        if not fondo or not PINTA_TEXTO.search(linea):
            continue
        if not ("Text(" in linea or "Icon" in linea or "tint" in linea
                or re.search(r"^\s*color\s*=", linea)):
            continue
        r = resuelve(linea, modo)
        if not r:
            continue
        hexa, etq, txt_es_token = r
        a = ALPHA.search(linea)
        if a:
            hexa = mezcla(hexa, fondo[0], float(a.group(1)))
            etq += " @%s" % a.group(1)
        if hexa == fondo[0]:
            continue
        tam = GRANDE.search(linea)
        sp = int(tam.group(1)) if tam else 14
        umbral = AA_GRANDE if (sp >= 18 or (sp >= 14 and NEGRITA.search(linea))) else AA_NORMAL
        c = contraste(hexa, fondo[0])
        # alta confianza: fondo escrito a mano y texto que SI cambia con el tema
        critico = (not fondo[3]) and txt_es_token
        if c < umbral:
            salida.append((n, fondo[1], fondo[2], etq, c, umbral, critico))
    return salida


def tabla_tokens():
    print("=" * 74)
    print("  1. TOKENS DEL TEMA ENTRE SI  (exacto: sale de Color.kt)")
    print("=" * 74)
    malos = 0
    for modo, etiqueta in ((0, "claro"), (1, "oscuro")):
        print("\n  tema %s" % etiqueta)
        for texto, fnd in itertools.product(TEXTOS_TOKEN, FONDOS_TOKEN):
            c = contraste(TOKENS[texto][modo], TOKENS[fnd][modo])
            marca = "ok " if c >= AA_NORMAL else ("~  " if c >= AA_GRANDE else "MAL")
            if c < AA_NORMAL:
                malos += 1
            print("    %s  %-14s sobre %-14s %5.2f" % (marca, texto, fnd, c))
    print("\n  pares de acento suave (fondo de tarjeta destacada y su texto)")
    for modo, etiqueta in ((0, "claro"), (1, "oscuro")):
        print("\n  tema %s" % etiqueta)
        for texto, fnd in PARES_ACENTO:
            c = contraste(TOKENS[texto][modo], TOKENS[fnd][modo])
            marca = "ok " if c >= AA_NORMAL else ("~  " if c >= AA_GRANDE else "MAL")
            if c < AA_NORMAL:
                malos += 1
            print("    %s  %-19s sobre %-14s %5.2f" % (marca, texto, fnd, c))
    print("\n  (ok = pasa AA para texto normal; ~ = solo vale para texto grande;"
          " MAL = no llega ni a eso)")
    return malos


def main():
    todo = "--todo" in sys.argv
    col = io.open(os.path.join(FUENTES, "ui/theme/Color.kt"), encoding="utf-8").read().upper()
    for nombre, hexa in FIJOS_TEMA.items():
        if not nombre.startswith("Color.") and ("0X" + hexa) not in col:
            print("AVISO: %s ya no vale 0x%s en Color.kt -- actualiza este script" % (nombre, hexa))
    for nombre, (claro, oscuro) in TOKENS.items():
        for v in (claro, oscuro):
            if ("0X" + v) not in col:
                print("AVISO: %s=0x%s no esta en Color.kt -- actualiza este script" % (nombre, v))

    tabla_tokens()

    print("\n" + "=" * 74)
    print("  2. FONDO FIJO + TEXTO DEL TEMA  (el patron que rompe en un modo)")
    print("=" * 74)
    vistos, otros = [], []
    for modo, etiqueta in ((0, "CLARO"), (1, "OSCURO")):
        for base, _, ficheros in os.walk(FUENTES):
            if "/ui/theme" in base.replace("\\", "/"):
                continue
            for f in sorted(ficheros):
                if not f.endswith(".kt"):
                    continue
                ruta = os.path.join(base, f)
                rel = os.path.relpath(ruta, RAIZ).replace("\\", "/")
                for n, fl, fe, te, c, u, critico in analiza(ruta, modo):
                    fila = (etiqueta, rel, n, fe, fl, te, c, u)
                    (vistos if critico else otros).append(fila)

    for etiqueta, rel, n, fe, fl, te, c, u in vistos:
        print("\n  [%s] %s:%d" % (etiqueta, rel, n))
        print("      texto %s  sobre fondo fijo %s (linea %d)" % (te, fe, fl))
        print("      contraste %.2f  <  %.1f" % (c, u))
    if not vistos:
        print("\n  ninguno")

    print("\n  %d criticos" % len(vistos))
    if todo:
        print("\n  --- los demas pares por debajo del minimo (mas ruido) ---")
        for etiqueta, rel, n, fe, fl, te, c, u in otros:
            print("  [%s] %s:%d  %s sobre %s  %.2f < %.1f" % (etiqueta, rel, n, te, fe, c, u))
    else:
        print("  (%d pares mas por debajo del minimo, con --todo)" % len(otros))
    return 1 if vistos else 0


if __name__ == "__main__":
    sys.exit(main())
