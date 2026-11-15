"""El slug del plato se calcula en dos sitios. Que no divergan.

`String.toDishSlug()` (Kotlin, en la app) y `toDishSlug()` (TypeScript, en las Cloud
Functions) tienen que dar exactamente el mismo resultado. El slug es la segunda mitad del
id de documento de un plato, `{venueId}__{dishSlug}`, de la que depende el deduplicado de
la identidad canónica plato+local — ver `docs/VENUES.md`.

Si divergen, `renamePlate` empieza a rechazar correcciones legítimas ("Ñoquis" sobre un
plato guardado como `noquis`) o a aceptar nombres que no corresponden al slug. No corrompe
la identidad —esa función nunca escribe `dishSlug`— pero sí deja la app mintiendo sobre
cómo se llama un plato, y nadie se entera hasta que un usuario lo nota.

No se mantiene una lista de casos aquí a propósito: se leen de `DishSlugTest.kt`, que es el
test que ya define el comportamiento esperado. Así, añadir un caso al test lo mete
automáticamente en esta comprobación, y es imposible cubrir uno solo de los dos lados.

Hace falta haber compilado las functions antes (`npm --prefix functions run build`),
porque se ejecuta el JavaScript ya generado y no el TypeScript.

    python -I tools/i18n/check_slug_parity.py
"""

import json
import pathlib
import re
import subprocess
import sys

RAIZ = pathlib.Path(__file__).resolve().parents[2]
TEST_KT = RAIZ / "app/src/test/java/com/app/foodranker/utils/DishSlugTest.kt"
JS = RAIZ / "functions/lib/dishSlug.js"

# assertEquals("esperado", "entrada".toDishSlug())
CASO = re.compile(r'assertEquals\(\s*"((?:[^"\\]|\\.)*)"\s*,\s*"((?:[^"\\]|\\.)*)"\.toDishSlug\(\)\s*\)')


def descomilla(s: str) -> str:
    """Deshace los escapes de una cadena de Kotlin. Solo los que aparecen en el test."""
    return s.replace('\\"', '"').replace("\\\\", "\\").replace("\\n", "\n").replace("\\t", "\t")


def main() -> int:
    if not TEST_KT.exists():
        print(f"no encuentro {TEST_KT}", file=sys.stderr)
        return 2
    if not JS.exists():
        print(
            f"no encuentro {JS}\n"
            "compila primero las functions:  npm --prefix functions run build",
            file=sys.stderr,
        )
        return 2

    texto = TEST_KT.read_text(encoding="utf-8")
    casos = [(descomilla(entrada), descomilla(esperado)) for esperado, entrada in CASO.findall(texto)]
    if not casos:
        print("no he sacado ni un caso de DishSlugTest.kt — ¿cambió el formato?", file=sys.stderr)
        return 2

    # El JS se ejecuta de una sola vez con todas las entradas, para no pagar un arranque
    # de node por caso.
    guion = (
        "const {toDishSlug} = require(process.argv[1]);"
        "const e = JSON.parse(process.argv[2]);"
        "console.log(JSON.stringify(e.map(toDishSlug)));"
    )
    entradas = [c[0] for c in casos]
    try:
        salida = subprocess.run(
            ["node", "-e", guion, str(JS), json.dumps(entradas)],
            capture_output=True, text=True, encoding="utf-8", check=True,
        ).stdout
    except FileNotFoundError:
        print("node no está en el PATH", file=sys.stderr)
        return 2
    except subprocess.CalledProcessError as err:
        print(f"el JS falló:\n{err.stderr}", file=sys.stderr)
        return 2

    obtenidos = json.loads(salida)
    fallos = [
        (entrada, kotlin, ts)
        for (entrada, kotlin), ts in zip(casos, obtenidos)
        if kotlin != ts
    ]

    print(f"casos sacados de DishSlugTest.kt: {len(casos)}")
    if fallos:
        print(f"\nDIVERGEN en {len(fallos)}:\n")
        for entrada, kotlin, ts in fallos:
            print(f'  entrada : {entrada!r}')
            print(f'  Kotlin  : {kotlin!r}')
            print(f'  TypeScript: {ts!r}\n')
        return 1

    print("OK: Kotlin y TypeScript dan el mismo slug en todos")
    return 0


if __name__ == "__main__":
    sys.exit(main())
