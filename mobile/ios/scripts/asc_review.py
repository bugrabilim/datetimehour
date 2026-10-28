#!/usr/bin/env python3
"""App Store Connect inceleme durumu ve yeniden gönderme (iOS inceleme iş akışı çalıştırır).

  durum         : uygulamanın App Store sürümlerini, inceleme gönderimlerini (öğeleriyle) ve TestFlight test gruplarını
                  (kullanıcıları ve yapılarıyla) yazar; hiçbir şey değiştirmez. Depo herkese açık olduğu için iş akışı
                  çıktısı da herkese açıktır: e-posta adresleri kısaltılarak yazılır (ör. e***@gmail.com), ad yazılmaz.
  yeniden-gonder: "Unresolved Issues" durumundaki gönderimde reddedilen öğeleri çözüldü olarak işaretler ve gönderimi
                  yeniden incelemeye yollar. Apple'ın bilgi isteğine App Store Connect'te yanıt verildikten sonra
                  "Resubmit to App Review" düğmesi kapalı kalırsa kullanılır.

Anahtar ortam değişkenlerinden gelir (ASC_KEY_ID, ASC_ISSUER_ID, ASC_KEY_P8); değerleri hiçbir yere yazılmaz.
"""
import json
import os
import sys
import time
import urllib.error
import urllib.request

import jwt

API = "https://api.appstoreconnect.apple.com/v1"
BUNDLE_ID = "tr.bumba.time"


def token():
    now = int(time.time())
    return jwt.encode(
        {"iss": os.environ["ASC_ISSUER_ID"], "iat": now, "exp": now + 15 * 60, "aud": "appstoreconnect-v1"},
        os.environ["ASC_KEY_P8"],
        algorithm="ES256",
        headers={"kid": os.environ["ASC_KEY_ID"], "typ": "JWT"},
    )


def call(method, path, body=None):
    url = path if path.startswith("http") else API + path
    req = urllib.request.Request(url, method=method, data=json.dumps(body).encode() if body else None)
    req.add_header("Authorization", "Bearer " + token())
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req) as r:
            raw = r.read()
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        print(f"HATA {method} {path}: {e.code} {e.read().decode(errors='replace')}")
        sys.exit(1)


def app_id():
    apps = call("GET", f"/apps?filter[bundleId]={BUNDLE_ID}")["data"]
    if not apps:
        sys.exit(f"{BUNDLE_ID} bulunamadı")
    return apps[0]["id"]


def submissions(aid):
    return call("GET", f"/reviewSubmissions?filter[app]={aid}&filter[platform]=IOS&limit=20")["data"]


def items(sid):
    return call("GET", f"/reviewSubmissions/{sid}/items?include=appStoreVersion&limit=50")["data"]


def status(aid):
    for v in call("GET", f"/apps/{aid}/appStoreVersions?limit=5")["data"]:
        a = v["attributes"]
        print(f"Sürüm {a['versionString']}: appStoreState={a.get('appStoreState')} appVersionState={a.get('appVersionState')}")
    for s in submissions(aid):
        a = s["attributes"]
        print(f"Gönderim {s['id']}: state={a.get('state')} submitted={a.get('submittedDate')}")
        for it in items(s["id"]):
            print(f"  öğe {it['id']}: state={it['attributes'].get('state')}")


def mask(email):
    if not email or "@" not in email:
        return "(e-posta yok)"
    user, domain = email.split("@", 1)
    return user[:1] + "***@" + domain


def beta_groups(aid):
    """TestFlight grupları: iç/dış, otomatik dağıtım, kullanıcılar (kısaltılmış e-posta, durum) ve gruptaki yapılar."""
    groups = call("GET", f"/apps/{aid}/betaGroups?limit=50")["data"]
    print(f"--- TestFlight grupları ({len(groups)}) ---")
    for g in groups:
        a = g["attributes"]
        kind = "iç" if a.get("isInternalGroup") else "dış"
        auto = " · otomatik dağıtım açık" if a.get("hasAccessToAllBuilds") else ""
        link = " · herkese açık bağlantı açık" if a.get("publicLinkEnabled") else ""
        print(f"Grup \"{a.get('name')}\" ({kind}{auto}{link})")
        testers = call("GET", f"/betaGroups/{g['id']}/betaTesters?limit=200")["data"]
        print(f"  kullanıcılar ({len(testers)}):")
        for t in testers:
            ta = t["attributes"]
            print(f"    {mask(ta.get('email'))} · {ta.get('inviteType') or '-'} · {ta.get('state') or '-'}")
        builds = call("GET", f"/betaGroups/{g['id']}/builds?limit=200")["data"]
        nums = sorted((b["attributes"].get("version") for b in builds), key=lambda v: int(v) if str(v).isdigit() else 0)
        print(f"  yapılar ({len(builds)}): {', '.join(map(str, nums)) or '-'}")


def resubmit(aid):
    open_subs = [s for s in submissions(aid) if s["attributes"].get("state") == "UNRESOLVED_ISSUES"]
    if not open_subs:
        print("Çözülmemiş sorunlu gönderim yok; yapılacak bir şey yok.")
        return
    for s in open_subs:
        for it in items(s["id"]):
            if it["attributes"].get("state") == "REJECTED":
                call("PATCH", f"/reviewSubmissionItems/{it['id']}",
                     {"data": {"type": "reviewSubmissionItems", "id": it["id"], "attributes": {"resolved": True}}})
                print(f"Öğe {it['id']} çözüldü olarak işaretlendi")
        r = call("PATCH", f"/reviewSubmissions/{s['id']}",
                 {"data": {"type": "reviewSubmissions", "id": s["id"], "attributes": {"submitted": True}}})
        print(f"Gönderim {s['id']} yeniden incelemeye yollandı: state={r['data']['attributes'].get('state')}")


if __name__ == "__main__":
    action = sys.argv[1] if len(sys.argv) > 1 else "durum"
    aid = app_id()
    if action == "yeniden-gonder":
        resubmit(aid)
        print("--- güncel durum ---")
    status(aid)
    beta_groups(aid)
