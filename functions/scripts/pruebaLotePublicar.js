/**
 * Reproduce de verdad EL LOTE DE PUBLICAR UN PLATO contra las reglas reales.
 *
 * ## Por que existe este fichero
 *
 * Las reglas de bloqueo necesitan saber quien es el autor del plato que se valora. La
 * primera version lo buscaba con `get()`, que lee el estado ANTERIOR a la escritura. Pero
 * la app crea el plato y su valoracion inicial EN EL MISMO LOTE (AddPlateViewModel), asi
 * que en ese momento el plato todavia no existe: `get()` sobre un documento inexistente
 * revienta la evaluacion y **deniega el lote entero**. Nadie habria podido publicar.
 *
 * `tools/rules/check_bloqueo.py` ya cubre el caso contra el motor real, pero lo hace con
 * `functionMocks`: ahi el comportamiento del lote lo **modelo yo**. Si mi modelo fuese
 * falso -- si `getAfter` no viera al hermano del lote -- la prueba pasaria igual y
 * publicar seguiria roto. Esto lo ejecuta: lote real, reglas reales, denegacion real.
 *
 * Es la leccion que aqui costo dos versiones: un arreglo no esta bien hasta que se ve el
 * fallo reproducido y despues desaparecer.
 *
 * ## Como se usa
 *
 *   export JAVA_HOME=/c/Users/User/.jdks/ms-21.0.9   # firebase-tools ya NO admite Java < 21
 *   npx firebase-tools emulators:start --only firestore,auth --project demo-foodranker
 *   node functions/scripts/pruebaLotePublicar.js
 *
 * Usa el SDK de CLIENTE, no el Admin: el Admin se salta las reglas y no probaria nada.
 */
"use strict";

// Mismas guardas que pruebaCarreras.js, y por el mismo motivo: la primera version de
// aquel fichero prometia una proteccion que no daba. Se comprueba ANTES de poner ningun
// valor por defecto, se exige loopback, y el proyecto lleva prefijo `demo-` (la
// convencion de Firebase para lo que solo existe en el emulador).
const HOST = process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8080";
const HOST_AUTH = process.env.FIREBASE_AUTH_EMULATOR_HOST || "127.0.0.1:9099";
const loopback = (h) => /^(127\.0\.0\.1|\[::1\]|localhost):\d+$/.test(h);
for (const par of [["FIRESTORE_EMULATOR_HOST", HOST], ["FIREBASE_AUTH_EMULATOR_HOST", HOST_AUTH]]) {
  if (!loopback(par[1])) {
    console.error(par[0] + ' apunta a "' + par[1] + '", que no es loopback.');
    console.error("Abortado: esto solo corre contra un emulador local.");
    process.exit(2);
  }
}
const PROYECTO = process.env.GCLOUD_PROJECT || "demo-foodranker";
if (!/^demo-/.test(PROYECTO)) {
  console.error('GCLOUD_PROJECT es "' + PROYECTO + '", sin prefijo demo-.');
  console.error("Abortado: usa un proyecto demo-* para que no pueda tocar nada real.");
  process.exit(2);
}

const { initializeApp } = require("firebase/app");
const { getAuth, connectAuthEmulator, signInAnonymously } = require("firebase/auth");
const {
  getFirestore, connectFirestoreEmulator, doc, setDoc, writeBatch, getDoc, deleteDoc,
} = require("firebase/firestore");

const VENUE = "venueDePrueba";
const SLUG = "tortilla-de-prueba";
const PLATE = VENUE + "__" + SLUG;

function plato(uid) {
  return {
    id: PLATE, name: "Tortilla de prueba", description: "", category: "SPANISH",
    restaurantName: "Bar de prueba", restaurantAddress: "Calle Falsa 123",
    city: "Madrid", country: "Espana", countryCode: "ES", latitude: 40.4, longitude: -3.7,
    imageUrl: "https://example.invalid/x.jpg", addedByUserId: uid, addedByUserName: "Prueba",
    averageScore: 0, totalRatings: 0, createdAt: Date.now(),
    likes: 0, likedByUsers: [], reportCount: 0, status: "pending",
    venueId: VENUE, dishSlug: SLUG,
    rankingScore: 0, wouldOrderAgainCount: 0, wouldOrderAgainResponses: 0,
    priceMedianCents: null, priceReportCount: 0, priceUpdatedAt: 0,
  };
}

function valoracion(uid, plateId) {
  return {
    id: plateId + "_" + uid, plateId: plateId, userId: uid,
    userName: "Prueba", userPhotoUrl: "",
    flavorScore: 8, presentationScore: 8, satisfactionScore: 8, valueScore: 0,
    wouldOrderAgain: true, pricePaidCents: 1200, verifiedAtVenue: false,
    averageScore: 8, comment: "", createdAt: Date.now(),
  };
}

