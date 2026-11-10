import { Stack } from 'expo-router';

import { OrderDraftProvider } from '@/providers/order-draft-provider';

export default function ClientLayout() {
  return (
    <OrderDraftProvider>
      <Stack screenOptions={{ headerShown: false }} />
    </OrderDraftProvider>
  );
}
