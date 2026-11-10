import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type PropsWithChildren,
} from 'react';
import { AppState } from 'react-native';

import { signIn } from '@/services/auth';
import { clearSession, getSession, type Session } from '@/services/session';

type SessionContextValue = {
  session: Session | null;
  loading: boolean;
  notice: string | null;
  authenticate: (email: string, password: string) => Promise<Session>;
  signOut: () => Promise<void>;
  expireSession: (accessToken: string) => Promise<void>;
  clearNotice: () => void;
};

const SessionContext = createContext<SessionContextValue | null>(null);

export function SessionProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState<Session | null>(null);
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState<string | null>(null);
  const currentSession = useRef<Session | null>(null);

  const updateSession = useCallback((value: Session | null) => {
    currentSession.current = value;
    setSession(value);
  }, []);

  useEffect(() => {
    let active = true;
    getSession()
      .then((stored) => {
        if (active) updateSession(stored);
      })
      .catch(() => {
        if (active) setNotice('Não foi possível recuperar sua sessão. Entre novamente.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [updateSession]);

  const authenticate = useCallback(
    async (email: string, password: string) => {
      const authenticated = await signIn(email, password);
      updateSession(authenticated);
      setNotice(null);
      return authenticated;
    },
    [updateSession],
  );

  const signOut = useCallback(async () => {
    await clearSession();
    updateSession(null);
    setNotice(null);
  }, [updateSession]);

  const expireSession = useCallback(
    async (accessToken: string) => {
      if (currentSession.current?.accessToken !== accessToken) return;
      updateSession(null);
      setNotice('Sua sessão expirou. Entre novamente.');
      await clearSession();
    },
    [updateSession],
  );

  useEffect(() => {
    if (!session) return;
    const checkExpiration = () => {
      if (session.expiresAt <= Date.now()) {
        void expireSession(session.accessToken).catch(() => {
          console.error('Não foi possível limpar a sessão expirada.');
        });
      }
    };
    const timeout = setTimeout(
      checkExpiration,
      Math.max(0, Math.min(session.expiresAt - Date.now(), 2_147_483_647)),
    );
    const subscription = AppState.addEventListener('change', (state) => {
      if (state === 'active') checkExpiration();
    });
    return () => {
      clearTimeout(timeout);
      subscription.remove();
    };
  }, [expireSession, session]);

  const clearNotice = useCallback(() => setNotice(null), []);

  return (
    <SessionContext.Provider
      value={{ session, loading, notice, authenticate, signOut, expireSession, clearNotice }}>
      {children}
    </SessionContext.Provider>
  );
}

export function useSession(): SessionContextValue {
  const context = useContext(SessionContext);
  if (!context) throw new Error('SessionProvider não encontrado.');
  return context;
}
