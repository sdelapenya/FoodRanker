// Migración del rediseño de valoraciones (ver docs/RATINGS.md).
//
// Qué hace, de una sola vez:
//   1. Recalcula `ratings.averageScore` con la fórmula ponderada nueva
//      (50 % sabor / 20 % presentación / 30 % satisfacción).
//   2. Recalcula `plates.averageScore` y `plates.totalRatings` sumando SOLO los ratings ya
//      procesados (`processed == true`) — los mismos que cuentan para las Cloud Functions.
//   3. Siembra los agregados nuevos del plato: `wouldOrderAgainCount`, `rankingScore`
//      (media bayesiana) y la mediana de precio si alguien la ha aportado.
//   4. Escribe `stats/global.averageScore`, la C de la bayesiana.
//
// ⚠️ Las valoraciones antiguas NO tienen `satisfactionScore`, y NO se rellena con
// `valueScore`: medían cosas distintas (precio/calidad vs. saciedad) y copiarlo sería
// inventar datos. Para esas se renormaliza sobre los ejes disponibles:
//     nota = (0.50 · sabor + 0.20 · presentación) / 0.70
// Es matemáticamente correcto y no se apoya en ningún dato que no exista.
//
// No toca XP, ni votos de liga, ni fechas, ni borra nada. `valueScore` se conserva tal cual
// (decisión explícita: no se muestra, pero permite reconstruir si la fórmula sale mal
// calibrada).
//
// Uso (desde e:\FoodRanker\functions, con ADC activo):
//   node scripts/migrateRatings.js            # simulación: calcula y enseña, no escribe
//   node scripts/migrateRatings.js --confirm  # aplica los cambios de verdad

const admin = require("firebase-admin");

const confirmed = process.argv.includes("--confirm");

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  projectId: "foodranker-51270",
});

const db = admin.firestore();

// Espejo de functions/src/index.ts y Rating.kt — si cambian allí, cambian aquí.
const WEIGHT_FLAVOR = 0.50;
const WEIGHT_PRESENTATION = 0.20;
const WEIGHT_SATISFACTION = 0.30;
const BAYESIAN_MIN_VOTES = 5;
const DEFAULT_GLOBAL_AVERAGE = 7.0;

function weightedAverage(flavor, presentation, satisfaction) {
  const weighted = WEIGHT_FLAVOR * (flavor || 0) + WEIGHT_PRESENTATION * (presentation || 0);
  if (typeof satisfaction !== "number") {
    return weighted / (WEIGHT_FLAVOR + WEIGHT_PRESENTATION);
  }
  return weighted + WEIGHT_SATISFACTION * satisfaction;
}

function bayesianScore(average, count, globalAverage) {
  if (count <= 0) return 0;
  return (count / (count + BAYESIAN_MIN_VOTES)) * average
       + (BAYESIAN_MIN_VOTES / (count + BAYESIAN_MIN_VOTES)) * globalAverage;
}

function median(values) {
  const sorted = [...values].sort((a, b) => a - b);
  const mid = Math.floor(sorted.length / 2);
  return sorted.length % 2 === 0
    ? Math.round((sorted[mid - 1] + sorted[mid]) / 2)
    : sorted[mid];
}

