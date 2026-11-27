# -*- coding: utf-8 -*-
"""Comprueba que ninguna pantalla se olvida del filtro de bloqueo.

El bloqueo del servidor (firestore.rules) impide INTERACTUAR. Lo que impide VER es el
cliente, y eso son treinta y pico sitios donde se convierte un documento de Firestore en
un modelo. Treinta y pico sitios es, exactamente, la clase de lista de la que alguien se
deja uno fuera -- y aqui dejarse uno fuera significa ensenarle a alguien justo al usuario
del que pidio no saber nada nunca mas.

Asi que no depende de que me acuerde: depende de esto.

## Que exige

Cada conversion a modelo (`toObject(...)`, mas los modelos que se mapean a mano) tiene que
tener una de estas dos cosas:

  * una llamada a `sinBloqueados(` o a `bloqueos.ocultos` en las lineas siguientes, o
  * un comentario `// bloqueo-ok: <razon>` justo encima.

La marca NO es un interruptor para callar al comprobador: es una razon escrita que alguien
puede leer y discutir. "Es MI perfil", "un reto semanal no lo escribe ningun usuario", "la
liga va entera porque filtrarla renumeraria a los de abajo". Una marca sin texto detras no
cuenta -- `// bloqueo-ok:` a secas se rechaza.

## El fallo que este comprobador tuvo y por el que mira lo que mira

La primera version buscaba el filtro en las 30 lineas siguientes a cada conversion, sin
mas. Le quite a proposito el filtro a una de las dos conversiones de TrendingViewModel, y
siguio diciendo "0 sin cubrir": estaba viendo el filtro de la conversion de al lado, quince
lineas mas abajo. Un comprobador que no falla con el codigo roto no vale nada, y este ya
estaba a punto de colarse.

Por eso la ventana se corta en la conversion siguiente (y la busqueda de la marca, en la
anterior). Cada conversion tiene que traer la suya.

## Lo que este comprobador NO ve

Los modelos que se construyen a mano campo a campo, sin `toObject`. Hoy solo hay uno
(`FoodNotification`, en NotificationsViewModel) y esta en PATRONES. Si manana alguien mapea
otro a mano, este fichero no se entera. Queda dicho aqui en vez de fingir que la cobertura
es total.

Y es una comprobacion de PROXIMIDAD, no un analisis de flujo: comprueba que el filtro esta
ahi al lado, a la vista de quien lea la conversion. No puede demostrar que se aplique a los
datos correctos.

## Uso

    python -I tools/ui/check_bloqueo_cliente.py

Sale 0 si todas las conversiones estan cubiertas, 1 si falta alguna.
"""
import io
import os
import re
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
FUENTES = os.path.join(RAIZ, "app", "src", "main", "java")

# Conversiones que hay que vigilar. `toObject` es la general; los demas son modelos que
# en algun sitio se construyen a mano a partir de un documento.
PATRONES = [
    re.compile(r"\.toObject\("),
    re.compile(r"\bFoodNotification\("),
]

# Formas que cuentan como "aqui se filtra".
FILTRA = re.compile(r"sinBloqueados\(|bloqueos\.ocultos")

# La marca, con su razon obligatoria detras.
MARCA = re.compile(r"//\s*bloqueo-ok:\s*(\S.*)$")

LINEAS_ARRIBA = 8     # hasta donde se busca la marca
LINEAS_ABAJO = 30     # hasta donde se busca el filtro (lo que ocupa el mapeo mas largo)


def conversiones(lineas):
    """Indices de las lineas que convierten un documento en un modelo."""
    fuera = []
    for i, linea in enumerate(lineas):
        if not any(p.search(linea) for p in PATRONES):
            continue
        pelada = linea.lstrip()
        # Ni un comentario que mencione toObject, ni la declaracion del propio modelo.
        if pelada.startswith("//") or pelada.startswith("*"):
            continue
        if pelada.startswith("data class") or pelada.startswith("class "):
            continue
        fuera.append(i)
    return fuera


def revisa(ruta):
    """Devuelve (cubiertas, marcadas, fallos) de un fichero."""
    lineas = io.open(ruta, encoding="utf-8").read().split("\n")
    sitios = conversiones(lineas)
    cubiertas, marcadas, fallos = 0, 0, []
    for n, i in enumerate(sitios):
        # La ventana NO cruza a la conversion de al lado. Ver la cabecera: sin esto el
        # comprobador daba por cubierta una conversion usando el filtro de la siguiente.
        anterior = sitios[n - 1] if n > 0 else -1
        siguiente = sitios[n + 1] if n + 1 < len(sitios) else len(lineas)

        razon = None
        for j in range(max(0, i - LINEAS_ARRIBA, anterior + 1), i):
            m = MARCA.search(lineas[j])
            if m:
                razon = m.group(1).strip()
        if razon:
            marcadas += 1
            continue

        tope = min(i + LINEAS_ABAJO + 1, siguiente)
        if FILTRA.search("\n".join(lineas[i:tope])):
            cubiertas += 1
            continue

        fallos.append((i + 1, lineas[i].strip()[:96]))
    return cubiertas, marcadas, fallos


def main():
    if not os.path.isdir(FUENTES):
        print("No encuentro %s" % FUENTES)
        return 2

    total_c, total_m, total_f = 0, 0, 0
    for base, _, ficheros in os.walk(FUENTES):
        for f in sorted(ficheros):
            if not f.endswith(".kt"):
                continue
            # El propio fichero del filtro define las funciones, no las usa.
            if f == "Bloqueo.kt":
                continue
            ruta = os.path.join(base, f)
            c, m, fallos = revisa(ruta)
            total_c += c
            total_m += m
            if fallos:
                rel = os.path.relpath(ruta, RAIZ).replace("\\", "/")
                for n, texto in fallos:
                    total_f += 1
                    print("  SIN FILTRO  %s:%d" % (rel, n))
                    print("              %s" % texto)

    print("\n  %d conversiones filtradas, %d marcadas con razon, %d sin cubrir"
          % (total_c, total_m, total_f))
    if total_f:
        print("\n  Cada una necesita `.sinBloqueados(bloqueos.ocultos.value)` o, si de verdad")
        print("  no le corresponde, un `// bloqueo-ok: <por que>` encima. La razon se escribe:")
        print("  quien lea esto dentro de un ano tiene que poder discutirla.")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
