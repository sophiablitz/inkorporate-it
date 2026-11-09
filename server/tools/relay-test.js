// Dependency-free WebSocket client to verify the live-streaming /relay protocol.
// Scenarios:
//   1. Client A streams begin -> points -> end. Client B (already connected) receives
//      each frame, and NOT its own echo.
//   2. A late joiner C connects after the stroke and receives a snapshot containing
//      the finished stroke (points preserved, in order).
const net = require("net");
const crypto = require("crypto");

const HOST = "127.0.0.1";
const PORT = Number(process.env.RELAY_PORT) || 8080;
const PATH = "/relay";

function connect(name, onText, onOpen) {
  const key = crypto.randomBytes(16).toString("base64");
  const sock = net.connect(PORT, HOST, () => {
    sock.write(
      `GET ${PATH} HTTP/1.1\r\n` +
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

const aGot = [];
const bGot = [];
const cGot = [];
let a;

const b = connect("B", (t) => bGot.push(t), () => {
  // B is connected (and will have received its own empty snapshot). Now connect A.
  a = connect("A", (t) => aGot.push(t), () => {
    // Stream a stroke from A.
    setTimeout(() => a.sendText('{"type":"begin","id":"s1","brush":"Marker","color":-16777216,"size":12.0,"epsilon":0.1}'), 100);
    setTimeout(() => a.sendText('{"type":"points","id":"s1","points":[{"x":1.0,"y":2.0,"t":0}]}'), 200);
    setTimeout(() => a.sendText('{"type":"points","id":"s1","points":[{"x":3.0,"y":4.0,"t":10}]}'), 300);
    setTimeout(() => a.sendText('{"type":"end","id":"s1"}'), 400);
  });
});

// Late joiner C connects after the stroke is done.
let c;
setTimeout(() => {
  c = connect("C", (t) => cGot.push(t));
}, 700);

setTimeout(() => {
  const only = (arr) => arr.filter((m) => !m.includes('"snapshot"'));
  const bLive = only(bGot);
  const aLive = only(aGot);

  console.log("B received (live):", JSON.stringify(bLive, null, 0));
  console.log("A received (live):", JSON.stringify(aLive, null, 0));
  console.log("C snapshot:", JSON.stringify(cGot.filter((m) => m.includes('"snapshot"'))));

  const bSawBegin = bLive.some((m) => m.includes('"begin"') && m.includes('s1'));
  const bSawPoints = bLive.filter((m) => m.includes('"points"')).length === 2;
  const bSawEnd = bLive.some((m) => m.includes('"end"'));
  const aSawNothing = aLive.length === 0;
  const cSnap = cGot.find((m) => m.includes('"snapshot"')) || "";
  const cHasStroke = cSnap.includes('"x":1') && cSnap.includes('"x":3');

  const checks = [
    ["B saw begin", bSawBegin],
    ["B saw 2 points frames", bSawPoints],
    ["B saw end", bSawEnd],
    ["A got no echo of its own frames", aSawNothing],
    ["C snapshot contains the finished stroke's points", cHasStroke],
  ];
  checks.forEach(([label, ok]) => console.log(`${ok ? "PASS" : "FAIL"}: ${label}`));
  const allPass = checks.every(([, ok]) => ok);
  console.log(allPass ? "\nALL PASS" : "\nSOME FAILED");

  a && a.close();
  b.close();
  c && c.close();
  process.exit(allPass ? 0 : 1);
}, 1200);
