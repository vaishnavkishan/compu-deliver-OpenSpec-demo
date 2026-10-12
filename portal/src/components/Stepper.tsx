import type { OrderStatus } from '../types'
import { STATUS_FLOW, STATUS_LABEL } from '../format'

export default function Stepper({ status, large = false }: { status: OrderStatus; large?: boolean }) {
  if (status === 'CANCELLED') {
    return (
      <div role="alert" className="w-full bg-red-50 border border-red-200 text-red-700 text-xs p-3 rounded-md font-semibold text-center">
        This order was cancelled and is no longer moving through fulfillment pipelines.
      </div>
    )
  }
  const current = STATUS_FLOW.indexOf(status)
  const circle = large ? 'w-7 h-7 text-xs' : 'w-6 h-6 text-[10px]'
  return (
    <div className="relative flex justify-between items-center py-2">
      <div className="absolute top-1/2 left-8 right-8 h-0.5 bg-slate-200 -translate-y-1/2 z-0" />
      {STATUS_FLOW.map((s, i) => {
        // Delivered is terminal, so it shows as completed rather than pulsing.
        const done = i < current || (i === current && s === 'DELIVERED')
        const color = done ? 'bg-emerald-600 text-white'
          : i === current ? 'bg-blue-600 text-white animate-pulse'
          : 'bg-slate-200 text-slate-500'
        return (
          <div key={s} className="relative z-10 flex flex-col items-center gap-1 bg-white px-1">
            <div data-testid={`step-${s}`} data-state={done ? 'done' : i === current ? 'current' : 'future'}
              className={`${circle} rounded-full flex items-center justify-center font-bold ${color}`}>{i + 1}</div>
            <span className={`${large ? 'text-xs' : 'text-[10px]'} font-medium text-slate-600`}>{STATUS_LABEL[s]}</span>
          </div>
        )
      })}
    </div>
  )
}
