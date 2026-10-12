import { render, screen, within, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ApiError, type Api } from '../api'
import type { Order, OrderStatus } from '../types'
import { ToastProvider } from '../components/Toast'
import Dashboard from './Dashboard'
import OrderPage from './OrderPage'
import WarehouseView from './WarehouseView'

const order = (id: number, status: OrderStatus, extra: Partial<Order> = {}): Order => ({
  id, clientId: 1, clientName: 'Acme Corp', status, grossCents: 100000, discountBp: 1000, netCents: 90000,
  totalUnits: 5, createdAt: '2026-08-16T10:00:00Z',
  lines: [{ sku: 'HW-1', name: 'Blade', quantity: 5, unitPriceCents: 20000, subtotalCents: 100000 }],
  history: [{ status: 'SUBMITTED', changedAt: '2026-08-16T10:00:00Z' }], edits: [], ...extra,
})
const products = [
  { sku: 'HW-1', name: 'Blade Server', unitPriceCents: 20000 },
  { sku: 'HW-2', name: 'Switch', unitPriceCents: 5000 },
]
const mk = (o: Partial<Record<keyof Api, unknown>> = {}) => ({
  orders: vi.fn().mockResolvedValue([]), order: vi.fn(), products: vi.fn().mockResolvedValue(products),
  clients: vi.fn(), place: vi.fn(), edit: vi.fn(), cancel: vi.fn(), advance: vi.fn(), ...o }) as unknown as Api & Record<string, ReturnType<typeof vi.fn>>
const wrap = (ui: React.ReactNode) => render(<ToastProvider>{ui}</ToastProvider>)

describe('Dashboard', () => {
  it('shows empty state', async () => {
    wrap(<Dashboard api={mk()} onNew={vi.fn()} onView={vi.fn()} />)
    expect(await screen.findByText('No active orders currently processing.')).toBeInTheDocument()
    expect(screen.getByTestId('active-count')).toHaveTextContent('0 Active')
    expect(screen.getByText('No historical orders logged.')).toBeInTheDocument()
  })
  it('splits active and history, cancel only for SUBMITTED/CONFIRMED', async () => {
    const api = mk({ orders: vi.fn().mockResolvedValue([
      order(1, 'SUBMITTED'), order(2, 'CONFIRMED'), order(3, 'PROCESSING'), order(4, 'SHIPPED'), order(5, 'DELIVERED'), order(6, 'CANCELLED')]) })
    wrap(<Dashboard api={api} onNew={vi.fn()} onView={vi.fn()} />)
    const cards = await screen.findAllByTestId('active-card')
    expect(cards).toHaveLength(4)
    expect(screen.getByTestId('active-count')).toHaveTextContent('4 Active')
    const cancellable = cards.map(c => !!within(c).queryByText('Cancel'))
    expect(cancellable).toEqual([true, true, false, false])
    expect(screen.getByText('ORD-5').closest('tr')).toHaveTextContent('Delivered')
    expect(screen.getByText('ORD-6').closest('tr')).toHaveTextContent('Cancelled')
  })
  it('cancels through the modal', async () => {
    const api = mk({ orders: vi.fn().mockResolvedValue([order(1, 'SUBMITTED')]), cancel: vi.fn().mockResolvedValue(order(1, 'CANCELLED')) })
    wrap(<Dashboard api={api} onNew={vi.fn()} onView={vi.fn()} />)
    await userEvent.click(await screen.findByText('Cancel'))
    expect(api.cancel).not.toHaveBeenCalled()
    await userEvent.click(screen.getByText('Yes, Cancel Request'))
    expect(api.cancel).toHaveBeenCalledWith(1)
    expect(await screen.findByText('Order ORD-1 was cancelled.')).toBeInTheDocument()
  })
  it('Keep Order closes modal without cancelling', async () => {
    const api = mk({ orders: vi.fn().mockResolvedValue([order(1, 'CONFIRMED')]) })
    wrap(<Dashboard api={api} onNew={vi.fn()} onView={vi.fn()} />)
    await userEvent.click(await screen.findByText('Cancel'))
    await userEvent.click(screen.getByText('Keep Order'))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(api.cancel).not.toHaveBeenCalled()
  })
  it('create button calls onNew', async () => {
    const onNew = vi.fn()
    wrap(<Dashboard api={mk()} onNew={onNew} onView={vi.fn()} />)
    await userEvent.click(screen.getByText('+ Create New Order'))
    expect(onNew).toHaveBeenCalled()
  })
})

