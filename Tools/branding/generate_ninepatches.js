// Usage: node Tools/branding/generate_ninepatches.js TMessagesProj/src/main/res
// Radii must match LiquidGlassStyle.MENU_RADIUS_DP / SHEET_RADIUS_DP.
// Regenerates Telegram's popup / sheet nine-patches in an iOS style:
// larger continuous corners and a soft, wide, slightly offset shadow.
// Padding (content area) is read from the original file and preserved exactly.
const fs = require('fs'), zlib = require('zlib'), path = require('path');

function readPng(file) {
  const b = fs.readFileSync(file); let o = 8, ihdr, idat = [], plte, trns;
  while (o < b.length) { const len = b.readUInt32BE(o), t = b.toString('ascii', o + 4, o + 8), d = b.slice(o + 8, o + 8 + len);
    if (t === 'IHDR') ihdr = { w: d.readUInt32BE(0), h: d.readUInt32BE(4), depth: d[8], ct: d[9], il: d[12] };
    else if (t === 'IDAT') idat.push(d); else if (t === 'PLTE') plte = d; else if (t === 'tRNS') trns = d; o += 12 + len; }
  const ch = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[ihdr.ct]; if (ihdr.depth !== 8 || ihdr.il) throw new Error('unsupported png ' + file);
  const raw = zlib.inflateSync(Buffer.concat(idat)), st = ihdr.w * ch, px = Buffer.alloc(ihdr.h * st);
  for (let y = 0; y < ihdr.h; y++) { const f = raw[y * (st + 1)];
    for (let x = 0; x < st; x++) { const a = x >= ch ? px[y * st + x - ch] : 0, u = y ? px[(y - 1) * st + x] : 0, c = (y && x >= ch) ? px[(y - 1) * st + x - ch] : 0;
      let v = raw[y * (st + 1) + 1 + x];
      if (f === 1) v += a; else if (f === 2) v += u; else if (f === 3) v += (a + u) >> 1;
      else if (f === 4) { const p = a + u - c, pa = Math.abs(p - a), pb = Math.abs(p - u), pc = Math.abs(p - c); v += (pa <= pb && pa <= pc) ? a : (pb <= pc ? u : c); }
      px[y * st + x] = v & 255; } }
  const get = (x, y) => { const i = y * st + x * ch;
    if (ihdr.ct === 6) return [px[i], px[i+1], px[i+2], px[i+3]]; if (ihdr.ct === 2) return [px[i], px[i+1], px[i+2], 255];
    if (ihdr.ct === 4) return [px[i], px[i], px[i], px[i+1]]; if (ihdr.ct === 0) return [px[i], px[i], px[i], 255];
    const k = px[i]; return [plte[k*3], plte[k*3+1], plte[k*3+2], trns && k < trns.length ? trns[k] : 255]; };
  return { w: ihdr.w, h: ihdr.h, get };
}

function originalPadding(file) {
  const { w, h, get } = readPng(file), black = p => p[3] > 128 && p[0] < 50 && p[1] < 50 && p[2] < 50;
  const run = (n, g) => { let s = -1, e = -1; for (let i = 1; i < n - 1; i++) if (black(g(i))) { if (s < 0) s = i; e = i; } return [s - 1, e - 1]; };
  const hx = run(w, x => get(x, h - 1)), vy = run(h, y => get(w - 1, y));
  return { l: hx[0], r: w - 3 - hx[1], t: vy[0], b: h - 3 - vy[1] };
}

const CRC = new Int32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1; return c; });
const crc32 = buf => { let c = -1; for (const b of buf) c = CRC[(c ^ b) & 255] ^ (c >>> 8); return (c ^ -1) >>> 0; };
function chunk(type, data) { const len = Buffer.alloc(4); len.writeUInt32BE(data.length); const td = Buffer.concat([Buffer.from(type), data]); const c = Buffer.alloc(4); c.writeUInt32BE(crc32(td)); return Buffer.concat([len, td, c]); }
function writePng(file, w, h, rgba) {
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  const raw = Buffer.alloc(h * (w * 4 + 1)); for (let y = 0; y < h; y++) rgba.copy(raw, y * (w * 4 + 1) + 1, y * w * 4, (y + 1) * w * 4);
  fs.writeFileSync(file, Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw, { level: 9 })), chunk('IEND', Buffer.alloc(0))]));
}

