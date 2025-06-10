<div align="center">
  <img src="img\logo.png" alt="MCP Cloud Compliance Logo" width="500"/>
  <h1>MCP Cloud Compliance</h1>
</div>

[![Build Status](https://github.com/uprightsleepy/mcp-cloud-compliance/actions/workflows/ci.yml/badge.svg)](https://github.com/uprightsleepy/mcp-cloud-compliance/actions)
[![codecov](https://codecov.io/gh/uprightsleepy/mcp-cloud-compliance/branch/main/graph/badge.svg)](https://codecov.io/gh/uprightsleepy/mcp-cloud-compliance)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-green)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

A Model Context Protocol (MCP) server for AWS cloud compliance auditing.

## Project Goal

This MCP server aims to automate cloud compliance reporting for major security frameworks including SOC2, NIST, and PCI-DSS. Instead of manually gathering evidence and checking hundreds of controls across your AWS infrastructure, you'll be able to have a conversation with Claude to generate comprehensive compliance reports. 

The vision is to transform compliance auditing from a manual, time-consuming process into an intelligent, conversational experience where you can ask questions like:
- "Generate a SOC2 Type II report for our AWS infrastructure"
- "Check our NIST 800-53 compliance status"
- "Are we PCI-DSS compliant for our payment processing environment?"
- "Show me all non-compliant resources in production"

Currently, the project supports S3 bucket compliance checks across multiple compliance frameworks, with plans to expand to full AWS service coverage and automated report generation.

## Features

### Compliance Standards Supported
- **SOC 2** (Service Organization Control 2) - Security, availability, processing integrity, confidentiality, and privacy
- **CIS** (Center for Internet Security Benchmarks) - Security configuration guidelines and best practices  
- **NIST** (NIST Cybersecurity Framework) - Comprehensive cybersecurity risk management framework

### Resource Types Supported
- **Storage Resources** - AWS S3 buckets with comprehensive compliance checking
- Additional resource types (compute, database, network) planned for future releases

### Current Capabilities
- List S3 buckets by region with detailed metadata
- Check storage compliance against SOC2, CIS, and NIST standards
- Real-time compliance status reporting with detailed findings
- Regional resource filtering and management

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

## Usage Examples

Once connected to Claude, you can interact with your cloud compliance setup conversationally:

### Check Compliance Status
```
"Check SOC2 compliance for storage resources"
"What's my current NIST compliance status?"
"Show me CIS benchmark compliance for my S3 buckets"
```

### List and Manage Resources
```
"List my S3 buckets in us-west-2"
"Show me all storage resources across regions"
"What buckets were created this month?"
```

### Generate Reports
```
"Generate a compliance summary report"
"Show me all non-compliant resources"
"What are the high-severity compliance findings?"
```

## Available Tools

- `health_check` - Verify server is running
- `list_supported_standards` - Get supported compliance frameworks
- `list_supported_resource_types` - Get available resource types for compliance checking
- `list_s3_buckets` - List S3 buckets with detailed metadata by region
- `check_resource_compliance` - Check compliance status against specific standards

## Running Tests

```bash
# Run all tests
mvn test

# Run with coverage report
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Example Requests

The easiest way to see the tool in action is through screenshots showing the conversational interface:

1. **Compliance Check**: ![Screenshot where the assistant confirms that the only supported resource type for compliance checking is “Storage,” which includes AWS S3, Azure Blob Storage, and Google Cloud Storage (GCS). The assistant asks whether the user wants to run compliance checks or view supported standards.](img\resource_types.png)

2. **Resource Listing**: ![Screenshot of a chat interface where the assistant lists 12 S3 buckets in the us-east-1 AWS region. Buckets are categorized into Application Buckets, System/Utility Buckets, and Test Buckets. Application Buckets include names like shareframe-branding, shareframe-processed-videos, and user-management-service-lambda. One utility bucket is named do-not-delete-ssm-diagnosis-..., and three test buckets are noted as created on June 4, 2025. The assistant offers to run compliance checks on these storage resources.](img\list_buckets.png)

3. **Standards Overview**: ![Screenshot showing the assistant listing three supported compliance frameworks for cloud resource checks: SOC 2 (focused on security, availability, integrity, confidentiality, and privacy), CIS Benchmarks (focused on security configuration best practices), and NIST Cybersecurity Framework (focused on risk management). The assistant asks if the user wants to check their storage resources against one of these standards.](img\frameworks.png)

For a live demo, simply connect the MCP server to Claude Desktop and start asking compliance questions!

## Contributing
Contributions are welcome! Whether you're adding new compliance checks, improving documentation, or fixing bugs, we appreciate your help. Please feel free to:

- Open issues for bugs or feature requests
- Submit pull requests with improvements
- Add support for new AWS services or compliance frameworks
- Improve test coverage

For major changes, please open an issue first to discuss what you would like to change.

## License

MIT
