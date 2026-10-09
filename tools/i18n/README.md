# Comprobaciones de idiomas

Scripts cortos para no publicar la app con un texto suelto o, peor, con una cadena que
reviente al formatearse en uno de los dos idiomas. Se ejecutan **desde la raíz del repo**
y no necesitan dependencias:

```sh
python -I tools/i18n/check_strings.py      # paridad de claves y marcadores
python -I tools/i18n/check_args.py         # argumentos suficientes en cada llamada
python -I tools/i18n/check_notif_parity.py # app y servidor redactan igual los avisos
python -I tools/i18n/check_listing.py      # la ficha de Play cabe en los límites
python -I tools/i18n/find_unused.py        # claves que no referencia nadie
python -I tools/i18n/scan_es.py            # literales castellanos en el código
python -I tools/i18n/scan_ui.py            # literales en posición de UI (cualquier idioma)
./gradlew assembleDebug                    # hace falta para el siguiente
"$ANDROID_HOME/build-tools/36.0.0/aapt2" dump resources \
    app/build/outputs/apk/debug/app-debug.apk > "$TEMP/res.txt"
python -I tools/i18n/check_apk.py          # cobertura sobre el APK ya construido
```

`-I` (modo aislado) evita que un `.py` cualquiera del directorio de trabajo se cuele
por un `import`. Los dos escáneres escriben el detalle en `$TEMP`, porque la consola
de Windows es cp1252 y se atraganta con los acentos.

## Qué busca cada uno, y por qué

**`check_strings.py` — el importante.** Compara `values/strings.xml` (inglés, por
defecto) con `values-es/strings.xml`: claves que falten o sobren, y que los
marcadores de formato coincidan. Un `%1$s` en un idioma y un `%1$d` en el otro
**compila igual de bien y revienta en ejecución, solo en ese idioma**. Es el fallo
que ninguna otra herramienta ve.

**`check_args.py`.** El compilador no comprueba que `getString`/`stringResource` pase
tantos argumentos como marcadores tiene la cadena. Si faltan, salta
`MissingFormatArgumentException` en cuanto se pinta esa pantalla.

**`check_notif_parity.py` — el no evidente.** Un aviso puede llegar por dos caminos: el
push que redacta Cloud Functions y el escuchador de Firestore que redacta la app. El id de
la notificación sale del `hashCode()` del propio texto (ver `NotificationHelper.show`),
justo para que el segundo reemplace al primero en vez de apilarse. **Si una coma cambia en
un lado, el usuario ve dos avisos del mismo hecho.** No vale comparar plantillas, porque
una usa `{from}` y la otra `%1$s`: el script rellena las dos con los mismos valores y
compara el resultado. Al mover estos textos a recursos se colaron comillas tipográficas
donde el servidor pone rectas, y eso ya bastaba para duplicarlos.

**`find_unused.py`.** Las claves que no referencia nadie, buscando cada una en todas las
formas en que se puede usar (`R.string.x`, `R.plurals.x`, `@string/x`, `@plurals/x`) por el
fuente, los recursos y el manifest — no se fia de la lista de lint. Respeta una lista de
intocables que el SDK lee por nombre, como `default_web_client_id`. Con `--borrar` las quita
de los dos `strings.xml`. Importa más de lo normal porque el reductor de recursos está
apagado: lo que sobre, viaja en el APK.

**`check_listing.py`.** Cuenta los caracteres de `docs/play-store-listing.md` contra los
límites de Play Console, que rechaza el texto al pegarlo.

**`scan_es.py`.** Literales en castellano todavía en el código Kotlin. Separa los que
solo van a `Log` (que no se traducen) de los visibles. Ignora `SeedData.kt` y
`MealDBSeeder.kt`, que son código muerto.

**`scan_ui.py`.** El criterio aquí no es el idioma sino **la posición**: lo que va en
un `Text(`, un `title=`, un `label=`, un `contentDescription=`... debería salir de los
recursos en cualquier idioma. Hace falta porque las palabras sueltas ("Buscando...",
"Todos", "Limpiar") se escapan de cualquier búsqueda por palabras castellanas.

**`check_apk.py`.** Lo mismo pero sobre el artefacto que se va a publicar, por si algo
se queda por el camino entre el fuente y el APK. Descarta por nombre los recursos de
las librerías y de los plugins de Google, que no son texto nuestro.

## Lo que NO cubren

Ninguno sustituye abrir la app. Comprueban que el texto existe y encaja, no que esté
bien traducido ni que quepa en su botón. Para eso:

```sh
adb -s <emulador> shell cmd locale set-app-locales com.app.foodranker --locales en-GB
adb -s <emulador> shell am start -S -n com.app.foodranker/.MainActivity
```

⚠️ Tras un `adb install`, comprobar que la salida dice `Success`: si el emulador anda
mal de espacio falla y se acaba verificando el build anterior sin darse cuenta.
