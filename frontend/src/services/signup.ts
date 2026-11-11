import { postJson } from '@/services/api';

export type UserSignUp = {
  nome: string;
  email: string;
  celular: string;
  cpf: string;
  senha: string;
};

export type UserSignUpResponse = {
  idCliente: number;
  idUsuario: number;
  nome: string;
  email: string;
  tipoUsuario: 'cliente';
};

export async function signUpUser(data: UserSignUp): Promise<UserSignUpResponse> {
  const response = await postJson<UserSignUpResponse>('/auth/cadastro/cliente', data);

  if (
    !response ||
    !Number.isInteger(response.idCliente) || response.idCliente <= 0 ||
    !Number.isInteger(response.idUsuario) || response.idUsuario <= 0 ||
    typeof response.nome !== 'string' ||
    typeof response.email !== 'string' ||
    response.tipoUsuario !== 'cliente'
  ) {
    throw new Error('O servidor não confirmou os dados do cadastro.');
  }

  return response;
}
