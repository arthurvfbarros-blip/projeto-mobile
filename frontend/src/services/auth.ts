export class InvalidCredentialsError extends Error {
  remainingAttempts: number;

  constructor(remainingAttempts: number) {
    super('E-mail ou senha inválidos');
    this.name = 'InvalidCredentialsError';
    this.remainingAttempts = remainingAttempts;
  }
}

// Simulação temporária enquanto o backend não tem a rota de login:
// a senha "123456" entra com qualquer e-mail, qualquer outra senha falha.
export async function signIn(email: string, password: string): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, 1500));

  if (!email || password !== '123456') {
    throw new InvalidCredentialsError(3);
  }
}
