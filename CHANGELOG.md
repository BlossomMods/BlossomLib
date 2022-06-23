# 1.2.2

* Fix accidental Text conversion to String

# 1.2.1

* Improve CI

# 1.2.0

* Add JoiningCollector

# 1.1.0

* Add another way to register commands

# 1.0.0-rc.3

* Fix CTB (CommandTextBuilder) failing when no description provided
* Add more options to CTB
* Add & document locale keys for CTB

# 1.0.0-rc.2

* Add CommandTextBuilder to simplify commands with hover action & click action
* Add ConfigManager to simplify config reloading
* Add commands for ConfigManager

# 0.2.0-rc.4

* Add more constructors to TeleportUtils.TeleportDestination
* Fix TextUtils.player not applying the style correctly

# 0.2.0-rc.3

* Fix ListDataController#getArrayClassType not being public

# 0.2.0-rc.2

* Add ListDataController for parsing JSON arrays

# 0.2.0-rc.1

* Remove CSV, NBT from SaveController
* Rename SaveController to DataController to better reflect its purpose
* Add more links to README.md

# 0.1.0-rc.2

* Fix SaveController not saving JSON properly

# 0.1.0-rc.1

* Add warn color to config
* 11 new translation keys: `blossom.config-reload`, `blossom.clear-countdowns.all`, `blossom.clear-countdowns.one`,
  `blossom.clear-cooldowns.all`, `blossom.clear-cooldowns.one`, `blossom.clear-cooldowns.type`,
  `blossom.debug.countdown.start`, `blossom.debug.countdown.end`, `blossom.debug.teleport.no-cooldown`,
  `blossom.debug.teleport.cooldown`, `blossom.tpcancel`
* Add SaveController for managing NBT, CSV, Json data

# 0.0.1-beta1

* Add teleport and cooldown helper to TeleportUtils
* Add previous location storing for /back
* Add allowBack config option
* Add /tpcancel
* Document commands in README.md
* Add command registration to reduce children mod dependency on FabricAPI

# 0.0.1-alpha2

* Add a config
* Update logger to use the config
* Add CHANGELOG.md
* Add TeleportUtils utility class
* Add README.md to document config shape
* Add Fabric API
* Add commands for reloading config, clearing countdown, debug
* Add [Server Translations API](https://github.com/arthurbambou/Server-Translations)
* Update config
* Update TeleportUtils to have translations
* Separate TeleportConfig into its own file

# 0.0.1-alpha1

* Initialize project
* Add [fabric-permissions-api](https://github.com/lucko/fabric-permissions-api) as an optional dependency
* Add custom logger that could write to a logfile
* Add GitHub CI
