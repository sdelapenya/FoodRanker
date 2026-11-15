/**
 * Gemelo en TypeScript de `app/src/main/java/com/app/foodranker/utils/DishSlug.kt`.
 *
 * ⚠️ LOS DOS TIENEN QUE DAR EXACTAMENTE EL MISMO RESULTADO. El slug forma la segunda
 * mitad del id de documento de un plato (`{venueId}__{dishSlug}`), que es la identidad
 * canónica plato+local de la que depende el deduplicado — ver `docs/VENUES.md`.
 *
 * Existe aquí porque `renamePlate` tiene que comprobar, EN EL SERVIDOR, que el nombre
 * nuevo normaliza al mismo slug que el viejo. Las reglas de Firestore no sirven para eso:
 * no tienen normalización Unicode, así que no pueden saber que "Ñoquis" y "Noquis"
 * colapsan, y rechazarían justo las correcciones que queremos permitir.
 *
 * Es el mismo criterio que `getLeagueId`: cuando un identificador lo entienden dos lados,
 * manda el servidor.
 *
 * La paridad no se confía a la buena voluntad: `tools/i18n/check_slug_parity.py` saca los
 * casos de `DishSlugTest.kt` y los pasa por esta función. Hay que ejecutarlo al tocar
 * cualquiera de las dos.
 *
 * Equivalencias con Kotlin, por si alguna vez hay que revisarlas:
 *   Normalizer.normalize(s, NFD)  →  s.normalize("NFD")
 *   .replace(Regex("\\p{Mn}+"),"") →  .replace(/\p{Mn}+/gu, "")   (la u hace falta)
 *   .lowercase()                   →  .toLowerCase()   (ambos invariantes de locale)
 *   .replace(Regex("[^a-z0-9]+"),"-") → igual
 *   .trim('-')                     →  .replace(/^-+|-+$/g, "")
 *
 * El orden importa: los diacríticos se quitan ANTES de pasar a minúsculas, igual que en
 * Kotlin. Y no se eliminan artículos ni palabras vacías a propósito: "la carbonara" y
 * "carbonara" son técnicamente distintos y fusionarlos a ciegas uniría platos que no lo son.
 */
export function toDishSlug(name: string): string {
  return name
    .normalize("NFD")
    // Descarta los diacríticos que NFD acaba de separar. La ñ se descompone en n + tilde,
    // así que "Ñoquis" y "Noquis" acaban en el mismo slug: es el mismo plato mal escrito.
    .replace(/\p{Mn}+/gu, "")
    .toLowerCase()
    // Cualquier cosa que no sea letra o dígito pasa a ser separador.
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}
