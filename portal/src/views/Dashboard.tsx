import { useCallback, useEffect, useState } from 'react'
import type { Api } from '../api'
import type { Order } from '../types'
import { dateOf, isActive, isCancellable, money } from '../format'
import Stepper from '../components/Stepper'
import StatusBadge from '../components/StatusBadge'
import ConfirmModal from '../components/ConfirmModal'
import { useToast } from '../components/Toast'

interface Props { api: Api; onNew: () => void; onView: (id: number) => void }

const btn = 'text-xs bg-white border border-slate-200 hover:bg-slate-100 text-slate-700 px-3 py-1.5 rounded-md font-semibold transition-colors shadow-sm'

export default function Dashboard({ api, onNew, onView }: Props) {
  const toast = useToast()
  const [orders, setOrders] = useState<Order[]>([])
  const [pending, setPending] = useState<number>()

  const load = useCallback(() => {
    api.orders().then(setOrders).catch(e => toast(e.message, 'error'))
  }, [api, toast])
  useEffect(load, [load])

  const active = orders.filter(o => isActive(o.status))
  const history = orders.filter(o => !isActive(o.status))

  async function confirmCancel() {
    const id = pending!
    setPending(undefined)
    try {
      await api.cancel(id)
      toast(`Order ORD-${id} was cancelled.`)
    } catch (e) {
      toast((e as Error).message, 'error')
    }
    load()
  }

  return (
    <>
      <div className="bg-white border border-slate-200 rounded-xl p-5 mb-6 shadow-sm flex justify-between items-center">
        <div>
          <h2 className="text-lg font-bold text-slate-900">Procurement Dashboard</h2>
          <p className="text-xs text-slate-500">Overview of active orders, lifecycle progression, and historical audit logs</p>
        </div>
        <button onClick={onNew} className="inline-flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white px-4 py-2.5 rounded-lg text-xs font-semibold shadow-sm transition-all">
          + Create New Order
        </button>
      </div>

      <section className="bg-white border border-slate-200 rounded-xl p-6 mb-6 shadow-sm">
        <div className="flex justify-between items-center mb-4 pb-2 border-b border-slate-100">
          <div>
            <h3 className="text-base font-bold text-slate-900">Active Procurement Tracker</h3>
            <p className="text-xs text-slate-500">Orders currently moving through fulfillment pipelines</p>
          </div>
          <span data-testid="active-count" className="bg-blue-100 text-blue-800 px-2.5 py-0.5 rounded-full text-xs font-semibold">{active.length} Active</span>
        </div>
        <div className="space-y-4">
          {active.length === 0 && (
            <div className="p-6 text-center text-slate-400 border border-dashed border-slate-200 rounded-lg text-sm">No active orders currently processing.</div>
          )}
          {active.map(o => (
            <div key={o.id} data-testid="active-card" className="border border-slate-200 rounded-lg p-4 bg-slate-50/50 space-y-3">
              <div className="flex justify-between items-start">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-slate-900 text-sm">ORD-{o.id}</span>
                    <span className="text-xs text-slate-400 font-mono">{dateOf(o.createdAt)}</span>
                  </div>
                  <div className="text-xs text-slate-600 mt-0.5">
                    {o.totalUnits} Items &bull; Total: <strong className="text-slate-900">{money(o.netCents)}</strong>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <button onClick={() => onView(o.id)} className={btn}>View Order</button>
                  {isCancellable(o.status) && (
                    <button onClick={() => setPending(o.id)} className="text-xs text-red-600 border border-red-200 hover:bg-red-50 px-2.5 py-1.5 rounded-md transition-colors font-medium">Cancel</button>
                  )}
                </div>
              </div>
              <Stepper status={o.status} />
            </div>
          ))}
        </div>
      </section>

      <section className="bg-white border border-slate-200 rounded-xl p-6 shadow-sm">
        <div className="mb-4 pb-2 border-b border-slate-100">
          <h3 className="text-base font-bold text-slate-900">Order History Log</h3>
          <p className="text-xs text-slate-500">Past orders that have been fulfilled or cancelled</p>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="text-xs uppercase text-slate-400 border-b border-slate-200">
                <th className="pb-3">Order ID</th><th className="pb-3">Submission Date</th><th className="pb-3">Volume</th>
                <th className="pb-3">Net Total</th><th className="pb-3">Fulfillment Status</th><th className="pb-3 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {history.length === 0 && <tr><td colSpan={6} className="py-4 text-center text-slate-400">No historical orders logged.</td></tr>}
              {history.map(o => (
                <tr key={o.id} className="hover:bg-slate-50 transition-colors">
                  <td className="py-2.5 font-semibold text-slate-800">ORD-{o.id}</td>
                  <td className="py-2.5 text-slate-500 text-xs">{dateOf(o.createdAt)}</td>
                  <td className="py-2.5 text-slate-700">{o.totalUnits} Units</td>
                  <td className="py-2.5 font-semibold text-slate-900">{money(o.netCents)}</td>
                  <td className="py-2.5"><StatusBadge status={o.status} /></td>
                  <td className="py-2.5 text-right"><button onClick={() => onView(o.id)} className={btn}>View Order</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {pending != null && <ConfirmModal orderId={pending} onKeep={() => setPending(undefined)} onConfirm={confirmCancel} />}
    </>
  )
}
