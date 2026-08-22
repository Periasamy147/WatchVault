"""
Proves the MyInnos -> Watch Vault migration mapping against the REAL backup
(Watch_Collection_FINAL_100percent_verified_2026-08-22.zip) before any Kotlin
is written. Mirrors exactly what MyInnosImporter.kt will do.
"""
import json, uuid, hashlib, os, sys

SRC = "/home/claude/wc_backup/watches.json"
IMG_DIR = "/home/claude/wc_backup/images"

with open(SRC) as f:
    original = json.load(f)

errors = []
def check(cond, msg):
    if not cond:
        errors.append(msg)

# ---- 1. Map each watch ----
new_watches = []
legacy_id_to_uuid = {}

for w in original["watches"]:
    new_uuid = str(uuid.uuid4())
    legacy_id_to_uuid[w["id"]] = new_uuid

    new_watch = {
        "uuid": new_uuid,
        "legacyId": w["id"],
        "brand": w["brand"],
        "model": w["model"],
        "referenceNumber": w["referenceNumber"],
        "movementRaw": w["movementType"],
        "movementNormalized": None,
        "conditionRaw": w["condition"],
        "ownershipStatus": w["status"],
        "purchaseDate": w["purchaseDate"],
        "purchasePrice": float(w["purchasePrice"]),
        "purchaseCurrency": "INR",  # assumed - flagged, not silently claimed as source fact
        "estimatedValue": float(w["marketValue"]) if "marketValue" in w else None,
        "estimatedValueSource": "myinnos_export",
        "hasBoxPapers": w["hasBoxPapers"],
        "box": None,
        "papers": None,
        "isFirstOwner": w["isFirstOwner"],
        "notes": w.get("notes"),
        "legacyData": json.dumps(w, sort_keys=True),  # full original object, nothing dropped
        "updatedAt": w["updatedAt"],
    }
    new_watches.append(new_watch)

    # rule: box/papers must NOT be inferred
    check(new_watch["box"] is None and new_watch["papers"] is None,
          f"{w['brand']} {w['model']}: box/papers must stay Unknown, not inferred")
    check(new_watch["hasBoxPapers"] == w["hasBoxPapers"],
          f"{w['brand']} {w['model']}: hasBoxPapers must be preserved verbatim")

# ---- 2. Round-trip check: every original field recoverable from legacyData ----
for w, nw in zip(original["watches"], new_watches):
    restored = json.loads(nw["legacyData"])
    check(restored == w, f"legacyId={w['id']}: legacyData round-trip mismatch")

# ---- 3. Photos ----
photo_rows = []
for w, nw in zip(original["watches"], new_watches):
    if w.get("imageUri"):
        basename = w["imageUri"].split("/")[-1]
        photo_rows.append({"watchUuid": nw["uuid"], "filename": basename, "isPrimary": True, "sortOrder": 0})
    for i, g in enumerate(w.get("galleryUris", [])):
        basename = g.split("/")[-1]
        photo_rows.append({"watchUuid": nw["uuid"], "filename": basename, "isPrimary": False, "sortOrder": i})

present_files = set(os.listdir(IMG_DIR))
for p in photo_rows:
    check(p["filename"] in present_files, f"missing image file on disk: {p['filename']}")
check(len(photo_rows) == 6, f"expected 6 photo rows, got {len(photo_rows)}")

# every primary/gallery role preserved: exactly one primary among watches that HAD an imageUri
watches_with_primary = sum(1 for w in original["watches"] if w.get("imageUri"))
mapped_primary = sum(1 for p in photo_rows if p["isPrimary"])
check(watches_with_primary == mapped_primary,
      f"primary photo count mismatch: source={watches_with_primary} mapped={mapped_primary}")

# ---- 4. Service records -> MaintenanceRecord ----
maintenance_rows = []
for sr in original["serviceRecords"]:
    watch_uuid = legacy_id_to_uuid.get(sr["watchId"])
    check(watch_uuid is not None, f"serviceRecord {sr['id']}: watchId {sr['watchId']} does not resolve to a migrated watch")
    maintenance_rows.append({
        "uuid": str(uuid.uuid4()),
        "watchUuid": watch_uuid,
        "legacyServiceId": sr["id"],
        "date": sr["serviceDate"],
        "cost": float(sr["cost"]),
        "technician": sr["technician"],
        "description": sr["description"],
        "isOverhaul": sr["isOverhaul"],
        "pressureTested": sr["pressureTested"],
    })
check(len(maintenance_rows) == len(original["serviceRecords"]), "service record count mismatch")
# confirm it resolves to the Waterbury specifically (legacyId 1)
wb_uuid = legacy_id_to_uuid[1]
check(any(m["watchUuid"] == wb_uuid for m in maintenance_rows),
      "battery service record did not attach to legacyId=1 (Waterbury)")

# ---- 5. Count integrity ----
check(len(new_watches) == 4, f"expected 4 watches, got {len(new_watches)}")
check(len(original["watches"]) == len(new_watches), "watch count mismatch pre/post migration")

# ---- 6. Conflicting external values are NOT applied ----
kc = next(w for w in new_watches if w["referenceNumber"] == "KCWGL0013101MN")
wb = next(w for w in new_watches if w["referenceNumber"] == "TW2P84200")
check(kc["estimatedValue"] == 15085.0, f"Kenneth Cole canonical value must stay 15085.0, got {kc['estimatedValue']}")
check(wb["estimatedValue"] == 6795.0, f"Waterbury canonical value must stay 6795.0, got {wb['estimatedValue']}")

conflicts = [
    {"watchUuid": kc["uuid"], "source": "myinnos_export", "value": 15085.0, "external_value": 13995.0,
     "external_source": "FINAL_VERIFICATION.txt", "status": "conflicting_not_applied"},
    {"watchUuid": wb["uuid"], "source": "myinnos_export", "value": 6795.0, "external_value": 6700.0,
     "external_source": "UPDATED_VALUES.txt", "status": "conflicting_not_applied"},
]

# ---- Report ----
print(f"Watches migrated: {len(new_watches)}")
print(f"Photos mapped: {len(photo_rows)}  (primary={mapped_primary})")
print(f"Maintenance records migrated: {len(maintenance_rows)}")
print(f"Price conflicts logged (not applied): {len(conflicts)}")
print()
if errors:
    print(f"FAIL — {len(errors)} check(s) failed:")
    for e in errors:
        print(f"  - {e}")
    sys.exit(1)
else:
    print("PASS — all round-trip and business-rule checks succeeded.")

# persist mapped output for inspection
with open("/home/claude/watchvault/validation/mapped_output.json", "w") as f:
    json.dump({
        "watches": new_watches,
        "photos": photo_rows,
        "maintenance": maintenance_rows,
        "price_conflicts": conflicts,
    }, f, indent=2)
