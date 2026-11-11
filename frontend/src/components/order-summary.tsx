import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { useAppTheme } from '@/constants/theme';
import { type NovoEndereco } from '@/services/addresses';
import { formatCep } from '@/utils/masks';
import { formatOrderDate } from '@/utils/order-validation';

type Props = {
  titulo: string;
  descricao: string;
  categoria: string;
  dataDesejada: string | null;
  endereco: NovoEndereco | null;
  showTitle?: boolean;
};

export function OrderSummary({
  titulo,
  descricao,
  categoria,
  dataDesejada,
  endereco,
  showTitle = true,
}: Props) {
  const { colors } = useAppTheme();
  return (
    <View style={styles.container}>
      <View style={styles.section}>
        <Text variant="labelLarge" style={{ color: colors.onSurfaceVariant }}>
          Categoria do serviço
        </Text>
        <Text variant="bodyLarge">{categoria}</Text>
      </View>
      {showTitle && (
        <View style={styles.section}>
          <Text variant="labelLarge" style={{ color: colors.onSurfaceVariant }}>
            Título do pedido
          </Text>
          <Text variant="bodyLarge">{titulo}</Text>
        </View>
      )}
      <View style={styles.section}>
        <Text variant="labelLarge" style={{ color: colors.onSurfaceVariant }}>
          Descrição
        </Text>
        <Text variant="bodyLarge">{descricao}</Text>
      </View>
      <View style={styles.section}>
        <Text variant="labelLarge" style={{ color: colors.onSurfaceVariant }}>
          Data desejada
        </Text>
        <Text variant="bodyLarge">{formatOrderDate(dataDesejada)}</Text>
      </View>
      <View style={styles.section}>
        <Text variant="labelLarge" style={{ color: colors.onSurfaceVariant }}>
          Endereço do serviço
        </Text>
        {endereco ? (
          <>
            <Text variant="bodyLarge">
              {endereco.logradouro}, {endereco.numero}
            </Text>
            {endereco.complemento ? <Text variant="bodyMedium">{endereco.complemento}</Text> : null}
            <Text variant="bodyMedium">
              {endereco.bairro} · {endereco.cidade} / {endereco.uf}
            </Text>
            <Text variant="bodyMedium">CEP {formatCep(endereco.cep)}</Text>
          </>
        ) : (
          <Text variant="bodyLarge">Não informado</Text>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({ container: { gap: 20 }, section: { gap: 6 } });
