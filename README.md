# Gradle Plugin Bytecode Analyzer

This tool analyzes the bytecode of Gradle plugins and
helps developers discover common issues during implementation.
It is available as a library, which is separate from Gradle Build Tool distribution.
To be published as a Gradle plugin.

![Build status](https://github.com/lptr/gradle-plugin-analyzer/actions/workflows/build-gradle-project.yml/badge.svg)

## Supported use-cases

### Gradle internal API usage reporting

It is not trivial at the moment, and Gradle plugins tend to use internal APIs, which offer no compatibility guarantee.
We use the analyzer to report usage issues to plugin developers,
and decide what APIs should be converted to public ones.

We publish reporting on [this web page](https://gradle.github.io/gradle-plugin-analyzer/).
This repository is refreshed automatically in GitHub Actions.

### Other use-cases

See the tests. Coming soon!

## Usage

Documentation is coming soon.
See the tests and the GitHub Actions for the examples.

## Wishlist

- Converting the library to a plugin that can be used by Gradle developers
- Emit data to the Gradle Problems API for future reporting and propagation,
  and also include it in Plugin Metadata and Gradle Build Scan
- Support for many Gradle versions in the plugin
- Support for analyzing Beta/Preview APIs (`@Incubating` annotation)
  and reporting on the issues when
  developers use preview APIs without communicating that to users
- The plugin should leverage Build API from Gradle
- Integrate the reports with the Gradle Plugin Portal,
  and consider it in the plugin quality score
- Make it possible to run the analyzer against the compiled build
- Make it a part of Gradle Build Tool when stable/ready

## References

- [Plugin Report](https://gradle.github.io/gradle-plugin-analyzer/)
