/**
 * Ejercita las DOS secuencias que ni la compilación ni la auditoría de datos tocan:
 *
 *   A) la edición de una valoración entregada ANTES que su alta
 *   B) el mismo borrado entregado DOS veces sobre un plato que conserva otros votos
 *
 * ## Por qué así y no con el emulador "a pelo"
 *
 * Las dos son carreras de **orden de entrega**, y el emulador entrega los triggers en
 * orden: dejándolo a su aire, la secuencia mala no sale nunca. Lo que sí se puede es
 * invocar el handler directamente —las funciones v2 exponen `.run(evento)`— con un evento
 * **fabricado**, y así se fuerza exactamente el estado que produce la carrera:
 *
 *   A) el documento vivo tiene la nota EDITADA y aún no está `processed`, y al handler del
 *      alta se le pasa un evento con la nota VIEJA. Es, campo por campo, lo que ve la
 *      función cuando la edición se procesó primero.
 *   B) se invoca el handler del borrado dos veces con el MISMO `event.id`, que es lo que
 *      significa "at-least-once".
 *
 * ## Cómo se usa
 *
 *   export JAVA_HOME=/c/Users/User/.jdks/ms-21.0.9   # firebase-tools ya NO admite Java < 21
 *   npx firebase-tools emulators:start --only firestore --project demo-foodranker
 *   cd functions && npm run build && node scripts/pruebaCarreras.js
 *
 * ⚠️ Habla SOLO con el emulador, y las dos guardas de abajo son de verdad: exige un host
 * de loopback y un proyecto con prefijo `demo-`. Ver el comentario que las acompaña — la
 * primera versión de este fichero prometía esa protección y NO la daba.
 */
"use strict";

// ⚠️ ESTA GUARDA ESTUVO MUERTA Y LA PROTECCIÓN QUE PROMETÍA ERA FALSA.
//
// La primera versión hacía `FIRESTORE_EMULATOR_HOST = process.env.X || "127.0.0.1:8080"`
// y DESPUÉS comprobaba `if (!process.env.FIRESTORE_EMULATOR_HOST)`. Esa condición no podía
// ser cierta nunca: la línea de arriba acababa de rellenarla. El comentario decía "no
// puede correr contra producción ni por error" y no lo garantizaba nada.
//
// Ahora la comprobación va ANTES de poner ningún valor por defecto, y además:
//
//  • el host tiene que ser de loopback — si alguien exporta el de un Firestore real,
//    se planta en vez de escribir en él;
//  • el proyecto por defecto es `demo-foodranker`. El prefijo `demo-` es la convención
//    de Firebase para proyectos que SOLO existen en el emulador: con él, las librerías
//    se niegan a hablar con servicios reales. Antes ponía el id de producción, que es
//    justo el que no debe aparecer en un script de pruebas.
const HOST = process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8080";
const sinHostExplicito = !process.env.FIRESTORE_EMULATOR_HOST;
const esLocal = /^(127\.0\.0\.1|\[::1\]|localhost):\d+$/.test(HOST);
if (!esLocal) {
  console.error(`FIRESTORE_EMULATOR_HOST apunta a "${HOST}", que no es loopback.`);
  console.error("Abortado: esto solo corre contra un emulador local.");
  process.exit(2);
}
process.env.FIRESTORE_EMULATOR_HOST = HOST;
process.env.GCLOUD_PROJECT = process.env.GCLOUD_PROJECT || "demo-foodranker";
if (!/^demo-/.test(process.env.GCLOUD_PROJECT)) {
  console.error(`GCLOUD_PROJECT es "${process.env.GCLOUD_PROJECT}", sin prefijo demo-.`);
  console.error("Abortado: usa un proyecto demo-* para que no pueda tocar nada real.");
  process.exit(2);
}

// ⚠️ El orden importa: `lib/index.js` llama a `admin.initializeApp()` al cargarse, así que
// hay que cargarlo ANTES de pedir el firestore. Inicializar aquí primero revienta con
// "The default Firebase app already exists".
const fns = require("../lib/index.js");
const admin = require("firebase-admin");
const db = admin.firestore();

