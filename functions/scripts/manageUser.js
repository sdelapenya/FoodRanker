// Acciones administrativas a mano sobre un usuario. Único mecanismo de moderación/gestión
// que existe hoy: no hay rol de admin ni pantalla en la app (ver docs/HANDOFF.md).
//
// ban/unban: bloquea/restaura el acceso (cuenta de Firebase Auth desactivada +
// revocación de refresh tokens en el baneo, para que no siga con acceso hasta que
// caduque el ID token que ya tuviera en el móvil, hasta 1h). El baneo de momento SOLO
// bloquea el acceso futuro; el contenido ya publicado por el usuario se queda visible
// tal cual. Si en el futuro se decide borrar también su contenido, el punto de partida
// es reutilizar la lógica de cascade-delete que ya existe en `deleteUserAccount`
// (functions/src/index.ts) en vez de reescribirla aquí.
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

const admin = require("firebase-admin");

const ACTIONS = {
  "ban":             { field: "banned",  value: true  },
  "unban":           { field: "banned",  value: false },
  "grant-premium":   { field: "premium", value: true  },
  "revoke-premium":  { field: "premium", value: false },
};

const [, , action, uid] = process.argv;

if (!ACTIONS[action] || !uid) {
  console.error("Uso: node scripts/manageUser.js <ban|unban|grant-premium|revoke-premium> <uid>");
  process.exit(1);
}

admin.initializeApp({ credential: admin.credential.applicationDefault() });

async function main() {
  const { field, value } = ACTIONS[action];

  await admin.firestore().collection("users").doc(uid).update({ [field]: value });
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
