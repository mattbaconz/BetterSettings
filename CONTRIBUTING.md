# Contributing to BetterSettings

Thank you for your interest in contributing to BetterSettings! This document provides guidelines and instructions for contributing.

## Code of Conduct

- Be respectful and inclusive
- Focus on constructive feedback
- Help create a welcoming environment for all contributors

## How to Contribute

### Reporting Bugs

1. Check if the bug has already been reported in [Issues](../../issues)
2. If not, create a new issue with:
   - Clear title and description
   - Steps to reproduce
   - Expected vs actual behavior
   - Server version, plugin version, and relevant logs
   - Configuration file (if relevant)

### Suggesting Features

1. Check [Discussions](../../discussions) for similar suggestions
2. Create a new discussion or issue describing:
   - The feature and its benefits
   - Use cases
   - Potential implementation approach

### Pull Requests

1. Fork the repository
2. Create a feature branch from `main`
3. Make your changes following our coding standards
4. Test your changes thoroughly
5. Commit with clear, descriptive messages
6. Push to your fork
7. Open a Pull Request with:
   - Description of changes
   - Related issue numbers
   - Testing performed

## Development Setup

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Git
- A Paper/Purpur/Folia test server (1.20.5+)

### Building

```bash
git clone https://github.com/mattbaconz/BetterSettings.git
cd BetterSettings
mvn clean package
```

### Testing

1. Copy the built JAR from `target/` to your test server's `plugins/` folder
2. Start the server
3. Test your changes thoroughly
4. Check console for errors
5. Verify thread safety with multiple players

## Coding Standards

### Java Style

- Use 4 spaces for indentation (no tabs)
- Follow standard Java naming conventions
- Maximum line length: 120 characters
- Use meaningful variable and method names

### Documentation

- Add Javadoc comments for all public classes and methods
- Include `@param`, `@return`, and `@throws` tags
- Explain complex logic with inline comments
- Update README.md if adding user-facing features

### Thread Safety

- All code must be thread-safe for Folia compatibility
- Use appropriate schedulers (player, region, global, async)
- Avoid blocking operations on game threads
- Use concurrent collections where appropriate

### Performance

- Minimize object allocation in hot paths
- Use async I/O for file operations
- Avoid unnecessary database/file access
- Profile performance-critical code

### Error Handling

- Use try-catch blocks for risky operations
- Log errors with appropriate severity levels
- Provide helpful error messages
- Never swallow exceptions silently

## Code Review Process

1. All PRs require at least one approval
2. CI checks must pass
3. Code must follow style guidelines
4. Changes must be tested
5. Documentation must be updated

## Commit Messages

Use clear, descriptive commit messages:

```
Add auto-save configuration option

- Added auto-save-interval config option
- Implemented scheduled save task
- Updated documentation
```

### Commit Message Format

- Use present tense ("Add feature" not "Added feature")
- First line: brief summary (50 chars or less)
- Blank line
- Detailed description if needed
- Reference issues: "Fixes #123" or "Relates to #456"

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

## Questions?

Feel free to ask questions in:
- [GitHub Discussions](../../discussions)
- [GitHub Issues](../../issues)

Thank you for contributing to BetterSettings! 🎉
