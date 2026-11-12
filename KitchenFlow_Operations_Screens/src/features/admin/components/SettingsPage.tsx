import { useEffect, useState } from "react"
import { saveSettings } from "../../../api"
import { actionMessage } from "../../../shared/logic"
import { updateSettings, useSettingsStore } from "../../../store/settings"

export function SettingsPage() {
  const settings = useSettingsStore()
  const [outletName, setOutletName] = useState(settings.outletName)
  const [gstin, setGstin] = useState(settings.gstin)
  const [amberPercent, setAmberPercent] = useState(settings.amberPercent)
  const [redPercent, setRedPercent] = useState(settings.redPercent)
  const [message, setMessage] = useState("")
  const [saving, setSaving] = useState(false)
  useEffect(() => {
    setOutletName(settings.outletName)
    setGstin(settings.gstin)
    setAmberPercent(settings.amberPercent)
    setRedPercent(settings.redPercent)
  }, [settings])
  const save = async () => {
    if (![amberPercent, redPercent].every((value) => Number.isFinite(value) && value > 0) || amberPercent >= redPercent) { setMessage("Red must be greater than amber; both percentages must be positive."); return }
    setSaving(true)
    try {
      const next = { outletName: outletName.trim() || settings.outletName, gstin: gstin.trim().toUpperCase(), amberPercent, redPercent }
      await saveSettings(next)
      updateSettings(next)
      setMessage("Settings saved.")
    } catch (cause) { setMessage(actionMessage(cause)) } finally { setSaving(false) }
  }
  return <main className="light-screen workspace-screen"><header className="page-header"><div><h1>SETTINGS</h1></div></header><section className="settings-grid"><section><h2>OUTLET BILLING CONFIGURATION</h2><p>These values appear on customer bills and the public status board.</p><label>OUTLET NAME<input maxLength={120} value={outletName} onChange={(event) => setOutletName(event.target.value)} /></label><label>GSTIN<input maxLength={15} value={gstin} onChange={(event) => setGstin(event.target.value)} /></label></section><section><h2>TICKET SLA LIMITS (%)</h2><p>Green is below amber; red is above the red threshold.</p><label>AMBER AT (%)<input type="number" min="1" step="1" value={amberPercent} onChange={(event) => setAmberPercent(Number(event.target.value))} /></label><label>RED OVER (%)<input type="number" min="2" step="1" value={redPercent} onChange={(event) => setRedPercent(Number(event.target.value))} /></label><p role="status">{message}</p><button type="button" disabled={saving} onClick={() => void save()}>{saving ? "SAVING…" : "SAVE SETTINGS"}</button></section></section></main>
}
