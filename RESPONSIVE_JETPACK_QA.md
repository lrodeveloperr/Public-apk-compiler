# Responsive Jetpack QA matrix

This matrix is the acceptance basis for the native scale pass. It preserves the existing Calm Workbench hierarchy and visual language; only responsive sizing/reflow changes by window shape.

| Window form | Representative window | Expected native behavior |
| --- | --- | --- |
| Phone portrait | 360×800dp / 412×915dp | Bottom navigation, stacked Home flow, 16dp gutter, 29sp hero title, full-width Live lane view, 52dp+ primary actions. |
| Phone landscape | 720×360dp / 800×400dp | Bottom navigation remains; compact-height override prevents tablet rail; spacing/hero height compress but live-task text remains 13sp; native TimeInput replaces tall clock picker. |
| Medium tablet portrait | 600–839dp wide | Bottom navigation as in the existing design breakpoint, 24dp gutter, side-by-side hero, fields may share rows where width permits. |
| Tablet portrait | 840–1199dp wide | Navigation rail, Home two-pane layout, four concurrent Live lanes, 32dp gutter, 38sp hero scale, no horizontal page scrolling. |
| Tablet landscape | 1200dp+ wide | Navigation rail, two-pane Home, four Live lanes, 40–48dp outer gutters, 52sp large hero at true large width, content capped/centered at 1440dp. |
| Short-height large/foldable landscape | width may exceed 840dp but height <480dp | Compact-height override wins: no rail/four-column squeeze; phone-style navigation and one Live lane at a time. |

Cross-form invariants:
- No WebView or browser scaling.
- Text uses `sp`; layout space uses `dp`.
- Compact mode changes density/spacing, not core readability.
- Long translated labels may wrap to two lines in constrained controls.
- RTL uses Compose `LayoutDirection`, not CSS mirroring.
- Top content respects status-bar insets; bottom navigation owns navigation-bar insets.
- Ads occupy layout space only after a successful load and remain separate from app controls.
- No seeded sample boards on a fresh install.
