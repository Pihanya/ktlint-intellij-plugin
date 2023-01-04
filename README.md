<img src='/plugin/src/main/resources/META-INF/pluginIcon.svg?raw=true' alt="plugin icon" width='128' />  

# ktlint-intellij-plugin

[![Build](https://github.com/Pihanya/ktlint-intellij-plugin/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/Pihanya/ktlint-intellij-plugin/actions/workflows/build.yml)
[![Version](https://img.shields.io/jetbrains/plugin/v/15057.svg)](https://plugins.jetbrains.com/plugin/15057)
[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/15057.svg)](https://plugins.jetbrains.com/plugin/15057)
[![GitHub license](https://img.shields.io/github/license/Pihanya/ktlint-intellij-plugin.svg)](https://github.com/Pihanya/ktlint-intellij-plugin/blob/main/LICENSE)

<!-- Plugin description -->
Automatically runs [ktlint](https://ktlint.github.io/) on Kotlin files, and annotates any errors found.

Enable and configure in `Preferences` > `Tools` > `ktlint`.

Provides integration with [ktlint](https://ktlint.github.io/), a linter for Kotlin.
Shows errors reported by ktlint in the editor
Provides a quick fix (Alt-Enter) and an action ("Format With Ktlint") to fix errors and warnings using ktlint
Imports matching code style rules defined in the TSLint configuration to the IDE code style settings
<!-- Plugin description end -->

Adding more features over time: [TODO.md](TODO.md)

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Ktlint"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/Pihanya/ktlint-intellij-plugin/releases/latest) and install it manually using
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
