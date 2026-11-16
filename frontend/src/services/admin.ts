import { getJson, postJson } from '@/services/api';
import { isProviderProfile, type ApprovalStatus, type ProviderProfile } from '@/services/providers';

export type ProviderSummary = { perfil: ProviderProfile; ativo: boolean };
export type ProviderReview = ProviderSummary & {
  email: string;
  telefone: string | null;
  analise: {
    decisao: 'aprovado' | 'reprovado';
    motivo: string | null;
    analisadaEm: string;
    responsavel: string;
  } | null;
};
export type ProviderPage = {
  itens: ProviderSummary[];
  pagina: number;
  tamanho: number;
  totalItens: number;
  totalPaginas: number;
};

export const approvalLabels: Record<ApprovalStatus, string> = {
  pendente: 'Pendente', aprovado: 'Aprovado', reprovado: 'Reprovado',
};

export async function listProviders(token: string, status: ApprovalStatus, pagina: number): Promise<ProviderPage> {
  const data = await getJson<ProviderPage>(`/admin/prestadores?status=${status}&pagina=${pagina}&tamanho=10`, token);
  if (!data || !Array.isArray(data.itens) || !data.itens.every(p =>
    p && isProviderProfile(p.perfil) && typeof p.ativo === 'boolean') ||
    ![data.pagina, data.tamanho, data.totalItens, data.totalPaginas].every(n => Number.isInteger(n) && n >= 0)) {
    throw new Error('O servidor não retornou uma lista de prestadores válida.');
  }
  return data;
}

function validateReview(data: ProviderReview): ProviderReview {
  if (!data || !isProviderProfile(data.perfil) || typeof data.ativo !== 'boolean' ||
    typeof data.email !== 'string' || (data.telefone !== null && typeof data.telefone !== 'string') ||
    (data.analise !== null && (!data.analise ||
      !['aprovado', 'reprovado'].includes(data.analise.decisao) ||
      typeof data.analise.responsavel !== 'string' || !Number.isFinite(Date.parse(data.analise.analisadaEm)) ||
      (data.analise.motivo !== null && typeof data.analise.motivo !== 'string')))) {
    throw new Error('O servidor não retornou os dados da análise.');
  }
  return data;
}

export async function getProviderReview(token: string, id: number): Promise<ProviderReview> {
  return validateReview(await getJson<ProviderReview>(`/admin/prestadores/${id}`, token));
}

export async function reviewProvider(token: string, id: number, decisao: 'aprovado' | 'reprovado', motivo: string): Promise<ProviderReview> {
  return validateReview(await postJson<ProviderReview>(`/admin/prestadores/${id}/analise`,
    { decisao, motivo: decisao === 'reprovado' ? motivo.trim() : null }, token));
}
