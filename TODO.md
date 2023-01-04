# ktlint-intellij-plugin

### Roadmap

- [ ] Possibility to import ktlint rules from remote repositories (e.g. from [Maven Central][maven-central])
- [ ] Improve ruleset configuration UI: show more info about rules
- [ ] Add settings buttons for pre-push and pre-commit hooks if VCS is present
- [ ] Technical debt: add more tests, eliminate TODOs in source code
- [ ] Integration with [Inspection features][ij_code_inspections]
- [ ] Add settings buttons to apply global/project ktlint configs
- [ ] Support providing a few .editorconfig files

### Done ✓

- [x] `--baseline` support
- [x] Add plugin [error reporting][error-reporting]
- [x] Support multiple ruleset jars
- [x] Add support for external rule sets
- [x] Add `.editorconfig` override setting
- [x] Add `.editorconfig` support
- [x] Add disabled rules setting
- [x] Add formatting action
- [x] Add annotation action to disable ktlint plugin
- [x] MVP (automatic inspections)

[ij_code_inspections]: https://jetbrains.org/intellij/sdk/docs/reference_guide/custom_language_support/code_inspections_and_intentions.html
[maven-central]: https://mvnrepository.com/repos/central
[error-reporting]: https://www.plugin-dev.com/intellij/general/error-reporting/
