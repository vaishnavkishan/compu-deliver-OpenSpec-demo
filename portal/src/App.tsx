import { useEffect, useMemo, useState } from 'react'
import { createApi } from './api'
import type { Client } from './types'
import Header, { type UiRole } from './components/Header'
import { ToastProvider } from './components/Toast'
import Dashboard from './views/Dashboard'
import OrderPage from './views/OrderPage'
import WarehouseView from './views/WarehouseView'

type Page = { kind: 'dashboard' } | { kind: 'new' } | { kind: 'order'; id: number }

export default function App() {
  const [role, setRole] = useState<UiRole>('ENTERPRISE_CLIENT')
  const [clients, setClients] = useState<Client[]>([])
  const [clientId, setClientId] = useState<number>()
  const [page, setPage] = useState<Page>({ kind: 'dashboard' })

  const api = useMemo(
    () => createApi(role === 'WAREHOUSE_OPERATOR' ? { role } : { role: 'ENTERPRISE_CLIENT', clientId }),
    [role, clientId])

  useEffect(() => {
    createApi({ role: 'WAREHOUSE_OPERATOR' }).clients().then(cs => {
      setClients(cs)
      setClientId(id => id ?? cs[0]?.id)
    }).catch(() => {})
  }, [])

  const back = () => setPage({ kind: 'dashboard' })
  const client = clients.find(c => c.id === clientId)

  return (
    <ToastProvider>
      <div className="p-4 md:p-6 max-w-7xl mx-auto min-h-screen bg-slate-50 text-slate-900 font-sans">
        <Header role={role} onRole={r => { setRole(r); back() }} clients={clients} clientId={clientId}
          onClient={id => { setClientId(id); back() }} />
        {role === 'INVOICE_DEPARTMENT' && (
          <section className="bg-white border border-slate-200 rounded-xl p-10 shadow-sm text-center">
            <h2 className="text-lg font-bold text-slate-900">Invoice Department</h2>
            <p className="text-sm text-slate-500 mt-1">Coming soon. This portal is not available yet.</p>
          </section>
        )}
        {role === 'WAREHOUSE_OPERATOR' && <WarehouseView api={api} />}
        {role === 'ENTERPRISE_CLIENT' && clientId != null && (
          page.kind === 'dashboard'
            ? <Dashboard api={api} onNew={() => setPage({ kind: 'new' })} onView={id => setPage({ kind: 'order', id })} />
            : <OrderPage api={api} client={client} orderId={page.kind === 'order' ? page.id : undefined} onBack={back} />
        )}
      </div>
    </ToastProvider>
  )
}
