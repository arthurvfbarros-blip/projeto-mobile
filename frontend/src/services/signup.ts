import { ApiError, NetworkError, postJson } from '@/services/api';

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

export type CompanySignUp = {
  razaoSocial: string;
  nomeFantasia: string;
  cnpj: string;
  email: string;
  telefone: string;
  endereco: string;
  nomeResponsavel: string;
  cpfResponsavel: string;
  senha: string;
};

async function simulateCompanySignUp(email: string): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, 1500));

  if (email === 'offline@example.com') {
    throw new NetworkError();
  }
  if (email === 'existente@example.com') {
    throw new ApiError(409, {
      mensagem: 'E-mail já cadastrado',
      campos: { email: 'Este e-mail já está cadastrado' },
    });
  }
}

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

export async function signUpCompany(data: CompanySignUp): Promise<void> {
  await simulateCompanySignUp(data.email);
}
