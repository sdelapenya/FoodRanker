// Siembra el sello `appliedAvg` / `appliedWouldOrderAgain` en las valoraciones que ya
// existen, para que `onRatingUpdated` pueda reconciliar contra un valor conocido en vez de
// fiarse del `before` del evento.
//
// POR QUÉ HACE FALTA
// ------------------
// `onRatingUpdated` ya no suma al plato el delta del evento (`after - before`), porque los
// triggers son at-least-once y una reentrega aplicaba el mismo delta dos veces. Ahora le
// aplica `avgVivo - appliedAvg`, donde `appliedAvg` es lo que esa valoración tiene
// incorporado al agregado del plato. Las valoraciones que ya están en producción no tienen
// ese campo: para ellas hay un camino de respaldo que tira del `before` del evento, que es
// justo lo que no aguanta un desorden de entrega. Este script cierra esa ventana.
//
// DE DÓNDE SALE EL VALOR
// ----------------------
// Del propio `averageScore` de la valoración, y no es una aproximación: los tres sitios que
// incorporan una valoración al plato (`onRatingCreated`, `approveplate` y el propio
// `onRatingUpdated` de antes) reescriben `rating.averageScore` con la nota que acaban de
// aplicar, en la MISMA transacción que toca el plato. O sea que el `averageScore` guardado
// es, por construcción, lo que el plato tiene dentro. Si faltara o no fuera un número, se
// recalcula con la fórmula ponderada del servidor.
//
// Se sellan TODAS las valoraciones, también las que tienen `processed != true`. Esas nunca
// llegaron a sumar en el plato (se valoró un plato pendiente, por ejemplo), y precisamente
// por eso conviene sellarlas: con el sello, una edición futura da diferencia 0 y el plato no
// se mueve, que es lo correcto para algo que nunca se contó. Sin sello, esa edición le
// metería al plato un delta de una valoración que no está en su `totalRatings`.
//
// QUÉ NO HACE
// -----------
// No toca los platos, ni XP, ni liga, ni badges, ni `processed`, ni las notas. Solo añade
// dos campos a `ratings`. Es idempotente: una valoración que ya tiene `appliedAvg` numérico
// se salta, así que se puede repetir sin miedo.
//
// Escribir estos dos campos dispara `onRatingUpdated`, pero su primer filtro solo deja pasar
// cambios en notas, en `wouldOrderAgain` o en el precio, así que estas escrituras salen por
// el `return` de arriba sin tocar ningún agregado. Está comprobado en el emulador contra la
// versión desplegada.
//
// ORDEN DE EJECUCIÓN: este script va ANTES de desplegar el `onRatingUpdated` nuevo. Al
// revés queda abierta la ventana en la que el código nuevo corre sobre valoraciones sin
// sellar.
//
// Uso (desde e:\FoodRanker\functions, con ADC activo):
//   node scripts/backfillAppliedAvg.js            # simulación: enseña qué haría, no escribe
//   node scripts/backfillAppliedAvg.js --confirm  # aplica

const admin = require("firebase-admin");

const confirmed = process.argv.includes("--confirm");

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  projectId: "foodranker-51270",
});

const db = admin.firestore();

// Espejo de functions/src/index.ts — si cambian allí, cambian aquí.
const WEIGHT_FLAVOR = 0.50;
const WEIGHT_PRESENTATION = 0.20;
const WEIGHT_SATISFACTION = 0.30;

function weightedAverage(flavor, presentation, satisfaction) {
  const weighted = WEIGHT_FLAVOR * (flavor || 0) + WEIGHT_PRESENTATION * (presentation || 0);
  if (typeof satisfaction !== "number") {
    return weighted / (WEIGHT_FLAVOR + WEIGHT_PRESENTATION);
  }
  return weighted + WEIGHT_SATISFACTION * satisfaction;
}

function ratingAverage(data) {
  return weightedAverage(
    data.flavorScore,
    data.presentationScore,
    typeof data.satisfactionScore === "number" ? data.satisfactionScore : null
  );
}

