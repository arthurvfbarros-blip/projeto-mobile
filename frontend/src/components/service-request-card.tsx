import { StyleSheet, View } from 'react-native';
import { Button, Text } from 'react-native-paper';

import { AppIcon } from '@/components/app-icon';
import { useAppTheme } from '@/constants/theme';
import { type PedidoResumo } from '@/services/orders';
import { formatOrderDate, formatPublishedTime, orderStatusLabel } from '@/utils/order-validation';

type ServiceRequestCardProps = {
  pedido: PedidoResumo;
  onPress: () => void;
  accessibilityLabel: string;
  showStatus?: boolean;
};

export function ServiceRequestCard({
  pedido,
  onPress,
  accessibilityLabel,
  showStatus = false,
}: ServiceRequestCardProps) {
  const { colors } = useAppTheme();
  return (
    <View
      style={[
        styles.card,
        { borderColor: colors.outlineVariant, backgroundColor: colors.surface },
      ]}>
      <View style={styles.heading}>
        <Text variant="titleMedium" style={styles.title}>
          {pedido.titulo}
        </Text>
        {showStatus && (
          <Text
            variant="labelSmall"
            style={[
              styles.status,
              { backgroundColor: colors.secondaryContainer, color: colors.onSecondaryContainer },
            ]}>
            {orderStatusLabel(pedido.status)}
          </Text>
        )}
      </View>
      {pedido.cidade && (
        <View style={styles.location}>
          <AppIcon name="pin" size={16} />
          <Text
            variant="bodySmall"
            style={[styles.locationText, { color: colors.onSurfaceVariant }]}>
            {pedido.bairro ? pedido.bairro + ', ' : ''}
            {pedido.cidade} / {pedido.uf}
          </Text>
        </View>
      )}
      {showStatus && (
        <Text variant="bodySmall" style={{ color: colors.onSurfaceVariant }}>
          Data desejada: {formatOrderDate(pedido.dataDesejada)}
        </Text>
      )}
      <View style={styles.footer}>
        <View style={styles.metadata}>
          <Text
            variant="labelLarge"
            numberOfLines={1}
            style={[
              styles.category,
              { color: colors.onPrimaryContainer, backgroundColor: colors.primaryContainer },
            ]}>
            {pedido.categoria.nome}
          </Text>
          {pedido.criadoEm && (
            <Text variant="labelSmall" style={{ color: colors.onSurfaceVariant }}>
              {formatPublishedTime(pedido.criadoEm)}
            </Text>
          )}
        </View>
        <Button
          mode="contained"
          onPress={onPress}
          accessibilityLabel={accessibilityLabel}
          style={styles.action}>
          Ver pedido
        </Button>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: { padding: 16, gap: 12, borderWidth: 1, borderRadius: 12 },
  heading: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'flex-start', gap: 8 },
  title: { flexGrow: 1, flexShrink: 1, flexBasis: 160 },
  status: { paddingHorizontal: 8, paddingVertical: 4, borderRadius: 8 },
  location: { flexDirection: 'row', gap: 6, alignItems: 'center' },
  locationText: { flex: 1 },
  footer: { flexDirection: 'row', flexWrap: 'wrap', gap: 12, alignItems: 'center' },
  metadata: {
    flex: 1,
    minWidth: 116,
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'center',
    gap: 6,
  },
  category: { maxWidth: '100%', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 8 },
  action: { minWidth: 116 },
});
