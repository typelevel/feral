# Contributing to feral

Thank you for your interest in contributing to feral! This document provides guidelines and instructions for contributing to the project.

## Code of Conduct

By participating in this project, you agree to abide by the [Typelevel Code of Conduct](https://typelevel.org/code-of-conduct.html).

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/your-username/feral.git`
3. Create a new branch: `git checkout -b feature/your-feature-name`
4. Make your changes
5. Run tests: `sbt test`
6. Commit your changes: `git commit -am 'Add your feature'`
7. Push to your fork: `git push origin feature/your-feature-name`
8. Create a Pull Request

## Development Setup

1. Make sure you have the following prerequisites installed:
   - Scala 2.13 or 3.2+
   - sbt
   - Node.js 18 (for Scala.js development)

2. Clone the repository and run:
   ```bash
   sbt compile
   ```

## Project Structure

- `lambda/` - Core Lambda functionality
- `lambda-http4s/` - HTTP4s integration for Lambda
- `lambda-cloudformation-custom-resource/` - CloudFormation custom resource support
- `google-cloud-http4s/` - Google Cloud integration
- `examples/` - Example implementations
- `scalafix/` - ScalaFix rules
- `sbt-lambda/` - sbt plugin for Lambda deployment

## Coding Standards

1. Follow the [Scala Style Guide](https://docs.scala-lang.org/style/)
2. Use [scalafmt](https://scalameta.org/scalafmt/) for code formatting
3. Run `sbt scalafmt` before committing
4. Ensure all tests pass with `sbt test`
5. Add tests for new functionality
6. Update documentation as needed

## Pull Request Guidelines

1. Keep PRs focused and small
2. Include tests for new functionality
3. Update documentation if needed
4. Add a clear description of your changes
5. Reference any related issues
6. Ensure CI checks pass

## Testing

- Run all tests: `sbt test`
- Run specific test: `sbt "testOnly *TestName"`
- Run with coverage: `sbt coverage test coverageReport`

## Documentation

- Update README.md if needed
- Add or update scaladoc comments
- Include examples for new features
- Update the [website](https://typelevel.org/feral/) if necessary

## Questions?

- Join the [Discord server](https://discord.gg/AJASeCq8gN)
- Open an issue for discussion
- Check existing issues and PRs

## License

By contributing, you agree that your contributions will be licensed under the same terms as the project's license. 