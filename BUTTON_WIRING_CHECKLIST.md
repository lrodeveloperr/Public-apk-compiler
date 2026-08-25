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
- [x] Native time picker
- [x] Area menu
- [x] Notes field
- [x] Add task
- [x] Delete/reset task
- [x] Task lane chips
- [x] Task quantity fields
- [x] Timing mode chips
- [x] Back
- [x] Next/Create
- [x] First-board food-safety acknowledgement

## Paste
- [x] Paste field
- [x] Back
- [x] Import/Next
- [x] Parsed tasks become a real native board
- [x] First-board food-safety acknowledgement

## Boards
- [x] New
- [x] Upcoming filter
- [x] All filter
- [x] Featured board Open
- [x] Every board row opens the selected board
- [x] Same stored board-art enum is used as Home

## Live
- [x] Review waiting alert -> Waiting lane
- [x] Pause/Resume all
- [x] Now / Waiting / Next / Done lane tabs
- [x] Task timer chip starts/pauses timer
- [x] Now task Done -> Done lane
- [x] Waiting task Check -> Now lane
- [x] Next task priority -> toggles priority
- [x] Next task Now -> Now lane
- [x] Undo restores last completed task
- [x] Finished board Repeat -> new reset board

## Settings / legal
- [x] Language menu -> native per-app locale
- [x] Theme System / Light / Dark
- [x] Alerts toggle
- [x] Screen awake toggle
- [x] Compact toggle
- [x] Haptics toggle
- [x] Privacy -> external HTTPS policy
- [x] Terms -> external HTTPS policy
- [x] Support -> external HTTPS policy
- [x] Safety -> external HTTPS policy
- [x] Privacy choices -> Google UMP
- [x] Remove ads -> Google Play Billing
- [x] Manage subscription -> Google Play subscription management
- [x] Delete local data -> confirmation -> DataStore + timer state cleared

## Layout invariants
- [x] Phone top bar is fixed by Scaffold
- [x] Phone bottom navigation is fixed and uses Android navigation-bar insets
- [x] Ad rail is in layout, not overlaid over app controls
- [x] Tablet rail uses larger fixed logo/icon/label metrics
- [x] Repeat/New/Paste cards share one component and one scale system
- [x] Dark bottom-navigation labels/icons share the same selected/unselected color tokens
- [x] No sample Dinner/Brunch/Lunch/Late-night boards are seeded on a fresh install
