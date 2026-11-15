#!/usr/bin/env python3
"""App Store özel ürün sayfaları (15 Kasım 2026; fastlane/custom-pages.json, docs/urun-sayfalari-2026-11.md).

  durum            : App Store sürümlerini (dil sürümleri ve anahtar kelimeleriyle) ve var olan özel ürün sayfalarını yazar;
                     hiçbir şey değiştirmez.
  olustur <klasör> : custom-pages.json'daki her sayfayı (aynı adla yoksa) taslak olarak açar: canlı sürümde bulunan dil
                     sürümleri, tanıtım metni, çerçevelenmiş görseller (<klasör>/<sayfa>/<dil sürümü>/NN-ad.png ve ipad-…)
                     ve arama kelimeleri (yalnız uygulamanın o dildeki anahtar kelime listesinde olanlar; olmayanlar yazılır).
                     İncelemeye göndermez.
  gonder           : taslak sayfa sürümlerini bir inceleme gönderimine ekler ve gönderir (yalnız sahibin onayıyla).

Anahtar ortam değişkenlerinden gelir (ASC_KEY_ID, ASC_ISSUER_ID, ASC_KEY_P8); değerleri hiçbir yere yazılmaz.
"""
import hashlib
import json
import os
import sys
import time
import urllib.error
import urllib.request

import jwt

API = "https://api.appstoreconnect.apple.com/v1"
BUNDLE_ID = "tr.bumba.time"
HERE = os.path.dirname(os.path.abspath(__file__))
PAGES = os.path.join(HERE, "..", "fastlane", "custom-pages.json")
# Görselin cihaz türü: 6,9" iPhone (1320×2868) ve 13" iPad (2064×2752)
DISPLAY = {"phone": "APP_IPHONE_67", "ipad": "APP_IPAD_PRO_3GEN_129"}


def token():
    now = int(time.time())
    return jwt.encode(
        {"iss": os.environ["ASC_ISSUER_ID"], "iat": now, "exp": now + 15 * 60, "aud": "appstoreconnect-v1"},
        os.environ["ASC_KEY_P8"],
        algorithm="ES256",
        headers={"kid": os.environ["ASC_KEY_ID"], "typ": "JWT"},
    )


class ApiError(Exception):
    pass


def call(method, path, body=None, soft=False):
    url = path if path.startswith("http") else API + path
    req = urllib.request.Request(url, method=method, data=json.dumps(body).encode() if body is not None else None)
    req.add_header("Authorization", "Bearer " + token())
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req) as r:
            raw = r.read()
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        msg = f"HATA {method} {path}: {e.code} {e.read().decode(errors='replace')[:1500]}"
        if soft:
            print(msg)
            raise ApiError(msg)
        print(msg)
        sys.exit(1)


def app_id():
    apps = call("GET", f"/apps?filter[bundleId]={BUNDLE_ID}")["data"]
    if not apps:
        sys.exit(f"{BUNDLE_ID} bulunamadı")
    return apps[0]["id"]


def versions(aid):
    return call("GET", f"/apps/{aid}/appStoreVersions?filter[platform]=IOS&limit=10")["data"]


def live_version(aid):
    for v in versions(aid):
        if v["attributes"].get("appStoreState") == "READY_FOR_SALE":
            return v
    sys.exit("Canlı (READY_FOR_SALE) sürüm yok")


def version_locs(vid):
    return call("GET", f"/appStoreVersions/{vid}/appStoreVersionLocalizations?limit=50")["data"]


def pages(aid):
    return call("GET", f"/apps/{aid}/appCustomProductPages?limit=50")["data"]


def page_versions(pid):
    return call("GET", f"/appCustomProductPages/{pid}/appCustomProductPageVersions?limit=20")["data"]


def page_locs(pvid):
    return call("GET", f"/appCustomProductPageVersions/{pvid}/appCustomProductPageLocalizations?limit=50")["data"]


