import { type ComponentProps } from 'react';
import { Button } from 'react-native-paper';

import { useAppTheme } from '@/constants/theme';

type SubmitButtonProps = Omit<ComponentProps<typeof Button>, 'mode' | 'theme'>;

export function SubmitButton({ loading, disabled, ...props }: SubmitButtonProps) {
  const { colors } = useAppTheme();
  return (
    <Button
      {...props}
      mode="contained"
      loading={loading}
      disabled={disabled || loading}
      theme={
        loading
          ? {
              colors: { surfaceDisabled: colors.primary, onSurfaceDisabled: colors.onPrimary },
            }
          : undefined
      }
    />
  );
}
