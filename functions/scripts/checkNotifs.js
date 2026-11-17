// Radiografía de solo lectura del sistema de avisos. No escribe nada.
//
// Nació de una duda concreta (2026-11-17): "cuando sigues a alguien, ¿te avisa si sube un
// plato?". El código del servidor dice que sí (`avisarASeguidores` en functions/src/index.ts
// y el caso "new_plate" del switch de `onNotificationCreated`), pero eso solo prueba que
// está escrito, no que funcione. Esto mira los datos reales.
//
// Requiere ADC activo (mismo patrón que manageUser.js: refresh_token de
// firebase-tools.json). Desde e:\FoodRanker\functions:
//
//   node scripts/checkNotifs.js
//
const admin = require("firebase-admin");

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  projectId: "foodranker-51270",
});

const db = admin.firestore();

(async () => {
  // 1. Cuántos avisos hay de cada tipo, en todos los usuarios.
  const items = await db.collectionGroup("items").get();
  const porTipo = {};
  let sinLeer = 0;
  for (const d of items.docs) {
    const t = d.get("type") || "(sin tipo)";
    porTipo[t] = (porTipo[t] || 0) + 1;
    if (d.get("isRead") === false) sinLeer += 1;
  }
  console.log(`\n=== AVISOS EXISTENTES (${items.size} en total, ${sinLeer} sin leer) ===`);
  for (const [t, n] of Object.entries(porTipo).sort((a, b) => b[1] - a[1])) {
    console.log(`  ${t.padEnd(22)} ${n}`);
  }

  // 2. El grafo de seguimiento: sin seguidores, no puede haber avisos de new_plate.
  const follows = await db.collection("follows").get();
  const seguidoresDe = {};
  for (const f of follows.docs) {
    const a = f.get("followingId");
    if (a) (seguidoresDe[a] = seguidoresDe[a] || []).push(f.get("followerId"));
  }
  console.log(`\n=== SEGUIMIENTOS (${follows.size}) ===`);
  for (const [autor, segs] of Object.entries(seguidoresDe)) {
    console.log(`  a ${autor} lo siguen ${segs.length}`);
  }

  // 3. ¿Han publicado algo los que tienen seguidores? Es la condición que falta para que
  //    `avisarASeguidores` llegue a ejecutarse.
  console.log(`\n=== PLATOS DE QUIEN TIENE SEGUIDORES ===`);
  for (const autor of Object.keys(seguidoresDe)) {
    const platos = await db.collection("plates").where("addedByUserId", "==", autor).get();
    const aprobados = platos.docs.filter((p) => p.get("status") === "approved").length;
    const u = await db.collection("users").doc(autor).get();
    const fecha = platos.docs
      .map((p) => p.get("createdAt") || 0)
      .sort((a, b) => b - a)[0];
    console.log(
      `  ${(u.get("name") || autor).padEnd(24)} platos=${platos.size} aprobados=${aprobados}` +
      `  ultimo=${fecha ? new Date(fecha).toISOString().slice(0, 10) : "-"}`
    );
  }

  // 4. Quién puede recibir un push: sin fcmToken, `onNotificationCreated` se rinde.
  const users = await db.collection("users").get();
  const conToken = users.docs.filter((u) => !!u.get("fcmToken")).length;
  console.log(`\n=== TOKENS FCM ===`);
  console.log(`  usuarios=${users.size}  con fcmToken=${conToken}  sin token=${users.size - conToken}`);

  process.exit(0);
})().catch((e) => {
  console.error(e);
  process.exit(1);
});
