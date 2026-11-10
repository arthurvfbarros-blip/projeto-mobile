import { postJson } from '@/services/api';
import { isSession, saveSession, type Session } from '@/services/session';

type LoginResponse = {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  usuario: Session['usuario'];
};

export async function signIn(email: string, password: string): Promise<Session> {
  const response = await postJson<LoginResponse>('/auth/login', { email, senha: password });

  if (!response || response.tokenType !== 'Bearer' || !Number.isFinite(response.expiresIn) || response.expiresIn <= 0) {
    throw new Error('O servidor não confirmou os dados de acesso.');
  }

  const session: Session = {
    accessToken: response.accessToken,
    expiresAt: Date.now() + response.expiresIn * 1000,
    usuario: response.usuario,
  };
  if (!isSession(session)) throw new Error('O servidor não confirmou os dados de acesso.');
  await saveSession(session);
  return session;
}
