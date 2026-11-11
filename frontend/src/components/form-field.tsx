import { Image, type ImageProps } from 'expo-image';
import { useState } from 'react';
import { Platform, StyleSheet, TextInput, View, type TextInputProps } from 'react-native';
import { Text } from 'react-native-paper';

import { useAppTheme } from '@/constants/theme';

type FormFieldProps = Omit<TextInputProps, 'style' | 'placeholderTextColor'> & {
  label: string;
  icon?: ImageProps['source'];
  helperText?: string;
  error?: string;
  appearance?: 'filled' | 'outlined';
};

export function FormField({
  label,
  icon,
  helperText,
  error,
  appearance = 'filled',
  onFocus,
  onBlur,
  ...rest
}: FormFieldProps) {
  const { colors, fonts } = useAppTheme();
  const [focused, setFocused] = useState(false);

  const hasError = !!error;
  const highlighted = hasError || focused;
  const supportingText = error ?? helperText;

  return (
    <View style={styles.container}>
      <Text variant="labelLarge" style={{ color: hasError ? colors.error : colors.onSurface }}>
        {label}
      </Text>
      <View
        style={[
          styles.field,
          {
            backgroundColor: hasError
              ? colors.errorContainer
              : appearance === 'outlined'
                ? colors.surfaceContainerLowest
                : colors.surfaceVariant,
            borderColor: hasError
              ? colors.error
              : focused
                ? colors.primary
                : appearance === 'outlined'
                  ? colors.outlineVariant
                  : colors.outline,
            borderWidth: highlighted ? 2 : 1,
            paddingHorizontal: highlighted ? 15 : 16,
          },
        ]}>
        {icon && <Image source={icon} style={styles.icon} />}
        <TextInput
          {...rest}
          accessibilityLabel={label}
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
      {supportingText && (
        <Text
          variant="bodyMedium"
          style={{ color: hasError ? colors.error : colors.onSurfaceVariant }}>
          {supportingText}
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: 8,
  },
  field: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    height: 52,
    borderRadius: 12,
  },
  icon: {
    width: 18,
    height: 18,
  },
  input: {
    flex: 1,
    height: 24,
    padding: 0,
    ...Platform.select({
      android: { includeFontPadding: false, textAlignVertical: 'center' },
      web: { outlineWidth: 0, outlineStyle: 'solid' as const },
    }),
  },
});
