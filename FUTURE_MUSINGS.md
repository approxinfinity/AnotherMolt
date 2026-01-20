# Future Musings - AnotherThread

## Vertical Space (Z-axis)
- Z coordinate for dungeons, towers, air fortresses, underwater locations
- Future directional additions: UP, DOWN (will not generate wilderness)
- Wilderness generation should only apply to outdoor locations
- Consider location types that don't get wilderness:
  - Underground locations
  - Indoor locations
  - Locations adjacent to the sea (maybe generate "Open Water" instead?)
  - Sky/aerial locations

## Location Type Considerations
- May need a `locationType` enum: OUTDOOR, INDOOR, UNDERGROUND, UNDERWATER, AERIAL
- Or derive from features/terrain (e.g., if has CAVE feature, no wilderness)
- Sea-adjacent locations might generate "Shallows" or "Open Water" instead of "Wilderness"

## Exit System Evolution
- One-way exits create complexity for coordinate movement
- When moving a location B that has incoming one-way exits, need to move the entire connected subgraph
- Consider: should one-way exits be rare/special? Or common?

## Wilderness Naming
- Currently all auto-generated get name "Wilderness"
- Could vary based on terrain: "Dense Forest", "Open Plains", "Rocky Wastes", etc.
- User editing the name removes the "wilderness" status (it becomes a real location)

## Reevaluate isWilderness Property
- Currently we derive "is wilderness" from `name == "Wilderness"`, but this has limitations
- Consider re-adding an explicit `isWilderness` boolean property:
  - Set to `true` when a wilderness location is auto-created
  - Set to `false` when a user edits and saves the location (any edit "claims" the wilderness)
  - This allows varying wilderness names (e.g., "Dense Forest") while retaining wilderness status
- **Important for subgraph merging**: When determining if two subgraphs can be joined:
  - Wilderness locations (where `isWilderness == true`) should NOT block placement
  - When placing a subgraph, if a target coordinate has a wilderness location, that wilderness can be replaced/merged
  - Only non-wilderness locations should block coordinate placement
- This would make the grid system more flexible - wilderness acts as "unoccupied but navigable" space

## Cloudflare Caching Issues
- Cloudflare aggressively caches responses, causing stale data in the UI
- Current workaround: cache buster query param (`?_=<refreshKey>`) on `/locations` API calls
- Problems observed:
  - After deleting a location, the LocationGraph may not refresh due to cached JS or API responses
  - Mobile Safari + Cloudflare is particularly problematic
- Solutions to investigate:
  - Set up Cloudflare Page Rule to bypass cache for `anotherthread.ez2bgood.com/*`
  - Or use "Development Mode" in Cloudflare during active development
  - Add cache-busting to ALL API calls that affect the graph (create, update, delete)
  - Consider adding `Cache-Control: no-store` headers to API responses
  - May need to bust cache on more than just getLocations - any CRUD operation should trigger a full refresh
