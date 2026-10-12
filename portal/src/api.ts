import type { Client, Order, Product, Session } from './types'

export class ApiError extends Error {
  constructor(public status: number, message: string) { super(message) }
}

export function createApi(session: Session, base = '/api') {
  async function call<T>(path: string, init: RequestInit = {}): Promise<T> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json', 'X-Role': session.role }
    if (session.clientId != null) headers['X-Client-Id'] = String(session.clientId)
    const res = await fetch(base + path, { ...init, headers })
    if (!res.ok) {
      const body = await res.json().catch(() => null)
      throw new ApiError(res.status, body?.message ?? res.statusText)
    }
    return res.json() as Promise<T>
  }
  const post = <T,>(path: string, body?: unknown) =>
    call<T>(path, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) })
  const put = <T,>(path: string, body: unknown) => call<T>(path, { method: 'PUT', body: JSON.stringify(body) })
  return {
    clients: () => call<Client[]>('/clients'),
    products: () => call<Product[]>('/products'),
    orders: () => call<Order[]>('/orders'),
    order: (id: number) => call<Order>(`/orders/${id}`),
    place: (lines: { sku: string; quantity: number }[]) => post<Order>('/orders', { lines }),
    edit: (id: number, lines: { sku: string; quantity: number }[]) => put<Order>(`/orders/${id}`, { lines }),
    cancel: (id: number) => post<Order>(`/orders/${id}/cancel`),
    advance: (id: number) => post<Order>(`/orders/${id}/advance`),
  }
}
export type Api = ReturnType<typeof createApi>
