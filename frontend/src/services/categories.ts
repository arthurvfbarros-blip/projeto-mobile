import { getJson } from '@/services/api';

export type Categoria = {
  idCategoria: number;
  nome: string;
  descricao: string | null;
};

export function isCategoria(value: unknown): value is Categoria {
  if (typeof value !== 'object' || value === null) return false;
  const categoria = value as Partial<Categoria>;
  return (
    Number.isInteger(categoria.idCategoria) &&
    (categoria.idCategoria ?? 0) > 0 &&
    typeof categoria.nome === 'string' &&
    (categoria.descricao === null || typeof categoria.descricao === 'string')
  );
}

export async function listCategories(accessToken?: string): Promise<Categoria[]> {
  const response = await getJson<unknown>('/categorias', accessToken);
  if (!Array.isArray(response) || !response.every(isCategoria)) {
    throw new Error('Não foi possível carregar as categorias. Tente novamente.');
  }
  return response;
}
