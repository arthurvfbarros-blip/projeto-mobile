import { Redirect } from 'expo-router';

import { useSession } from '@/providers/session-provider';

export default function Index() {
  const { session } = useSession();
  if (!session) return <Redirect href="/login" />;
  return <Redirect href={session.usuario.tipoUsuario === 'cliente' ? '/cliente' : '/prestador'} />;
}
