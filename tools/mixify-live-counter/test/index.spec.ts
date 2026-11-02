import { env, createExecutionContext, waitOnExecutionContext } from 'cloudflare:test';
import { describe, it, expect } from 'vitest';
import worker from '../src/index';

const IncomingRequest = Request<unknown, IncomingRequestCfProperties>;

describe('Mixify Live Counter Worker', () => {
  it('responds with 0 live users initially', async () => {
    const ctx = createExecutionContext();
    const request = new IncomingRequest('http://example.com/api/stats/live');
    const response = await worker.fetch(request, env, ctx);
    await waitOnExecutionContext(ctx);

    expect(response.status).toBe(200);
    expect(response.headers.get('Cache-Control')).toBe('no-store');
    const body = await response.json();
    expect(body).toEqual({ listeningNow: 0 });
  });

  it('adds sessions and updates the count with deduplication', async () => {
    const ctx = createExecutionContext();

    // First heartbeat for user-1
    const req1 = new IncomingRequest('http://example.com/api/listening/heartbeat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId: 'user-1' })
    });
    const res1 = await worker.fetch(req1, env, ctx);
    expect(res1.status).toBe(200);
    await res1.json();

    // Duplicate heartbeat for user-1 (deduplication check)
    const req1Dup = new IncomingRequest('http://example.com/api/listening/heartbeat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId: 'user-1' })
    });
    const res1Dup = await worker.fetch(req1Dup, env, ctx);
    expect(res1Dup.status).toBe(200);
    await res1Dup.json();

    // Heartbeat for user-2
    const req2 = new IncomingRequest('http://example.com/api/listening/heartbeat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId: 'user-2' })
    });
    const res2 = await worker.fetch(req2, env, ctx);
    expect(res2.status).toBe(200);
    await res2.json();

    // Query live stats
    const reqStats = new IncomingRequest('http://example.com/api/stats/live');
    const resStats = await worker.fetch(reqStats, env, ctx);
    await waitOnExecutionContext(ctx);

    expect(resStats.status).toBe(200);
    const body = await resStats.json();
    expect(body).toEqual({ listeningNow: 2 });
  });

  it('removes a session on stop', async () => {
    const ctx = createExecutionContext();

    // Add user-1 and user-2
    const req1 = new IncomingRequest('http://example.com/api/listening/heartbeat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId: 'user-1' })
    });
    await (await worker.fetch(req1, env, ctx)).json();

    const req2 = new IncomingRequest('http://example.com/api/listening/heartbeat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId: 'user-2' })
    });
    await (await worker.fetch(req2, env, ctx)).json();

    // Stop user-1
    const reqStop = new IncomingRequest('http://example.com/api/listening/stop', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId: 'user-1' })
    });
    const resStop = await worker.fetch(reqStop, env, ctx);
    expect(resStop.status).toBe(200);
    await resStop.json();

    // Verify count drops to 1
    const reqStats = new IncomingRequest('http://example.com/api/stats/live');
    const resStats = await worker.fetch(reqStats, env, ctx);
    await waitOnExecutionContext(ctx);

    expect(resStats.status).toBe(200);
    const body = await resStats.json();
    expect(body).toEqual({ listeningNow: 1 });
  });

  it('rejects invalid sessionId', async () => {
    const ctx = createExecutionContext();
    const req = new IncomingRequest('http://example.com/api/listening/heartbeat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId: '' })
    });
    const res = await worker.fetch(req, env, ctx);
    await waitOnExecutionContext(ctx);

    expect(res.status).toBe(400);
    await res.json();
  });

  it('returns valid SVG badge with current count', async () => {
    const ctx = createExecutionContext();

    // Add 3 sessions
    for (let i = 1; i <= 3; i++) {
      const req = new IncomingRequest('http://example.com/api/listening/heartbeat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sessionId: `badge-user-${i}` })
      });
      await (await worker.fetch(req, env, ctx)).json();
    }

    const reqBadge = new IncomingRequest('http://example.com/api/stats/live/badge.svg');
    const resBadge = await worker.fetch(reqBadge, env, ctx);
    await waitOnExecutionContext(ctx);

    expect(resBadge.status).toBe(200);
    expect(resBadge.headers.get('Content-Type')).toBe('image/svg+xml');
    expect(resBadge.headers.get('Cache-Control')).toBe('no-store');
    const svgText = await resBadge.text();
    expect(svgText).toContain('<svg');
    expect(svgText).toContain('LISTENING NOW');
    expect(svgText).toContain('3');
  });
});
