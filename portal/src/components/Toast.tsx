import { createContext, useCallback, useContext, useState, type ReactNode } from 'react'

type Notify = (message: string, kind?: 'success' | 'error') => void
const ToastContext = createContext<Notify>(() => {})
export const useToast = () => useContext(ToastContext)

interface Item { id: number; message: string; kind: 'success' | 'error' }
let seq = 0

export function ToastProvider({ children, duration = 3000 }: { children: ReactNode; duration?: number }) {
  const [items, setItems] = useState<Item[]>([])
  const notify = useCallback<Notify>((message, kind = 'success') => {
    const id = ++seq
    setItems(xs => [...xs, { id, message, kind }])
    setTimeout(() => setItems(xs => xs.filter(x => x.id !== id)), duration)
  }, [duration])
  return (
    <ToastContext.Provider value={notify}>
      {children}
      <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2 pointer-events-none">
        {items.map(t => (
          <div key={t.id} role="status" className="pointer-events-auto bg-slate-900 text-white px-4 py-2.5 rounded-xl shadow-2xl text-xs font-semibold flex items-center gap-2 border border-slate-700 max-w-md">
            <span className={t.kind === 'error' ? 'text-red-400' : 'text-emerald-400'}>{t.kind === 'error' ? '!' : '✓'}</span>
            <span>{t.message}</span>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}
