import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import StatusBadge from './StatusBadge'
import Stepper from './Stepper'
import ConfirmModal from './ConfirmModal'
import { ToastProvider, useToast } from './Toast'
import type { OrderStatus } from '../types'

const BADGE: [OrderStatus, string, string][] = [
  ['SUBMITTED', 'Submitted', 'bg-blue-100'], ['CONFIRMED', 'Confirmed', 'bg-sky-100'],
  ['PROCESSING', 'Processing', 'bg-amber-100'], ['SHIPPED', 'Shipped', 'bg-indigo-100'],
  ['DELIVERED', 'Delivered', 'bg-emerald-100'], ['CANCELLED', 'Cancelled', 'bg-red-100'],
]

describe('StatusBadge', () => {
  it.each(BADGE)('%s renders label and color', (status, label, cls) => {
    render(<StatusBadge status={status} />)
    expect(screen.getByTestId('status-badge')).toHaveTextContent(label)
    expect(screen.getByTestId('status-badge')).toHaveClass(cls)
  })
})

describe('Stepper', () => {
  const states = (s: OrderStatus) =>
    ['SUBMITTED', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED'].map(x => screen.getByTestId(`step-${x}`).dataset.state)
  it.each([
    ['SUBMITTED', ['current', 'future', 'future', 'future', 'future']],
    ['CONFIRMED', ['done', 'current', 'future', 'future', 'future']],
    ['PROCESSING', ['done', 'done', 'current', 'future', 'future']],
    ['SHIPPED', ['done', 'done', 'done', 'current', 'future']],
    ['DELIVERED', ['done', 'done', 'done', 'done', 'done']],
  ] as [OrderStatus, string[]][])('%s step states', (status, expected) => {
    render(<Stepper status={status} />)
    expect(states(status)).toEqual(expected)
  })
  it('current step pulses and has the blue color', () => {
    render(<Stepper status="PROCESSING" />)
    expect(screen.getByTestId('step-PROCESSING')).toHaveClass('animate-pulse', 'bg-blue-600')
    expect(screen.getByTestId('step-SUBMITTED')).toHaveClass('bg-emerald-600')
    expect(screen.getByTestId('step-SHIPPED')).toHaveClass('bg-slate-200')
  })
  it('CANCELLED shows a banner instead of steps', () => {
    render(<Stepper status="CANCELLED" />)
    expect(screen.getByRole('alert')).toHaveTextContent(/cancelled/i)
    expect(screen.queryByTestId('step-SUBMITTED')).not.toBeInTheDocument()
  })
})

describe('ConfirmModal', () => {
  it('calls keep and confirm handlers', async () => {
    const keep = vi.fn(), ok = vi.fn()
    render(<ConfirmModal orderId={7} onKeep={keep} onConfirm={ok} />)
    expect(screen.getByText('Confirm Cancellation')).toBeInTheDocument()
    await userEvent.click(screen.getByText('Keep Order'))
    await userEvent.click(screen.getByText('Yes, Cancel Request'))
    expect(keep).toHaveBeenCalled(); expect(ok).toHaveBeenCalled()
  })
})

describe('Toast', () => {
  function Fire() { const n = useToast(); return <button onClick={() => n('Saved')}>go</button> }
  it('shows then fades after ~3s', () => {
    vi.useFakeTimers()
    render(<ToastProvider><Fire /></ToastProvider>)
    act(() => screen.getByText('go').click())
    expect(screen.getByRole('status')).toHaveTextContent('Saved')
    act(() => { vi.advanceTimersByTime(3100) })
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
    vi.useRealTimers()
  })
})
