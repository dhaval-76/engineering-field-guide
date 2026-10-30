// ws + Redis pub/sub: any instance can deliver to any room.
import { WebSocketServer } from 'ws';
import Redis from 'ioredis';

const pub = new Redis(process.env.REDIS_URL);
const sub = new Redis(process.env.REDIS_URL); // a subscribed connection can't run other commands
const rooms = new Map(); // room -> Set<socket>

const wss = new WebSocketServer({ port: 8080 });
sub.psubscribe('room:*');
sub.on('pmessage', (_pattern, room, payload) => {
  for (const ws of rooms.get(room) ?? []) {
    if (ws.bufferedAmount > 1_000_000) ws.terminate(); // too slow: drop rather than buffer forever
    else ws.send(payload);
  }
});

wss.on('connection', (ws) => {
  ws.isAlive = true;
  ws.on('pong', () => (ws.isAlive = true));
  ws.on('message', (raw) => {
    const m = JSON.parse(raw);
    const room = `room:${m.room}`;
    if (m.type === 'join') {
      if (!rooms.has(room)) rooms.set(room, new Set());
      rooms.get(room).add(ws);
    } else if (m.type === 'say') {
      pub.publish(room, JSON.stringify({ room: m.room, text: m.text, at: Date.now() }));
    }
  });
  ws.on('close', () => rooms.forEach((set) => set.delete(ws)));
});

// Heartbeat: detect dead connections that never sent a close frame.
setInterval(() => {
  for (const ws of wss.clients) {
    if (!ws.isAlive) ws.terminate();
    else {
      ws.isAlive = false;
      ws.ping();
    }
  }
}, 30_000);
