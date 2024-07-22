# Changelog

### General
- Update to version 0.6.22
- Code cleanup
- Remove custom ItemStackHandler implementation
- Add ClientRecipesUpdated event

### Fixes
- Try to fix a crash with compression upgrade and e.g. hopper, caused by a simulate extraction getting rolled back.
- Fixes plus sign symbol visible without the trinket slot being visible.
- Fixes plus sign not visible while trinket slot is visible.
- Fix a bug where inserting matching items into a storage via a controller block caused item duplicates
- Mouse Scrolled function was named keyPressed instead of mouseScrolled
- Fix a bug with Emi and Rei where memorized/no sort slots where not used for crafting recipes
- Fix a dupe bug due to a recent change in the InventoryHandler 
- Fix a lithium incompatibility
- Fix a bug with the pump upgrade crashing the game whenever it tries to place a fluid.

### Compatiblity
- Added AudioPlayer compatibility