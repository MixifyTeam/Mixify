import type { Env } from './index';

const STALE_TIMEOUT_MS = 150 * 1000; // 150 seconds

export class CounterDO {
  state: DurableObjectState;

  constructor(state: DurableObjectState, env: Env) {
    this.state = state;
  }

  async cleanup(): Promise<number> {
    const now = Date.now();
    const stored = await this.state.storage.list<number>({ prefix: 'sess:' });
    const toDelete: string[] = [];
    let activeCount = 0;

    for (const [key, timestamp] of stored.entries()) {
      if (now - timestamp > STALE_TIMEOUT_MS) {
        toDelete.push(key);
      } else {
        activeCount++;
      }
    }

    if (toDelete.length > 0) {
      await this.state.storage.delete(toDelete);
    }

    return activeCount;
  }

  async fetch(request: Request): Promise<Response> {
    const url = new URL(request.url);
    const now = Date.now();

    if (url.pathname === '/heartbeat' && request.method === 'POST') {
      const body = await request.json() as { sessionId: string };
      await this.state.storage.put(`sess:${body.sessionId}`, now);
      const count = await this.cleanup();
      return new Response(JSON.stringify({ success: true, count }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      });
    }

    if (url.pathname === '/stop' && request.method === 'POST') {
      const body = await request.json() as { sessionId: string };
      await this.state.storage.delete(`sess:${body.sessionId}`);
      const count = await this.cleanup();
      return new Response(JSON.stringify({ success: true, count }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      });
    }

    if (url.pathname === '/live' && request.method === 'GET') {
      const count = await this.cleanup();
      return new Response(JSON.stringify({ listeningNow: count }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      });
    }

    return new Response('Not found', { status: 404 });
  }
}
