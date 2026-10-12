import type { OrderStatus } from '../types'
import { STATUS_LABEL } from '../format'

const COLORS: Record<OrderStatus, string> = {
  SUBMITTED: 'bg-blue-100 text-blue-800 border-blue-200',
  CONFIRMED: 'bg-sky-100 text-sky-800 border-sky-200',
  PROCESSING: 'bg-amber-100 text-amber-800 border-amber-200',
  SHIPPED: 'bg-indigo-100 text-indigo-800 border-indigo-200',
  DELIVERED: 'bg-emerald-100 text-emerald-800 border-emerald-200',
  CANCELLED: 'bg-red-100 text-red-800 border-red-200',
}

export default function StatusBadge({ status }: { status: OrderStatus }) {
  return (
    <span data-testid="status-badge" className={`inline-block px-2 py-0.5 rounded text-[11px] font-bold border ${COLORS[status]}`}>
      {STATUS_LABEL[status]}
    </span>
  )
}
