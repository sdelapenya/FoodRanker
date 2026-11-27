/**
 * Ejercita onBloqueoEscrito: el espejo `bloqueadoPor` y el deshacer de los seguimientos.
 *
 * ## Por que hace falta
 *
 * Es la pieza del bloqueo que NO se puede comprobar ni compilando ni con las reglas,
 * porque corre con el Admin SDK y se las salta todas. Y si falla, falla en silencio hacia
 * el lado malo: sin espejo, el movil de quien fue bloqueado nunca se entera y le sigue
 * viendo los platos a quien pidio no verle. Eso es un bloqueo que miente.
 *
 * Tambien comprueba lo que el cliente NO puede hacer por si mismo: borrar el seguimiento
 * de la OTRA persona hacia ti. La regla de borrado de `follows` exige ser el seguidor, asi
 * que ese documento solo lo quita el servidor. Si esta llamada se cayera, quien te bloqueo
 * seguiria recibiendo tus avisos de plato nuevo.
 *
 * Y la idempotencia: los triggers son at-least-once. Aqui no hace falta acta como en
 * onRatingDeleted —todo son `set`/`delete` de ids deterministas— pero eso hay que verlo,
 * no suponerlo: el mismo evento entregado dos veces tiene que dejar lo mismo.
 *
 * ## Como se usa
 *
 *   export JAVA_HOME=/c/Users/User/.jdks/ms-21.0.9   # firebase-tools ya NO admite Java < 21
 *   npx firebase-tools emulators:start --only firestore --project demo-foodranker
 *   cd functions && npm run build && node scripts/pruebaBloqueoTrigger.js
 *
 * ⚠️ Las guardas de abajo son las mismas que las de pruebaCarreras.js y por el mismo
 * motivo: la primera version de aquel fichero prometia una proteccion que no daba.
 */
"use strict";

const HOST = process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8080";
if (!/^(127\.0\.0\.1|\[::1\]|localhost):\d+$/.test(HOST)) {
  console.error('FIRESTORE_EMULATOR_HOST apunta a "' + HOST + '", que no es loopback.');
  console.error("Abortado: esto solo corre contra un emulador local.");
  process.exit(2);
}
process.env.FIRESTORE_EMULATOR_HOST = HOST;
process.env.GCLOUD_PROJECT = process.env.GCLOUD_PROJECT || "demo-foodranker";
if (!/^demo-/.test(process.env.GCLOUD_PROJECT)) {
  console.error('GCLOUD_PROJECT es "' + process.env.GCLOUD_PROJECT + '", sin prefijo demo-.');
  console.error("Abortado: usa un proyecto demo-* para que no pueda tocar nada real.");
  process.exit(2);
}

// El orden importa: lib/index.js llama a admin.initializeApp() al cargarse.
const fns = require("../lib/index.js");
const admin = require("firebase-admin");
const db = admin.firestore();

const A = "pruebaA";
const B = "pruebaB";

let fallos = 0;
function anota(nombre, ok, detalle) {
  if (!ok) fallos++;
  console.log("  " + (ok ? "OK " : "MAL") + "  " + nombre + (detalle ? "  -- " + detalle : ""));
}

/** Un evento de onDocumentWritten fabricado a mano. */
function evento(uid, otroUid, existeDespues) {
  return {
    id: "evt_" + uid + "_" + otroUid,
    params: { uid: uid, otroUid: otroUid },
    data: {
      before: { exists: !existeDespues },
      after: { exists: existeDespues },
    },
  };
}

async function existe(ruta) {
  return (await db.doc(ruta).get()).exists;
}

async function limpia() {
  const rutas = [
    "users/" + A + "/bloqueados/" + B, "users/" + B + "/bloqueadoPor/" + A,
    "users/" + B + "/bloqueados/" + A, "users/" + A + "/bloqueadoPor/" + B,
    "follows/" + A + "_" + B, "follows/" + B + "_" + A,
  ];
  for (const r of rutas) await db.doc(r).delete();
}

