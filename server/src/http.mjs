/* Küçük HTTP yardımcıları (çerçevesiz node:http) */
export async function readForm(req, limit = 16 * 1024) {
  let size = 0;
  const chunks = [];
  for await (const c of req) {
    size += c.length;
    if (size > limit) throw Object.assign(new Error("too_large"), { status: 413 });
    chunks.push(c);
  }
  return new URLSearchParams(Buffer.concat(chunks).toString("utf8"));
}
