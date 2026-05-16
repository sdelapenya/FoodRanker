import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { setGlobalOptions } from "firebase-functions/v2";
import { logger } from "firebase-functions";
import * as admin from "firebase-admin";
import { ImageAnnotatorClient, protos } from "@google-cloud/vision";

admin.initializeApp();

// Región europea y memoria mínima razonable para llamadas a Vision.
setGlobalOptions({ region: "europe-west1", maxInstances: 10 });

// Lazy-init del cliente Vision para evitar timeouts durante el análisis del
// código en deploy (Firebase abre el módulo y espera a que se determinen las
// funciones; si tardamos demasiado, falla con "Cannot determine backend spec").
let _visionClient: ImageAnnotatorClient | null = null;
function getVisionClient(): ImageAnnotatorClient {
  if (!_visionClient) {
    _visionClient = new ImageAnnotatorClient();
  }
  return _visionClient;
}

const db = admin.firestore();

// Likelihood de Vision: UNKNOWN | VERY_UNLIKELY | UNLIKELY | POSSIBLE | LIKELY | VERY_LIKELY
const FAIL_LIKELIHOODS = new Set(["LIKELY", "VERY_LIKELY"]);

// Palabras clave para detectar si la imagen es de comida. Las etiquetas de
// Vision Label Detection son muy amplias: incluyen nombres genéricos ("Food",
// "Dish", "Cuisine", "Recipe", "Meal"), categorías ("Dessert", "Bakery",
// "Seafood"), y platos específicos ("Pizza", "Sushi", "Paella", "Ramen").
// Comprobamos en minúsculas con un "contains" para máxima cobertura.
const FOOD_KEYWORDS = [
  // Genéricos
  "food",
  "dish",
  "cuisine",
  "recipe",
  "ingredient",
  "meal",
  "cooking",
  "tableware",
  "dinnerware",
  "plate",
  "bowl",
  // Categorías
  "dessert",
  "bakery",
  "pastry",
  "snack",
  "fast food",
  "junk food",
  "breakfast",
  "lunch",
  "brunch",
  "dinner",
  "appetizer",
  "salad",
  "soup",
  "stew",
  "sauce",
  "dip",
  "garnish",
  // Bebidas
  "drink",
  "beverage",
  "cocktail",
  "wine",
  "beer",
  "coffee",
  "tea",
  "juice",
  // Carnes y pescados
  "meat",
  "beef",
  "pork",
  "chicken",
  "lamb",
  "fish",
  "seafood",
  "shellfish",
  "shrimp",
  "salmon",
  "tuna",
  // Pastas y panes
  "pasta",
  "noodle",
  "bread",
  "pizza",
  "sandwich",
  "burger",
  "hamburger",
  "burrito",
  "taco",
  "wrap",
  // Cocinas concretas
  "sushi",
  "ramen",
  "paella",
  "tapas",
  "curry",
  "kebab",
  "barbecue",
  "tempura",
  // Postres
  "cake",
  "pie",
  "tart",
  "cookie",
  "biscuit",
  "chocolate",
  "ice cream",
  "pudding",
  "doughnut",
  // Vegetales y frutas
  "vegetable",
  "fruit",
  "rice",
  "egg",
  "cheese",
  "tomato",
  "potato",
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
  if (!labels || labels.length === 0) {
    return { isFood: false, matched: [] };
  }
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

/**
 * Se dispara al crear un nuevo plato. Llama a Vision API con dos features en
 * una misma petición:
 *   - SafeSearch  → detecta contenido inapropiado (adulto/violencia/sugerente)
 *   - Labels      → detecta de qué es la imagen para comprobar que es comida
 *
 * Si la imagen NO es apta:
 *   - Borra el plato.
 *   - Borra el rating inicial que el autor creó al subirlo.
 *   - Envía notificación al autor con el motivo.
 *
 * Si Vision falla, dejamos el plato (no penalizamos al usuario por un fallo
 * del servicio) y registramos el error en logs.
 */
export const moderatePlateImage = onDocumentCreated(
  "plates/{plateId}",
  async (event) => {
    const snap = event.data;
    if (!snap) {
      logger.warn("Sin snapshot en el evento");
      return;
    }

    const plate = snap.data();
    const plateId = event.params.plateId;
    const imageUrl: string | undefined = plate.imageUrl;
    const authorId: string | undefined = plate.addedByUserId;
    const plateName: string = plate.name ?? "tu plato";

    if (!imageUrl) {
      logger.warn(`Plato ${plateId} sin imageUrl, lo borramos`);
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

      // 1) Comprobación de contenido inapropiado
      if (safeSearch) {
        const inappropriate = isImageProblematic(safeSearch);
        if (inappropriate.rejected) {
          logger.info(
            `Plato ${plateId} rechazado por contenido inapropiado: ${inappropriate.reasons.join(", ")}`
          );
          await deleteRejectedPlate(snap.ref, authorId, plateId, plateName, inappropriate.reasons);
          return;
        }
      }

      // 2) Comprobación de "es comida"
      const { isFood, matched } = looksLikeFood(labels);
      if (!isFood) {
        const topLabels = (labels ?? [])
          .slice(0, 5)
          .map((l) => `${l.description}(${(l.score ?? 0).toFixed(2)})`)
          .join(", ");
        logger.info(
          `Plato ${plateId} rechazado: no parece comida. Top labels: ${topLabels}`
        );
        await deleteRejectedPlate(snap.ref, authorId, plateId, plateName, ["not_food"]);
        return;
      }

      logger.info(`Plato ${plateId} aprobado. Match: ${matched.join(", ")}`);
    } catch (err) {
      logger.error(`Error moderando ${plateId}, dejamos el plato:`, err);
    }
  }
);

/**
 * Borra el plato rechazado y todos sus datos asociados, y notifica al autor.
 * - Borra el documento del plato.
 * - Borra el rating inicial que el autor creó al subir el plato.
 * - Envía una notificación al autor con el motivo.
 * La imagen en Cloudinary queda huérfana (acceptable para MVP).
 */
async function deleteRejectedPlate(
  plateRef: admin.firestore.DocumentReference,
  authorId: string | undefined,
  plateId: string,
  plateName: string,
  reasons: string[]
) {
  if (authorId) {
    const ratings = await db
      .collection("ratings")
      .where("plateId", "==", plateId)
      .where("userId", "==", authorId)
      .get();
    const batch = db.batch();
    ratings.docs.forEach((doc) => batch.delete(doc.ref));
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
