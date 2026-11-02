# tools/legal — las páginas legales se generan, no se escriben

Los textos legales viven en **un solo sitio**: `app/src/main/res/values/strings.xml` (inglés)
y `values-es/strings.xml` (castellano). Las claves son `pp_*` para la política de privacidad
y `tos_*` para los términos.

Las páginas de la web pública salen de ahí:

```
python -I tools/legal/build_pages.py            # escribe en tools/legal/out/
python -I tools/legal/build_pages.py <destino>  # o donde le digas
```

Genera cuatro ficheros:

| Fichero | Qué es |
|---|---|
| `privacy.html` | Política de privacidad, castellano |
| `privacy-en.html` | Política de privacidad, inglés |
| `terms.html` | Términos, castellano |
| `terms-en.html` | Términos, inglés |

**Los nombres en castellano no se pueden cambiar.** Play, AdMob, el formulario de Seguridad
de los datos y la ficha de la tienda apuntan a `privacy.html` y `terms.html`.

## Por qué existe esto

Antes la web se mantenía a mano, copiando los textos de la app. Dejó de funcionar sin que
nadie lo notara. Cuando se escribió el generador (2026-11-01), la web publicada:

- **no tenía la sección "Quién es responsable de tus datos"** — la política a la que apunta
  Play no decía quién responde de los datos
- daba **otro correo de contacto** (`sdelapenya1991@`, no `foodranker.app@`), en dos sitios
- hablaba de **"la Unión Europea"** donde la app ya hablaba del EEE y el Reino Unido, con
  AEPD e ICO
- ponía **agosto** donde la app ponía octubre
- no mencionaba el idioma del dispositivo entre los datos recogidos
- **solo existía en castellano**, mientras las fichas `en-GB` y `en-US` de Play apuntaban a ella

Ninguno de esos fallos era visible sin comparar las dos cosas línea a línea. Ahora no pueden
volver a separarse: si cambia `strings.xml`, se regenera y se vuelve a publicar.

## Publicar

Las páginas viven en la rama huérfana **`gh-pages`** de `origin`, servida por GitHub Pages
desde la raíz. No se usa `/docs` de `main` a propósito: Pages publicaría `docs/HANDOFF.md`
como página web.

```bash
git worktree add e:/tmp/foodranker-ghpages gh-pages
python -I tools/legal/build_pages.py e:/tmp/foodranker-ghpages
cd e:/tmp/foodranker-ghpages && git add -A && git commit && git push origin gh-pages
```

Comprobar después que las cuatro responden 200.

## Lo que NO genera

`index.html`, `style.css`, `delete-account.html` y `delete-account-en.html` no salen de
`strings.xml` y se mantienen a mano en `gh-pages`.

Las generadas **sí las enlazan**, y cada una a la de su idioma: eso lo fija el diccionario
`BORRADO` del script. Hasta noviembre de 2026 solo existía la castellana y las páginas
inglesas llevaban a ella, así que quien leía la política en inglés y pulsaba "Delete
account" aterrizaba en castellano. Si se renombra alguna de las dos, hay que tocar `BORRADO`.

⚠️ **`index.html` sigue existiendo solo en castellano.** No está declarada en ningún sitio
de Play (allí van `privacy.html` y `delete-account.html`), así que no es un bloqueante, pero
es la primera página que ve quien llega a la raíz del dominio.

## Si añades una sección

Añade `pp_N_t` y `pp_N_b` (o `tos_N_t`/`tos_N_b`) **en los dos idiomas** y añádela a la lista
de `PrivacyPolicyScreen.kt` o `TermsOfServiceScreen.kt`. El generador descubre las secciones
solo, no hay que tocarlo. Pasa después `tools/i18n/check_strings.py`.
