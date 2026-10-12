import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import App from './App'

const clients = [
  { id: 1, name: 'Acme Corp', discountBp: 1000, hasContract: true },
  { id: 2, name: 'NoDeal Ltd', discountBp: null, hasContract: false },
]
beforeEach(() => {
  vi.stubGlobal('fetch', vi.fn().mockImplementation(async (url: string) => ({
    ok: true, json: async () => (url.endsWith('/clients') ? clients : []) })))
})
afterEach(() => vi.unstubAllGlobals())

describe('role-gated shell', () => {
  it('shows company dropdown and contract badge for Enterprise Client', async () => {
    render(<App />)
    expect(await screen.findByLabelText('Company')).toBeInTheDocument()
    expect(await screen.findByTestId('contract-badge')).toHaveTextContent('Tier 1 (10% Discount)')
    expect(screen.getByText('Enterprise Portal')).toBeInTheDocument()
    expect(screen.getByTestId('icon-tile')).toHaveClass('bg-blue-600')
  })
  it('shows no contract for a client without one', async () => {
    render(<App />)
    await userEvent.selectOptions(await screen.findByLabelText('Company'), '2')
    expect(screen.getByTestId('contract-badge')).toHaveTextContent('No contract')
  })
  it('hides company for Warehouse Operator with slate tile', async () => {
    render(<App />)
    await userEvent.selectOptions(await screen.findByLabelText('Role'), 'WAREHOUSE_OPERATOR')
    expect(screen.queryByLabelText('Company')).not.toBeInTheDocument()
    expect(screen.getByTestId('icon-tile')).toHaveClass('bg-slate-900')
  })
  it('shows coming soon for Invoice Department', async () => {
    render(<App />)
    await userEvent.selectOptions(await screen.findByLabelText('Role'), 'INVOICE_DEPARTMENT')
    expect(screen.getByText(/coming soon/i)).toBeInTheDocument()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })
})
