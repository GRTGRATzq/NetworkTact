# `:core:demo`

## Overview
NetworkTact demo mode: the app's own screens over a fictitious data set held in memory. Nothing in this module talks
to a radio, and nothing it holds is ever written to the real stores (message database, team list, preferences).

## How it plugs in
- `DemoMode` (interface in `:core:repository`) is switched on and off from the settings; it is not persisted, so every
  launch starts with it off.
- The screens' view models take their data sources under the Koin qualifier `SCREEN_DATA`. This module binds those to
  facades that read and write the real implementation while demo mode is off, and the in-memory demo one while it is
  on, switching live.
- The radio service keeps the unqualified, real implementations: real packets keep being received, stored and
  notified during a demo.