async function main() {
  console.log(confirmed
    ? "=== MODO REAL: se van a escribir cambios ==="
    : "=== SIMULACIÓN (sin --confirm no se escribe nada) ===");

  const ratingsSnap = await db.collection("ratings").get();

  const pendientes = [];
  let yaSelladas = 0;
  let sinAverageScore = 0;
  let noProcesadas = 0;
  let discrepan = 0;

  for (const doc of ratingsSnap.docs) {
    const data = doc.data();

    if (typeof data.appliedAvg === "number") {
      yaSelladas++;
      continue;
    }

    const calculado = ratingAverage(data);
    let aplicado;
    if (typeof data.averageScore === "number") {
      aplicado = data.averageScore;
      // Si el guardado no coincide con la fórmula del servidor, manda el guardado: es lo que
      // el plato tiene incorporado de verdad, aunque la nota se calculara con otra fórmula.
      if (Math.abs(aplicado - calculado) > 1e-9) discrepan++;
    } else {
      aplicado = calculado;
      sinAverageScore++;
    }

    const respuesta = typeof data.wouldOrderAgain === "boolean" ? data.wouldOrderAgain : null;
    if (data.processed !== true) noProcesadas++;

    pendientes.push({
      ref: doc.ref,
      id: doc.id,
      procesada: data.processed === true,
      aplicado,
      calculado,
      respuesta,
    });
  }

  console.log(`\n--- ratings: ${ratingsSnap.size} en total ---`);
  console.log(`    ya selladas (se saltan):      ${yaSelladas}`);
  console.log(`    por sellar:                   ${pendientes.length}`);
  console.log(`      de ellas sin processed:     ${noProcesadas}`);
  console.log(`      sin averageScore guardado:  ${sinAverageScore}  (se usa la fórmula)`);
  console.log(`      averageScore != fórmula:    ${discrepan}  (manda el guardado)`);

  pendientes.slice(0, 25).forEach((r) => {
    const marca = r.procesada ? "        " : " [sin procesar]";
    const aviso = Math.abs(r.aplicado - r.calculado) > 1e-9
      ? `  (la fórmula daría ${r.calculado.toFixed(3)})`
      : "";
    console.log(`    ${r.id}${marca}  appliedAvg=${r.aplicado.toFixed(3)}  ` +
                `appliedWouldOrderAgain=${r.respuesta}${aviso}`);
  });
  if (pendientes.length > 25) console.log(`    ... y ${pendientes.length - 25} más`);

  if (pendientes.length === 0) {
    console.log("\nNada que hacer: todas las valoraciones están ya selladas.");
    return;
  }

  if (!confirmed) {
    console.log("\nSimulación terminada. Nada se ha escrito. Repite con --confirm para aplicar.");
    return;
  }

  let escritas = 0;
  for (let i = 0; i < pendientes.length; i += 400) {
    const batch = db.batch();
    pendientes.slice(i, i + 400).forEach((r) => {
      batch.update(r.ref, {
        appliedAvg: r.aplicado,
        appliedWouldOrderAgain: r.respuesta,
      });
      escritas++;
    });
    await batch.commit();
  }

  console.log(`\n✅ Sello sembrado en ${escritas} valoración(es).`);

  // Relectura de verdad: que no quede ninguna sin sellar.
  const repaso = await db.collection("ratings").get();
  const sueltas = repaso.docs.filter((d) => typeof d.data().appliedAvg !== "number");
  if (sueltas.length === 0) {
    console.log(`   Comprobado releyendo: las ${repaso.size} valoraciones tienen appliedAvg numérico.`);
  } else {
    console.log(`   ⚠️  quedan ${sueltas.length} sin sellar: ${sueltas.map((d) => d.id).join(", ")}`);
  }
}

main().then(() => process.exit(0)).catch((err) => {
  console.error("Error:", err);
  process.exit(1);
});