// Los mismos pesos que functions/src/index.ts. Si allí cambian, esto deja de medir.
const WF = 0.5, WP = 0.2, WS = 0.3;
const media = (r) => WF * r.flavorScore + WP * r.presentationScore + WS * r.satisfactionScore;

let fallos = 0;
function comprueba(etiqueta, real, esperado, tol = 1e-6) {
  const ok = typeof esperado === "number"
    ? Math.abs(real - esperado) <= tol
    : real === esperado;
  console.log(`  ${ok ? "OK  " : "MAL "} ${etiqueta}: ${real}${ok ? "" : `  (esperado ${esperado})`}`);
  if (!ok) fallos++;
}

async function limpia() {
  for (const col of ["plates", "ratings", "users", "stats"]) {
    const s = await db.collection(col).get();
    for (const d of s.docs) {
      // ⚠️ Borrar el documento NO borra sus subcolecciones, ni en el emulador. Sin esto,
      // el acta de `eventosAplicados` de una ejecución sobrevivía a la siguiente y el caso
      // B daba "OK" aunque el código no tuviera el arreglo: la prueba se mentía sola.
      for (const sub of await d.ref.listCollections()) {
        const hijos = await sub.get();
        await Promise.all(hijos.docs.map((h) => h.ref.delete()));
      }
      await d.ref.delete();
    }
  }
}

/** Un snapshot de mentira con los datos que tenía el documento en el momento del evento. */
function snapshotFalso(ref, datosDelEvento) {
  return {
    data: () => datosDelEvento,
    get: (k) => datosDelEvento[k],
    ref,
    exists: true,
  };
}

async function casoA() {
  console.log("\n=== A) la EDICIÓN se entrega antes que el ALTA ===");
  await limpia();

  const plateId = "plato-carrera-a";
  const autorId = "autor-1";
  const votanteId = "votante-1";

  await db.collection("users").doc(autorId).set({ name: "Autor", xp: 0, level: 1 });
  await db.collection("users").doc(votanteId).set({ name: "Votante", xp: 0, level: 1 });

  // El plato ya tiene UN voto de otra persona: así el `totalRatings === 0` no frena nada.
  const previo = { flavorScore: 6, presentationScore: 6, satisfactionScore: 6 };
  await db.collection("plates").doc(plateId).set({
    name: "Plato de prueba",
    status: "approved",
    addedByUserId: autorId,
    averageScore: media(previo),
    totalRatings: 1,
    wouldOrderAgainCount: 0,
    wouldOrderAgainResponses: 0,
    rankingScore: 0,
  });

  const vieja = { flavorScore: 5, presentationScore: 5, satisfactionScore: 5, wouldOrderAgain: false };
  const nueva = { flavorScore: 8, presentationScore: 8, satisfactionScore: 8, wouldOrderAgain: true };
  const comun = { plateId, userId: votanteId, userName: "Votante", createdAt: Date.now() };

  // Estado real tras la edición: la nota NUEVA en el documento, y sin `processed`,
  // porque onRatingUpdated se salió sin tocar nada (aún no estaba incorporada).
  const ratingRef = db.collection("ratings").doc(`${plateId}_${votanteId}`);
  await ratingRef.set({ ...comun, ...nueva, averageScore: media(nueva) });

  // Y ahora llega el alta, con la foto VIEJA dentro del evento.
  await fns.onRatingCreated.run({
    id: "evt-alta-1",
    params: { ratingId: ratingRef.id },
    data: snapshotFalso(ratingRef, { ...comun, ...vieja, averageScore: media(vieja) }),
  });

  const plato = (await db.collection("plates").doc(plateId).get()).data();
  const rating = (await ratingRef.get()).data();

  // El plato tenía un 6; si se incorpora el 8 vivo, la media es 7. Con la nota vieja (5)
  // saldría 5,5 y el sello quedaría en 5 para siempre.
  comprueba("el plato incorpora la nota VIVA (media 7, no 5,5)", plato.averageScore, 7);
  comprueba("totalRatings", plato.totalRatings, 2);
  comprueba("el sello appliedAvg es la nota viva", rating.appliedAvg, media(nueva));
  comprueba("averageScore del rating cuadra con sus notas", rating.averageScore, media(nueva));
  comprueba("'lo volvería a pedir' cuenta el valor vivo (true)", plato.wouldOrderAgainCount, 1);
  comprueba("y su denominador", plato.wouldOrderAgainResponses, 1);
}

