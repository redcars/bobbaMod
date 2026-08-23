# Changelog

## [3.0.0](https://github.com/redcars/bobbaMod/compare/v2.0.0...v3.0.0) (2026-08-23)


### ⚠ BREAKING CHANGES

* replace party rank-kick with watchlist auto-kick

### Features

* add Party config category with quick-kick button ([7ff4633](https://github.com/redcars/bobbaMod/commit/7ff4633d352141d21100c0898d131a0c5ea42caf))
* allow an optional note in /watchlist add ([a1731bf](https://github.com/redcars/bobbaMod/commit/a1731bf6ce60b81fb94f5d23644d13fd46004165))
* auto-swap keybind presets based on SkyBlock island ([52fbb69](https://github.com/redcars/bobbaMod/commit/52fbb695ce482e4f4e6eac7ff60c23f42270fe43))
* auto-swap keybind presets based on SkyBlock island ([194996b](https://github.com/redcars/bobbaMod/commit/194996b8a3575fea930e86f558c326a73f7807bf))
* infer rank from name color for party-finder joins ([531da83](https://github.com/redcars/bobbaMod/commit/531da8306d6261fda465e52a15bf6b7839f5b74c))
* replace party rank-kick with watchlist auto-kick ([045ac58](https://github.com/redcars/bobbaMod/commit/045ac5821de76a663e3dc37cf71cedcee9619523))


### Bug Fixes

* address auto-kick watchlist review findings ([69c3013](https://github.com/redcars/bobbaMod/commit/69c301341437c8e4356480b5bac6bae0c51e6340))
* address PR [#11](https://github.com/redcars/bobbaMod/issues/11) review findings for island preset auto-swap ([416dd74](https://github.com/redcars/bobbaMod/commit/416dd749db14636a2d857b2c802e8f260ad9410a))
* fire every keybind sharing a key; harden grace window ([88e0f85](https://github.com/redcars/bobbaMod/commit/88e0f85c5c6d5e8726b4de74d6c1421bf6d46408))
* prevent keybind leak when closing chat/screens ([544cdfc](https://github.com/redcars/bobbaMod/commit/544cdfc93750a7fbd91a167a4afc14e0aa07d43c))
* prevent keybind leak when closing chat/screens ([7be89cd](https://github.com/redcars/bobbaMod/commit/7be89cd25e0fc237d522a0a1c1c248a8e3623693))

## [2.0.0](https://github.com/redcars/bobbaMod/compare/v1.2.0...v2.0.0) (2026-07-18)


### ⚠ BREAKING CHANGES

* requires Minecraft 26.1.x and Java 25; 1.21.11 support continues on the 1.21.11 branch.

### Features

* add chat right-click actions for watchlist ([ccdd3ec](https://github.com/redcars/bobbaMod/commit/ccdd3ec829ef98d896b7bae00c36bd0e35a62889))
* port to Minecraft 26.1.2 ([794d38f](https://github.com/redcars/bobbaMod/commit/794d38f2689c3b08965cf51f5ecd863566661036))


### Bug Fixes

* normalize whitespace in PartyDetection ([ee00224](https://github.com/redcars/bobbaMod/commit/ee002243659086ae6edfc5c7d098b6e10307bea8))

## [1.2.0](https://github.com/redcars/bobbaMod/compare/v1.1.0...v1.2.0) (2026-05-24)


### Features

* add auto-kick for watchlisted party members ([99c8c1e](https://github.com/redcars/bobbaMod/commit/99c8c1e3528767adcde87ff74dad151ac73526f0))
* add enabled toggle support and editor ([dff7ec0](https://github.com/redcars/bobbaMod/commit/dff7ec04ed85a9b2274dfa48580e8adde6775074))
* add party auto-kick filter configuration ([60aba8e](https://github.com/redcars/bobbaMod/commit/60aba8ec583ca078b5c8d8436ed94b2c94a955d4))
* add preset support to keybind system ([6f934c3](https://github.com/redcars/bobbaMod/commit/6f934c379f40feb7ded2dabea650ddb3ad4126a3))

## [1.1.0](https://github.com/redcars/bobbaMod/compare/v1.0.0...v1.1.0) (2026-05-19)


### Features

* keybinds system ([ffa53a5](https://github.com/redcars/bobbaMod/commit/ffa53a56a472fed34a0b22876355414532c30a32))
* migrate editor screens to BobbaScreen base class ([88c9f9c](https://github.com/redcars/bobbaMod/commit/88c9f9c26fa5aa63a3b1f6e27d547137cf5ec0ce))

## [1.0.0](https://github.com/redcars/bobbaMod/compare/v1.0.0...v1.0.0) (2026-05-17)


### Features

* **config,party,watchlist:** add API key config and improve rank detection ([a53ac6f](https://github.com/redcars/bobbaMod/commit/a53ac6f557b7fbba140843f6da4c6097aa236e2c))
* **party:** add party detection and test command modules ([a34c120](https://github.com/redcars/bobbaMod/commit/a34c1203bf92e676a45e160b540b0c62ad0d75ac))
* **presence:** add server presence detection module ([d1b4e62](https://github.com/redcars/bobbaMod/commit/d1b4e6244671d40c460c8312c2917b27f66cf48a))
* release ([84fb94b](https://github.com/redcars/bobbaMod/commit/84fb94b3a7f8d95a6a7322f36b6476a46abac2c3))
* release ([59b3095](https://github.com/redcars/bobbaMod/commit/59b30957630c995a4883b9f03cb9b48a4e2e828a))
* reset version to 0.0.0 and pin next release to 1.0.0 ([e21dd14](https://github.com/redcars/bobbaMod/commit/e21dd145e5fb171ae8a5a09c0781fb42e7a31513))
* **update:** add version status config editor to About page ([a6066f5](https://github.com/redcars/bobbaMod/commit/a6066f5c648679e716d047c7fb6eeb41e4b9a6f2))
* **update:** integrate UpdateChecker into client initialization ([3947c0a](https://github.com/redcars/bobbaMod/commit/3947c0a53e0cbee3038bf2e1e2e820cb954da47b))
* **watchlist:** add Hypixel rank lookup and watchlist auto-refresh ([93c13ed](https://github.com/redcars/bobbaMod/commit/93c13ed20214e3003e364f9a931e9641bd30abd3))
* **watchlist:** add watchlist editor screen integration and config button ([0dc16e8](https://github.com/redcars/bobbaMod/commit/0dc16e8d29e0dc1685b261e6011f52d4d568ed2b))

## 1.0.0 (2026-05-17)


### Features

* **config,party,watchlist:** add API key config and improve rank detection ([a53ac6f](https://github.com/redcars/bobbaMod/commit/a53ac6f557b7fbba140843f6da4c6097aa236e2c))
* **party:** add party detection and test command modules ([a34c120](https://github.com/redcars/bobbaMod/commit/a34c1203bf92e676a45e160b540b0c62ad0d75ac))
* **presence:** add server presence detection module ([d1b4e62](https://github.com/redcars/bobbaMod/commit/d1b4e6244671d40c460c8312c2917b27f66cf48a))
* release ([59b3095](https://github.com/redcars/bobbaMod/commit/59b30957630c995a4883b9f03cb9b48a4e2e828a))
* reset version to 0.0.0 and pin next release to 1.0.0 ([e21dd14](https://github.com/redcars/bobbaMod/commit/e21dd145e5fb171ae8a5a09c0781fb42e7a31513))
* **update:** add version status config editor to About page ([a6066f5](https://github.com/redcars/bobbaMod/commit/a6066f5c648679e716d047c7fb6eeb41e4b9a6f2))
* **update:** integrate UpdateChecker into client initialization ([3947c0a](https://github.com/redcars/bobbaMod/commit/3947c0a53e0cbee3038bf2e1e2e820cb954da47b))
* **watchlist:** add Hypixel rank lookup and watchlist auto-refresh ([93c13ed](https://github.com/redcars/bobbaMod/commit/93c13ed20214e3003e364f9a931e9641bd30abd3))
* **watchlist:** add watchlist editor screen integration and config button ([0dc16e8](https://github.com/redcars/bobbaMod/commit/0dc16e8d29e0dc1685b261e6011f52d4d568ed2b))

## [1.1.0](https://github.com/redcars/bobbaMod/compare/v1.0.0...v1.1.0) (2026-05-17)


### Features

* **config,party,watchlist:** add API key config and improve rank detection ([a53ac6f](https://github.com/redcars/bobbaMod/commit/a53ac6f557b7fbba140843f6da4c6097aa236e2c))
* **party:** add party detection and test command modules ([a34c120](https://github.com/redcars/bobbaMod/commit/a34c1203bf92e676a45e160b540b0c62ad0d75ac))
* **presence:** add server presence detection module ([d1b4e62](https://github.com/redcars/bobbaMod/commit/d1b4e6244671d40c460c8312c2917b27f66cf48a))
* release ([59b3095](https://github.com/redcars/bobbaMod/commit/59b30957630c995a4883b9f03cb9b48a4e2e828a))
* **update:** add version status config editor to About page ([a6066f5](https://github.com/redcars/bobbaMod/commit/a6066f5c648679e716d047c7fb6eeb41e4b9a6f2))
* **update:** integrate UpdateChecker into client initialization ([3947c0a](https://github.com/redcars/bobbaMod/commit/3947c0a53e0cbee3038bf2e1e2e820cb954da47b))
* **watchlist:** add Hypixel rank lookup and watchlist auto-refresh ([93c13ed](https://github.com/redcars/bobbaMod/commit/93c13ed20214e3003e364f9a931e9641bd30abd3))
* **watchlist:** add watchlist editor screen integration and config button ([0dc16e8](https://github.com/redcars/bobbaMod/commit/0dc16e8d29e0dc1685b261e6011f52d4d568ed2b))