// signed distance to rounded rect with per-corner radii (tl, tr, br, bl)
function sdRect(px, py, L, T, R, B, rad) {
  const cx = (L + R) / 2, cy = (T + B) / 2, hx = (R - L) / 2, hy = (B - T) / 2, x = px - cx, y = py - cy;
  const r = x < 0 ? (y < 0 ? rad[0] : rad[3]) : (y < 0 ? rad[1] : rad[2]);
  const qx = Math.abs(x) - hx + r, qy = Math.abs(y) - hy + r;
  return Math.hypot(Math.max(qx, 0), Math.max(qy, 0)) + Math.min(Math.max(qx, qy), 0) - r;
}

/** spec: { radiusDp, topOnly, shadowAlpha, shadowOffsetDp } */
function generate(file, scale, spec) {
  const pad = originalPadding(file);
  const R = Math.round(spec.radiusDp * scale);
  const rad = spec.topOnly ? [R, R, 0, 0] : [R, R, R, R];
  const stretch = 2;
  const W = pad.l + R + stretch + R + pad.r;
  const H = spec.topOnly ? pad.t + R + stretch + 2 : pad.t + R + stretch + R + pad.b;
  const L = pad.l, T = pad.t, Rt = W - pad.r, Bt = spec.topOnly ? H + R * 4 : H - pad.b; // sheets run off the bottom
  const sigma = Math.max(1, Math.min(pad.l, pad.t) / 2.4), off = spec.shadowOffsetDp * scale;
  const iw = W + 2, ih = H + 2, img = Buffer.alloc(iw * ih * 4);
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    const sx = x + 0.5, sy = y + 0.5;
    const sd = sdRect(sx, sy, L, T, Rt, Bt, rad);
    const body = Math.min(1, Math.max(0, 0.5 - sd));
    const ssd = sdRect(sx, sy - off, L, T, Rt, Bt, rad);
    const shadow = spec.shadowAlpha * Math.exp(-Math.pow(Math.max(ssd, 0), 2) / (2 * sigma * sigma));
    const a = body + shadow * (1 - body);
    const c = a > 0 ? Math.round(255 * body / a) : 0;
    const i = ((y + 1) * iw + (x + 1)) * 4;
    img[i] = img[i + 1] = img[i + 2] = c; img[i + 3] = Math.round(a * 255);
  }
  const mark = (x, y) => { const i = (y * iw + x) * 4; img[i] = img[i + 1] = img[i + 2] = 0; img[i + 3] = 255; };
  // stretch guides (top row / left column) — middle segment between corners
  for (let x = L + R; x < L + R + stretch; x++) mark(x + 1, 0);
  if (spec.topOnly) { for (let y = T + R; y < H; y++) mark(0, y + 1); }
  else { for (let y = T + R; y < T + R + stretch; y++) mark(0, y + 1); }
  // content padding guides (bottom row / right column)
  for (let x = pad.l; x < W - pad.r; x++) mark(x + 1, ih - 1);
  for (let y = pad.t; y < H - pad.b; y++) mark(iw - 1, y + 1);
  writePng(file, iw, ih, img);
  return { file: path.relative(process.cwd(), file), size: [W, H], pad, R };
}

const SCALES = { 'drawable-mdpi': 1, 'drawable-hdpi': 1.5, 'drawable-xhdpi': 2, 'drawable-xxhdpi': 3 };
const SPECS = {
  popup_fixed_alert2: { radiusDp: 20, shadowAlpha: 0.13, shadowOffsetDp: 1.5 },
  popup_fixed_alert4: { radiusDp: 20, shadowAlpha: 0.13, shadowOffsetDp: 1.5 },
  sheet_shadow_round: { radiusDp: 28, topOnly: true, shadowAlpha: 0.10, shadowOffsetDp: 0 },
};
const res = process.argv[2];
for (const [dir, scale] of Object.entries(SCALES)) for (const [name, spec] of Object.entries(SPECS)) {
  const f = path.join(res, dir, name + '.9.png');
  if (fs.existsSync(f)) console.log(JSON.stringify(generate(f, scale, spec)));
}
