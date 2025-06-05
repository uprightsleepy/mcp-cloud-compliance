# MCP Cloud Compliance
[![Build Status](https://github.com/uprightsleepy/mcp-cloud-compliance/actions/workflows/ci.yml/badge.svg)](https://github.com/YOUR-GITHUB-USERNAME/mcp-cloud-compliance/actions)
[![codecov](https://codecov.io/gh/uprightsleepy/mcp-cloud-compliance/branch/main/graph/badge.svg)](https://codecov.io/gh/YOUR-GITHUB-USERNAME/mcp-cloud-compliance)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-green)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

# MCP Cloud Compliance

A Model Context Protocol (MCP) server for AWS cloud compliance auditing.

## Project Goal

This MCP server aims to automate cloud compliance reporting for major security frameworks including SOC2, NIST, and PCI-DSS. Instead of manually gathering evidence and checking hundreds of controls across your AWS infrastructure, you'll be able to have a conversation with Claude to generate comprehensive compliance reports. 

The vision is to transform compliance auditing from a manual, time-consuming process into an intelligent, conversational experience where you can ask questions like:
- "Generate a SOC2 Type II report for our AWS infrastructure"
- "Check our NIST 800-53 compliance status"
- "Are we PCI-DSS compliant for our payment processing environment?"
- "Show me all non-compliant resources in production"

Currently, the project supports basic S3 bucket compliance checks, with plans to expand to full AWS service coverage and automated report generation.

## Prerequisites
- Java 21 or higher
- Maven 3.6+
- AWS credentials configured

## Installation

```bash
# Clone the repository
git clone https://github.com/uprightsleepy/mcp-cloud-compliance.git
cd mcp-cloud-compliance

# Build the project
mvn clean package
```

## Running the MCP Server

### Option 1: Using Maven
```bash
mvn spring-boot:run
```

### Option 2: Using the JAR
```bash
java -jar target/cloud-compliance-mcp-0.1.0.jar
```

## Configuring Claude Desktop

Add this to your Claude configuration file:

**macOS/Linux:** `~/Library/Application Support/Claude/claude_desktop_config.json`  
**Windows:** `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "cloud-compliance": {
      "command": "java",
      "args": ["-jar", "/absolute/path/to/mcp-cloud-compliance/target/cloud-compliance-mcp-0.1.0.jar"]
    }
  }
}
```

Restart Claude Desktop and look for the 🔨 icon to confirm the server is connected.

## Running Tests

```bash
# Run all tests
mvn test

# Run with coverage report
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Available Tools

- `health_check` - Verify server is running
- `list_s3_buckets` - List S3 buckets with compliance info

## Contributing

Contributions are welcome! Whether you're adding new compliance checks, improving documentation, or fixing bugs, we appreciate your help. Please feel free to:

Open issues for bugs or feature requests
Submit pull requests with improvements
Add support for new AWS services or compliance frameworks
Improve test coverage

For major changes, please open an issue first to discuss what you would like to change.

## License

MIT