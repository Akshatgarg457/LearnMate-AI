import { createContext, useContext } from 'react'

export type ToastTone = 'info' | 'success' | 'error'
export type ToastApi = (message: string, tone?: ToastTone) => void

export const ToastContext = createContext<ToastApi>(() => {})

export function useToast(): ToastApi {
  return useContext(ToastContext)
}
