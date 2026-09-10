// Acciones administrativas a mano sobre un usuario. Único mecanismo de moderación/gestión
// que existe hoy: no hay rol de admin ni pantalla en la app (ver docs/HANDOFF.md).
//
// ban/unban: bloquea/restaura el acceso (cuenta de Firebase Auth desactivada +
// revocación de refresh tokens en el baneo, para que no siga con acceso hasta que
// caduque el ID token que ya tuviera en el móvil, hasta 1h). El baneo por sí solo SOLO
// bloquea el acceso futuro; el contenido ya publicado por el usuario se queda visible
// tal cual — para borrarlo también, usa wipe-content (independiente, a propósito: no
// todo baneo merece borrar el contenido, p.ej. alguien que se va pero no incumplió nada).
//
// wipe-content: borra el contenido de un usuario (platos, valoraciones que dio,
// comentarios), sin tocar el baneo ni la cuenta. No borra el documento users/{uid}
// (el perfil se queda, solo que vacío) ni la cuenta de Auth — para eso ya existe el
// borrado de cuenta propio (deleteUserAccount, functions/src/index.ts), pensado para
// que lo dispare el propio usuario, no un admin sobre otra cuenta.
//   - Sus platos: se borran uno a uno y de eso se encarga el trigger onPlateDeleted ya
//     desplegado (revierte la XP del autor, borra en cascada ratings/comments/saves de
//     ESE plato, y borra la imagen de Cloudinary) — no hay que reimplementar nada de eso
//     aquí, con borrar el documento basta.
//   - Sus valoraciones dadas en platos de otros, y sus comentarios: se borran
//     directamente. Limitación conocida (la misma que ya tiene deleteUserAccount): no
//     recalcula el averageScore/totalRatings del plato afectado ni revierte la XP que
//     se le dio a SU autor por recibir la valoración — motivo por el que corre en modo
//     simulación (dry-run) por defecto, y solo borra de verdad con --confirm.
//
// grant-premium/revoke-premium: regala/quita Premium a mano (amigos, familia, etc.), sin
// pasar por Google Play Billing. BillingManager (cliente) escucha el campo `premium` de
// Firestore en tiempo real y lo combina con la compra real y el premium temporal por
// anuncio, así que el efecto (quitar anuncios, badge) es inmediato para quien lo recibe,
// sin que tenga que reabrir la app.
//
// Requiere ADC activo en el shell (mismo patrón ya usado para los scripts de limpieza
// de datos de prueba: refresh_token de firebase-tools.json). Se ejecuta desde
// e:\FoodRanker\functions con:
//
//   node scripts/manageUser.js ban <uid>
//   node scripts/manageUser.js unban <uid>
//   node scripts/manageUser.js grant-premium <uid>
//   node scripts/manageUser.js revoke-premium <uid>
//   node scripts/manageUser.js wipe-content <uid>            (simulación: solo cuenta, no borra)
//   node scripts/manageUser.js wipe-content <uid> --confirm  (borra de verdad)
//
// Para banear Y borrar contenido, son dos llamadas: ban <uid> + wipe-content <uid> --confirm.

const admin = require("firebase-admin");

const SIMPLE_ACTIONS = {
  "ban":             { field: "banned",  value: true  },
  "unban":           { field: "banned",  value: false },
  "grant-premium":   { field: "premium", value: true  },
  "revoke-premium":  { field: "premium", value: false },
};

const [, , action, uid, ...rest] = process.argv;
const confirmed = rest.includes("--confirm");

if (action !== "wipe-content" && !SIMPLE_ACTIONS[action]) {
  console.error(
    "Uso: node scripts/manageUser.js <ban|unban|grant-premium|revoke-premium|wipe-content> <uid> [--confirm]"
  );
  process.exit(1);
}
if (!uid) {
  console.error("Falta el <uid>.");
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  projectId: "foodranker-51270",
});

const db = admin.firestore();

/** Borra en lotes de 500 (límite de Firestore) todo lo que matchee `query`, paginando. */
async function deleteQueryBatch(query) {
  const snap = await query.limit(500).get();
  if (snap.empty) return 0;
  const batch = db.batch();
  snap.docs.forEach((doc) => batch.delete(doc.ref));
  await batch.commit();
  const deletedNow = snap.size;
  const deletedMore = snap.size >= 500 ? await deleteQueryBatch(query) : 0;
  return deletedNow + deletedMore;
}

async function wipeContent(uid, confirm) {
  const platesSnap = await db.collection("plates").where("addedByUserId", "==", uid).get();
  const ratingsSnap = await db.collection("ratings").where("userId", "==", uid).get();
  const commentsSnap = await db.collection("comments").where("userId", "==", uid).get();

  console.log(`Contenido de ${uid}:`);
  console.log(`  Platos publicados: ${platesSnap.size}`);
  console.log(`  Valoraciones dadas: ${ratingsSnap.size}`);
  console.log(`  Comentarios: ${commentsSnap.size}`);

  if (!confirm) {
    console.log("\nSimulación (no se ha borrado nada). Repite con --confirm para borrar de verdad.");
    return;
  }

  // Uno a uno, no por lote: cada delete debe disparar el trigger onPlateDeleted, que
  // revierte la XP del autor y hace su propia cascada (ratings/comments/saves de ESE
  // plato + imagen de Cloudinary) — ya verificado en producción, no se reimplementa aquí.
  for (const doc of platesSnap.docs) {
    await doc.ref.delete();
  }
  const ratingsDeleted = await deleteQueryBatch(db.collection("ratings").where("userId", "==", uid));
  const commentsDeleted = await deleteQueryBatch(db.collection("comments").where("userId", "==", uid));

  console.log(`\n✅ Borrado: ${platesSnap.size} platos, ${ratingsDeleted} valoraciones, ${commentsDeleted} comentarios.`);
  console.log(
    "Nota: no se ha recalculado el averageScore/totalRatings de los platos de OTROS " +
    "usuarios que recibieron esas valoraciones, ni la XP que ganó su autor por recibirlas " +
    "(misma limitación conocida que el borrado de cuenta propio)."
  );
}

async function main() {
  if (action === "wipe-content") {
    await wipeContent(uid, confirmed);
    return;
  }

  const { field, value } = SIMPLE_ACTIONS[action];
  await db.collection("users").doc(uid).update({ [field]: value });
  if (field === "banned") {
    await admin.auth().updateUser(uid, { disabled: value });
    if (value) await admin.auth().revokeRefreshTokens(uid);
  }

  console.log(`✅ ${action} aplicado a ${uid} correctamente.`);
}

main().catch((err) => {
  console.error("❌ Error:", err.message);
  process.exit(1);
});
