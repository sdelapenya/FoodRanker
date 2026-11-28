/**
 * Ejercita el filtro de bloqueados de `avisarASeguidores`.
 *
 * ## Por que hace falta
 *
 * Era la unica pieza del bloqueo que no vigilaba nada. Las reglas tienen 24 casos, el
 * disparador 11, el cliente su comprobador; esto, cero. Y ademas **no se ha ejecutado ni
 * una vez en produccion**: `validateFoodImage`, que es quien la llama, no corre desde el
 * 1 de noviembre. O sea que el dia que alguien publique un plato sera la primera vez que
 * este codigo se ejecute de verdad, con usuarios reales delante.
 *
 * ## Por que importa que este filtro funcione
 *
 * Este aviso lo escribe el **Admin SDK**, que no pasa por las reglas. Las reglas son la
 * cerradura de todo lo demas, pero aqui no protegen nada: si el filtro se cae, quien te
 * bloqueo recibe tus avisos de plato nuevo con tu nombre y tu foto, en su campana, cada
 * vez que publiques. Eso es exactamente lo que el bloqueo promete que no pasa.
 *
 * Y hay dos sentidos, no uno. El facil es "no avisar a quien yo bloquee". El que de
 * verdad cuesta es el contrario: **a quien me bloqueo a MI**. Ese seguimiento el cliente
 * del autor no lo puede borrar --la regla de borrado exige ser el seguidor--, asi que si
 * el servidor no lo filtra aqui, no lo filtra nadie.
 *
 * ## Como se usa
 *
 *   export JAVA_HOME=/c/Users/User/.jdks/ms-21.0.9   # firebase-tools ya NO admite Java < 21
 *   npx firebase-tools emulators:start --only firestore --project demo-foodranker
 *   cd functions && npm run build && node scripts/pruebaAvisoSeguidores.js
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

const AUTOR = "pruebaAutor";
const AMIGO = "pruebaAmigo";       // seguidor normal: debe recibir el aviso
const BLOQUEADO = "pruebaBloqueado"; // el autor le bloqueo: no debe recibirlo
const ENEMIGO = "pruebaEnemigo";   // bloqueo al autor: tampoco, y este es el dificil
const TODOS = [AUTOR, AMIGO, BLOQUEADO, ENEMIGO];

const PLATO = "platoDePrueba";

let fallos = 0;
function anota(nombre, ok, detalle) {
  if (!ok) fallos++;
  console.log("  " + (ok ? "OK " : "MAL") + "  " + nombre + (detalle ? "  -- " + detalle : ""));
}

const dia = () => new Date().toISOString().slice(0, 10);
const avisoDe = (uid) =>
  db.collection("notifications").doc(uid).collection("items")
    .doc("newplate_" + AUTOR + "_" + dia());

async function recibio(uid) {
  return (await avisoDe(uid).get()).exists;
}

async function limpia() {
  for (const uid of TODOS) {
    await avisoDe(uid).delete();
    await db.collection("users").doc(uid).delete();
    for (const otro of TODOS) {
      await db.collection("users").doc(uid).collection("bloqueados").doc(otro).delete();
      await db.collection("follows").doc(uid + "_" + otro).delete();
    }
  }
}

async function sigue(de, a) {
  await db.collection("follows").doc(de + "_" + a)
    .set({ followerId: de, followingId: a, createdAt: Date.now() });
}

async function main() {
  console.log("\nEmulador " + HOST + ", proyecto " + process.env.GCLOUD_PROJECT + "\n");
  await limpia();

  await db.collection("users").doc(AUTOR).set({ name: "Autor de prueba" });
  await sigue(AMIGO, AUTOR);
  await sigue(BLOQUEADO, AUTOR);
  await sigue(ENEMIGO, AUTOR);

  // Los dos sentidos del bloqueo, cada uno escrito donde de verdad vive.
  await db.collection("users").doc(AUTOR).collection("bloqueados").doc(BLOQUEADO)
    .set({ creadoEn: Date.now() });
  await db.collection("users").doc(ENEMIGO).collection("bloqueados").doc(AUTOR)
    .set({ creadoEn: Date.now() });

  await fns.avisarASeguidores(AUTOR, PLATO, "Tortilla de prueba");

  anota("el seguidor sin bloqueo SI recibe el aviso", await recibio(AMIGO));
  anota("a quien el autor bloqueo NO le llega", !(await recibio(BLOQUEADO)));
  anota("a quien bloqueo AL AUTOR tampoco  (el que el cliente no puede arreglar)",
    !(await recibio(ENEMIGO)));

  // Nadie se avisa a si mismo aunque figure siguiendose (dato roto, pasa).
  await sigue(AUTOR, AUTOR);
  await avisoDe(AUTOR).delete();
  await fns.avisarASeguidores(AUTOR, PLATO, "Tortilla de prueba");
  anota("el autor no se avisa a si mismo", !(await recibio(AUTOR)));

  // Dos platos el mismo dia agrupan en un solo aviso y suman el contador: el id lleva la
  // fecha a proposito. Si esto se rompiera, publicar tres veces llenaria la campana.
  const antes = (await avisoDe(AMIGO).get()).get("plateCount");
  await fns.avisarASeguidores(AUTOR, "otroPlato", "Segundo plato");
  const despues = (await avisoDe(AMIGO).get()).get("plateCount");
  anota("dos platos el mismo dia = un aviso con el contador sumado",
    despues === antes + 1, "antes " + antes + ", despues " + despues);

  // Sin seguidores no debe reventar ni escribir nada.
  await limpia();
  await db.collection("users").doc(AUTOR).set({ name: "Autor de prueba" });
  await fns.avisarASeguidores(AUTOR, PLATO, "Plato sin publico");
  anota("sin seguidores no falla y no escribe nada", !(await recibio(AMIGO)));

  // Y si TODOS los seguidores estan bloqueados, tampoco.
  await sigue(BLOQUEADO, AUTOR);
  await db.collection("users").doc(AUTOR).collection("bloqueados").doc(BLOQUEADO)
    .set({ creadoEn: Date.now() });
  await fns.avisarASeguidores(AUTOR, PLATO, "Plato para nadie");
  anota("si todos los seguidores estan bloqueados, no se escribe nada",
    !(await recibio(BLOQUEADO)));

  await limpia();
  console.log("\n  " + (fallos === 0 ? "TODO BIEN" : fallos + " FALLO(S)") + "\n");
  process.exit(fallos ? 1 : 0);
}

main().catch(function (e) { console.error(e); process.exit(1); });
