export type Role = 'ENTERPRISE_CLIENT' | 'WAREHOUSE_OPERATOR'
export type OrderStatus = 'SUBMITTED' | 'CONFIRMED' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'
export interface Client { id: number; name: string; discountBp: number | null; hasContract: boolean }
export interface Product { sku: string; name: string; unitPriceCents: number }
export interface Line { sku: string; name: string; quantity: number; unitPriceCents: number; subtotalCents: number }
export interface History { status: OrderStatus; changedAt: string }
export interface Order {
  id: number; clientId: number; clientName: string; status: OrderStatus
  grossCents: number; discountBp: number; netCents: number; totalUnits: number
  createdAt: string; lines: Line[]; history: History[]
}
export interface Session { role: Role; clientId?: number }
