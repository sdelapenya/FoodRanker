// Reintenta la limpieza de las cuentas que se borraron a medias.
//
// Contexto: `deleteUserAccount` (functions/src/index.ts) borra en siete pasos. Los cinco
// primeros son datos asociados (valoraciones, platos, comentarios, follows, guardados,
// colecciones, denuncias, entradas de liga, avisos) y corren best-effort: si uno falla, se
// apunta y se sigue. Eso es deliberado — los pasos 6 y 7, que borran el perfil y la cuenta
// de Firebase Auth, son los que exige Google Play, y un fallo al limpiar valoraciones no
// puede dejar la cuenta viva. El precio es que pueden quedar datos huérfanos.
//
// Desde el 2026-11-18 esos fallos quedan anotados en `borradosIncompletos/{uid}`, con la
// lista de pasos que no salieron. Este script los relee y los vuelve a ejecutar. El
// documento se borra solo cuando TODO sale bien; si vuelve a fallar algo, se queda con la
// lista actualizada y el contador de intentos subido, para volver a intentarlo otro día.
//
// La cuenta de Auth y el documento de perfil ya no existen a estas alturas: aquí solo se
// limpia lo que quedó colgando. Por eso el uid es lo único que hace falta.
//
// Uso (desde functions/):
//   node scripts/retryDeletions.js              # simulación: dice qué haría
//   node scripts/retryDeletions.js --confirm    # borra de verdad
//
// Credenciales: igual que manageUser.js, por Application Default Credentials.
//   GOOGLE_APPLICATION_CREDENTIALS=... node scripts/retryDeletions.js

const admin = require("firebase-admin");

const confirmed = process.argv.includes("--confirm");

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  projectId: "foodranker-51270",
});

const db = admin.firestore();

/** Borra en lotes de 500 (límite de Firestore) todo lo que matchee `query`, paginando. */
async function deleteQueryBatch(query) {
  let total = 0;
  for (;;) {
    const snap = await query.limit(500).get();
    if (snap.empty) return total;
    const batch = db.batch();
    snap.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();
    total += snap.size;
    if (snap.size < 500) return total;
  }
}

/**
 * Los mismos pasos que deleteUserAccount, con las mismas etiquetas.
 *
 * ⚠️ Las etiquetas son la clave que une este script con la función: si allí se renombra
 * un paso y aquí no, ese paso dejará de reintentarse en silencio. Por eso al final se
 * avisa de cualquier etiqueta anotada que no se reconozca, en vez de ignorarla.
 */
function pasosPara(uid) {
  return {
    "ratings by user": () => deleteQueryBatch(db.collection("ratings").where("userId", "==", uid)),
    "user's plates": async () => {
      const platesSnap = await db.collection("plates").where("addedByUserId", "==", uid).get();
      let n = 0;
      for (const plateDoc of platesSnap.docs) {
        await deleteQueryBatch(db.collection("ratings").where("plateId", "==", plateDoc.id));
        await deleteQueryBatch(db.collection("comments").where("plateId", "==", plateDoc.id));
        await plateDoc.ref.delete();
        n++;
      }
      return n;
    },
    "comments": () => deleteQueryBatch(db.collection("comments").where("userId", "==", uid)),
    "follows (follower)": () => deleteQueryBatch(db.collection("follows").where("followerId", "==", uid)),
    "follows (following)": () => deleteQueryBatch(db.collection("follows").where("followingId", "==", uid)),
    "saves": () => deleteQueryBatch(db.collection("saves").where("userId", "==", uid)),
    "collections": () => deleteQueryBatch(db.collection("collections").where("userId", "==", uid)),
    "reports": () => deleteQueryBatch(db.collection("reports").where("reportedByUserId", "==", uid)),
    "league entries": async () => {
      const entries = await db.collectionGroup("entries").where("userId", "==", uid).get();
      if (entries.empty) return 0;
      const lb = db.batch();
      entries.docs.forEach((doc) => lb.delete(doc.ref));
      await lb.commit();
      return entries.size;
    },
    "notifications": async () => {
      const items = await db.collection("notifications").doc(uid).collection("items").get();
      if (!items.empty) {
        const nb = db.batch();
        items.docs.forEach((doc) => nb.delete(doc.ref));
        await nb.commit();
      }
      await db.collection("notifications").doc(uid).delete();
      return items.size;
    },
  };
}

(async () => {
  const pendientes = await db.collection("borradosIncompletos").get();
  if (pendientes.empty) {
    console.log("No hay borrados incompletos. Nada que hacer.");
    return;
  }

  console.log(`${pendientes.size} cuenta(s) con limpieza pendiente.`);
  if (!confirmed) console.log("MODO SIMULACIÓN — no se borra nada. Añade --confirm para hacerlo de verdad.\n");

  for (const doc of pendientes.docs) {
    const uid = doc.id;
    const pasosFallidos = doc.get("pasosFallidos") || [];
    const intentos = doc.get("intentos") || 0;
    console.log(`\n--- ${uid} (intentos previos: ${intentos})`);
    console.log(`    pendiente: ${pasosFallidos.join(", ") || "(nada anotado)"}`);

    const disponibles = pasosPara(uid);
    const desconocidos = pasosFallidos.filter((p) => !disponibles[p]);
    if (desconocidos.length > 0) {
      console.error(`    ⚠️  pasos que este script no sabe reintentar: ${desconocidos.join(", ")}`);
      console.error("        (¿se renombraron en deleteUserAccount? hay que actualizarlos aquí)");
    }

    if (!confirmed) continue;

    const siguenFallando = [...desconocidos];
    for (const etiqueta of pasosFallidos) {
      const fn = disponibles[etiqueta];
      if (!fn) continue;
      try {
        const n = await fn();
        console.log(`    ✓ ${etiqueta} — ${n ?? 0} documento(s)`);
      } catch (err) {
        console.error(`    ✗ ${etiqueta} — ${err.message}`);
        siguenFallando.push(etiqueta);
      }
    }

    if (siguenFallando.length === 0) {
      await doc.ref.delete();
      console.log("    limpieza completada, registro borrado.");
    } else {
      await doc.ref.set(
        {
          pasosFallidos: siguenFallando,
          intentos: admin.firestore.FieldValue.increment(1),
          ultimoIntentoEn: Date.now(),
        },
        { merge: true }
      );
      console.log(`    siguen pendientes: ${siguenFallando.join(", ")}`);
    }
  }
})()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error("ERROR:", err);
    process.exit(1);
  });
