import { Hono } from 'hono';

export interface Env {
  COUNTER_DO: DurableObjectNamespace;
}

const app = new Hono<{ Bindings: Env }>();

app.post('/api/listening/heartbeat', async (c) => {
  const body = await c.req.json().catch(() => null);
  if (!body || typeof body.sessionId !== 'string' || body.sessionId.length < 1 || body.sessionId.length > 255) {
    return c.json({ error: 'Invalid sessionId' }, 400);
  }

  const id = c.env.COUNTER_DO.idFromName('mixify-live-counter');
  const stub = c.env.COUNTER_DO.get(id);

  const response = await stub.fetch('http://do/heartbeat', {
    method: 'POST',
    body: JSON.stringify({ sessionId: body.sessionId }),
  });

  return new Response(response.body, response);
});

app.post('/api/listening/stop', async (c) => {
  const body = await c.req.json().catch(() => null);
  if (!body || typeof body.sessionId !== 'string' || body.sessionId.length < 1 || body.sessionId.length > 255) {
    return c.json({ error: 'Invalid sessionId' }, 400);
  }

  const id = c.env.COUNTER_DO.idFromName('mixify-live-counter');
  const stub = c.env.COUNTER_DO.get(id);

  const response = await stub.fetch('http://do/stop', {
    method: 'POST',
    body: JSON.stringify({ sessionId: body.sessionId }),
  });

  return new Response(response.body, response);
});

app.get('/api/stats/live', async (c) => {
  const id = c.env.COUNTER_DO.idFromName('mixify-live-counter');
  const stub = c.env.COUNTER_DO.get(id);

  const response = await stub.fetch('http://do/live', { method: 'GET' });
  const data = await response.json();

  return c.json(data, 200, { 'Cache-Control': 'no-store' });
});

app.get('/api/stats/live/badge.svg', async (c) => {
  const id = c.env.COUNTER_DO.idFromName('mixify-live-counter');
  const stub = c.env.COUNTER_DO.get(id);

  const response = await stub.fetch('http://do/live', { method: 'GET' });
  const data = await response.json() as { listeningNow: number };
  const count = data.listeningNow;
  const countStr = String(count);

  const labelText = 'LISTENING NOW';
  const labelWidth = 118;
  const countWidth = Math.max(32, countStr.length * 10 + 16);
  const totalWidth = labelWidth + countWidth;

  const labelX = Math.round(labelWidth / 2);
  const countX = Math.round(labelWidth + countWidth / 2);

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${totalWidth}" height="28" role="img" aria-label="${labelText}: ${countStr}">
  <title>${labelText}: ${countStr}</title>
  <g shape-rendering="crispEdges">
    <rect width="${labelWidth}" height="28" fill="#0d1117"/>
    <rect x="${labelWidth}" width="${countWidth}" height="28" fill="#ff4775"/>
  </g>
  <g fill="#fff" text-anchor="middle" font-family="Verdana,Geneva,DejaVu Sans,sans-serif" text-rendering="geometricPrecision" font-size="10" font-weight="bold">
    <text x="${labelX}" y="18" fill="#fff">${labelText}</text>
    <text x="${countX}" y="18" fill="#fff">${countStr}</text>
  </g>
</svg>`;

  return c.html(svg, 200, {
    'Content-Type': 'image/svg+xml',
    'Cache-Control': 'no-store'
  });
});

export default app;
export { CounterDO } from './counter-do';
