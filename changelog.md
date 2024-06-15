# Changelog

### Fixes
- Check if the items are empty before trying to extract them. Fixes a crash with Void Upgrade in GUI mode 
- Fix litematica server side due to an accidentally used class from malilib.
- Fixes an incompatibility with other storage mods extracting items via the extract function that was not implemented 
- Fix a crashes in the FilteredItemHandler
- Renamed porting_lib loot to base and added the level snapshot functionality. Fixes a bug with the pump upgrade crashing the game whenever it tries to place a fluid.
- Fix a bug with Limited Barrels and Void upgrade not working properly. Items where not inserted when right-clicking and also not voided.
- Fix a bug where items from the compression upgrade do not show up for pulling out and also did not show up in jade