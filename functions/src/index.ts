import { onDocumentCreated, onDocumentUpdated, onDocumentDeleted } from "firebase-functions/v2/firestore";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { setGlobalOptions } from "firebase-functions/v2";
import { logger } from "firebase-functions";
import * as admin from "firebase-admin";
import { ImageAnnotatorClient, protos } from "@google-cloud/vision";

admin.initializeApp();

setGlobalOptions({ region: "europe-west1", maxInstances: 10 });

let _visionClient: ImageAnnotatorClient | null = null;
function getVisionClient(): ImageAnnotatorClient {
  if (!_visionClient) {
    _visionClient = new ImageAnnotatorClient();
  }
  return _visionClient;
}

const db = admin.firestore();

// XP amounts — must mirror RewardManager.kt constants
const XP_PLATE_WITH_PHOTO = 50;
const XP_GIVE_RATING = 5;
const XP_RECEIVE_RATING = 10;
const XP_REFERRAL_REFERRER = 100;
const XP_REFERRAL_REFERRED = 50;
const XP_GIVE_COMMENT = 5;

const FAIL_LIKELIHOODS = new Set(["LIKELY", "VERY_LIKELY"]);

const FOOD_KEYWORDS = [
  "food", "dish", "cuisine", "recipe", "ingredient", "meal", "cooking",
  "tableware", "dinnerware", "plate", "bowl",
  "dessert", "bakery", "pastry", "snack", "fast food", "junk food",
  "breakfast", "lunch", "brunch", "dinner", "appetizer", "salad",
  "soup", "stew", "sauce", "dip", "garnish",
  "drink", "beverage", "cocktail", "wine", "beer", "coffee", "tea", "juice",
  "meat", "beef", "pork", "chicken", "lamb", "fish", "seafood", "shellfish",
  "shrimp", "salmon", "tuna",
  "pasta", "noodle", "bread", "pizza", "sandwich", "burger", "hamburger",
  "burrito", "taco", "wrap",
  "sushi", "ramen", "paella", "tapas", "curry", "kebab", "barbecue", "tempura",
  "cake", "pie", "tart", "cookie", "biscuit", "chocolate", "ice cream",
  "pudding", "doughnut",
  "vegetable", "fruit", "rice", "egg", "cheese", "tomato", "potato",
];

interface SafeSearchResult {
  adult: string;
  violence: string;
  racy: string;
  medical: string;
  spoof: string;
}

function isImageProblematic(safeSearch: SafeSearchResult): {
  rejected: boolean;
  reasons: string[];
} {
  const reasons: string[] = [];
  if (FAIL_LIKELIHOODS.has(safeSearch.adult)) reasons.push("adult");
  if (FAIL_LIKELIHOODS.has(safeSearch.violence)) reasons.push("violence");
  if (FAIL_LIKELIHOODS.has(safeSearch.racy)) reasons.push("racy");
  return { rejected: reasons.length > 0, reasons };
}

function looksLikeFood(
  labels: protos.google.cloud.vision.v1.IEntityAnnotation[] | null | undefined,
  minScore: number = 0.5
): { isFood: boolean; matched: string[] } {
  if (!labels || labels.length === 0) return { isFood: false, matched: [] };
  const matched: string[] = [];
  for (const label of labels) {
    const description = (label.description ?? "").toLowerCase();
    const score = label.score ?? 0;
    if (score < minScore) continue;
    for (const keyword of FOOD_KEYWORDS) {
      if (description.includes(keyword)) {
        matched.push(`${label.description}(${score.toFixed(2)})`);
        break;
      }
    }
  }
  return { isFood: matched.length > 0, matched };
}

// ── Helpers ────────────────────────────────────────────────────────────────

function currentWeekKey(): string {
  // ISO 8601 week number — matches Calendar.WEEK_OF_YEAR with ISO locale
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  d.setDate(d.getDate() + 3 - ((d.getDay() + 6) % 7)); // Thursday of current week
  const year = d.getFullYear();
  const jan4 = new Date(year, 0, 4);
  const week = 1 + Math.round(
    ((d.getTime() - jan4.getTime()) / 86400000 - 3 + ((jan4.getDay() + 6) % 7)) / 7
  );
  return `${year}-W${String(week).padStart(2, "0")}`;
}

function normalizeCity(city: string): string {
  return city.trim().toLowerCase();
}

/**
 * Adds xpDelta to the caller's weekly league entry (creating it if needed).
 * Looks up the user's city fresh each time so league placement always
 * reflects the current profile city. No-op if the user has no city set.
 *
 * If sourceRatingRef is given, the city/weekKey actually used are stamped onto
 * that rating doc right after (best-effort, separate write — NOT atomic with the
 * league increment above, deliberately: if the rating was concurrently deleted,
 * the stamp write failing must not roll back the XP already granted) as
 * leagueCity/leagueWeekKey/leagueXpAmount. This lets a future clawback (e.g.
 * if the plate this rating belongs to is later rejected by moderation) reverse
 * the exact league entry/amount, even if the user's city changes meanwhile or
 * the clawback happens to fall on a different ISO week.
 */
