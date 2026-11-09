// Dependency-free WebSocket client verifying room ISOLATION on /relay?room=<code>.
// Scenario:
//   - A1 and A2 are in room "alpha"; B1 is in room "beta".
//   - A1 streams a stroke.
//   - A2 (same room) must receive the live frames.
//   - B1 (different room) must receive NOTHING live.
//   - Late joiner AL in "alpha" gets a snapshot WITH the stroke.
//   - Late joiner BL in "beta" gets an EMPTY snapshot (rooms don't share canvas).
const net = require("net");
const crypto = require("crypto");

const HOST = "127.0.0.1";
const PORT = Number(process.env.RELAY_PORT) || 8080;

function connect(name, room, onText, onOpen) {
  const key = crypto.randomBytes(16).toString("base64");
  const path = `/relay?room=${encodeURIComponent(room)}`;
  const sock = net.connect(PORT, HOST, () => {
    sock.write(
      `GET ${path} HTTP/1.1\r\n` +
        `Host: ${HOST}:${PORT}\r\n` +
        `Upgrade: websocket\r\n` +
        `Connection: Upgrade\r\n` +
        `Sec-WebSocket-Key: ${key}\r\n` +
        `Sec-WebSocket-Version: 13\r\n\r\n`
    );
  });

  let handshakeDone = false;
  let buf = Buffer.alloc(0);

  sock.on("data", (chunk) => {
    buf = Buffer.concat([buf, chunk]);
    if (!handshakeDone) {
      const idx = buf.indexOf("\r\n\r\n");
      if (idx === -1) return;
      handshakeDone = true;
      buf = buf.slice(idx + 4);
      onOpen && onOpen();
    }
    while (buf.length >= 2) {
      const opcode = buf[0] & 0x0f;
      let len = buf[1] & 0x7f;
      let offset = 2;
      if (len === 126) {
        if (buf.length < 4) return;
        len = buf.readUInt16BE(2);
        offset = 4;
      } else if (len === 127) {
        if (buf.length < 10) return;
        len = Number(buf.readBigUInt64BE(2));
        offset = 10;
      }
      if (buf.length < offset + len) return;
      const payload = buf.slice(offset, offset + len);
      buf = buf.slice(offset + len);
      if (opcode === 0x1) onText(payload.toString("utf8"));
      else if (opcode === 0x8) sock.end();
    }
  });

  sock.on("error", (e) => console.log(`[${name}] error:`, e.message));

  return {
    sendText(str) {
      const payload = Buffer.from(str, "utf8");
      const mask = crypto.randomBytes(4);
      const header = [0x81];
      if (payload.length < 126) header.push(0x80 | payload.length);
      else header.push(0x80 | 126, (payload.length >> 8) & 0xff, payload.length & 0xff);
      const masked = Buffer.from(payload);
      for (let i = 0; i < masked.length; i++) masked[i] ^= mask[i % 4];
      sock.write(Buffer.concat([Buffer.from(header), mask, masked]));
    },
    close() {
      sock.end();
    },
  };
}

const a2Got = [];
const b1Got = [];
const alGot = [];
const blGot = [];
let a1, a2, b1, al, bl;

// Bring up the two "already present" clients first: A2 in alpha, B1 in beta.
a2 = connect("A2", "alpha", (t) => a2Got.push(t), () => {
  b1 = connect("B1", "beta", (t) => b1Got.push(t), () => {
    // Now A1 joins alpha and streams a stroke.
    a1 = connect("A1", "alpha", () => {}, () => {
      setTimeout(() => a1.sendText('{"type":"begin","id":"r1","brush":"Marker","color":-16777216,"size":9.0,"epsilon":0.1}'), 100);
      setTimeout(() => a1.sendText('{"type":"points","id":"r1","points":[{"x":5.0,"y":6.0,"t":0}]}'), 200);
      setTimeout(() => a1.sendText('{"type":"end","id":"r1"}'), 300);
    });
  });
});

// Late joiners after the stroke is finished.
setTimeout(() => {
  al = connect("AL", "alpha", (t) => alGot.push(t));
  bl = connect("BL", "beta", (t) => blGot.push(t));
}, 600);

setTimeout(() => {
  const live = (arr) => arr.filter((m) => !m.includes('"snapshot"'));
  const a2Live = live(a2Got);
  const b1Live = live(b1Got);

  const alSnap = alGot.find((m) => m.includes('"snapshot"')) || "";
  const blSnap = blGot.find((m) => m.includes('"snapshot"')) || "";

  console.log("A2 (alpha) live:", JSON.stringify(a2Live));
  console.log("B1 (beta)  live:", JSON.stringify(b1Live));
  console.log("AL snapshot:", alSnap);
  console.log("BL snapshot:", blSnap);

  const checks = [
    ["A2 (same room) saw the begin frame", a2Live.some((m) => m.includes('"begin"') && m.includes('r1'))],
    ["A2 (same room) saw the end frame", a2Live.some((m) => m.includes('"end"'))],
    ["B1 (other room) received NOTHING live", b1Live.length === 0],
    ["AL snapshot (alpha) contains the stroke", alSnap.includes('"x":5')],
    ["BL snapshot (beta) is EMPTY", blSnap.includes('"strokes":[]')],
  ];
  checks.forEach(([label, ok]) => console.log(`${ok ? "PASS" : "FAIL"}: ${label}`));
  const allPass = checks.every(([, ok]) => ok);
  console.log(allPass ? "\nALL PASS" : "\nSOME FAILED");

  [a1, a2, b1, al, bl].forEach((c) => c && c.close());
  process.exit(allPass ? 0 : 1);
}, 1100);