async function main() {
  console.log("\nEmulador " + HOST + ", proyecto " + process.env.GCLOUD_PROJECT + "\n");
  await limpia();

  // Punto de partida: se siguen MUTUAMENTE. Ese es el caso que importa, porque el
  // seguimiento de B hacia A el cliente de A no lo puede borrar.
  await db.doc("follows/" + A + "_" + B).set({ followerId: A, followingId: B });
  await db.doc("follows/" + B + "_" + A).set({ followerId: B, followingId: A });
  await db.doc("users/" + A + "/bloqueados/" + B).set({ creadoEn: Date.now() });

  await fns.onBloqueoEscrito.run(evento(A, B, true));

  anota("escribe el espejo en users/B/bloqueadoPor/A",
    await existe("users/" + B + "/bloqueadoPor/" + A));
  anota("deshace el seguimiento A -> B",
    !(await existe("follows/" + A + "_" + B)));
  anota("deshace el seguimiento B -> A  (el que el cliente NO puede borrar)",
    !(await existe("follows/" + B + "_" + A)));

  // At-least-once: el mismo evento otra vez no debe cambiar nada ni reventar.
  await fns.onBloqueoEscrito.run(evento(A, B, true));
  anota("entregado dos veces, el espejo sigue ahi y no falla",
    await existe("users/" + B + "/bloqueadoPor/" + A));

  // Desbloquear retira el espejo...
  await db.doc("users/" + A + "/bloqueados/" + B).delete();
  await fns.onBloqueoEscrito.run(evento(A, B, false));
  anota("al desbloquear, el espejo desaparece",
    !(await existe("users/" + B + "/bloqueadoPor/" + A)));

  // ...y NO devuelve los seguimientos: volver a seguir es una decision.
  anota("y los seguimientos NO vuelven solos",
    !(await existe("follows/" + A + "_" + B)) && !(await existe("follows/" + B + "_" + A)));

  // Borrar un desbloqueo que ya no estaba tampoco debe reventar.
  await fns.onBloqueoEscrito.run(evento(A, B, false));
  anota("desbloquear dos veces no falla", true);

  // Eventos FUERA DE ORDEN. Los triggers no garantizan el orden de entrega, solo que
  // cada evento llega al menos una vez. Si alguien bloquea y desbloquea seguido, el
  // evento ANTIGUO de bloqueo puede procesarse el ultimo.
  //
  // Esto NO lo cubre el caso de doble entrega de arriba: alli es el mismo evento dos
  // veces, aqui son dos eventos distintos en orden inverso. Decidir mirando
  // `event.data.after.exists` hace que el evento rezagado resucite el espejo de un
  // bloqueo que ya no existe. Y ese espejo no lo puede quitar nadie: la lista de A esta
  // vacia, asi que no queda ninguna escritura que vuelva a disparar la limpieza. Un
  // bloqueo fantasma indeleble, y la victima sin saber por que no ve a esa persona.
  await limpia();
  await db.doc("users/" + A + "/bloqueados/" + B).set({ creadoEn: Date.now() });
  await fns.onBloqueoEscrito.run(evento(A, B, true));
  await db.doc("users/" + A + "/bloqueados/" + B).delete();
  await fns.onBloqueoEscrito.run(evento(A, B, false));
  // Se reconcilian y vuelven a seguirse: despues de un desbloqueo es lo normal.
  await db.doc("follows/" + A + "_" + B).set({ followerId: A, followingId: B });
  // Y AHORA aparece el evento de bloqueo rezagado.
  await fns.onBloqueoEscrito.run(evento(A, B, true));
  anota("un evento de bloqueo rezagado NO resucita el espejo",
    !(await existe("users/" + B + "/bloqueadoPor/" + A)));
  anota("   ...ni deshace un seguimiento que ya no tiene bloqueo detras",
    await existe("follows/" + A + "_" + B));

  // Y el simetrico: un evento de DESBLOQUEO rezagado no debe borrar un bloqueo vigente.
  await limpia();
  await db.doc("users/" + A + "/bloqueados/" + B).set({ creadoEn: Date.now() });
  await fns.onBloqueoEscrito.run(evento(A, B, true));
  await fns.onBloqueoEscrito.run(evento(A, B, false));   // rezagado, miente
  anota("un evento de desbloqueo rezagado NO borra un bloqueo vigente",
    await existe("users/" + B + "/bloqueadoPor/" + A));

  await limpia();

  // Y que no se cuele un bloqueo de alguien contra si mismo.
  await fns.onBloqueoEscrito.run(evento(A, A, true));
  anota("bloquearse a uno mismo no escribe nada",
    !(await existe("users/" + A + "/bloqueadoPor/" + A)));

  await limpia();
  console.log("\n  " + (fallos === 0 ? "TODO BIEN" : fallos + " FALLO(S)") + "\n");
  process.exit(fallos ? 1 : 0);
}

main().catch(function (e) { console.error(e); process.exit(1); });
