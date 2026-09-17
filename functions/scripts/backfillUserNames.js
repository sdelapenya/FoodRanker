// Repara comments.userName, ratings.userName y plates.addedByUserName cuando están vacíos
// o en "Usuario" pero el usuario real (users/{uid}.name) ya tiene su nombre correcto.
//
// Causa del desajuste (ver docs/HANDOFF.md, sesión 2026-09-17): addComment/submitRating/
// AddPlateViewModel leían auth.currentUser?.displayName directamente para guardar el
// nombre, y ese campo puede venir NULO o **vacío** ("") tras el login por navegador (ver
// AuthRepository) — el `?: "Usuario"` de entonces solo cubría el caso nulo, así que un
// displayName vacío se guardaba tal cual, en blanco. Ya arreglado en el código (los 4 sitios
// ahora leen users/{uid}.name), pero el contenido ya publicado se quedó con el nombre malo
// para siempre si nadie lo repara. Este script es ese repaso, de una sola vez.
//
// Solo toca un documento si el nombre real de ese usuario hoy es distinto de vacío/"Usuario"
// — así no se sobreescribe nada de alguien a quien de verdad le sigue faltando el nombre.
//
// Uso (desde e:\FoodRanker\functions, con ADC activo):
//   node scripts/backfillUserNames.js            # simulación: solo cuenta y lista
//   node scripts/backfillUserNames.js --confirm  # aplica los cambios de verdad

const admin = require("firebase-admin");

const confirmed = process.argv.includes("--confirm");

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  projectId: "foodranker-51270",
});

const db = admin.firestore();

const TARGETS = [
  { collection: "comments", userIdField: "userId", nameField: "userName" },
  { collection: "ratings", userIdField: "userId", nameField: "userName" },
  { collection: "plates", userIdField: "addedByUserId", nameField: "addedByUserName" },
];

async function main() {
  const usersSnap = await db.collection("users").get();
  const nameByUid = {};
  usersSnap.docs.forEach((d) => { nameByUid[d.id] = d.data().name || ""; });

  let totalFixed = 0;

  for (const { collection, userIdField, nameField } of TARGETS) {
    const snap = await db.collection(collection).get();
    const toFix = [];
    for (const doc of snap.docs) {
      const data = doc.data();
      const stored = data[nameField] || "";
      const uid = data[userIdField];
      const real = nameByUid[uid];
      if (real && real !== stored && (stored === "" || stored === "Usuario")) {
        toFix.push({ id: doc.id, ref: doc.ref, stored, real });
      }
    }

    console.log(`\n=== ${collection}.${nameField}: ${toFix.length} documento(s) a corregir ===`);
    toFix.forEach((f) => console.log(`  ${f.id}: "${f.stored}" -> "${f.real}"`));

    if (confirmed && toFix.length > 0) {
      // 500 es el máximo por batch de Firestore; aquí nunca se acerca, pero por si crece.
      for (let i = 0; i < toFix.length; i += 500) {
        const batch = db.batch();
        toFix.slice(i, i + 500).forEach((f) => batch.update(f.ref, { [nameField]: f.real }));
        await batch.commit();
      }
      totalFixed += toFix.length;
    }
  }

  if (!confirmed) {
    console.log("\nSimulación (no se ha escrito nada). Repite con --confirm para aplicar de verdad.");
  } else {
    console.log(`\n✅ ${totalFixed} documento(s) corregido(s) en total.`);
  }
}

main().catch((err) => {
  console.error("❌ Error:", err.message);
  process.exit(1);
});
