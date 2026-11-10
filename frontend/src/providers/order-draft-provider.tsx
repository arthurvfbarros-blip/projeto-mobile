import { createContext, useCallback, useContext, useState, type PropsWithChildren } from 'react';

import { type OrderDraft } from '@/utils/order-validation';

type DraftContextValue = {
  draft: OrderDraft;
  step: number;
  updateDraft: (changes: Partial<OrderDraft>) => void;
  setStep: (step: number) => void;
  resetDraft: () => void;
};

const emptyDraft: OrderDraft = {
  idCategoria: null,
  titulo: '',
  descricao: '',
  idEndereco: null,
  dataDesejada: '',
};
const DraftContext = createContext<DraftContextValue | null>(null);

export function OrderDraftProvider({ children }: PropsWithChildren) {
  const [draft, setDraft] = useState<OrderDraft>(emptyDraft);
  const [step, setStep] = useState(1);
  const updateDraft = useCallback(
    (changes: Partial<OrderDraft>) => setDraft((current) => ({ ...current, ...changes })),
    [],
  );
  const resetDraft = useCallback(() => {
    setDraft(emptyDraft);
    setStep(1);
  }, []);
  return (
    <DraftContext.Provider value={{ draft, step, updateDraft, setStep, resetDraft }}>
      {children}
    </DraftContext.Provider>
  );
}

export function useOrderDraft(): DraftContextValue {
  const context = useContext(DraftContext);
  if (!context) throw new Error('OrderDraftProvider não encontrado.');
  return context;
}