async function addLeagueXP(
  userId: string,
  userName: string,
  userPhotoUrl: string,
  xpDelta: number,
  sourceRatingRef?: admin.firestore.DocumentReference
): Promise<void> {
  try {
    const userSnap = await db.collection("users").doc(userId).get();
    const city = normalizeCity((userSnap.get("city") as string) || "");
    if (!city) return;

    const wk = currentWeekKey();
    const leagueEntryRef = db
      .collection("leagues")
      .doc(`${city}_${wk}`)
      .collection("entries")
      .doc(userId);

    // El incremento de liga es el efecto que importa de verdad: se escribe siempre,
    // sin depender de que el rating de origen siga existiendo en este momento.
    await leagueEntryRef.set(
      {
        userId,
        userName,
        userPhotoUrl,
        city,
        weekKey: wk,
        xp: admin.firestore.FieldValue.increment(xpDelta),
        updatedAt: Date.now(),
      },
      { merge: true }
    );

    if (sourceRatingRef) {
      // Best-effort y separado del incremento anterior: si el rating ya no existe
      // (borrado de forma concurrente por deleteRejectedPlate o deleteUserAccount
      // mientras esto corría), no hay nada que estampar ni clawback futuro posible
      // para él — pero eso no debe tirar abajo el XP de liga ya concedido arriba
      // (un único batch atómico haría justo eso si el update() fallase).
      try {
        await sourceRatingRef.update({
          leagueCity: city,
          leagueWeekKey: wk,
          leagueXpAmount: xpDelta,
        });
      } catch (err) {
        logger.warn(`addLeagueXP: no se pudo estampar leagueCity/leagueWeekKey en el rating (probablemente ya borrado):`, err);
      }
    }
  } catch (err) {
    logger.warn(`addLeagueXP failed for ${userId}:`, err);
  }
}

// Umbrales deben coincidir con RewardManager.LEVELS (app/src/main/java/com/app/foodranker/utils/RewardManager.kt).
// El cliente recalcula el nivel a partir de xp para mostrarlo (no lee este campo), pero si los
// umbrales divergen, el campo `level` guardado aquí dejaría de ser coherente con lo que ve el usuario.
function getLevel(xp: number): number {
  if (xp >= 10000) return 6;
  if (xp >= 4000) return 5;
  if (xp >= 1500) return 4;
  if (xp >= 600) return 3;
  if (xp >= 200) return 2;
  return 1;
}

async function awardXP(userId: string, amount: number): Promise<void> {
  if (!userId || userId.startsWith("seed")) return;
  const ref = db.collection("users").doc(userId);
  try {
    await db.runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      if (!snap.exists) return;
      const current = (snap.get("xp") as number) || 0;
      const updated = current + amount;
      tx.update(ref, { xp: updated, level: getLevel(updated) });
    });
    logger.debug(`XP +${amount} → ${userId}`);
  } catch (err) {
    logger.warn(`awardXP failed for ${userId}:`, err);
  }
}

async function checkAndAwardBadges(userId: string): Promise<void> {
  if (!userId || userId.startsWith("seed")) return;
  try {
    const userRef = db.collection("users").doc(userId);
    const userDoc = await userRef.get();
    if (!userDoc.exists) return;

    const current: string[] = (userDoc.get("badges") as string[]) || [];
    const earned = new Set(current);

    // All badge checks run in parallel — none depend on each other
    const [firstPlateSnap, platesSnap, ratingsSnap, top10Snap] = await Promise.all([
      earned.has("first_plate") ? null :
        db.collection("plates").where("addedByUserId", "==", userId).limit(1).get(),
      (earned.has("globetrotter") && earned.has("popular")) ? null :
        db.collection("plates").where("addedByUserId", "==", userId).limit(200).get(),
      earned.has("critic") ? null :
        db.collection("ratings").where("userId", "==", userId).limit(10).get(),
      earned.has("top10") ? null :
        db.collection("plates").orderBy("averageScore", "desc").limit(10).get(),
    ]);

    if (firstPlateSnap && firstPlateSnap.size >= 1) earned.add("first_plate");

    if (platesSnap) {
      if (!earned.has("globetrotter")) {
        const countries = new Set(platesSnap.docs.map((d) => d.get("country") as string).filter(Boolean));
        if (countries.size >= 3) earned.add("globetrotter");
      }
      if (!earned.has("popular")) {
        const totalLikes = platesSnap.docs.reduce((s, d) => s + ((d.get("likes") as number) || 0), 0);
        if (totalLikes >= 50) earned.add("popular");
      }
    }

    if (ratingsSnap && ratingsSnap.size >= 10) earned.add("critic");

    if (top10Snap && top10Snap.docs.some((d) => d.get("addedByUserId") === userId)) earned.add("top10");

    const newBadges = [...earned].filter((b) => !current.includes(b));
    if (newBadges.length > 0) {
      await userRef.update({ badges: admin.firestore.FieldValue.arrayUnion(...newBadges) });
      logger.info(`New badges for ${userId}: ${newBadges.join(", ")}`);
    }
  } catch (err) {
    logger.warn(`checkAndAwardBadges failed for ${userId}:`, err);
  }
}

