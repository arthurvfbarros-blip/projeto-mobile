import { postJson } from '@/services/api';
import { saveSession, type Session } from '@/services/session';

type LoginResponse = {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  usuario: Session['usuario'];
};

export async function signIn(email: string, password: string): Promise<Session> {
  const response = await postJson<LoginResponse>('/auth/login', { email, senha: password });

  const session: Session = {
    accessToken: response.accessToken,
    expiresAt: Date.now() + response.expiresIn * 1000,
    usuario: response.usuario,
  };
  await saveSession(session);
  return session;
}
