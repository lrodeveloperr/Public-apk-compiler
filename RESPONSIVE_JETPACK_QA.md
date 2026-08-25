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

## Screen acceptance checks

| Screen | Phone portrait | Phone / foldable landscape | Tablet portrait / landscape |
| --- | --- | --- | --- |
| Home | Active, hero, Start and Recent remain one scrollable stack. Four status tiles stay inside the card. | Uses compact vertical gaps and the approved side-by-side hero only when at least 600dp wide. Content scrolls; nothing is height-clipped. | Rail/two-pane activates only at 840dp+ with at least 480dp height and normal font scale. Content is centered and width-capped at very large widths. |
| Live | One selected lane, fixed native navigation, independently scrollable task content. | Compact task spacing; one lane at a time even when raw width exceeds 840dp. Core task text stays 13sp. | Four equal scrollable lanes; undo affordance has reserved scroll clearance and does not cover the last task action. |
| Boards | Header, filters, featured board and list share one vertical scroll container. | New/filter labels can wrap without horizontal scrolling. | Uses the same centered content width and native `BoardArt`; no alternate tablet-only visuals. |
| New / Paste | Scrollable form with fixed 52dp action area. Quantity fields stack on phones. | IME inset keeps Back/Next/Continue reachable; compact native `TimeInput` replaces the tall picker. | Quantity fields share a row only from 600dp with normal font scale; large text forces the safe stacked form. |
| Settings | Theme controls reflow below their title; language control stacks on very narrow or large-text layouts. | Entire screen scrolls, including legal and subscription rows. | Row treatment is retained where labels and controls have sufficient width. |

## Accessibility and localization checks

- Re-run the matrix at font scales 1.0, 1.3 and 2.0. At 1.3+, rail, Home two-pane, hero side-by-side and three-field quantity rows reflow instead of globally reducing text.
- Re-run with Arabic or Hebrew to confirm Compose reverses layout direction while semantic item order and lane state remain correct.
- Re-run with German and Finnish to confirm navigation, action, filter and Settings labels wrap to two lines instead of clipping.
- All interactive surfaces are at least 48dp high/wide or inherit Material's enforced 48dp minimum touch target; primary actions remain 52dp+.
- With an IME open in New and Paste, the fixed action row remains reachable and the editable content remains scrollable.
- With the undo affordance visible, the final task action can scroll above it in both one-lane and four-lane Live layouts.
- With no successful ad load, the banner rail is 0dp and reserves no blank space. After successful load, its adaptive height is inserted between content and native navigation, never over either.

Cross-form invariants:
- No WebView or browser scaling.
- Text uses `sp`; layout space uses `dp`.
- Compact mode changes density/spacing, not core readability.
- Long translated labels may wrap to two lines in constrained controls.
- RTL uses Compose `LayoutDirection`, not CSS mirroring.
- Top content respects status-bar insets; bottom navigation owns navigation-bar insets.
- Ads occupy layout space only after a successful load and remain separate from app controls.
- No seeded sample boards on a fresh install.
