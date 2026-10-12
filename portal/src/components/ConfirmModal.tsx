interface Props { orderId: number; onKeep: () => void; onConfirm: () => void }

export default function ConfirmModal({ orderId, onKeep, onConfirm }: Props) {
  return (
    <div role="dialog" aria-modal="true" className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm" onClick={onKeep} />
      <div className="bg-white rounded-xl border border-slate-200 shadow-2xl z-10 w-full max-w-sm p-6 relative m-4 text-center">
        <div className="w-12 h-12 bg-red-100 text-red-600 rounded-full flex items-center justify-center mx-auto mb-3 text-xl font-bold">!</div>
        <h3 className="text-base font-bold text-slate-900 mb-1">Confirm Cancellation</h3>
        <p className="text-xs text-slate-500 mb-5">
          Are you sure you want to cancel order <span className="font-bold text-slate-800">ORD-{orderId}</span>? This action cannot be reversed.
        </p>
        <div className="flex justify-center gap-3">
          <button onClick={onKeep} className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-md border border-slate-200">Keep Order</button>
          <button onClick={onConfirm} className="px-4 py-2 text-xs font-semibold text-white bg-red-600 hover:bg-red-700 rounded-md shadow-sm">Yes, Cancel Request</button>
        </div>
      </div>
    </div>
  )
}
