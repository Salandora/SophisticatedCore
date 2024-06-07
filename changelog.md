# Changelog

### General
- Update to version 0.6.22
- Code cleanup
- Remove custom ItemStackHandler implementation
- Add ClientRecipesUpdated event

### Fixes
- Fix a bug that lead to the nonEmptyView of InventoryHandler to be empty when it shouldn't
- Commit the extract transactions in dropItem as soon as possible instead of collecting them all.
- Fix a bug in InventoryHelper that could have lead to item loss/duplication and broke mass item handling
- Fix a bug with upgrades not getting applied properly when added via right-clicking with an upgrade item
- Fix a crash bug when a placed down backpack gets broken by another player while someone has the gui ope
- Fix a bug where inventories would leave empty spaces and refuses items at all when piped in