#!/usr/bin/env node
/**
 * Crea o actualiza la cuenta de revisión de Google Play (correo + contraseña).
 *
 * ## Por qué existe
 *
 * La revisión de Play exige credenciales **reutilizables y que se salten los desafíos de
 * identidad**. Una cuenta de Google no puede serlo: su motor de riesgo bloquea al revisor
 * por entrar desde sitios cambiantes, y tumbó tres cuentas de revisión seguidas (agosto y
 * noviembre de 2026 — ver docs/HANDOFF.md). Una credencial de correo/contraseña de
 * Firebase no pasa por Google: no hay desafío, ni aviso al móvil de nadie, ni caducidad.
 *
 * ## Uso
 *
 *     REVIEWER_PASSWORD='...' node scripts/reviewerAccount.js <correo> [--name "..."] [--confirm]
 *
 * Sin `--confirm` solo dice lo que haría. La contraseña se lee de la variable de entorno
 * para que no quede en el historial del shell; `--password` existe como último recurso.
 *
 * Requiere ADC (igual que manageUser.js):
 *
 *     GOOGLE_APPLICATION_CREDENTIALS=adc_tmp.json node scripts/reviewerAccount.js ...
 *
 * ⚠️ Esto NO da Premium. La declaración de Play marca "acceso completo a todo el
 * contenido, incluido el de pago", así que después hay que pasar por:
 *
 *     node scripts/manageUser.js grant-premium <uid> --confirm
 *
 * ⚠️ Si el correo ya tiene cuenta de Google en este proyecto, Firebase trae activado
 * "una cuenta por dirección": este script le AÑADE la contraseña al mismo usuario, no
 * crea otro. Es lo que se quiere — conserva uid, XP y lo que tuviera.
 */
const admin = require("firebase-admin");

const args = process.argv.slice(2);
const correo = args.find((a) => !a.startsWith("--"));
const confirm = args.includes("--confirm");

function opcion(nombre) {
  const i = args.indexOf(nombre);
  return i >= 0 ? args[i + 1] : undefined;
}

const password = process.env.REVIEWER_PASSWORD || opcion("--password");
const displayName = opcion("--name") || "Play reviewer";

if (!correo || !password) {
  console.error(
    "Uso: REVIEWER_PASSWORD='...' node scripts/reviewerAccount.js <correo> [--name \"...\"] [--confirm]"
  );
  process.exit(1);
}

if (password.length < 8) {
  // El cliente exige 8; crear aquí una más corta dejaría una cuenta que el propio
  // formulario de la app se negaría a aceptar.
  console.error("La contraseña debe tener al menos 8 caracteres (es lo que pide la app).");
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  projectId: "foodranker-51270",
});

(async () => {
  let usuario = null;
  try {
    usuario = await admin.auth().getUserByEmail(correo);
  } catch (e) {
    if (e.code !== "auth/user-not-found") throw e;
  }

  if (usuario) {
    const proveedores = usuario.providerData.map((p) => p.providerId);
    console.log(`La cuenta ya existe: uid ${usuario.uid}`);
    console.log(`  Proveedores actuales: ${proveedores.join(", ") || "(ninguno)"}`);
    console.log(`  Deshabilitada: ${usuario.disabled}`);
    console.log(
      proveedores.includes("password")
        ? "  Acción: cambiar la contraseña existente."
        : "  Acción: añadir contraseña a este mismo usuario (conserva uid, XP y progreso)."
    );
  } else {
    console.log(`No existe ninguna cuenta con ${correo}.`);
    console.log(`  Acción: crearla con nombre "${displayName}".`);
  }

  if (!confirm) {
    console.log("\nSimulación (no se ha tocado nada). Repite con --confirm.");
    return;
  }

  const resultado = usuario
    ? await admin.auth().updateUser(usuario.uid, { password, emailVerified: true })
    : await admin.auth().createUser({
        email: correo,
        password,
        emailVerified: true,
        displayName,
      });

  console.log(`\n✅ Listo. uid: ${resultado.uid}`);
  console.log("Siguiente paso, si hace falta Premium para la revisión:");
  console.log(`  node scripts/manageUser.js grant-premium ${resultado.uid} --confirm`);
  console.log(
    "\nEl documento de Firestore del usuario lo crea la app sola en el primer login" +
      " (AuthRepository.syncUserDocument); no hay que tocarlo aquí."
  );
})().catch((e) => {
  console.error("Error:", e.message);
  process.exit(1);
});
