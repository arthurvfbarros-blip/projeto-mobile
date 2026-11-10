import { getJson, postJson } from '@/services/api';
import { type NovoEndereco } from '@/services/addresses';
import { isCategoria, type Categoria } from '@/services/categories';

export type PedidoResumo = {
  idPedido: number;
  titulo: string;
  categoria: Categoria;
  status: string;
  dataDesejada: string | null;
  criadoEm: string | null;
  bairro: string | null;
  cidade: string | null;
  uf: string | null;
};

export type Pedido = Omit<PedidoResumo, 'bairro' | 'cidade' | 'uf'> & {
  descricao: string;
  endereco: NovoEndereco | null;
};

export type NovoPedido = {
  idCategoria: number;
  idEndereco: number;
  titulo: string;
  descricao: string;
  dataDesejada: string;
};

export type PaginaPedidos = {
  itens: PedidoResumo[];
  pagina: number;
  tamanho: number;
  totalItens: number;
  totalPaginas: number;
};

const PATH = '/clientes/me/pedidos';
const nullableString = (value: unknown) => value === null || typeof value === 'string';

function hasOrderFields(value: unknown): value is Omit<PedidoResumo, 'bairro' | 'cidade' | 'uf'> {
  if (typeof value !== 'object' || value === null) return false;
  const pedido = value as Partial<PedidoResumo>;
  return (
    Number.isInteger(pedido.idPedido) &&
    (pedido.idPedido ?? 0) > 0 &&
    typeof pedido.titulo === 'string' &&
    isCategoria(pedido.categoria) &&
    typeof pedido.status === 'string' &&
    nullableString(pedido.dataDesejada) &&
    nullableString(pedido.criadoEm)
  );
}

function isResumo(value: unknown): value is PedidoResumo {
  if (!hasOrderFields(value)) return false;
  const pedido = value as PedidoResumo;
  return (
    nullableString(pedido.bairro) && nullableString(pedido.cidade) && nullableString(pedido.uf)
  );
}

function isPedido(value: unknown): value is Pedido {
  if (!hasOrderFields(value)) return false;
  const pedido = value as Pedido;
  if (typeof pedido.descricao !== 'string') return false;
  if (pedido.endereco === null) return true;
  if (typeof pedido.endereco !== 'object' || !pedido.endereco) return false;
  const endereco = pedido.endereco;
  return (
    typeof endereco.cep === 'string' &&
    typeof endereco.logradouro === 'string' &&
    typeof endereco.numero === 'string' &&
    nullableString(endereco.complemento) &&
    typeof endereco.bairro === 'string' &&
    typeof endereco.cidade === 'string' &&
    typeof endereco.uf === 'string'
  );
}

export async function listOrders(accessToken: string, pagina = 0): Promise<PaginaPedidos> {
  const response = await getJson<PaginaPedidos | null>(
    `${PATH}?pagina=${pagina}&tamanho=10`,
    accessToken,
  );
  if (
    !response ||
    !Array.isArray(response.itens) ||
    !response.itens.every(isResumo) ||
    !Number.isInteger(response.pagina) ||
    response.pagina < 0 ||
    !Number.isInteger(response.tamanho) ||
    response.tamanho < 1 ||
    !Number.isInteger(response.totalItens) ||
    response.totalItens < 0 ||
    !Number.isInteger(response.totalPaginas) ||
    response.totalPaginas < 0
  ) {
    throw new Error('Não foi possível carregar seus pedidos. Tente novamente.');
  }
  return response;
}

export async function getOrder(accessToken: string, idPedido: number): Promise<Pedido> {
  const response = await getJson<unknown>(`${PATH}/${idPedido}`, accessToken);
  if (!isPedido(response)) throw new Error('Não foi possível carregar o pedido. Tente novamente.');
  return response;
}

export async function createOrder(accessToken: string, data: NovoPedido): Promise<Pedido> {
  const response = await postJson<unknown>(PATH, data, accessToken);
  if (!isPedido(response)) {
    throw new Error(
      'O servidor não confirmou o envio. Confira Meus pedidos antes de tentar novamente.',
    );
  }
  return response;
}
