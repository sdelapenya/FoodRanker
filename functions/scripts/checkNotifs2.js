// Segunda pasada, también de solo lectura: ¿a QUIÉN le han llegado los avisos de
// new_plate, y a quién debería haberle llegado y no le llegó?
//
//   node scripts/checkNotifs2.js <email-o-nombre>
//
const admin = require("firebase-admin");

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  projectId: "foodranker-51270",
});

const db = admin.firestore();
const quien = (process.argv[2] || "Sergio").toLowerCase();

(async () => {
  const users = await db.collection("users").get();
  const nombre = {};
  for (const u of users.docs) nombre[u.id] = u.get("name") || u.id;

  // 1. Todos los new_plate, con destinatario y autor.
  // Se traen todos y se filtra aquí: un `where` sobre collectionGroup exigiría crear un
  // índice COLLECTION_GROUP solo para este diagnóstico, y no compensa.
  const todos = await db.collectionGroup("items").get();
  const items = { docs: todos.docs.filter((d) => d.get("type") === "new_plate") };
  items.size = items.docs.length;
  console.log(`\n=== LOS ${items.size} AVISOS new_plate ===`);
  for (const d of items.docs) {
    const destinatario = d.ref.parent.parent.id;
    console.log(
      `  para ${(nombre[destinatario] || destinatario).padEnd(24)}` +
      ` de ${(d.get("fromUserName") || "?").padEnd(20)}` +
      ` platos=${d.get("plateCount")} leido=${d.get("isRead")}` +
      ` ${new Date(d.get("createdAt") || 0).toISOString().slice(0, 10)}`
    );
  }

  // 2. El usuario que nos interesa: a quién sigue y qué avisos tiene.
  const yo = users.docs.find((u) => (u.get("name") || "").toLowerCase().includes(quien));
  if (!yo) { console.log(`\nNo encuentro a "${quien}"`); process.exit(0); }
  console.log(`\n=== ${yo.get("name")} (${yo.id}) ===`);
  console.log(`  fcmToken: ${yo.get("fcmToken") ? "sí" : "NO"}`);

  const sigo = await db.collection("follows").where("followerId", "==", yo.id).get();
  console.log(`\n  SIGUE A ${sigo.size}:`);
  for (const f of sigo.docs) {
    const autor = f.get("followingId");
    const platos = await db.collection("plates").where("addedByUserId", "==", autor).get();
    // ¿Publicó algo DESPUÉS de que empezara a seguirle? Es la única ventana en la que
    // avisarASeguidores podía haberle avisado.
    const desde = f.get("createdAt") || 0;
    const posteriores = platos.docs.filter((p) => (p.get("createdAt") || 0) > desde).length;
    console.log(
      `    ${(nombre[autor] || autor).padEnd(24)} platos=${platos.size}` +
      ` publicados_despues_de_seguirle=${posteriores}` +
      ` (sigue desde ${desde ? new Date(desde).toISOString().slice(0, 10) : "?"})`
    );
  }

  const mios = await db.collection("notifications").doc(yo.id).collection("items").get();
  const porTipo = {};
  for (const d of mios.docs) {
    const t = d.get("type") || "?";
    porTipo[t] = (porTipo[t] || 0) + 1;
  }
  console.log(`\n  SUS AVISOS (${mios.size}):`);
  for (const [t, n] of Object.entries(porTipo).sort((a, b) => b[1] - a[1])) {
    console.log(`    ${t.padEnd(22)} ${n}`);
  }

  process.exit(0);
})().catch((e) => { console.error(e); process.exit(1); });
