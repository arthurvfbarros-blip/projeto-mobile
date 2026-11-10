import Constants from 'expo-constants';
import { Platform } from 'react-native';

const API_PORT = 8080;
const REQUEST_TIMEOUT_MS = 15_000;

function resolveBaseUrl(): string {
  const configured = process.env.EXPO_PUBLIC_API_URL;
  if (configured) return configured.replace(/\/+$/, '');

  const devHost = Constants.expoConfig?.hostUri?.split(':')[0];
  const host = Platform.OS === 'web' || !devHost ? 'localhost' : devHost;
  return `http://${host}:${API_PORT}`;
}

export const API_URL = resolveBaseUrl();

type ErrorBody = {
  mensagem: string;
  campos: Record<string, string>;
};

export class ApiError extends Error {
  status: number;
  fields: Record<string, string>;

  constructor(status: number, body: ErrorBody | null) {
    super(body?.mensagem ?? 'Ocorreu um erro inesperado. Tente novamente.');
    this.name = 'ApiError';
    this.status = status;
    this.fields = body?.campos ?? {};
  }
}

export class NetworkError extends Error {
  constructor() {
    super('Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.');
    this.name = 'NetworkError';
  }
}

function isErrorBody(data: unknown): data is ErrorBody {
  return typeof data === 'object' && data !== null && typeof (data as ErrorBody).mensagem === 'string';
}

async function requestJson<T>(path: string, method: 'GET' | 'POST', body?: unknown, accessToken?: string): Promise<T> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  let response: Response;
  let data: unknown;
  try {
    response = await fetch(`${API_URL}${path}`, {
      method,
      headers: {
        ...(method === 'POST' ? { 'Content-Type': 'application/json' } : {}),
        ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
      signal: controller.signal,
    });
    data = await response.json().catch((error: unknown) => {
      if (controller.signal.aborted) throw error;
      return null;
    });
  } catch {
    throw new NetworkError();
  } finally {
    clearTimeout(timeout);
  }

  if (!response.ok) {
    throw new ApiError(response.status, isErrorBody(data) ? data : null);
  }
  return data as T;
}

export function postJson<T>(path: string, body: unknown, accessToken?: string): Promise<T> {
  return requestJson<T>(path, 'POST', body, accessToken);
}

export function getJson<T>(path: string, accessToken?: string): Promise<T> {
  return requestJson<T>(path, 'GET', undefined, accessToken);
}
