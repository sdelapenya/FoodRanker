# -*- coding: utf-8 -*-
"""Audita el APK ya construido: que todo texto tenga las dos variantes.

Se parsea por lineas porque los valores con saltos de linea ocupan varias y
rompen cualquier regex de bloque.
"""
import io, os, re

RES = os.environ.get("TEMP", ".") + "/res.txt"

CABECERA = re.compile(r'^\s+resource 0x[0-9a-f]+ (\w+)/([A-Za-z0-9_.]+)\s*$')
VALOR = re.compile(r'^\s+\(([^)]*)\)\s')

# Generados por el plugin de Google Services o que vienen de las librerias:
# no son texto nuestro y no tienen por que estar traducidos.
AJENOS = re.compile(
    r'^(default_web_client_id|gcm_defaultSenderId|google_api_key|google_app_id|'
    r'google_crash_reporting_api_key|google_storage_bucket|project_id|'
    r'firebase_database_url|abc_|androidx_|common_|m3_|mtrl_|fcm_|material_|'
    r'call_notification_|status_bar_notification_|search_menu_title|'
    r'bottom_sheet_|bottomsheet_|side_sheet_|exposed_dropdown_|'
    r'character_counter_|password_toggle_|clear_text_|error_icon_|'
    r'item_view_role_|path_password_|icon_content_description|not_set|chip_text|'
    r'expand_button_title|summary_collapsed_preference_list|v7_preference_|'
    r'preference_|copy_toast_msg|fallback_menu_item_|tooltip_|in_progress|'
    r'template_percent|appbar_scrolling_view_behavior|overflow_tab_title|'
    r'offline_|s[0-9]|admob_|gma_|play_|adwords_|debug_menu_|native_|'
    r'(?:com_)?google_|watermark_|user_agent|wallet_)')

bloques = []   # (tipo, nombre, set(configs))
tipo = nombre = None
cfgs = set()
for linea in io.open(RES, encoding="utf-8", errors="replace"):
    m = CABECERA.match(linea)
    if m:
        if nombre is not None:
            bloques.append((tipo, nombre, cfgs))
        tipo, nombre, cfgs = m.group(1), m.group(2), set()
        continue
    if nombre is None:
        continue
    v = VALOR.match(linea)
    if v:
        cfgs.add(v.group(1))
if nombre is not None:
    bloques.append((tipo, nombre, cfgs))

ambos = 0
solo_def, solo_es = [], []
for tipo, nombre, cfgs in bloques:
    if tipo not in ("string", "plurals"):
        continue
    hay_def = "" in cfgs
    hay_es = any(c.split("-")[0] == "es" for c in cfgs)
    if hay_def and hay_es:
        ambos += 1
    elif hay_def:
        if not AJENOS.match(nombre):
            solo_def.append("%s/%s" % (tipo, nombre))
    elif hay_es:
        solo_es.append("%s/%s" % (tipo, nombre))

print("bloques de recursos leidos: %d" % len(bloques))
print("texto con los dos idiomas: %d" % ambos)
print("solo en el idioma por defecto (descartando libreria): %d" % len(solo_def))
for n in sorted(solo_def):
    print("   " + n)
print("solo en castellano: %d" % len(solo_es))
for n in sorted(solo_es):
    print("   " + n)
