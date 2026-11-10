import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

const SESSION_KEY = 'appservicos.session';

export type TipoUsuario = 'cliente' | 'prestador';

export type Session = {
  accessToken: string;
  expiresAt: number;
  usuario: {
    id: number;
    email: string;
    tipoUsuario: TipoUsuario;
  };
};

const storage =
  Platform.OS === 'web'
    ? {
        getItemAsync: async (key: string) => localStorage.getItem(key),
        setItemAsync: async (key: string, value: string) => localStorage.setItem(key, value),
        deleteItemAsync: async (key: string) => localStorage.removeItem(key),
      }
    : SecureStore;

export async function saveSession(session: Session): Promise<void> {
  await storage.setItemAsync(SESSION_KEY, JSON.stringify(session));
}

export async function getSession(): Promise<Session | null> {
  const stored = await storage.getItemAsync(SESSION_KEY);
  if (!stored) return null;

  let session: unknown;
  try {
    session = JSON.parse(stored);
  } catch {
    await clearSession();
    return null;
  }
  if (!isSession(session) || session.expiresAt <= Date.now()) {
    await clearSession();
    return null;
  }
  return session;
}

export function isSession(value: unknown): value is Session {
  if (typeof value !== 'object' || value === null) return false;
  const session = value as Partial<Session>;
  return (
    typeof session.accessToken === 'string' && session.accessToken.length > 0 &&
    typeof session.expiresAt === 'number' && Number.isFinite(session.expiresAt) &&
    typeof session.usuario === 'object' && session.usuario !== null &&
    Number.isInteger(session.usuario.id) && session.usuario.id > 0 &&
    typeof session.usuario.email === 'string' &&
    (session.usuario.tipoUsuario === 'cliente' || session.usuario.tipoUsuario === 'prestador')
  );
}

export async function clearSession(): Promise<void> {
  await storage.deleteItemAsync(SESSION_KEY);
}