def status(aid):
    for v in versions(aid):
        a = v["attributes"]
        print(f"Sürüm {a.get('versionString')} · {a.get('appStoreState')} · {v['id']}")
        for l in version_locs(v["id"]):
            la = l["attributes"]
            print(f"  {la['locale']}: anahtar kelimeler: {la.get('keywords') or '-'}")
    ps = pages(aid)
    print(f"\nÖzel ürün sayfaları: {len(ps)}")
    for p in ps:
        a = p["attributes"]
        print(f"- {a.get('name')} · görünür: {a.get('visible')} · {a.get('url') or '-'} · {p['id']}")
        for pv in page_versions(p["id"]):
            pa = pv["attributes"]
            print(f"  sürüm {pa.get('version')} · {pa.get('state')} · bağlantı: {pa.get('deepLink') or '-'}")
            for l in page_locs(pv["id"]):
                la = l["attributes"]
                print(f"    {la['locale']}: {la.get('promotionalText') or '-'}")


def upload_shot(set_id, path):
    data = open(path, "rb").read()
    r = call("POST", "/appScreenshots", {"data": {
        "type": "appScreenshots",
        "attributes": {"fileName": os.path.basename(path), "fileSize": len(data)},
        "relationships": {"appScreenshotSet": {"data": {"type": "appScreenshotSets", "id": set_id}}},
    }})["data"]
    for op in r["attributes"]["uploadOperations"]:
        chunk = data[op["offset"]:op["offset"] + op["length"]]
        req = urllib.request.Request(op["url"], method=op["method"], data=chunk)
        for h in op.get("requestHeaders") or []:
            req.add_header(h["name"], h["value"])
        with urllib.request.urlopen(req) as resp:
            resp.read()
    call("PATCH", f"/appScreenshots/{r['id']}", {"data": {
        "type": "appScreenshots", "id": r["id"],
        "attributes": {"uploaded": True, "sourceFileChecksum": hashlib.md5(data).hexdigest()},
    }})


def add_shots(loc_id, folder):
    if not os.path.isdir(folder):
        print(f"    görsel klasörü yok: {folder}")
        return
    files = sorted(f for f in os.listdir(folder) if f.endswith(".png"))
    for kind, prefix_ok in (("phone", lambda f: not f.startswith("ipad-")), ("ipad", lambda f: f.startswith("ipad-"))):
        mine = [f for f in files if prefix_ok(f)]
        if not mine:
            continue
        s = call("POST", "/appScreenshotSets", {"data": {
            "type": "appScreenshotSets",
            "attributes": {"screenshotDisplayType": DISPLAY[kind]},
            "relationships": {"appCustomProductPageLocalization": {"data": {"type": "appCustomProductPageLocalizations", "id": loc_id}}},
        }})["data"]
        for f in mine:
            upload_shot(s["id"], os.path.join(folder, f))
        print(f"    {kind}: {len(mine)} görsel")


def add_keywords(loc_id, wanted, available):
    have = {k.strip().lower() for k in (available or "").split(",") if k.strip()}
    ok = [k for k in wanted if k.lower() in have]
    missing = [k for k in wanted if k.lower() not in have]
    if missing:
        print(f"    anahtar kelime listesinde olmadığı için atanmadı: {', '.join(missing)}")
    if not ok:
        return
    try:
        call("POST", f"/appCustomProductPageLocalizations/{loc_id}/relationships/searchKeywords",
             {"data": [{"type": "appKeywords", "id": k} for k in ok]}, soft=True)
        print(f"    arama kelimeleri: {', '.join(ok)}")
    except ApiError:
        print("    arama kelimeleri atanamadı (API desteklemiyor olabilir); App Store Connect'te elle atanır")


