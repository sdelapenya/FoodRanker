// Banea / desbanea un usuario a mano. Único mecanismo de moderación de usuarios que
// existe hoy: no hay rol de admin ni pantalla en la app (ver docs/HANDOFF.md).
//
// El baneo de momento SOLO bloquea el acceso futuro (cuenta de Firebase Auth
// desactivada); el contenido ya publicado por el usuario se queda visible tal cual.
// Si en el futuro se decide borrar también su contenido, el punto de partida es
// reutilizar la lógica de cascade-delete que ya existe en `deleteUserAccount`
// (functions/src/index.ts) en vez de reescribirla aquí.
//
// Requiere ADC activo en el shell (mismo patrón ya usado para los scripts de limpieza
// de datos de prueba: refresh_token de firebase-tools.json). Se ejecuta desde
// e:\FoodRanker\functions con:
//
//   node scripts/manageUser.js ban <uid>
//   node scripts/manageUser.js unban <uid>

const admin = require("firebase-admin");

const [, , action, uid] = process.argv;

if (!["ban", "unban"].includes(action) || !uid) {
  console.error("Uso: node scripts/manageUser.js <ban|unban> <uid>");
  process.exit(1);
}

admin.initializeApp({ credential: admin.credential.applicationDefault() });

async function main() {
  const banned = action === "ban";

  await admin.firestore().collection("users").doc(uid).update({ banned });
  await admin.auth().updateUser(uid, { disabled: banned });

  console.log(`✅ Usuario ${uid} ${banned ? "baneado" : "desbaneado"} correctamente.`);
}

main().catch((err) => {
  console.error("❌ Error:", err.message);
  process.exit(1);
});
