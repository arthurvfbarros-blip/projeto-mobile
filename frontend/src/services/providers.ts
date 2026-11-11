import { getJson, postJson } from '@/services/api';
import { isCategoria, type Categoria } from '@/services/categories';

export type ProviderSignUp = {
  nome: string;
  email: string;
  cpf: string;
  telefone: string;
  senha: string;
  idCategorias: number[];
  cidade: string;
  uf: string;
  bairros: string[];
  empresa?: {
    razaoSocial: string;
    nomeFantasia: string | null;
    cnpj: string;
  };
};

type ApprovalStatus = 'pendente' | 'aprovado' | 'reprovado';

export type ProviderProfile = {
  idPrestador: number;
  nome: string;
  statusAprovacao: ApprovalStatus;
  cidade: string | null;
  uf: string | null;
  bairros: string[];
  categorias: Categoria[];
};

export async function signUpProvider(data: ProviderSignUp): Promise<void> {
  const response = await postJson<{ idPrestador: number; statusAprovacao: ApprovalStatus } | null>(
    '/auth/cadastro/prestador',
    data,
  );

  if (
    !response ||
    !Number.isInteger(response.idPrestador) ||
    response.idPrestador <= 0 ||
    response.statusAprovacao !== 'pendente'
  ) {
    throw new Error('O servidor não confirmou o cadastro. Tente entrar para verificar sua conta.');
  }
}

export async function getProviderProfile(accessToken: string): Promise<ProviderProfile> {
  const profile = await getJson<ProviderProfile | null>('/prestadores/me', accessToken);
  if (
    !profile ||
    !Number.isInteger(profile.idPrestador) ||
    profile.idPrestador <= 0 ||
    typeof profile.nome !== 'string' ||
    !['pendente', 'aprovado', 'reprovado'].includes(profile.statusAprovacao) ||
    (profile.cidade !== null && typeof profile.cidade !== 'string') ||
    (profile.uf !== null && typeof profile.uf !== 'string') ||
    !Array.isArray(profile.bairros) ||
    !profile.bairros.every((bairro) => typeof bairro === 'string') ||
    !Array.isArray(profile.categorias) ||
    !profile.categorias.every(isCategoria)
  ) {
    throw new Error('O servidor não retornou um perfil de prestador válido.');
  }
  return profile;
}
