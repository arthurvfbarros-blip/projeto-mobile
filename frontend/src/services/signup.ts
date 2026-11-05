import { ApiError, NetworkError } from '@/services/api';

export type UserSignUp = {
  nome: string;
  email: string;
  celular: string;
  cpf: string;
  senha: string;
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

// Simulação até o backend ter as rotas. E-mails de teste: existente@example.com e offline@example.com.
async function simulateSignUp(email: string): Promise<void> {
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

export async function signUpUser(data: UserSignUp): Promise<void> {
  await simulateSignUp(data.email);
}

export async function signUpCompany(data: CompanySignUp): Promise<void> {
  await simulateSignUp(data.email);
}
