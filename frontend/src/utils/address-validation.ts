import { onlyDigits } from '@/utils/masks';
import { requiredError } from '@/utils/validators';

export type AddressForm = {
  cep: string;
  logradouro: string;
  numero: string;
  complemento: string;
  bairro: string;
  cidade: string;
  uf: string;
};

export type AddressErrors = Partial<Record<keyof AddressForm, string>>;

const UFS = [
  'AC',
  'AL',
  'AP',
  'AM',
  'BA',
  'CE',
  'DF',
  'ES',
  'GO',
  'MA',
  'MT',
  'MS',
  'MG',
  'PA',
  'PB',
  'PR',
  'PE',
  'PI',
  'RJ',
  'RN',
  'RS',
  'RO',
  'RR',
  'SC',
  'SP',
  'SE',
  'TO',
];

function textError(value: string, label: string, max: number): string | undefined {
  return (
    requiredError(value, `Informe ${label}`) ??
    (value.trim().length > max ? `Use no máximo ${max} caracteres` : undefined)
  );
}

export function validateAddress(form: AddressForm): AddressErrors {
  return {
    cep:
      requiredError(form.cep, 'Informe o CEP') ??
      (onlyDigits(form.cep).length === 8 ? undefined : 'Informe um CEP com 8 dígitos'),
    logradouro: textError(form.logradouro, 'o logradouro', 150),
    numero: textError(form.numero, 'o número ou S/N', 20),
    complemento: form.complemento.trim().length > 100 ? 'Use no máximo 100 caracteres' : undefined,
    bairro: textError(form.bairro, 'o bairro', 100),
    cidade: textError(form.cidade, 'a cidade', 100),
    uf:
      requiredError(form.uf, 'Informe a UF') ??
      (UFS.includes(form.uf.trim().toUpperCase()) ? undefined : 'Informe uma UF válida'),
  };
}
