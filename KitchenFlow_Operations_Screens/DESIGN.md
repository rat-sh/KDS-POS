# DESIGN.md — KitchenFlow Operations Screens Design System

> Persistent design system specification and web design contract for KitchenFlow. Adheres to the **awesome-design-md** and **Impeccable** taste standards for high-tempo kitchen and restaurant operations.

---

## 1. Visual Theme & Atmosphere

* **Atmosphere:** Industrial, high-contrast, distraction-free operations hub. Engineered for glancing across a hot, fast-paced commercial kitchen from 2–3 meters away.
* **Mode:** Dark-first operational shell (`#111314` / `#1E2124`) paired with stark white ticket cards (`#FFFFFF`) for immediate contrast and legible order staging.
* **Density:** High information density without visual noise. Strict hierarchy: Token > Timer > Table > Station Items > Modifiers.
* **Anti-Slop Philosophy:**
  * **No** generic SaaS purple-to-blue gradients or glossy glassmorphism.
  * **No** low-contrast muted gray text on dark surfaces.
  * **No** decorative animations or delays: status transitions are crisp and instantaneous.
  * **No** cards nested inside cards without clear functional borders and roles.

---

## 2. Color Palette & Token Roles

### Core Surfaces & Backgrounds
* `--dark`: `#111314` — Deepest shell backdrop, framing outer navigation and system headers.
* `--board`: `#1E2124` — Primary workstation / KDS canvas background.
* `--side`: `#171A1D` — Inventory and secondary dock panel background.
* `--panel`: `#1B1F20` — Elevated container surfaces and popover controls.
* `--ticket-surface`: `#FFFFFF` — Ticket cards background for maximum contrast against station lighting.
* `--paper`: `#EBE9E2` — Light contrast surfaces and modal canvas elements.

### Lines & Borders
* `--line`: `#303536` — Subtle structural borders on dark backgrounds (`rgba(255, 255, 255, 0.12)`).
* `--ticket-line`: `#C8C8C8` — Dividers between items inside order tickets.
* `--lightline`: `#C4C2BB` — Dividers inside light modals and coupons.

### Text & Inks
* `--ink`: `#E6E3DA` — High-contrast white/ivory text for dark operational panels.
* `--white`: `#FFFFFF` — Pure white for headers, tokens, and highlighted counts.
* `--muted`: `#969C99` / `#AAB1B8` — Secondary meta labels, helper text, and timestamps.
* `--ticket-text`: `#111111` — Deep ink for high-legibility ticket item names.
* `--modifier-text`: `#4A4D50` — Secondary modifier descriptions.

### Semantic Kitchen SLA & Status Stages
* **On-Time (Green):** `#3DBE6C` / Chip `#21804A` / Soft `#EAF5EE` — Within normal prep window.
* **Watch (Yellow):** `#E8C530` / Accent `#C9A227` — Approaching threshold (warning zone).
* **Due (Orange):** `#F08A24` / Note `#B26A00` — Urgently needs plating / station clearance.
* **Late (Red):** `#D6332B` / Alert `#B3261E` — Exceeded target SLA.
* **Overdue Alert Banner:** `#5A1512` — Critical kitchen alert requiring immediate expediter action.
* **Accent Active / Focus:** `#2F6FEB` — Selected ticket ring, active station tab indicator.

---

## 3. Typography Rules & Scale

* **Primary Display & Data Font:** `Barlow Condensed`, sans-serif. Used for all numbers, ticket tokens, timers, counts, and item lines.
* **Body & UI Font:** `Barlow`, Arial, sans-serif. Used for modals, settings, forms, and descriptive notes.
* **Numeric Rendering:** Always enforce `font-variant-numeric: tabular-nums` to prevent layout jitter when timers and counters update.

### Type Scale
| Role | Font Family | Size | Weight | Tracking / Letter-Spacing |
| :--- | :--- | :--- | :--- | :--- |
| **Ticket Token ID** | Barlow Condensed | 40px | 800 | Normal |
| **Kitchen Timer** | Barlow Condensed | 32px | 800 | Tabular nums |
| **Summary Counter** | Barlow Condensed | 28px | 800 | Tabular nums |
| **Item Headline** | Barlow Condensed | 24px | 700 | Normal |
| **Station Tab / Item Qty** | Barlow Condensed | 20px | 800 | `0.04em` |
| **Modifier / Flag Label** | Barlow Condensed | 18px | 500 / 700 | Normal |
| **Meta Label / Helper** | Barlow | 14px–16px | 600 | `0.05em` |
| **Micro Badge / Chips** | Barlow Condensed | 12px–14px | 700 | Uppercase |

