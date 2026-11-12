export function hashForPage(page: string) {
  return "#/" + page.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "")
}
