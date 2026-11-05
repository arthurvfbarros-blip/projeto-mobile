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

  const session: Session = JSON.parse(stored);
  if (session.expiresAt <= Date.now()) {
    await clearSession();
    return null;
  }
  return session;
}

export async function clearSession(): Promise<void> {
  await storage.deleteItemAsync(SESSION_KEY);
}