describe('OrderPage', () => {
  const client = { id: 1, name: 'Acme', discountBp: 1000, hasContract: true }
  async function pick(name: string) {
    await userEvent.click(screen.getByLabelText('Search products'))
    await userEvent.click(await screen.findByText(name, { exact: false, selector: 'button span' }))
  }
  it('picks a product and shows contract pricing', async () => {
    wrap(<OrderPage api={mk()} client={client} onBack={vi.fn()} />)
    await pick('Blade Server')
    await userEvent.clear(screen.getByLabelText('Quantity'))
    await userEvent.type(screen.getByLabelText('Quantity'), '5')
    expect(screen.getByTestId('gross')).toHaveTextContent('$1,000.00')
    expect(screen.getByTestId('discount')).toHaveTextContent('-$100.00')
    expect(screen.getByTestId('net')).toHaveTextContent('$900.00')
    expect(screen.getByText('Waived')).toBeInTheDocument()
  })
  it('search filters and shows no-match message', async () => {
    wrap(<OrderPage api={mk()} client={client} onBack={vi.fn()} />)
    await userEvent.type(screen.getByLabelText('Search products'), 'zzz')
    expect(await screen.findByText('No matching products found')).toBeInTheDocument()
  })
  it('adds and removes lines', async () => {
    wrap(<OrderPage api={mk()} client={client} onBack={vi.fn()} />)
    await userEvent.click(screen.getByText('Add Line Item'))
    expect(screen.getAllByLabelText('Quantity')).toHaveLength(2)
    await userEvent.click(screen.getAllByLabelText('Remove line')[0])
    expect(screen.getAllByLabelText('Quantity')).toHaveLength(1)
  })
  it('submits lines and returns to dashboard', async () => {
    const api = mk({ place: vi.fn().mockResolvedValue(order(9, 'SUBMITTED')) })
    const onBack = vi.fn()
    wrap(<OrderPage api={api} client={client} onBack={onBack} />)
    await pick('Switch')
    await userEvent.click(screen.getByText('Submit Enterprise Order'))
    expect(api.place).toHaveBeenCalledWith([{ sku: 'HW-2', quantity: 1 }])
    await waitFor(() => expect(onBack).toHaveBeenCalled())
  })
  it('shows API errors (no contract / invalid line) as toasts', async () => {
    const api = mk({ place: vi.fn().mockRejectedValue(new ApiError(400, 'Client has no contract')) })
    const onBack = vi.fn()
    wrap(<OrderPage api={api} client={client} onBack={onBack} />)
    await userEvent.click(screen.getByText('Submit Enterprise Order'))
    expect(await screen.findByRole('status')).toHaveTextContent('Client has no contract')
    expect(onBack).not.toHaveBeenCalled()
  })
  it('detail is read-only with locked submit and transition timestamps', async () => {
    const api = mk({ order: vi.fn().mockResolvedValue(order(3, 'CONFIRMED', { history: [
      { status: 'SUBMITTED', changedAt: '2026-08-16T10:00:00Z' }, { status: 'CONFIRMED', changedAt: '2026-08-16T11:30:00Z' }] })) })
    wrap(<OrderPage api={api} orderId={3} client={client} onBack={vi.fn()} />)
    expect(await screen.findByText('Order Details: ORD-3')).toBeInTheDocument()
    expect(await screen.findByLabelText('Quantity HW-1')).toBeDisabled()
    expect(screen.getByRole('button', { name: /Order Locked/ })).toBeDisabled()
    expect(screen.getByTestId('history')).toHaveTextContent('Confirmed: 2026-08-16 11:30:00')
    expect(screen.getByTestId('net')).toHaveTextContent('$900.00')
    expect(screen.queryByText('Add Line Item')).not.toBeInTheDocument()
  })
  it('shows Edit Order only for SUBMITTED orders', async () => {
    for (const status of ['CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'] as OrderStatus[]) {
      const { unmount } = wrap(<OrderPage api={mk({ order: vi.fn().mockResolvedValue(order(3, status)) })} orderId={3} client={client} onBack={vi.fn()} />)
      await screen.findByLabelText('Quantity HW-1')
      expect(screen.queryByText('Edit Order')).not.toBeInTheDocument()
      expect(screen.queryByText('Save Changes')).not.toBeInTheDocument()
      unmount()
    }
    wrap(<OrderPage api={mk({ order: vi.fn().mockResolvedValue(order(3, 'SUBMITTED')) })} orderId={3} client={client} onBack={vi.fn()} />)
    expect(await screen.findByText('Edit Order')).toBeInTheDocument()
  })
  it('edits a SUBMITTED order with a prefilled form and saves changes', async () => {
    const updated = order(3, 'SUBMITTED', { totalUnits: 7, grossCents: 140000, netCents: 126000,
      lines: [{ sku: 'HW-1', name: 'Blade Server', quantity: 7, unitPriceCents: 20000, subtotalCents: 140000 }],
      edits: [{ editedAt: '2026-08-16T12:15:00Z' }] })
    const api = mk({ order: vi.fn().mockResolvedValue(order(3, 'SUBMITTED')), edit: vi.fn().mockResolvedValue(updated) })
    wrap(<OrderPage api={api} orderId={3} client={client} onBack={vi.fn()} />)
    await userEvent.click(await screen.findByText('Edit Order'))
    expect(screen.queryByText('Edit Order')).not.toBeInTheDocument()
    const qty = await screen.findByLabelText('Quantity')
    expect(qty).toHaveValue(5)
    await userEvent.clear(qty)
    await userEvent.type(qty, '7')
    expect(screen.getByTestId('net')).toHaveTextContent('$1,260.00')
    await userEvent.click(screen.getByText('Save Changes'))
    expect(api.edit).toHaveBeenCalledWith(3, [{ sku: 'HW-1', quantity: 7 }])
    expect(await screen.findByRole('status')).toHaveTextContent('ORD-3 updated')
    expect(await screen.findByLabelText('Quantity HW-1')).toBeDisabled()
    expect(screen.getByTestId('edits')).toHaveTextContent('Edited: 2026-08-16 12:15:00')
  })
  it('shows API errors from a rejected edit as a toast and stays in edit mode', async () => {
    const api = mk({ order: vi.fn().mockResolvedValue(order(3, 'SUBMITTED')),
      edit: vi.fn().mockRejectedValue(new ApiError(409, 'Order 3 is CONFIRMED and can no longer be edited')) })
    wrap(<OrderPage api={api} orderId={3} client={client} onBack={vi.fn()} />)
    await userEvent.click(await screen.findByText('Edit Order'))
    await userEvent.click(screen.getByText('Save Changes'))
    expect(await screen.findByRole('status')).toHaveTextContent('can no longer be edited')
    expect(screen.getByText('Save Changes')).toBeInTheDocument()
  })
})

