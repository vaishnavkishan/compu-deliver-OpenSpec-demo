import type { OrderStatus } from './types'

export const money = (cents: number) =>
  (cents / 100).toLocaleString('en-US', { style: 'currency', currency: 'USD' })

export const dateOf = (iso: string) => iso.slice(0, 10)

export const pct = (bp: number) => `${bp / 100}%`

export const STATUS_LABEL: Record<OrderStatus, string> = {
  SUBMITTED: 'Submitted', CONFIRMED: 'Confirmed', PROCESSING: 'Processing',
  SHIPPED: 'Shipped', DELIVERED: 'Delivered', CANCELLED: 'Cancelled',
}

export const STATUS_FLOW: OrderStatus[] = ['SUBMITTED', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED']

export const nextStatus = (s: OrderStatus): OrderStatus | null => {
  const i = STATUS_FLOW.indexOf(s)
  return i >= 0 && i < STATUS_FLOW.length - 1 ? STATUS_FLOW[i + 1] : null
}

export const isCancellable = (s: OrderStatus) => s === 'SUBMITTED' || s === 'CONFIRMED'
export const isActive = (s: OrderStatus) => s !== 'DELIVERED' && s !== 'CANCELLED'
