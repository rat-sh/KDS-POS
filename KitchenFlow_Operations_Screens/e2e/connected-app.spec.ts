import { expect, test } from "@playwright/test"

test.beforeEach(async ({ request }) => {
  const health = await request.get("/actuator/health")
  expect(health.ok(), "Spring Boot must be running and reachable through the Vite proxy").toBeTruthy()
  expect((await health.json()).status).toBe("UP")
})

test("starts at the live staff sign-in screen", async ({ page }) => {
  await page.goto("/")
  await expect(page.getByRole("heading", { name: "Welcome back" })).toBeVisible()
  await expect(page.getByLabel("USERNAME")).toBeVisible()
  await expect(page.getByLabel("PASSWORD")).toBeVisible()
  await expect(page.getByText("Demo accounts")).toHaveCount(0)
})

test("loads the public board from the API", async ({ page }) => {
  await page.goto("/status")
  await expect(page.locator(".public-status")).toBeVisible()
  await expect(page.getByText("SYNCING LIVE STATUS…")).toHaveCount(0)
})