// ── moderatePlateImage ─────────────────────────────────────────────────────

/**
 * Fires on new plate creation.
 * - Runs Vision SafeSearch + Label Detection.
 * - On rejection: deletes plate + initial rating, notifies author.
 * - On approval: sets status="approved", updates averageScore from the
 *   author's initial rating, awards XP, checks badges.
 */
export const moderatePlateImage = onDocumentCreated(
  "plates/{plateId}",
  async (event) => {
    const snap = event.data;
    if (!snap) {
      logger.warn("No snapshot in event");
      return;
    }

    const plate = snap.data();
    const plateId = event.params.plateId;
    const imageUrl: string | undefined = plate.imageUrl;
    const authorId: string | undefined = plate.addedByUserId;
    const plateName: string = plate.name ?? "tu plato";

    if (!imageUrl) {
      logger.warn(`Plate ${plateId} has no imageUrl, rejecting`);
      await deleteRejectedPlate(snap.ref, authorId, plateId, plateName, ["no_image"]);
      return;
    }

    try {
      const [result] = await getVisionClient().annotateImage({
        image: { source: { imageUri: imageUrl } },
        features: [
          { type: "SAFE_SEARCH_DETECTION" },
          { type: "LABEL_DETECTION", maxResults: 15 },
        ],
      });

      const safeSearch = result.safeSearchAnnotation as SafeSearchResult | null;
      const labels = result.labelAnnotations;

      if (safeSearch) {
        const inappropriate = isImageProblematic(safeSearch);
        if (inappropriate.rejected) {
          logger.info(`Plate ${plateId} rejected (inappropriate): ${inappropriate.reasons.join(", ")}`);
          await deleteRejectedPlate(snap.ref, authorId, plateId, plateName, inappropriate.reasons);
          return;
        }
      }

      const { isFood, matched } = looksLikeFood(labels);
      if (!isFood) {
        const topLabels = (labels ?? [])
          .slice(0, 5)
          .map((l) => `${l.description}(${(l.score ?? 0).toFixed(2)})`)
          .join(", ");
        logger.info(`Plate ${plateId} rejected (not food). Top labels: ${topLabels}`);
        await deleteRejectedPlate(snap.ref, authorId, plateId, plateName, ["not_food"]);
        return;
      }

      logger.info(`Plate ${plateId} approved. Match: ${matched.join(", ")}`);
      await approveplate(snap.ref, plateId, authorId);
    } catch (err) {
      logger.error(`Error moderating ${plateId}, auto-approving:`, err);
      await approveplate(snap.ref, plateId, authorId);
    }
  }
);

async function approveplate(
  plateRef: admin.firestore.DocumentReference,
  plateId: string,
  authorId: string | undefined
): Promise<void> {
  if (!authorId) {
    await plateRef.update({ status: "approved" });
    return;
  }

  const initialRatingRef = db.collection("ratings").doc(`${plateId}_${authorId}`);

  // Update plate status + apply initial rating score atomically.
  // Returns the rating author info only when this invocation actually set
  // processed=true, so XP/league are awarded exactly once even on retries.
  let awarded: { userName: string; userPhotoUrl: string } | null = null;
  try {
    awarded = await db.runTransaction(async (tx) => {
      const ratingSnap = await tx.get(initialRatingRef);

      if (!ratingSnap.exists || ratingSnap.get("processed") === true) {
        // Idempotent: mark approved if not already set, but skip XP.
        const plateSnap = await tx.get(plateRef);
        if (plateSnap.get("status") !== "approved") {
          tx.update(plateRef, { status: "approved" });
        }
        return null;
      }

      const fl = (ratingSnap.get("flavorScore") as number) || 0;
      const pr = (ratingSnap.get("presentationScore") as number) || 0;
      const vl = (ratingSnap.get("valueScore") as number) || 0;
      const ratingAvg = (fl + pr + vl) / 3;
      tx.update(plateRef, {
        status: "approved",
        averageScore: ratingAvg,
        totalRatings: 1,
      });
      tx.update(initialRatingRef, { processed: true });
      return {
        userName: (ratingSnap.get("userName") as string) ?? "Usuario",
        userPhotoUrl: (ratingSnap.get("userPhotoUrl") as string) ?? "",
      };
    });
  } catch (err) {
    logger.error(`Error approving plate ${plateId}:`, err);
    return;
  }

  // Award XP + league points only when this invocation performed the
  // first-time approval. This is also the user's only rating activity for
  // their own plate, so it must count towards the weekly league — otherwise
  // a user who only ever rates their own plates never appears in the league.
  if (awarded) {
    const xpEarned = XP_PLATE_WITH_PHOTO + XP_GIVE_RATING;
    // Las 3 operaciones tocan documentos independientes (users/{authorId}.xp,
    // users/{authorId}.badges, leagues/{id}/entries/{authorId}) y cada una ya
    // tiene su propio try/catch interno — no hay dependencia de orden entre ellas.
    await Promise.all([
      awardXP(authorId, xpEarned),
      checkAndAwardBadges(authorId),
      addLeagueXP(authorId, awarded.userName, awarded.userPhotoUrl, xpEarned),
    ]);
  }
}

