const knownPages: Record<string, string> = {
  counter: "Counter", "new-order": "New Order", orders: "Orders", payment: "Payment",
  station: "Station", "station-board": "Station Board", floor: "Floor", board: "Board",
  stations: "Stations", "manager-stations": "Manager Stations", handover: "Handover",
  availability: "Availability", "availability-and-stock": "Availability and Stock",
  stock: "Stock", menu: "Menu", users: "Users", settings: "Settings",
  "gst-and-thresholds": "GST and Thresholds", reports: "Reports", audit: "Audit",
}

export function pageFromHash(hash: string) {
  const slug = hash.replace(/^#\/?/, "").split("/")[0].toLowerCase()
  return knownPages[slug] ?? (slug ? slug.replace(/-/g, " ").replace(/\b\w/g, (char) => char.toUpperCase()) : "")
}