async function casoB() {
  console.log("\n=== B) el MISMO borrado entregado DOS veces ===");
  await limpia();

  const plateId = "plato-carrera-b";
  const autorId = "autor-2";

  await db.collection("users").doc(autorId).set({ name: "Autor", xp: 0, level: 1 });

  // Dos votos incorporados: 6 y 9. Media 7,5.
  const a = { flavorScore: 6, presentationScore: 6, satisfactionScore: 6 };
  const b = { flavorScore: 9, presentationScore: 9, satisfactionScore: 9 };
  await db.collection("plates").doc(plateId).set({
    name: "Plato de prueba B",
    status: "approved",
    addedByUserId: autorId,
    averageScore: (media(a) + media(b)) / 2,
    totalRatings: 2,
    wouldOrderAgainCount: 1,
    wouldOrderAgainResponses: 2,
    rankingScore: 0,
  });

  const borradoRef = db.collection("ratings").doc(`${plateId}_votante-b`);
  const borrado = {
    ...b, plateId, userId: "votante-b", processed: true,
    appliedAvg: media(b), appliedWouldOrderAgain: true, averageScore: media(b),
  };

  const evento = {
    id: "evt-borrado-1",
    params: { ratingId: borradoRef.id },
    data: snapshotFalso(borradoRef, borrado),
  };

  await fns.onRatingDeleted.run(evento);
  const tras1 = (await db.collection("plates").doc(plateId).get()).data();
  comprueba("tras la 1ª entrega, totalRatings", tras1.totalRatings, 1);
  comprueba("tras la 1ª entrega, averageScore", tras1.averageScore, media(a));
  comprueba("tras la 1ª entrega, 'lo repetiría'", tras1.wouldOrderAgainCount, 0);

  // Reentrega: mismo event.id. Es lo que significa at-least-once.
  await fns.onRatingDeleted.run(evento);
  const tras2 = (await db.collection("plates").doc(plateId).get()).data();
  comprueba("tras la 2ª entrega, totalRatings NO baja otra vez", tras2.totalRatings, 1);
  comprueba("tras la 2ª entrega, averageScore no se mueve", tras2.averageScore, media(a));

  const actas = await db.collection("plates").doc(plateId)
    .collection("eventosAplicados").get();
  comprueba("hay UN acta de idempotencia", actas.size, 1);
}

(async () => {
  console.log("emulador:", process.env.FIRESTORE_EMULATOR_HOST,
    "| proyecto:", process.env.GCLOUD_PROJECT,
    sinHostExplicito ? "(host por defecto)" : "(host del entorno)");
  // Que conteste alguien, y decirlo claro. Sin esto, un emulador apagado daba un error de
  // red enterrado en la primera escritura y parecía un fallo de la prueba.
  try {
    await db.collection("__ping").doc("__ping").get();
  } catch (e) {
    console.error(`\nNo contesta ningún emulador en ${process.env.FIRESTORE_EMULATOR_HOST}.`);
    console.error("Arráncalo primero (ver la cabecera de este fichero).");
    process.exit(2);
  }
  try {
    await casoA();
    await casoB();
  } catch (e) {
    console.error("\nLa prueba reventó:", e);
    process.exit(1);
  }
  console.log(fallos === 0 ? "\nTODO OK" : `\n${fallos} COMPROBACIONES MAL`);
  process.exit(fallos === 0 ? 0 : 1);
})();