async function deleteRejectedPlate(
  plateRef: admin.firestore.DocumentReference,
  authorId: string | undefined,
  plateId: string,
  plateName: string,
  reasons: string[]
) {
  // Idempotent against retries of the whole moderatePlateImage invocation: if the
  // plate doc is already gone, this rejection (clawback included) was already
  // processed — nothing left to do.
  const stillExists = (await plateRef.get()).exists;
  if (!stillExists) return;

  // Claw back league XP from anyone who voted on this plate while it was still
  // pending (see addLeagueXP/onRatingCreated) — the plate turned out to be
  // invalid, so those votes shouldn't have counted. Read+decrement BEFORE
  // deleting anything below, so this can't race with onPlateDeleted's async
  // cascade delete of the very rating docs read here.
  try {
    const ratingsSnap = await db.collection("ratings").where("plateId", "==", plateId).get();
    const clawbackBatch = db.batch();
    let hasClawback = false;
    for (const doc of ratingsSnap.docs) {
      const r = doc.data();
      if (r.userId === authorId) continue;
      const leagueCity = r.leagueCity as string | undefined;
      const leagueWeekKey = r.leagueWeekKey as string | undefined;
      const leagueXpAmount = r.leagueXpAmount as number | undefined;
      if (!leagueCity || !leagueWeekKey || !leagueXpAmount) continue;
      const leagueEntryRef = db
        .collection("leagues")
        .doc(`${leagueCity}_${leagueWeekKey}`)
        .collection("entries")
        .doc(r.userId as string);
      clawbackBatch.set(
        leagueEntryRef,
        { xp: admin.firestore.FieldValue.increment(-leagueXpAmount), updatedAt: Date.now() },
        { merge: true }
      );
      hasClawback = true;
    }
    if (hasClawback) await clawbackBatch.commit();
  } catch (err) {
    logger.warn(`League XP clawback failed for rejected plate ${plateId}:`, err);
  }

  if (authorId) {
    const initialRatingRef = db.collection("ratings").doc(`${plateId}_${authorId}`);
    const batch = db.batch();
    batch.delete(initialRatingRef);
    batch.delete(plateRef);
    await batch.commit();

    const notifRef = db
      .collection("notifications")
      .doc(authorId)
      .collection("items")
      .doc();
    await notifRef.set({
      id: notifRef.id,
      type: "moderation_rejected",
      plateId,
      plateName,
      reasons,
      isRead: false,
      createdAt: Date.now(),
    });
  } else {
    await plateRef.delete();
  }
}

// ── onRatingCreated ────────────────────────────────────────────────────────

/**
 * Fires on every new rating.
 * - Skips the author's own initial rating (handled by moderatePlateImage).
 * - Idempotent: checks + sets rating.processed inside a transaction.
 * - Updates plate averageScore + totalRatings.
 * - Awards XP to rater and plate owner.
 * - Sends "rating" notification to plate owner.
 * - Checks badges for rater.
 */