async function main() {
  console.log(confirmed ? "=== MODO REAL: se van a escribir cambios ===" : "=== SIMULACIÓN (sin --confirm no se escribe nada) ===");

  const [platesSnap, ratingsSnap] = await Promise.all([
    db.collection("plates").get(),
    db.collection("ratings").get(),
  ]);

  // ── 1. Ratings: nota ponderada ──────────────────────────────────────────
  const ratingUpdates = [];
  const byPlate = new Map();

  for (const doc of ratingsSnap.docs) {
    const data = doc.data();
    const satisfaction = typeof data.satisfactionScore === "number" ? data.satisfactionScore : null;
    const newAvg = weightedAverage(data.flavorScore, data.presentationScore, satisfaction);
    const oldAvg = typeof data.averageScore === "number" ? data.averageScore : 0;

    if (Math.abs(newAvg - oldAvg) > 1e-9) {
      ratingUpdates.push({ ref: doc.ref, id: doc.id, oldAvg, newAvg, legacy: satisfaction === null });
    }

    // Solo los procesados cuentan para el plato — mismo criterio que onRatingCreated.
    if (data.processed === true && data.plateId) {
      if (!byPlate.has(data.plateId)) byPlate.set(data.plateId, []);
      byPlate.get(data.plateId).push({ avg: newAvg, data });
    }
  }

  const legacyCount = ratingUpdates.filter((r) => r.legacy).length;
  console.log(`\n--- ratings: ${ratingUpdates.length} de ${ratingsSnap.size} cambian de nota ---`);
  console.log(`    (${legacyCount} sin satisfactionScore → renormalizados sobre sabor+presentación)`);
  ratingUpdates.slice(0, 15).forEach((r) => {
    console.log(`    ${r.id}: ${r.oldAvg.toFixed(2)} -> ${r.newAvg.toFixed(2)}${r.legacy ? "  (renormalizado)" : ""}`);
  });
  if (ratingUpdates.length > 15) console.log(`    ... y ${ratingUpdates.length - 15} más`);

  // ── 2. Platos: agregados ────────────────────────────────────────────────
  // C se calcula con las notas NUEVAS, antes de escribir nada, para que rankingScore y
  // stats/global salgan del mismo conjunto de datos y no de un estado intermedio.
  const plateAverages = [];
  for (const doc of platesSnap.docs) {
    if (doc.data().status !== "approved") continue;
    const entries = byPlate.get(doc.id) || [];
    if (entries.length === 0) continue;
    plateAverages.push(entries.reduce((acc, e) => acc + e.avg, 0) / entries.length);
  }
  const globalAverage = plateAverages.length > 0
    ? plateAverages.reduce((a, b) => a + b, 0) / plateAverages.length
    : DEFAULT_GLOBAL_AVERAGE;
  console.log(`\n--- media global (C) = ${globalAverage.toFixed(3)} sobre ${plateAverages.length} plato(s) aprobado(s) ---`);

  const plateUpdates = [];
  for (const doc of platesSnap.docs) {
    const data = doc.data();
    const entries = byPlate.get(doc.id) || [];
    const count = entries.length;
    const avg = count > 0 ? entries.reduce((acc, e) => acc + e.avg, 0) / count : 0;
    const repeats = entries.filter((e) => e.data.wouldOrderAgain === true).length;
    const responses = entries.filter((e) => typeof e.data.wouldOrderAgain === "boolean").length;
    const prices = entries
      .map((e) => e.data.pricePaidCents)
      .filter((p) => typeof p === "number" && p > 0);

    const update = {
      averageScore: avg,
      totalRatings: count,
      wouldOrderAgainCount: repeats,
      wouldOrderAgainResponses: responses,
      rankingScore: data.status === "approved" ? bayesianScore(avg, count, globalAverage) : 0,
    };
    if (prices.length > 0) {
      update.priceMedianCents = median(prices);
      update.priceReportCount = prices.length;
      update.priceUpdatedAt = Date.now();
    }

    const changed =
      Math.abs((data.averageScore || 0) - update.averageScore) > 1e-9 ||
      (data.totalRatings || 0) !== update.totalRatings ||
      (data.wouldOrderAgainCount || 0) !== update.wouldOrderAgainCount ||
      Math.abs((data.rankingScore || 0) - update.rankingScore) > 1e-9 ||
      prices.length > 0;

    if (changed) {
      plateUpdates.push({
        ref: doc.ref, id: doc.id, update,
        before: { avg: data.averageScore || 0, count: data.totalRatings || 0 },
      });
    }
  }

  console.log(`\n--- plates: ${plateUpdates.length} de ${platesSnap.size} cambian ---`);
  plateUpdates.slice(0, 20).forEach((p) => {
    const u = p.update;
    console.log(
      `    ${p.id}\n` +
      `      nota   ${p.before.avg.toFixed(2)} (${p.before.count} votos) -> ${u.averageScore.toFixed(2)} (${u.totalRatings} votos)\n` +
      `      rank   ${u.rankingScore.toFixed(3)}   repiten ${u.wouldOrderAgainCount}` +
      (u.priceMedianCents ? `   precio ${(u.priceMedianCents / 100).toFixed(2)} € (${u.priceReportCount})` : "")
    );
  });
  if (plateUpdates.length > 20) console.log(`    ... y ${plateUpdates.length - 20} más`);

  // Aviso: descuadres entre lo que decía el plato y lo que suman sus ratings procesados.
  const countMismatch = plateUpdates.filter((p) => p.before.count !== p.update.totalRatings);
  if (countMismatch.length > 0) {
    console.log(`\n⚠️  ${countMismatch.length} plato(s) tenían totalRatings descuadrado respecto a sus ratings procesados:`);
    countMismatch.forEach((p) => console.log(`    ${p.id}: ${p.before.count} -> ${p.update.totalRatings}`));
    console.log("    (esperable: hasta ahora no existía onRatingDeleted, así que los borrados en cascada nunca se restaron)");
  }

  if (!confirmed) {
    console.log("\nSimulación terminada. Nada se ha escrito. Repite con --confirm para aplicar.");
    return;
  }

  // ── 3. Escritura ────────────────────────────────────────────────────────
  let written = 0;
  for (let i = 0; i < ratingUpdates.length; i += 400) {
    const batch = db.batch();
    ratingUpdates.slice(i, i + 400).forEach((r) => {
      batch.update(r.ref, { averageScore: r.newAvg });
      written++;
    });
    await batch.commit();
  }
  for (let i = 0; i < plateUpdates.length; i += 400) {
    const batch = db.batch();
    plateUpdates.slice(i, i + 400).forEach((p) => {
      batch.update(p.ref, p.update);
      written++;
    });
    await batch.commit();
  }

  await db.collection("stats").doc("global").set({
    averageScore: globalAverage,
    plateCount: plateAverages.length,
    updatedAt: Date.now(),
  }, { merge: true });

  console.log(`\n✅ Migración aplicada: ${written} documento(s) actualizados + stats/global.`);
}

main().then(() => process.exit(0)).catch((err) => {
  console.error("Error:", err);
  process.exit(1);
});
