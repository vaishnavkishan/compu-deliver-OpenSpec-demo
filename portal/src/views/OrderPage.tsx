import { useEffect, useMemo, useState } from 'react'
import type { Api } from '../api'
import type { Client, Order, Product } from '../types'
import { dateOf, money, nextStatus, pct, STATUS_LABEL } from '../format'
import Stepper from '../components/Stepper'
import StatusBadge from '../components/StatusBadge'
import { useToast } from '../components/Toast'

interface Row { key: number; sku?: string; query: string; quantity: number | '' }
interface Props { api: Api; orderId?: number; client?: Client; operator?: boolean; onBack: () => void }

let rowSeq = 0
const qtyOf = (r: Row) => Math.max(1, r.quantity || 1)
const blank = (): Row => ({ key: ++rowSeq, query: '', quantity: 1 })

// Mirrors the server's half-up rounding so the preview matches the stored order.
const discountOf = (gross: number, bp: number) => Math.floor((gross * bp + 5000) / 10000)

export default function OrderPage({ api, orderId, client, operator, onBack }: Props) {
  const toast = useToast()
  const [products, setProducts] = useState<Product[]>([])
  const [rows, setRows] = useState<Row[]>([blank()])
  const [order, setOrder] = useState<Order>()
  const [open, setOpen] = useState<number>()
  const [editing, setEditing] = useState(false)
  const readOnly = orderId != null && !editing

  useEffect(() => { api.products().then(setProducts).catch(e => toast(e.message, 'error')) }, [api, toast])
  useEffect(() => {
    if (orderId == null) return
    api.order(orderId).then(setOrder).catch(e => toast(e.message, 'error'))
  }, [api, orderId, toast])

  const bySku = useMemo(() => new Map(products.map(p => [p.sku, p])), [products])

  const view = order && {
    lines: order.lines.map(l => ({ key: l.sku, name: l.name, sku: l.sku, unit: l.unitPriceCents, qty: l.quantity, sub: l.subtotalCents })),
    units: order.totalUnits, gross: order.grossCents, bp: order.discountBp, net: order.netCents,
  }
  const draft = useMemo(() => {
    const chosen = rows.filter(r => r.sku && bySku.has(r.sku))
    const lines = chosen.map(r => {
      const p = bySku.get(r.sku!)!
      return { key: r.key, name: p.name, sku: p.sku, unit: p.unitPriceCents, qty: qtyOf(r), sub: p.unitPriceCents * qtyOf(r) }
    })
    const gross = lines.reduce((s, l) => s + l.sub, 0)
    const bp = client?.discountBp ?? 0
    return { lines, units: lines.reduce((s, l) => s + l.qty, 0), gross, bp, net: gross - discountOf(gross, bp) }
  }, [rows, bySku, client])
  const totals = readOnly ? view : draft

  const update = (key: number, patch: Partial<Row>) => setRows(rs => rs.map(r => (r.key === key ? { ...r, ...patch } : r)))

  function startEdit() {
    if (!order) return
    setRows(order.lines.map(l => ({ key: ++rowSeq, sku: l.sku, query: l.name, quantity: l.quantity })))
    setEditing(true)
  }

  async function advance() {
    if (!order) return
    try {
      const next = await api.advance(order.id)
      setOrder(next)
      toast(`Order ORD-${order.id} moved to ${STATUS_LABEL[next.status]}.`)
    } catch (e) {
      toast((e as Error).message, 'error')
    }
  }

  async function submit() {
    const lines = rows.filter(r => r.sku).map(r => ({ sku: r.sku!, quantity: qtyOf(r) }))
    try {
      if (orderId != null) {
        setOrder(await api.edit(orderId, lines))
        setEditing(false)
        toast(`Order ORD-${orderId} updated.`)
        return
      }
      const placed = await api.place(lines)
      toast(`Order ORD-${placed.id} submitted.`)
      onBack()
    } catch (e) {
      toast((e as Error).message, 'error')
    }
  }

  const title = readOnly ? `Order Details: ORD-${orderId}` : 'Create New Enterprise Order'
  return (
    <>
      <div className="bg-white border border-slate-200 rounded-xl p-5 mb-6 shadow-sm flex flex-wrap items-center gap-3">
        <button onClick={onBack} aria-label="Back" className="text-xs border border-slate-200 hover:bg-slate-100 text-slate-700 px-3 py-1.5 rounded-md font-semibold">&larr; Back</button>
        <h2 className="text-lg font-bold text-slate-900">{title}</h2>
        {order && operator && <span data-testid="client-name" className="text-sm text-slate-500">{order.clientName}</span>}
        {order ? <StatusBadge status={order.status} /> : !readOnly && <span className="px-2.5 py-0.5 rounded text-xs font-bold bg-blue-100 text-blue-800">Draft</span>}
      </div>

      <section className="bg-white border border-slate-200 rounded-xl p-6 mb-6 shadow-sm">
        <h3 className="text-sm font-bold text-slate-900 mb-3">Order Lifecycle Progression</h3>
        <Stepper status={order?.status ?? 'SUBMITTED'} large />
        {order && order.edits?.length > 0 && (
          <ul data-testid="edits" className="mt-2 flex flex-wrap gap-x-6 gap-y-1 text-xs text-slate-500">
            {order.edits.map((e, i) => (
              <li key={i}>Edited: <span className="font-mono">{e.editedAt.replace('T', ' ').slice(0, 19)}</span></li>
            ))}
          </ul>
        )}
        {order && order.history.length > 0 && (
          <ul data-testid="history" className="mt-3 flex flex-wrap gap-x-6 gap-y-1 text-xs text-slate-500">
            {order.history.map((h, i) => (
              <li key={i}>{STATUS_LABEL[h.status]}: <span className="font-mono">{h.changedAt.replace('T', ' ').slice(0, 19)}</span></li>
            ))}
          </ul>
        )}
      </section>

      <main className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <section className="bg-white border border-slate-200 rounded-xl p-6 shadow-sm">
            <div className="flex justify-between items-center mb-4 pb-3 border-b border-slate-100">
              <div>
                <h3 className="text-lg font-semibold text-slate-900">Order Line Items</h3>
                <p className="text-xs text-slate-500">Search master catalog or enter quantities</p>
              </div>
              {!readOnly && <button onClick={() => setRows(rs => [...rs, blank()])} className="bg-blue-600 hover:bg-blue-700 text-white px-3 py-2 rounded-md text-xs font-semibold shadow-sm">Add Line Item</button>}
            </div>
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="text-xs uppercase text-slate-500 border-b border-slate-200 bg-slate-50/50">
                  <th className="py-3 px-3 w-1/2">Item Specification / Master Catalog Search</th>
                  <th className="py-3 px-3">Unit Price</th><th className="py-3 px-3 text-center">Quantity</th>
                  <th className="py-3 px-3 text-right">Subtotal</th>{!readOnly && <th className="py-3 px-3 text-center">Action</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {readOnly && view?.lines.map(l => (
                  <tr key={l.key}>
                    <td className="py-3 px-3"><div className="font-semibold text-slate-800">{l.name}</div><span className="text-[10px] bg-slate-100 text-slate-600 px-1.5 py-0.5 rounded font-mono">{l.sku}</span></td>
                    <td className="py-3 px-3">{money(l.unit)}</td>
                    <td className="py-3 px-3 text-center"><input disabled value={l.qty} readOnly aria-label={`Quantity ${l.sku}`} className="w-20 text-center border border-slate-200 rounded px-2 py-1 bg-slate-50" /></td>
                    <td className="py-3 px-3 text-right font-semibold">{money(l.sub)}</td>
                  </tr>
                ))}
                {!readOnly && rows.map(r => {
                  const p = r.sku ? bySku.get(r.sku) : undefined
                  const q = r.query.toLowerCase()
                  const matches = products.filter(x => x.name.toLowerCase().includes(q) || x.sku.toLowerCase().includes(q))
                  return (
                    <tr key={r.key}>
                      <td className="py-3 px-3 relative">
                        {p ? (
                          <div className="flex items-center gap-2">
                            <span className="font-semibold text-slate-800">{p.name}</span>
                            <span className="text-[10px] bg-slate-100 text-slate-600 px-1.5 py-0.5 rounded font-mono">{p.sku}</span>
                            <button onClick={() => update(r.key, { sku: undefined, query: '' })} className="text-xs text-blue-600 hover:underline">Change</button>
                          </div>
                        ) : (
                          <>
                            <input aria-label="Search products" placeholder="Search by name or SKU..." value={r.query}
                              onFocus={() => setOpen(r.key)} onChange={e => { update(r.key, { query: e.target.value }); setOpen(r.key) }}
                              className="w-full border border-slate-300 rounded-md px-3 py-1.5 text-xs focus:outline-none focus:ring-2 focus:ring-blue-500" />
                            {open === r.key && (
                              <ul className="absolute z-20 left-3 right-3 mt-1 bg-white border border-slate-200 rounded-lg shadow-lg max-h-56 overflow-auto">
                                {matches.length === 0 && <li className="px-3 py-2 text-xs text-slate-400">No matching products found</li>}
                                {matches.map(x => (
                                  <li key={x.sku}>
                                    <button onClick={() => { update(r.key, { sku: x.sku, query: x.name }); setOpen(undefined) }}
                                      className="w-full text-left px-3 py-2 text-xs hover:bg-blue-50 flex items-center justify-between gap-2">
                                      <span>{x.name} <span className="text-[10px] bg-slate-100 text-slate-600 px-1.5 py-0.5 rounded font-mono">{x.sku}</span></span>
                                      <span className="font-semibold">{money(x.unitPriceCents)}</span>
                                    </button>
                                  </li>
                                ))}
                              </ul>
                            )}
                          </>
                        )}
                      </td>
                      <td className="py-3 px-3">{p ? money(p.unitPriceCents) : '-'}</td>
                      <td className="py-3 px-3 text-center">
                        <input type="number" min={1} aria-label="Quantity" value={r.quantity}
                          onChange={e => update(r.key, { quantity: e.target.value === '' ? '' : Math.max(1, Math.floor(Number(e.target.value)) || 1) })}
                          className="w-20 text-center border border-slate-300 rounded px-2 py-1" />
                      </td>
                      <td className="py-3 px-3 text-right font-semibold">{p ? money(p.unitPriceCents * qtyOf(r)) : '-'}</td>
                      <td className="py-3 px-3 text-center">
                        <button aria-label="Remove line" onClick={() => setRows(rs => rs.filter(x => x.key !== r.key))} className="text-slate-400 hover:text-red-600">&times;</button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
            {!readOnly && (
              <div className="mt-4 pt-3 border-t border-slate-100">
                <button onClick={() => setRows(rs => [...rs, blank()])} className="w-full py-2.5 border-2 border-dashed border-slate-300 hover:border-blue-500 hover:bg-blue-50/50 text-slate-600 hover:text-blue-700 rounded-lg text-xs font-semibold">+ Add Product / Line Item</button>
              </div>
            )}
          </section>
        </div>

        <div className="lg:col-span-1">
          <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-sm sticky top-6">
            <h3 className="text-lg font-bold text-slate-900 mb-4 pb-2 border-b border-slate-100">Contract Net Pricing</h3>
            <div className="space-y-3 text-sm">
              <div className="flex justify-between text-slate-600"><span>Total Selected Items</span><span data-testid="units" className="font-semibold text-slate-800">{totals?.units ?? 0} Units</span></div>
              <div className="flex justify-between text-slate-600"><span>Gross Subtotal</span><span data-testid="gross" className="font-medium text-slate-800">{money(totals?.gross ?? 0)}</span></div>
              <div className="flex justify-between text-slate-600">
                <span className="flex items-center gap-1">Contract Discount
                  <span className="text-[10px] bg-emerald-100 text-emerald-800 px-1.5 py-0.5 rounded font-bold">{pct(totals?.bp ?? 0)}</span></span>
                <span data-testid="discount" className="text-emerald-600 font-semibold">-{money((totals?.gross ?? 0) - (totals?.net ?? 0))}</span>
              </div>
              <div className="flex justify-between text-slate-600"><span>Logistics &amp; Freight</span><span className="text-emerald-600 font-medium">Waived</span></div>
              <div className="pt-4 border-t-2 border-dashed border-slate-200 flex justify-between items-baseline">
                <span className="text-base font-bold text-slate-900">Final Net Total</span>
                <span data-testid="net" className="text-xl font-extrabold text-blue-600">{money(totals?.net ?? 0)}</span>
              </div>
            </div>
            <div className="mt-6 space-y-2">
              {operator && order && nextStatus(order.status) && (
                <button onClick={advance} className="w-full font-semibold py-3 px-4 rounded-lg shadow-sm text-sm bg-slate-900 hover:bg-slate-700 text-white">
                  Advance to {STATUS_LABEL[nextStatus(order.status)!]}
                </button>
              )}
              {!operator && order?.status === 'SUBMITTED' && !editing && (
                <button onClick={startEdit} className="w-full font-semibold py-3 px-4 rounded-lg shadow-sm text-sm bg-white border border-blue-600 text-blue-600 hover:bg-blue-50">Edit Order</button>
              )}
              {editing && (
                <button onClick={() => setEditing(false)} className="w-full font-semibold py-2 px-4 rounded-lg text-sm border border-slate-200 text-slate-700 hover:bg-slate-100">Discard Changes</button>
              )}
              {!operator && <button onClick={submit} disabled={readOnly}
                className={`w-full font-semibold py-3 px-4 rounded-lg shadow-sm text-sm ${readOnly ? 'bg-slate-300 text-slate-500 cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-700 text-white'}`}>
                {readOnly ? `Order Locked (${order ? dateOf(order.createdAt) : 'Read Only'})` : editing ? 'Save Changes' : 'Submit Enterprise Order'}
              </button>}
            </div>
          </div>
        </div>
      </main>
    </>
  )
}
