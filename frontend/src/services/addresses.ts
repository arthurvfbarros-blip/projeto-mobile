import { getJson, postJson } from '@/services/api';

export type Endereco = {
  idEndereco: number;
  cep: string;
  logradouro: string;
  numero: string;
  complemento: string | null;
  bairro: string;
  cidade: string;
  uf: string;
};

export type NovoEndereco = Omit<Endereco, 'idEndereco'>;

const PATH = '/clientes/me/enderecos';

function isEndereco(value: unknown): value is Endereco {
  if (typeof value !== 'object' || value === null) return false;
  const endereco = value as Partial<Endereco>;
  return (
    Number.isInteger(endereco.idEndereco) &&
    (endereco.idEndereco ?? 0) > 0 &&
    typeof endereco.cep === 'string' &&
    typeof endereco.logradouro === 'string' &&
    typeof endereco.numero === 'string' &&
    typeof endereco.bairro === 'string' &&
    typeof endereco.cidade === 'string' &&
    typeof endereco.uf === 'string' &&
    (endereco.complemento === null || typeof endereco.complemento === 'string')
  );
}

export async function listAddresses(accessToken: string): Promise<Endereco[]> {
  const response = await getJson<unknown>(PATH, accessToken);
  if (!Array.isArray(response) || !response.every(isEndereco)) {
    throw new Error('Não foi possível carregar seus endereços. Tente novamente.');
  }
  return response;
}

export async function createAddress(accessToken: string, data: NovoEndereco): Promise<Endereco> {
  const response = await postJson<unknown>(PATH, data, accessToken);
  if (!isEndereco(response)) {
    throw new Error(
      'O servidor não confirmou o cadastro do endereço. Confira a lista antes de tentar novamente.',
    );
  }
  return response;
}
