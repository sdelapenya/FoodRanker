// Enseña el estado del bloqueo entre dos usuarios, en PRODUCCION y en solo lectura.
//
// Para probar el bloqueo en el movil sin tener que creerse lo que se ve en pantalla. La
// app puede estar ensenando lo correcto por el motivo equivocado (una cache, una lista
// que no se recargo), asi que esto va a mirar los documentos.
//
// NO ESCRIBE NADA. Solo lee.
//
// Requiere ADC activo en el shell (refresh_token de firebase-tools.json, el mismo patron
// que manageUser.js). Se ejecuta desde e:\FoodRanker\functions con:
//
//   node scripts/verBloqueo.js <uidA> <uidB>
//   node scripts/verBloqueo.js --quien <texto>     busca uids por nombre o correo
//
// Lo que hay que ver despues de que A bloquee a B:
//
//   users/A/bloqueados/B        SI   <- lo escribe la app
//   users/B/bloqueadoPor/A      SI   <- lo escribe onBloqueoEscrito; si falta, el movil
//                                       de B nunca se entera y le sigue viendo a A
//   follows/A_B                 NO   <- los deshace el trigger, los dos
//   follows/B_A                 NO

const admin = require("firebase-admin");
admin.initializeApp({ projectId: "foodranker-51270" });
const db = admin.firestore();

const SI = "SI ";
const NO = "no ";

async function hay(ruta) {
  return (await db.doc(ruta).get()).exists ? SI : NO;
}

async function nombre(uid) {
  const d = await db.collection("users").doc(uid).get();
  if (!d.exists) return "(sin perfil)";
  return (d.get("name") || "(sin nombre)") + "";
}

async function buscar(texto) {
  const t = texto.toLowerCase();
  const snap = await db.collection("users").limit(500).get();
  const hits = snap.docs.filter((d) => ((d.get("name") || "") + "").toLowerCase().includes(t));
  if (hits.length === 0) {
    console.log("Ningun usuario con ese nombre. Prueba con menos letras.");
    return;
  }
  console.log("");
  hits.forEach((d) => console.log("  " + d.id + "   " + (d.get("name") || "(sin nombre)")));
  console.log("");
}

async function estado(a, b) {
  const [na, nb] = [await nombre(a), await nombre(b)];
  console.log("\n  A = " + a + "   " + na);
  console.log("  B = " + b + "   " + nb + "\n");

  const filas = [
    ["users/A/bloqueados/B    (A bloqueo a B)", "users/" + a + "/bloqueados/" + b],
    ["users/B/bloqueadoPor/A  (el espejo)    ", "users/" + b + "/bloqueadoPor/" + a],
    ["users/B/bloqueados/A    (B bloqueo a A)", "users/" + b + "/bloqueados/" + a],
    ["users/A/bloqueadoPor/B  (el espejo)    ", "users/" + a + "/bloqueadoPor/" + b],
    ["follows/A_B             (A sigue a B)  ", "follows/" + a + "_" + b],
    ["follows/B_A             (B sigue a A)  ", "follows/" + b + "_" + a],
  ];
  for (const [etiqueta, ruta] of filas) {
    console.log("  " + (await hay(ruta)) + "  " + etiqueta);
  }

  // Lo que el bloqueo NO deshace, para no confundirlo con un fallo: una valoracion o un
  // comentario anteriores siguen ahi. Lo que cambia es que ya no se ven ni se pueden
  // editar, no que se borren. Borrar lo escrito no es bloquear, es otra cosa.
  const [ra, rb, ca, cb] = await Promise.all([
    db.collection("ratings").where("userId", "==", a).limit(5).get(),
    db.collection("ratings").where("userId", "==", b).limit(5).get(),
    db.collection("comments").where("userId", "==", a).limit(5).get(),
    db.collection("comments").where("userId", "==", b).limit(5).get(),
  ]);
  console.log("\n  (sin tocar, y es lo correcto: A tiene " + ra.size + " valoraciones y " +
    ca.size + " comentarios; B, " + rb.size + " y " + cb.size + ")\n");
}

async function main() {
  const args = process.argv.slice(2);
  if (args[0] === "--quien" && args[1]) return buscar(args.slice(1).join(" "));
  if (args.length !== 2) {
    console.log("Uso: node scripts/verBloqueo.js <uidA> <uidB>");
    console.log("     node scripts/verBloqueo.js --quien <nombre>");
    process.exit(2);
  }
  await estado(args[0], args[1]);
}

main().then(() => process.exit(0)).catch((e) => { console.error(e); process.exit(1); });