export const onRatingCreated = onDocumentCreated(
  "ratings/{ratingId}",
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const rating = snap.data();
    const ratingId = event.params.ratingId;

    if (rating.processed === true) return;

    const plateId: string = rating.plateId;
    const raterId: string = rating.userId;
    // Recompute average server-side — never trust client-supplied averageScore
    const flavorScore: number = rating.flavorScore ?? 0;
    const presentationScore: number = rating.presentationScore ?? 0;
    const valueScore: number = rating.valueScore ?? 0;
    const avgScore: number = (flavorScore + presentationScore + valueScore) / 3;

    if (!plateId || !raterId) {
      logger.warn(`Rating ${ratingId}: missing plateId or userId`);
      return;
    }

    const plateRef = db.collection("plates").doc(plateId);
    const plateSnap = await plateRef.get();
    if (!plateSnap.exists) {
      logger.warn(`Rating ${ratingId}: plate ${plateId} not found`);
      return;
    }
    const plateData = plateSnap.data()!;
    const ownerId: string = plateData.addedByUserId ?? "";

    // Author's own initial rating is processed by moderatePlateImage
    if (raterId === ownerId) {
      logger.info(`Rating ${ratingId}: author's own rating, skipping`);
      return;
    }

    // League XP reflects voting activity regardless of moderation status —
    // otherwise votes cast while the plate is still pending are silently lost.
    // Pass snap.ref so the league city/weekKey actually used get stamped onto this
    // rating doc — needed for clawback if the plate is later rejected (see deleteRejectedPlate).
    await addLeagueXP(raterId, rating.userName ?? "Usuario", rating.userPhotoUrl ?? "", XP_GIVE_RATING, snap.ref);

    // Plate score, rater/owner XP, notifications and badges only apply once approved
    if (plateData.status !== "approved") {
      logger.info(`Rating ${ratingId}: plate ${plateId} is not approved (status: ${plateData.status}), skipping`);
      return;
    }

    // Update plate score + mark processed (idempotent)
    let processed = false;
    try {
      await db.runTransaction(async (tx) => {
        const ratingDoc = await tx.get(snap.ref);
        if (ratingDoc.get("processed") === true) return;

        const plateDoc = await tx.get(plateRef);
        const oldAvg = (plateDoc.get("averageScore") as number) || 0;
        const oldCount = (plateDoc.get("totalRatings") as number) || 0;
        const count = oldCount + 1;
        const avg = (oldAvg * oldCount + avgScore) / count;

        tx.update(plateRef, { averageScore: avg, totalRatings: count });
        tx.update(snap.ref, { processed: true });
        processed = true;
      });
    } catch (err) {
      logger.error(`Rating ${ratingId}: error updating plate score:`, err);
      return;
    }

    if (!processed) {
      logger.info(`Rating ${ratingId}: already processed, skipping`);
      return;
    }

    // Award XP
    const xpTasks: Promise<void>[] = [awardXP(raterId, XP_GIVE_RATING)];
    if (ownerId && ownerId !== raterId) {
      xpTasks.push(awardXP(ownerId, XP_RECEIVE_RATING));
    }
    await Promise.allSettled(xpTasks);

    // Notify plate owner
    if (ownerId && ownerId !== raterId) {
      try {
        const notifRef = db.collection("notifications").doc(ownerId).collection("items").doc();
        await notifRef.set({
          id: notifRef.id,
          type: "rating",
          fromUserId: raterId,
          fromUserName: rating.userName ?? "Alguien",
          plateId,
          plateName: plateData.name ?? "",
          score: avgScore,
          isRead: false,
          createdAt: Date.now(),
        });
      } catch (err) {
        logger.warn(`Rating ${ratingId}: error sending notification:`, err);
      }
    }

    // Check badges for rater
    await checkAndAwardBadges(raterId);
  }
);

// ── onNotificationCreated ─────────────────────────────────────────────────

/**
 * Fires when a notification item is created in notifications/{userId}/items/{itemId}.
 * Sends a real FCM push to the recipient's device.
 * Cleans up stale tokens automatically.
 */
export const onNotificationCreated = onDocumentCreated(
  "notifications/{userId}/items/{itemId}",
  async (event) => {
    const userId = event.params.userId;
    const notif = event.data?.data();
    if (!notif) return;

    const userSnap = await db.collection("users").doc(userId).get();
    const fcmToken = userSnap.get("fcmToken") as string | undefined;
    if (!fcmToken) return;

    const type: string = notif.type ?? "";
    const plateName: string = notif.plateName ?? "tu plato";
    const fromUserName: string = notif.fromUserName ?? "Alguien";

    let title: string;
    let body: string;
    let channelId = "foodranker_social";

    switch (type) {
      case "like":
        title = "❤️ Nuevo me gusta";
        body = `${fromUserName} le ha dado like a "${plateName}"`;
        break;
      case "rating": {
        const score: number = notif.score ?? 0;
        title = "⭐ Nueva valoración";
        body = `${fromUserName} ha valorado "${plateName}" con ${score.toFixed(1)}`;
        break;
      }
      case "moderation_rejected":
        title = "Plato no aprobado";
        body = `"${plateName}" no cumple las normas de la comunidad.`;
        channelId = "foodranker_moderation";
        break;
      default:
        return;
    }

    try {
      await admin.messaging().send({
        token: fcmToken,
        notification: { title, body },
        data: {
          plateId: (notif.plateId as string) ?? "",
          type,
          notifId: event.params.itemId,
        },
        android: {
          notification: { channelId, priority: "high" },
        },
        apns: {
          payload: { aps: { sound: "default", badge: 1 } },
        },
      });
    } catch (err: any) {
      if (err.code === "messaging/registration-token-not-registered" ||
          err.code === "messaging/invalid-registration-token") {
        await db.collection("users").doc(userId).update({
          fcmToken: admin.firestore.FieldValue.delete(),
        });
        logger.info(`Stale FCM token removed for user ${userId}`);
      } else {
        logger.warn(`FCM send failed for ${userId}:`, err);
      }
    }
  }
);

