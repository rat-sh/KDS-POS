/**
 * Shared station registry.
 *
 * Populated once at application startup (and whenever stations are reloaded)
 * by calling {@link setStationRegistry}. All API mappers use
 * {@link stationNameById} and {@link stationIdByName} instead of a hardcoded
 * lookup table, so the application works with any stations configured in the
 * database.
 */

let _byId: Map<number, string> = new Map()
let _byName: Map<string, number> = new Map()

/** Replace the in-memory registry with fresh data from the API. */
export function setStationRegistry(stations: Array<{ id: number; name: string }>): void {
  _byId = new Map(stations.map((s) => [s.id, s.name]))
  _byName = new Map(stations.map((s) => [s.name.toLowerCase(), s.id]))
}

/**
 * Resolve a station numeric ID to its configured name.
 * Falls back to `"Station-{id}"` so the UI never shows blank labels even
 * when the registry is not yet loaded.
 */
export function stationNameById(id: number): string {
  return _byId.get(id) ?? `Station-${id}`
}

/**
 * Resolve a station name to its numeric ID.
 * Returns `undefined` when the name is not found.
 */
export function stationIdByName(name: string): number | undefined {
  return _byName.get(name.toLowerCase())
}

/** All station names currently known to the registry, ordered by ID. */
export function allStationNames(): string[] {
  return [..._byId.entries()].sort((a, b) => a[0] - b[0]).map(([, name]) => name)
}