---

## 4. Layout & Spacing Principles

* **Header Height:** Fixed `48px` bar containing branding, station selectors, live indicators, and quick filters.
* **Summary Metrics Strip:** `44px` height with inline count widgets (`On Time`, `Due`, `Late`, `Rush`).
* **KDS Split Zones:**
  * Two-zone mode: Main zone flexes (`1fr`), large zone / expo clamp `400px–640px`.
  * Grid lanes: Horizontal scrolling lane array (`gap: 12px`) with zero scrollbar chrome (`scrollbar-width: none`).
* **Touch-First Ergonomics:**
  * Minimum touch target: `44px × 44px` for all clickable buttons, dismiss controls, and toggles.
  * Active/Focus outlines: `4px solid #2F6FEB` with `outline-offset: -4px`.
* **Spacing Scale:** Base `4px` grid: `4px`, `8px`, `12px`, `16px`, `20px`, `24px`.

---

## 5. Component Styling & States

### 1. Ticket Card (`.ticket-card`)
* **Container:** Background `#FFFFFF`, border `1px solid rgba(0,0,0,0.12)`, radius `2px`.
* **Header Band (`.ticket-band`):** Colored by status stage (`--stage-color`), minimum height `96px`. Shows `#Token` on left, elapsed `Timer` on right, and status chips (`RUSH`, `VIP`, `COOKING`) below.
* **Special Notes (`.ticket-note`):**
  * Special instructions: Background `#FFE9A8`, border-left `4px solid #B26A00`.
  * Allergy alert: Background `#FFD6D6`, border-left `4px solid #B3261E` with red bold text.
* **Item Row (`.ticket-item`):** Grid layout `[56px qty | 1fr details]`.
  * Quantity: Right-aligned, `20px` bold.
  * Item Name: `24px` bold, overflow-wrap anywhere.
  * Modifiers: `18px`, modifier removals highlighted with `#B3261E` bold.
* **Completed / Cancelled:** Cancelled orders strikethrough ticket items with `#8A8F94` neutral band.

### 2. Action Controls & Buttons
* **Station Tabs:** Transparent background, `font-weight: 700`, active state highlighted with `border-bottom: 3px solid #2F6FEB`.
* **Served Drawer / History Toggle:** Floating bottom dock with status count badge (`#21804A`), box shadow `0 3px 12px rgba(0, 0, 0, 0.22)`.
* **Dialogs / Modals:** Backdrop `rgba(0, 0, 0, 0.65)`, container `#F7F8FA`, header border `#D6DDE4`, clean slide-in animation (`0.25s ease-out`).

---

## 6. Interface rules

### Do:
* **DO** use semantic status tokens (`stage-on_time`, `stage-due`, `stage-late`, `stage-overdue`).
* **DO** format all timers, times, and order quantities with `tabular-nums`.
* **DO** keep touch targets at least `44px` tall for grease/gloves touchscreen friendliness.
* **DO** maintain strict high contrast between text and background in both dark shell and light ticket surfaces.
* **DO** highlight allergen warnings with high-visibility red backgrounds and borders.

### Don't:
* **DON'T** introduce smooth pastel gradients or decorative background blur (glassmorphism).
* **DON'T** use low-contrast text (e.g. light gray on medium gray or yellow text on white).
* **DON'T** let timers cause layout shifts (never variable-width numeral fonts without tabular spacing).
* **DON'T** hide critical action triggers inside nested menus when operating in KDS mode.
* **DON'T** use generic placeholder icons when clear, punchy typography conveys the state faster.

---

- Dark shell (#111314 / #1E2124) with stark white ticket surfaces (#FFFFFF).
- Clear stage color progression: Green (#3DBE6C) -> Yellow (#E8C530) -> Orange (#F08A24) -> Red (#D6332B).
- Touch target floor: 44px minimum height.
```

