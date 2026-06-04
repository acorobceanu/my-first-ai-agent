import type { SessionResponse } from './types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';
const SESSIONS_PATH = '/aptitude/sessions';

async function requestSession(path: string, init?: RequestInit): Promise<SessionResponse> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers,
    },
  });

  if (!response.ok) {
    const message = await readErrorMessage(response);
    throw new Error(message || `Request failed with status ${response.status}`);
  }

  return response.json() as Promise<SessionResponse>;
}

async function readErrorMessage(response: Response): Promise<string> {
  const contentType = response.headers.get('content-type') ?? '';

  if (contentType.includes('application/json')) {
    const body = (await response.json()) as { message?: string; detail?: string; error?: string };
    return body.message ?? body.detail ?? body.error ?? '';
  }

  return response.text();
}

export function createSession(): Promise<SessionResponse> {
  return requestSession(SESSIONS_PATH, { method: 'POST' });
}

export function getSession(sessionId: string): Promise<SessionResponse> {
  return requestSession(`${SESSIONS_PATH}/${sessionId}`);
}

export function submitAnswer(sessionId: string, answer: string): Promise<SessionResponse> {
  return requestSession(`${SESSIONS_PATH}/${sessionId}/answers`, {
    method: 'POST',
    body: JSON.stringify({ answer }),
  });
}
