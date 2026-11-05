import { onlyCnpjChars, onlyDigits } from '@/utils/masks';

export const PASSWORD_MIN_LENGTH = 8;
export const PASSWORD_MAX_LENGTH = 72;

export function requiredError(value: string, message: string): string | undefined {
  return value.trim() ? undefined : message;
}

export function emailError(value: string): string | undefined {
  if (!value.trim()) return 'Informe o e-mail';
  return isValidEmail(value) ? undefined : 'Informe um e-mail válido';
}

export function passwordError(value: string): string | undefined {
  if (!value) return 'Informe a senha';
  return value.length >= PASSWORD_MIN_LENGTH
    ? undefined
    : `A senha deve ter pelo menos ${PASSWORD_MIN_LENGTH} caracteres`;
}

export function phoneError(value: string, noun: string): string | undefined {
  if (!value) return `Informe o ${noun}`;
  return isValidPhone(value) ? undefined : `Informe o ${noun} com DDD`;
}

export function cpfError(value: string): string | undefined {
  if (!value) return 'Informe o CPF';
  return isValidCpf(value) ? undefined : 'Informe um CPF válido';
}

export function cnpjError(value: string): string | undefined {
  if (!value) return 'Informe o CNPJ';
  return isValidCnpj(value) ? undefined : 'Informe um CNPJ válido';
}

function isValidEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim());
}

function isValidPhone(value: string): boolean {
  const length = onlyDigits(value).length;
  return length === 10 || length === 11;
}

// No CNPJ alfanumérico cada caractere vale seu código ASCII menos 48.
function checkDigit(chars: string, weights: number[]): number {
  const sum = weights.reduce((total, weight, i) => total + (chars.charCodeAt(i) - 48) * weight, 0);
  const rest = sum % 11;
  return rest < 2 ? 0 : 11 - rest;
}

function isValidCpf(value: string): boolean {
  const cpf = onlyDigits(value);
  if (cpf.length !== 11 || /^(\d)\1+$/.test(cpf)) return false;

  const first = checkDigit(cpf, [10, 9, 8, 7, 6, 5, 4, 3, 2]);
  const second = checkDigit(cpf, [11, 10, 9, 8, 7, 6, 5, 4, 3, 2]);
  return cpf.endsWith(`${first}${second}`);
}

function isValidCnpj(value: string): boolean {
  const cnpj = onlyCnpjChars(value);
  if (!/^[0-9A-Z]{12}\d{2}$/.test(cnpj) || /^(.)\1+$/.test(cnpj)) return false;

  const first = checkDigit(cnpj, [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]);
  const second = checkDigit(cnpj, [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]);
  return cnpj.endsWith(`${first}${second}`);
}
