// Recalcula el `rankingScore` de todos los platos con la fórmula vigente.
//
// Hace falta cuando cambia la fórmula: el rankingScore solo se reescribe solo cuando el
// plato recibe un voto, y 47 de 53 platos tienen UN voto y no van a recibir otro. Sin esto
// habría que esperar a que `refreshGlobalStats` corra a las 04:00.
//
// La fórmula tiene que ser la MISMA que la de functions/src/index.ts (rankingScoreFor):
// la nota redondeada a un decimal —lo que pinta la app— más un desempate por número de
// votos que nunca puede saltar a otra nota.
//
// Requiere ADC activo en el shell (refresh_token de firebase-tools.json). Desde
// e:\FoodRanker\functions:
//
//   node scripts/recalcRankingScores.js            (simulación: solo enseña qué cambiaría)
//   node scripts/recalcRankingScores.js --confirm  (escribe de verdad)

const admin = require("firebase-admin");

const RANKING_VOTE_TIEBREAK = 0.001;
const RANKING_VOTE_TIEBREAK_CAP = 40;

function rankingScoreFor(average, count) {
  if (!(count > 0)) return 0;
  const notaVisible = Math.round(average * 10) / 10;
  return notaVisible + Math.min(count, RANKING_VOTE_TIEBREAK_CAP) * RANKING_VOTE_TIEBREAK;
}

const confirmed = process.argv.includes("--confirm");

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  projectId: "foodranker-51270",
});
const db = admin.firestore();

(async () => {
  const snap = await db.collection("plates").get();
  const cambios = [];

  snap.forEach((doc) => {
    const p = doc.data();
    const avg = p.averageScore;
    const n = p.totalRatings;
    if (typeof avg !== "number" || typeof n !== "number") return;
    const nuevo = rankingScoreFor(avg, n);
    const previo = typeof p.rankingScore === "number" ? p.rankingScore : null;
    if (previo !== null && Math.abs(previo - nuevo) < 1e-9) return;
    cambios.push({ ref: doc.ref, nombre: p.name || "?", avg, n, previo, nuevo });
  });

  cambios.sort((a, b) => b.nuevo - a.nuevo);
  console.log(`platos: ${snap.size} | a cambiar: ${cambios.length}`);
  console.log("");
  // La nota se enseña como la pinta la app (un decimal). Con dos decimales confunde:
  // un 9.6499993 se lee "9.65" y parece que debería ordenarse como 9,7 cuando la app
  // muestra 9,6.
  console.log("nota  votos     antes    despues   plato");
  cambios.forEach((c) => {
    console.log(
      String((Math.round(c.avg * 10) / 10).toFixed(1)).padStart(5),
      String(c.n).padStart(5),
      String(c.previo === null ? "-" : c.previo.toFixed(4)).padStart(9),
      String(c.nuevo.toFixed(4)).padStart(9),
      " ",
      c.nombre.slice(0, 36)
    );
  });

  if (!confirmed) {
    console.log("");
    console.log("SIMULACIÓN: no se ha escrito nada. Pasa --confirm para aplicarlo.");
    return;
  }

  let lote = db.batch();
  let enLote = 0;
  for (const c of cambios) {
    lote.update(c.ref, { rankingScore: c.nuevo });
    if (++enLote === 450) {          // 500 es el máximo por lote en Firestore
      await lote.commit();
      lote = db.batch();
      enLote = 0;
    }
  }
  if (enLote > 0) await lote.commit();
  console.log("");
  console.log(`escritos: ${cambios.length}`);
})().catch((e) => {
  console.error("ERROR:", e.message);
  process.exit(1);
});
