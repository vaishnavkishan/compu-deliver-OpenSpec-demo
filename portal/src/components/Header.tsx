import type { Client } from '../types'
import { pct } from '../format'

export type UiRole = 'ENTERPRISE_CLIENT' | 'WAREHOUSE_OPERATOR' | 'INVOICE_DEPARTMENT'

const ROLES: { value: UiRole; label: string }[] = [
  { value: 'ENTERPRISE_CLIENT', label: 'Enterprise Client' },
  { value: 'WAREHOUSE_OPERATOR', label: 'Warehouse Operator' },
  { value: 'INVOICE_DEPARTMENT', label: 'Invoice Department' },
]

const PILL: Record<UiRole, string> = {
  ENTERPRISE_CLIENT: 'Enterprise Portal',
  WAREHOUSE_OPERATOR: 'Warehouse Portal',
  INVOICE_DEPARTMENT: 'Invoice Portal',
}

interface Props {
  role: UiRole
  onRole: (r: UiRole) => void
  clients: Client[]
  clientId?: number
  onClient: (id: number) => void
}

const box = 'flex items-center gap-2 bg-slate-50 border border-slate-200 rounded-lg px-3 py-1.5 text-xs font-semibold text-slate-700'
const sel = 'bg-transparent font-bold text-slate-800 focus:outline-none cursor-pointer'

export default function Header({ role, onRole, clients, clientId, onClient }: Props) {
  const client = clients.find(c => c.id === clientId)
  const tile = role === 'WAREHOUSE_OPERATOR' ? 'bg-slate-900' : 'bg-blue-600'
  return (
    <header className="bg-white border border-slate-200 rounded-xl p-4 mb-6 shadow-sm flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
      <div className="flex items-center gap-3">
        <div data-testid="icon-tile" className={`${tile} text-white p-2.5 rounded-lg shadow`}>
          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
          </svg>
        </div>
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-xl font-bold text-slate-900 tracking-tight">Enterprise Hardware Procurement Portal</h1>
            <span className="bg-blue-100 text-blue-800 text-[11px] font-bold px-2 py-0.5 rounded border border-blue-200">{PILL[role]}</span>
          </div>
          <p className="text-xs text-slate-500">Bulk Procurement &amp; Dynamic Pricing Engine</p>
        </div>
      </div>
      <div className="flex flex-wrap items-center gap-3">
        <label className={box}>
          <span className="text-slate-400">Role:</span>
          <select aria-label="Role" value={role} onChange={e => onRole(e.target.value as UiRole)} className={sel}>
            {ROLES.map(r => <option key={r.value} value={r.value}>{r.label}</option>)}
          </select>
        </label>
        {role === 'ENTERPRISE_CLIENT' && (
          <>
            <label className={box}>
              <span className="text-slate-400">Company:</span>
              <select aria-label="Company" value={clientId ?? ''} onChange={e => onClient(Number(e.target.value))} className={sel}>
                {clients.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </label>
            {client && (
              <div data-testid="contract-badge" className="flex items-center gap-2 bg-blue-50 border border-blue-200 text-blue-700 rounded-lg px-3 py-1.5 text-xs font-semibold">
                <span className="font-bold">Contract #{client.id}</span>
                <span className="text-blue-300">&bull;</span>
                <span className="text-blue-600 font-medium">
                  {client.hasContract && client.discountBp != null ? `Tier 1 (${pct(client.discountBp)} Discount)` : 'No contract'}
                </span>
              </div>
            )}
          </>
        )}
      </div>
    </header>
  )
}
