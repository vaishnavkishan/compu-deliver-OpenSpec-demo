import { useCallback, useEffect, useState } from 'react'
import type { Api } from '../api'
import type { Order } from '../types'
import { dateOf, money, nextStatus, STATUS_LABEL } from '../format'
import StatusBadge from '../components/StatusBadge'
import { useToast } from '../components/Toast'

export default function WarehouseView({ api }: { api: Api }) {
  const toast = useToast()
  const [orders, setOrders] = useState<Order[]>([])
  const [search, setSearch] = useState('')

  const load = useCallback(() => {
    api.orders().then(setOrders).catch(e => toast(e.message, 'error'))
  }, [api, toast])
  useEffect(load, [load])

  async function advance(o: Order) {
    try {
      const next = await api.advance(o.id)
      toast(`Order ORD-${o.id} moved to ${STATUS_LABEL[next.status]}.`)
    } catch (e) {
      toast((e as Error).message, 'error')
    }
    load()
  }

  const q = search.toLowerCase()
  const shown = orders.filter(o => `ord-${o.id}`.includes(q) || o.clientName.toLowerCase().includes(q))

  return (
    <section className="bg-white border border-slate-200 rounded-xl p-6 shadow-sm">
      <div className="flex justify-between items-center mb-4 pb-2 border-b border-slate-100">
        <div>
          <h3 className="text-base font-bold text-slate-900">Order Queue</h3>
          <p className="text-xs text-slate-500">All client orders and their fulfillment progress</p>
        </div>
        <span data-testid="queue-count" className="bg-amber-100 text-amber-800 text-xs font-bold px-2.5 py-0.5 rounded-full">{shown.length} Orders</span>
      </div>
      <input aria-label="Search orders" value={search} onChange={e => setSearch(e.target.value)} placeholder="Search Order ID or client..."
        className="w-full max-w-sm mb-4 px-3 py-1.5 text-xs border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" />
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="text-xs uppercase text-slate-400 border-b border-slate-200">
              <th className="py-3 px-4">Order ID &amp; Date</th><th className="py-3 px-4">Client Account</th><th className="py-3 px-4">Volume</th>
              <th className="py-3 px-4">Net Total</th><th className="py-3 px-4">Status</th><th className="py-3 px-4 text-right">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {shown.length === 0 && <tr><td colSpan={6} className="py-6 text-center text-slate-400 text-xs">No matching orders found in queue.</td></tr>}
            {shown.map(o => {
              const next = nextStatus(o.status)
              return (
                <tr key={o.id} className="hover:bg-slate-50 transition-colors">
                  <td className="py-3 px-4"><div className="font-semibold text-slate-800">ORD-{o.id}</div><div className="text-xs text-slate-400 font-mono">{dateOf(o.createdAt)}</div></td>
                  <td className="py-3 px-4 text-slate-700">{o.clientName}</td>
                  <td className="py-3 px-4 text-xs font-medium text-slate-700">{o.totalUnits} Units</td>
                  <td className="py-3 px-4 font-semibold text-slate-900">{money(o.netCents)}</td>
                  <td className="py-3 px-4"><StatusBadge status={o.status} /></td>
                  <td className="py-3 px-4 text-right">
                    {next && (
                      <button onClick={() => advance(o)} className="text-xs bg-slate-900 hover:bg-slate-700 text-white px-3 py-1.5 rounded-md font-semibold">
                        Advance to {STATUS_LABEL[next]}
                      </button>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </section>
  )
}