describe('WarehouseView', () => {
  it('lists all orders with search and count', async () => {
    const api = mk({ orders: vi.fn().mockResolvedValue([order(1, 'SUBMITTED'), order(2, 'SHIPPED', { clientName: 'Globex' })]) })
    wrap(<WarehouseView api={api} />)
    expect(await screen.findByTestId('queue-count')).toHaveTextContent('2 Orders')
    await userEvent.type(screen.getByLabelText('Search orders'), 'globex')
    expect(screen.getByTestId('queue-count')).toHaveTextContent('1 Orders')
    expect(screen.queryByText('ORD-1')).not.toBeInTheDocument()
  })
  it('advance button shows next status and advances one step', async () => {
    const api = mk({ orders: vi.fn().mockResolvedValue([order(1, 'SUBMITTED')]), advance: vi.fn().mockResolvedValue(order(1, 'CONFIRMED')) })
    wrap(<WarehouseView api={api} />)
    await userEvent.click(await screen.findByText('Advance to Confirmed'))
    expect(api.advance).toHaveBeenCalledWith(1)
    expect(await screen.findByText('Order ORD-1 moved to Confirmed.')).toBeInTheDocument()
  })
  it('no advance on DELIVERED or CANCELLED and no cancel control', async () => {
    const api = mk({ orders: vi.fn().mockResolvedValue([order(1, 'DELIVERED'), order(2, 'CANCELLED')]) })
    wrap(<WarehouseView api={api} />)
    await screen.findByText('ORD-1')
    expect(screen.queryByText(/Advance/)).not.toBeInTheDocument()
    expect(screen.queryByText(/Cancel$/)).not.toBeInTheDocument()
  })
  it('shows 409 as a toast', async () => {
    const api = mk({ orders: vi.fn().mockResolvedValue([order(1, 'SUBMITTED')]), advance: vi.fn().mockRejectedValue(new ApiError(409, 'Order was modified')) })
    wrap(<WarehouseView api={api} />)
    await userEvent.click(await screen.findByText('Advance to Confirmed'))
    expect(await screen.findByRole('status')).toHaveTextContent('Order was modified')
  })
})
