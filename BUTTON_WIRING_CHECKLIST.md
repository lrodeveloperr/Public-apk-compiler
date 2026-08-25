# Native button/link wiring checklist

## Global navigation
- [x] Phone Home -> Home
- [x] Phone Live -> Live (empty-state if there is no active board)
- [x] Phone Boards -> Boards
- [x] Phone Settings -> Settings
- [x] Tablet Home -> Home
- [x] Tablet Live -> Live
- [x] Tablet Boards -> Boards
- [x] Tablet New -> New board
- [x] Tablet Settings -> Settings
- [x] Android system Back -> Home from secondary screens

## Home
- [x] Now tile -> Live / Now
- [x] Waiting tile -> Live / Waiting
- [x] Next tile -> Live / Next
- [x] Done tile -> Live / Done
- [x] Continue -> Live
- [x] Repeat -> repeats most recent board; if none exists, opens New
- [x] New -> New board
- [x] Paste -> Paste importer
- [x] All -> Boards
- [x] Recent board row -> opens that board in Live

## New board
- [x] Name field
- [x] Native time picker / compact-height TimeInput
- [x] Area menu
- [x] Notes field
- [x] Add task
- [x] Delete task
- [x] Task lane chips
- [x] Task quantity fields
- [x] Timing mode chips
- [x] Back
- [x] Next/Create
- [x] First-board food-safety acknowledgement

## Paste / Android share
- [x] Paste field
- [x] Back
- [x] Import/Next
- [x] Parsed text becomes editable native tasks before board creation
- [x] Android ACTION_SEND text/plain follows the same native import path
- [x] First-board food-safety acknowledgement

## Boards
- [x] New
- [x] Upcoming filter
- [x] All filter
- [x] Featured board Open
- [x] Every board row opens the selected board
- [x] Same native BoardArt component is used on Home and Boards

## Live
- [x] Review waiting alert -> Waiting lane
- [x] Pause/Resume board
- [x] Now / Waiting / Next / Done lane tabs
- [x] Task timer chip starts/pauses timer
- [x] Now task Done -> Done lane
- [x] Waiting task Check -> Now lane
- [x] Next task priority -> toggles priority
- [x] Next task Now -> Now lane
- [x] Undo restores last completed task
- [x] Finished board Repeat -> new reset board
- [x] Timer expiry never auto-completes a task

## Settings / legal
- [x] Language menu -> native per-app locale
- [x] Light mode is enforced; no theme selector or dark-mode path
- [x] Alerts toggle
- [x] Screen awake toggle
- [x] Compact toggle
- [x] Haptics toggle
- [x] Privacy -> external HTTPS policy
- [x] Terms -> external HTTPS policy
- [x] Support -> external HTTPS policy
- [x] Safety -> external HTTPS policy
- [x] Privacy choices -> Google UMP when required
- [x] Remove ads -> Google Play Billing
- [x] Manage subscription -> Google Play subscription management
- [x] Delete local data -> confirmation -> Room + DataStore + timer state cleared

## Layout invariants
- [x] Phone top bar respects system status-bar inset
- [x] Phone bottom navigation is fixed by Material 3 adaptive navigation
- [x] Ad rail is in layout, not overlaid over app controls
- [x] No visible ad reservation before a successful banner load
- [x] Tablet navigation rail uses native Material 3 adaptive layout
- [x] Repeat/New/Paste cards share one scale system
- [x] Navigation labels/icons use the approved light-theme Material tokens
- [x] No sample Dinner/Brunch/Lunch/Late-night boards are seeded on a fresh install
- [x] Phone landscape uses compact-height rules rather than tablet scaling