def create(aid, shots_dir):
    cfg = json.load(open(PAGES))["pages"]
    live = live_version(aid)
    locs = {l["attributes"]["locale"]: l["attributes"].get("keywords") for l in version_locs(live["id"])}
    print(f"Canlı sürüm {live['attributes']['versionString']}, dil sürümleri: {', '.join(sorted(locs))}")
    existing = {p["attributes"]["name"]: p["id"] for p in pages(aid)}
    for p in cfg:
        if p["name"] in existing:
            print(f"- {p['name']}: zaten var, atlandı")
            continue
        use = [l for l in p["locales"] if l in locs]
        if not use:
            print(f"- {p['name']}: canlı sürümde dil sürümü yok, atlandı")
            continue
        for l, v in p["locales"].items():
            if len(v["promo"]) > 170:
                sys.exit(f"{p['id']}/{l}: tanıtım metni 170 karakteri aşıyor")
        included = [{"type": "appCustomProductPageVersions", "id": "${v}", "relationships": {"appCustomProductPageLocalizations": {
            "data": [{"type": "appCustomProductPageLocalizations", "id": f"${{l{i}}}"} for i in range(len(use))]}}}]
        for i, l in enumerate(use):
            included.append({"type": "appCustomProductPageLocalizations", "id": f"${{l{i}}}",
                             "attributes": {"locale": l, "promotionalText": p["locales"][l]["promo"]}})
        r = call("POST", "/appCustomProductPages", {"data": {
            "type": "appCustomProductPages",
            "attributes": {"name": p["name"]},
            "relationships": {
                "app": {"data": {"type": "apps", "id": aid}},
                "appCustomProductPageVersions": {"data": [{"type": "appCustomProductPageVersions", "id": "${v}"}]},
            },
        }, "included": included})
        pid = r["data"]["id"]
        print(f"- {p['name']}: açıldı ({pid}), dil sürümleri: {', '.join(use)}")
        skipped = [l for l in p["locales"] if l not in locs]
        if skipped:
            print(f"  canlı sürümde olmadığı için sonraya: {', '.join(skipped)}")
        pv = page_versions(pid)[0]
        for l in page_locs(pv["id"]):
            loc = l["attributes"]["locale"]
            print(f"  {loc}")
            add_shots(l["id"], os.path.join(shots_dir, p["id"], loc))
            add_keywords(l["id"], p["locales"][loc].get("keywords", []), locs.get(loc))


def submit(aid):
    drafts = []
    for p in pages(aid):
        for pv in page_versions(p["id"]):
            if pv["attributes"].get("state") == "PREPARE_FOR_SUBMISSION":
                drafts.append((p["attributes"]["name"], pv["id"]))
    if not drafts:
        print("Gönderilecek taslak sayfa yok")
        return
    open_subs = call("GET", f"/reviewSubmissions?filter[app]={aid}&filter[platform]=IOS&filter[state]=READY_FOR_REVIEW&limit=5")["data"]
    if open_subs:
        sid = open_subs[0]["id"]
    else:
        sid = call("POST", "/reviewSubmissions", {"data": {"type": "reviewSubmissions", "attributes": {"platform": "IOS"},
                   "relationships": {"app": {"data": {"type": "apps", "id": aid}}}}})["data"]["id"]
    for name, pvid in drafts:
        call("POST", "/reviewSubmissionItems", {"data": {"type": "reviewSubmissionItems", "relationships": {
            "reviewSubmission": {"data": {"type": "reviewSubmissions", "id": sid}},
            "appCustomProductPageVersion": {"data": {"type": "appCustomProductPageVersions", "id": pvid}}}}})
        print(f"- {name}: gönderime eklendi")
    call("PATCH", f"/reviewSubmissions/{sid}", {"data": {"type": "reviewSubmissions", "id": sid, "attributes": {"submitted": True}}})
    print("Gönderim incelemeye yollandı")


if __name__ == "__main__":
    action = sys.argv[1] if len(sys.argv) > 1 else "durum"
    aid = app_id()
    if action == "durum":
        status(aid)
    elif action == "olustur":
        if len(sys.argv) < 3:
            sys.exit("Kullanım: asc_cpp.py olustur <çerçevelenmiş görsel klasörü>")
        create(aid, sys.argv[2])
    elif action == "gonder":
        submit(aid)
    else:
        sys.exit(f"Bilinmeyen işlem: {action}")
