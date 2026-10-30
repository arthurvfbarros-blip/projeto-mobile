import { useState } from 'react';
import { Platform, StyleSheet, TextInput, View, type TextInputProps } from 'react-native';
import { Text } from 'react-native-paper';

import { useAppTheme } from '@/constants/theme';

type TextFieldProps = Omit<TextInputProps, 'style' | 'placeholderTextColor'> & {
  label: string;
  error?: string;
};

// Campo "Filled" do M3 como está no Figma: o rótulo fica sempre acima do valor,
// inclusive quando o campo está vazio e mostra o placeholder.
export function TextField({ label, error, onFocus, onBlur, ...rest }: TextFieldProps) {
  const { colors, fonts } = useAppTheme();
  const [focused, setFocused] = useState(false);

  const hasError = !!error;
  const accentColor = hasError ? colors.error : focused ? colors.primary : colors.onSurfaceVariant;

  return (
    <View style={styles.container}>
      <View
        style={[
          styles.field,
          {
            backgroundColor: hasError ? colors.errorContainer : colors.surfaceVariant,
            borderBottomColor: accentColor,
            borderBottomWidth: hasError || focused ? 2 : 1,
          },
        ]}>
        <Text variant="bodySmall" style={{ color: accentColor }}>
          {label}
        </Text>
        <TextInput
          {...rest}
          onFocus={(event) => {
            setFocused(true);
            onFocus?.(event);
          }}
          onBlur={(event) => {
            setFocused(false);
            onBlur?.(event);
          }}
          placeholderTextColor={colors.onSurfaceVariant}
          selectionColor={colors.primary}
          cursorColor={colors.primary}
          style={[styles.input, fonts.bodyLarge, { color: colors.onSurface }]}
        />
      </View>
      {hasError && (
        <Text variant="bodySmall" style={[styles.supportingText, { color: colors.error }]}>
          {error}
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: 4,
  },
  field: {
    gap: 2,
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderTopLeftRadius: 4,
    borderTopRightRadius: 4,
  },
  input: {
    height: 24,
    padding: 0,
    ...Platform.select({
      android: { includeFontPadding: false, textAlignVertical: 'center' },
      web: { outlineWidth: 0 },
    }),
  },
  supportingText: {
    paddingHorizontal: 16,
  },
});
