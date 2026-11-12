import { useState } from "react"

export function CancelModal({ token, onClose, onConfirm }: { token: number; onClose: () => void; onConfirm: (reason: string, detail: string) => void }) {
  const [reason, setReason] = useState("")
  const [detail, setDetail] = useState("")
  return <div className="modal-backdrop"><section className="cancel-modal"><header><div><span>CANCEL ORDER</span><h2>TOKEN {token}</h2></div><button type="button" onClick={onClose}>×</button></header><div className="warning-box"><strong>REQUIRED</strong><p>Cancellation is recorded in the audit trail.</p></div><label>REASON<select value={reason} onChange={(event) => setReason(event.target.value)}><option value="">SELECT A REASON</option>{["Customer request", "Duplicate order", "Quality issue", "Wrong order", "Payment issue"].map((entry) => <option key={entry}>{entry}</option>)}</select></label><label>DETAILS<textarea value={detail} onChange={(event) => setDetail(event.target.value)} placeholder="Enter details" /></label><footer><button type="button" onClick={onClose}>KEEP ORDER</button><button type="button" className="confirm-cancel" disabled={!reason || !detail.trim()} onClick={() => onConfirm(reason, detail)}>CANCEL ORDER</button></footer></section></div>
}