// ── onRatingUpdated ───────────────────────────────────────────────────────

/**
 * Fires when a rating is updated (edit flow).
 * Recalculates the plate's averageScore by replacing the old rating value
 * with the new one: newAvg = (oldAvg * n - oldRating + newRating) / n.
 */
export const onRatingUpdated = onDocumentUpdated(
  "ratings/{ratingId}",
  async (event) => {
    const before = event.data?.before.data();
    const after  = event.data?.after.data();
    if (!before || !after) return;

    // Only process when scores actually changed
    const scoreFields = ["flavorScore", "presentationScore", "valueScore"];
    const changed = scoreFields.some((f) => before[f] !== after[f]);
    if (!changed) return;

    const plateId: string = after.plateId;
    const oldAvg = ((before.flavorScore ?? 0) + (before.presentationScore ?? 0) + (before.valueScore ?? 0)) / 3;
    const newAvg = ((after.flavorScore ?? 0) + (after.presentationScore ?? 0) + (after.valueScore ?? 0)) / 3;

    const plateRef = db.collection("plates").doc(plateId);
    try {
      await db.runTransaction(async (tx) => {
        const plateDoc = await tx.get(plateRef);
        if (!plateDoc.exists) return;
        const totalRatings: number = plateDoc.get("totalRatings") ?? 0;
        if (totalRatings === 0) {
          // Initial rating hasn't been folded into plate.averageScore yet
          // (approveplate hasn't run) — it will read the latest scores
          // directly from this rating doc once it does. Just refresh the
          // rating's own averageScore for display; recalculating against
          // the plate here would divide by zero.
          tx.update(event.data!.after.ref, { averageScore: newAvg });
          return;
        }
        const currentAvg: number = plateDoc.get("averageScore") ?? 0;
        const recalculated = (currentAvg * totalRatings - oldAvg + newAvg) / totalRatings;
        tx.update(plateRef, { averageScore: recalculated });
        tx.update(event.data!.after.ref, { averageScore: newAvg });
      });
    } catch (err) {
      logger.error(`onRatingUpdated error for rating ${event.params.ratingId}:`, err);
    }
  }
);

// ── onReferralCreated ──────────────────────────────────────────────────────

/**
 * Fires when a referral document is created by the client.
 * Awards XP to both parties and increments referralCount on the referrer.
 * The client no longer increments referralCount directly (security fix).
 */
export const onReferralCreated = onDocumentCreated(
  "referrals/{referralId}",
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const referral = snap.data();
    const referrerId: string = referral.referrerId;
    const referredId: string = referral.referredId;

    if (!referrerId || !referredId) {
      logger.warn(`Referral ${event.params.referralId}: missing referrerId or referredId`);
      return;
    }

    try {
      // Idempotent: mark the referral processed inside a transaction so
      // retries of this trigger don't double-count referralCount/XP.
      const shouldProcess = await db.runTransaction(async (tx) => {
        const referralSnap = await tx.get(snap.ref);
        if (referralSnap.get("processed") === true) return false;
        tx.update(snap.ref, { processed: true });
        tx.update(db.collection("users").doc(referrerId), {
          referralCount: admin.firestore.FieldValue.increment(1),
        });
        return true;
      });

      if (!shouldProcess) {
        logger.info(`Referral ${event.params.referralId} already processed, skipping`);
        return;
      }

      await Promise.all([
        awardXP(referrerId, XP_REFERRAL_REFERRER),
        awardXP(referredId, XP_REFERRAL_REFERRED),
      ]);
      logger.info(`Referral processed: ${referrerId} → ${referredId}`);
    } catch (err) {
      logger.error(`onReferralCreated error for ${event.params.referralId}:`, err);
    }
  }
);

// ── onCommentCreated ────────────────────────────────────────────────────────

/**
 * Fires when a user posts a comment. Awards XP to the commenter.
 * xp/level are Admin-SDK-only fields (see firestore.rules), so this can't
 * be done client-side — it has to happen here.
 */
