# Material de redes

Aquí va el **texto** de cada publicación. Las **imágenes no se versionan**: están
ignoradas en `.gitignore`.

## Por qué el texto sí y las imágenes no

El texto pesa nada, se lee en un diff y tener el historial de copys es útil — el mismo
criterio que `docs/play-store-listing.md` con las notas de la tienda.

Las imágenes son otra cosa: **git no olvida un binario**. Cada reintento de la misma foto
se queda en el historial para siempre y se lo descarga todo el que clone, aunque luego la
borres. El material de redes se reedita mucho, así que crecería sin parar. Y desde el
2026-11-20 **gitea es espejo de github**, con lo que cada MB se duplica en el servidor de
casa.

No es por confidencialidad: una publicación de Instagram es pública de todos modos.

## Dónde están entonces las imágenes

En esta misma carpeta, en el disco, pero **fuera del control de versiones**. Eso significa
que **no viajan a otro clon**: si se trabaja desde el servidor o desde otro equipo, hay que
copiarlas a mano o guardarlas en un sitio aparte.

Si algún día conviene tenerlo todo junto y versionado, lo limpio es un **repo propio de
marketing en Gitea**, privado, donde el peso de las imágenes no molesta al repo de código.

## Convención de nombres

`<red>-<n>.<ext>` para las imágenes y `<red>-texto.txt` para el copy, de modo que se vea a
simple vista qué texto acompaña a qué publicación.
