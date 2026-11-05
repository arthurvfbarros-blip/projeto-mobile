export function onlyDigits(value: string): string {
  return value.replace(/\D/g, '');
}

export function onlyCnpjChars(value: string): string {
  return value.toUpperCase().replace(/[^0-9A-Z]/g, '');
}

function applyMask(chars: string, mask: string): string {
  let result = '';
  let index = 0;
  for (const symbol of mask) {
    if (index >= chars.length) break;
    result += symbol === '0' ? chars[index++] : symbol;
  }
  return result;
}

export function formatCpf(value: string): string {
  return applyMask(onlyDigits(value), '000.000.000-00');
}

export function formatCnpj(value: string): string {
  return applyMask(onlyCnpjChars(value), '00.000.000/0000-00');
}

export function formatPhone(value: string): string {
  const digits = onlyDigits(value).slice(0, 11);
  return applyMask(digits, digits.length > 10 ? '(00) 00000-0000' : '(00) 0000-0000');
}