export const onCommentCreated = onDocumentCreated(
  "comments/{commentId}",
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const comment = snap.data();
    const userId: string = comment.userId;
    if (!userId) {
      logger.warn(`Comment ${event.params.commentId}: missing userId`);
      return;
    }

    try {
      // Idempotent: mark the comment processed inside a transaction so
      // retries of this trigger don't double-award XP.
      const shouldProcess = await db.runTransaction(async (tx) => {
        const commentSnap = await tx.get(snap.ref);
        if (commentSnap.get("processed") === true) return false;
        tx.update(snap.ref, { processed: true });
        return true;
      });

      if (!shouldProcess) {
        logger.info(`Comment ${event.params.commentId} already processed, skipping`);
        return;
      }

      await awardXP(userId, XP_GIVE_COMMENT);
    } catch (err) {
      logger.error(`onCommentCreated error for ${event.params.commentId}:`, err);
    }
  }
);

// ── onChallengeUpdated ──────────────────────────────────────────────────────

/**
 * Fires when a challenge document is updated. Awards xpReward XP to any
 * newly added participant. firestore.rules only allows a client update to
 * add exactly one uid to participantIds, but this handles any number of
 * new participants defensively.
 */
export const onChallengeUpdated = onDocumentUpdated(
  "challenges/{challengeId}",
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (!before || !after) return;

    const beforeIds: string[] = before.participantIds ?? [];
    const afterIds: string[] = after.participantIds ?? [];
    const newParticipants = afterIds.filter((id) => !beforeIds.includes(id));
    if (newParticipants.length === 0) return;

    const xpReward: number = after.xpReward ?? 0;
    const challengeId = event.params.challengeId;

    await Promise.all(
      newParticipants.map(async (userId) => {
        try {
          // Idempotency marker per participant, in case the trigger retries.
          const markerRef = db
            .collection("challenges")
            .doc(challengeId)
            .collection("awardedXp")
            .doc(userId);
          const shouldAward = await db.runTransaction(async (tx) => {
            const markerSnap = await tx.get(markerRef);
            if (markerSnap.exists) return false;
            tx.set(markerRef, { awardedAt: Date.now() });
            return true;
          });

          if (shouldAward && xpReward > 0) {
            await awardXP(userId, xpReward);
          }
        } catch (err) {
          logger.warn(`onChallengeUpdated: failed to award XP to ${userId} for challenge ${challengeId}:`, err);
        }
      })
    );
  }
);

// ── awardAdXp ─────────────────────────────────────────────────────────────

const XP_AD_REWARD = 50;
const AD_REWARD_COOLDOWN_MS = 24 * 60 * 60 * 1000; // 1 día entre recompensas

/**
 * Callable function invocada cuando el usuario completa un anuncio recompensado.
 * Otorga 50 XP con un cooldown de 24h para evitar abuso.
 */
export const awardAdXp = onCall(
  { region: "europe-west1" },
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) throw new HttpsError("unauthenticated", "Must be authenticated");

    const userRef = db.collection("users").doc(uid);
    try {
      const granted = await db.runTransaction(async (tx) => {
        const snap = await tx.get(userRef);
        if (!snap.exists) return false;
        const lastAdReward = (snap.get("lastAdRewardAt") as number) ?? 0;
        if (Date.now() - lastAdReward < AD_REWARD_COOLDOWN_MS) return false;
        const xp = ((snap.get("xp") as number) ?? 0) + XP_AD_REWARD;
        tx.update(userRef, { xp, level: getLevel(xp), lastAdRewardAt: Date.now() });
        return true;
      });
      return { granted, xp: XP_AD_REWARD };
    } catch (err) {
      logger.error(`awardAdXp error for ${uid}:`, err);
      throw new HttpsError("internal", "Failed to award XP");
    }
  }
);

// ── getLeagueId ──────────────────────────────────────────────────────────

/**
 * Callable function that returns the canonical leagues/{leagueId} document
 * id for a given city, computed the same way addLeagueXP computes it
 * server-side. The client calls this instead of reimplementing
 * normalizeCity()/currentWeekKey() locally, so there's a single source of
 * truth for the id and the two sides can never drift apart.
 */
export const getLeagueId = onCall(
  { region: "europe-west1" },
  async (request) => {
    if (!request.auth?.uid) throw new HttpsError("unauthenticated", "Must be authenticated");

    const rawCity = (request.data?.city as string) ?? "";
    const city = normalizeCity(rawCity);
    if (!city) throw new HttpsError("invalid-argument", "city is required");

    const weekKey = currentWeekKey();
    return { leagueId: `${city}_${weekKey}`, city, weekKey };
  }
);

// ── onPlateDeleted ────────────────────────────────────────────────────────

/**
 * Deletes every doc matched by `query`, paginating in batches of 500
 * (Firestore's batch-write limit) and recursing until the query is exhausted.
 */
