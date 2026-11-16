import { Redirect } from 'expo-router';

import { useSession } from '@/providers/session-provider';
import { homeRoute } from '@/services/session';

export default function Index() {
  const { session } = useSession();
  if (!session) return <Redirect href="/login" />;
  return <Redirect href={homeRoute(session.usuario.tipoUsuario)} />;
}
