// Connects to /relay and prints every frame it receives for a few seconds.
// Used to confirm the Android app streams begin/points/end live when you draw.
const net = require("net");
const crypto = require("crypto");
const s = net.connect(8080, "127.0.0.1", () => {
  const k = crypto.randomBytes(16).toString("base64");
  s.write(
    `GET /relay HTTP/1.1\r\nHost: x\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n` +
      `Sec-WebSocket-Key: ${k}\r\nSec-WebSocket-Version: 13\r\n\r\n`
  );
});
let hs = false,
  buf = Buffer.alloc(0);
const counts = { begin: 0, points: 0, end: 0, snapshot: 0, other: 0 };
s.on("data", (c) => {
  buf = Buffer.concat([buf, c]);
  if (!hs) {
    const i = buf.indexOf("\r\n\r\n");
    if (i < 0) return;
    hs = true;
    buf = buf.slice(i + 4);
  }
  while (buf.length >= 2) {
    const op = buf[0] & 0x0f;
    let len = buf[1] & 0x7f,
      off = 2;
    if (len === 126) {
      if (buf.length < 4) return;
      len = buf.readUInt16BE(2);
      off = 4;
    } else if (len === 127) {
      if (buf.length < 10) return;
      len = Number(buf.readBigUInt64BE(2));
      off = 10;
    }
    if (buf.length < off + len) return;
    const payload = buf.slice(off, off + len).toString("utf8");
    buf = buf.slice(off + len);
    if (op === 0x1) {
      let t = "other";
      try {
        t = JSON.parse(payload).type || "other";
      } catch {}
      counts[t] = (counts[t] || 0) + 1;
      const short = payload.length > 90 ? payload.slice(0, 90) + "…" : payload;
      console.log(`[recv ${t}] ${short}`);
    }
  }
});
setTimeout(() => {
  console.log("\ncounts:", JSON.stringify(counts));
  const live = counts.begin > 0 && counts.points > 0 && counts.end > 0;
  console.log(live ? "PASS: app streamed a live stroke (begin+points+end)" : "no live stroke seen");
  process.exit(0);
}, Number(process.env.LISTEN_MS || 8000));