let fallos = 0;
function anota(nombre, ok, detalle) {
  if (!ok) fallos++;
  console.log("  " + (ok ? "OK " : "MAL") + "  " + nombre + (detalle ? "  -- " + detalle : ""));
}

async function main() {
  process.env.FIRESTORE_EMULATOR_HOST = HOST;
  process.env.FIREBASE_AUTH_EMULATOR_HOST = HOST_AUTH;
  const app = initializeApp({ projectId: PROYECTO, apiKey: "demo" });
  const auth = getAuth(app);
  connectAuthEmulator(auth, "http://" + HOST_AUTH, { disableWarnings: true });
  const db = getFirestore(app);
  const trozos = HOST.split(":");
  connectFirestoreEmulator(db, trozos[0], Number(trozos[1]));

  const cred = await signInAnonymously(auth);
  const uid = cred.user.uid;
  console.log("\nEmulador " + HOST + ", proyecto " + PROYECTO + ", uid " + uid + "\n");

  // Siembra por REST del emulador, que entra como administrador y NO pasa por las
  // reglas: el venue solo lo escribe resolveVenue via Admin SDK, y el plato ajeno
  // tiene que existir ya para poder intentar valorarlo.
  const base = "http://" + HOST + "/v1/projects/" + PROYECTO + "/databases/(default)/documents";
  async function siembra(coleccion, id, campos) {
    const r = await fetch(base + "/" + coleccion + "?documentId=" + id, {
      method: "POST",
      headers: { "Authorization": "Bearer owner", "Content-Type": "application/json" },
      body: JSON.stringify({ fields: campos }),
    });
    if (!r.ok && r.status !== 409) {
      throw new Error("no se pudo sembrar " + coleccion + "/" + id + ": " + r.status + " " + (await r.text()));
    }
  }
  await siembra("venues", VENUE, { name: { stringValue: "Bar de prueba" } });

  // ── LA PRUEBA ───────────────────────────────────────────────────────────────
  // Plato + valoracion en el MISMO lote, igual que AddPlateViewModel.
  try {
    const lote = writeBatch(db);
    lote.set(doc(db, "plates", PLATE), plato(uid));
    lote.set(doc(db, "ratings", PLATE + "_" + uid), valoracion(uid, PLATE));
    await lote.commit();
    anota("publicar plato + valoracion EN EL MISMO LOTE", true);
  } catch (e) {
    anota("publicar plato + valoracion EN EL MISMO LOTE", false, e.code || e.message);
  }
  const creado = await getDoc(doc(db, "ratings", PLATE + "_" + uid));
  anota("la valoracion quedo escrita", creado.exists());

  // Y que el bloqueo sigue mordiendo donde debe.
  const otro = "usuarioFicticioDePrueba";
  const plateOtro = VENUE + "__plato-del-otro";
  await siembra("plates", plateOtro, { addedByUserId: { stringValue: otro } });
  await setDoc(doc(db, "users", uid, "bloqueados", otro), { creadoEn: Date.now() });
  try {
    await setDoc(doc(db, "ratings", plateOtro + "_" + uid), valoracion(uid, plateOtro));
    anota("valorar el plato de alguien a quien bloquee", false, "LO PERMITIO");
  } catch (e) {
    anota("valorar el plato de alguien a quien bloquee", e.code === "permission-denied", e.code);
  }

  // El control que convierte lo anterior en una prueba de verdad: si al quitar el
  // bloqueo la MISMA escritura pasa, la denegacion de arriba la causo el bloqueo y no
  // otra validacion de la regla que diera error por su cuenta.
  await deleteDoc(doc(db, "users", uid, "bloqueados", otro));
  try {
    await setDoc(doc(db, "ratings", plateOtro + "_" + uid), valoracion(uid, plateOtro));
    anota("y al desbloquear, la misma escritura pasa", true);
  } catch (e) {
    anota("y al desbloquear, la misma escritura pasa", false, e.code || e.message);
  }

  // ── limpieza ────────────────────────────────────────────────────────────────
  // Nada de esto debe sobrevivir entre ejecuciones: un residuo hace que la siguiente
  // pase por el motivo equivocado (ya paso con el acta de pruebaCarreras.js).
  const rutas = ["plates/" + PLATE, "plates/" + plateOtro, "ratings/" + PLATE + "_" + uid,
    "ratings/" + plateOtro + "_" + uid, "venues/" + VENUE];
  for (const ruta of rutas) {
    await fetch(base + "/" + ruta, { method: "DELETE", headers: { "Authorization": "Bearer owner" } });
  }

  console.log("\n  " + (fallos === 0 ? "TODO BIEN" : fallos + " FALLO(S)") + "\n");
  process.exit(fallos ? 1 : 0);
}

main().catch(function (e) { console.error(e); process.exit(1); });