async function deleteQueryBatch(query: admin.firestore.Query): Promise<void> {
  const snap = await query.limit(500).get();
  if (snap.empty) return;
  const batch = db.batch();
  snap.docs.forEach((doc) => batch.delete(doc.ref));
  await batch.commit();
  if (snap.size >= 500) await deleteQueryBatch(query);
}

/**
 * Fires when a plate document is deleted by the owner (client-side, via rules).
 * Cascades deletion of all ratings and comments for that plate so they don't
 * remain as orphaned documents. The Admin SDK bypasses the
 * "allow delete: if false" rule on ratings.
 */
export const onPlateDeleted = onDocumentDeleted(
  "plates/{plateId}",
  async (event) => {
    const plateId = event.params.plateId;

    try {
      await Promise.all([
        deleteQueryBatch(db.collection("ratings").where("plateId", "==", plateId)),
        deleteQueryBatch(db.collection("comments").where("plateId", "==", plateId)),
        deleteQueryBatch(db.collection("saves").where("plateId", "==", plateId)),
      ]);
      logger.info(`Cascade delete complete for plate ${plateId}`);
    } catch (err) {
      logger.error(`onPlateDeleted error for ${plateId}:`, err);
    }
  }
);

/**
 * deleteUserAccount — callable
 * Deletes all Firestore data for the calling user (including ratings, which
 * have allow delete: if false in rules) and removes the Firebase Auth account.
 * Required by Google Play for account deletion compliance.
 */
export const deleteUserAccount = onCall(
  { region: "europe-west1" },
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) throw new HttpsError("unauthenticated", "Must be authenticated");

    // Pasos 1-5 son limpieza de datos asociados: cada uno corre best-effort (loguea y
    // sigue) para que un fallo puntual en una colección no impida borrar el perfil y
    // la cuenta de Auth (pasos 6-7, los críticos para el derecho al olvido). El peor
    // caso pasa de "cuenta a medio borrar que sigue activa" a "algún dato huérfano de
    // bajo impacto" — ya documentado como limitación conocida de Firestore.
    const step = async (label: string, fn: () => Promise<void>) => {
      try {
        await fn();
      } catch (err) {
        logger.warn(`deleteUserAccount step "${label}" failed for ${uid}:`, err);
      }
    };

    // 1. Ratings by user
    await step("ratings by user", () => deleteQueryBatch(db.collection("ratings").where("userId", "==", uid)));

    // 2. User's plates — also cascade delete their ratings and comments
    await step("user's plates", async () => {
      const platesSnap = await db.collection("plates").where("addedByUserId", "==", uid).get();
      for (const plateDoc of platesSnap.docs) {
        await deleteQueryBatch(db.collection("ratings").where("plateId", "==", plateDoc.id));
        await deleteQueryBatch(db.collection("comments").where("plateId", "==", plateDoc.id));
        await plateDoc.ref.delete();
      }
    });

    // 3. Comments, follows, saves, collections, reports
    await step("comments", () => deleteQueryBatch(db.collection("comments").where("userId", "==", uid)));
    await step("follows (follower)", () => deleteQueryBatch(db.collection("follows").where("followerId", "==", uid)));
    await step("follows (following)", () => deleteQueryBatch(db.collection("follows").where("followingId", "==", uid)));
    await step("saves", () => deleteQueryBatch(db.collection("saves").where("userId", "==", uid)));
    await step("collections", () => deleteQueryBatch(db.collection("collections").where("userId", "==", uid)));
    await step("reports", () => deleteQueryBatch(db.collection("reports").where("reportedByUserId", "==", uid)));

    // 4. League entries across all weeks (collection group query)
    await step("league entries", async () => {
      const leagueEntries = await db.collectionGroup("entries").where("userId", "==", uid).get();
      if (!leagueEntries.empty) {
        const lb = db.batch();
        leagueEntries.docs.forEach((doc) => lb.delete(doc.ref));
        await lb.commit();
      }
    });

    // 5. Notifications subcollection + parent document
    await step("notifications", async () => {
      const notifItems = await db.collection("notifications").doc(uid).collection("items").get();
      if (!notifItems.empty) {
        const nb = db.batch();
        notifItems.docs.forEach((doc) => nb.delete(doc.ref));
        await nb.commit();
      }
      await db.collection("notifications").doc(uid).delete();
    });

    try {
      // 6. User document
      await db.collection("users").doc(uid).delete();

      // 7. Delete Firebase Auth account (Admin SDK bypasses rules)
      await admin.auth().deleteUser(uid);

      logger.info(`Account deleted: ${uid}`);
      return { success: true };
    } catch (err) {
      logger.error(`deleteUserAccount error for ${uid}:`, err);
      throw new HttpsError("internal", "Failed to delete account");
    }
  }
);
