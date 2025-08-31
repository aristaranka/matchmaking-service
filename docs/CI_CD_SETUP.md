# CI/CD Setup Guide

This document explains the comprehensive CI/CD pipeline setup for the Matchmaking Service using GitHub Actions.

## 🏗️ Pipeline Overview

The CI/CD pipeline consists of several workflows that handle different aspects of the development lifecycle:

### 1. **CI Pipeline** (`.github/workflows/ci.yml`)
- **Triggers**: Push to `main`/`develop`, Pull Requests
- **Purpose**: Build, test, and quality assurance
- **Features**:
  - Unit and integration testing with Redis
  - Code coverage with JaCoCo
  - SonarCloud quality analysis
  - OWASP dependency vulnerability scanning
  - Artifact generation

### 2. **Docker Pipeline** (`.github/workflows/docker.yml`)
- **Triggers**: Push to `main`/`develop`, Tags, Pull Requests
- **Purpose**: Build and publish Docker images
- **Features**:
  - Multi-platform builds (AMD64, ARM64)
  - Container vulnerability scanning with Trivy
  - Docker Scout security analysis
  - Automated tagging strategy
  - Docker Compose integration testing

### 3. **Security Pipeline** (`.github/workflows/security.yml`)
- **Triggers**: Push, Pull Requests, Daily schedule
- **Purpose**: Comprehensive security scanning
- **Features**:
  - CodeQL static analysis
  - Dependency vulnerability scanning
  - Secret scanning with TruffleHog and GitLeaks
  - Container security scanning
  - SARIF report generation

### 4. **Deployment Pipeline** (`.github/workflows/deploy.yml`)
- **Triggers**: Push to `main`, Tags, Manual dispatch
- **Purpose**: Automated deployment to staging and production
- **Features**:
  - Blue-green deployment strategy
  - Environment-specific configurations
  - Automated smoke tests
  - Rollback capabilities
  - Slack notifications

### 5. **Release Pipeline** (`.github/workflows/release.yml`)
- **Triggers**: Version tags, Manual dispatch
- **Purpose**: Automated release management
- **Features**:
  - Automated changelog generation
  - GitHub release creation
  - Multi-platform Docker image publishing
  - Release notifications
  - Deployment issue creation

### 6. **Cleanup Pipeline** (`.github/workflows/cleanup.yml`)
- **Triggers**: Weekly schedule, Manual dispatch
- **Purpose**: Resource cleanup and maintenance
- **Features**:
  - Old artifact cleanup
  - Docker image pruning
  - Cache management
  - Storage optimization

## 🔧 Required Secrets

Configure these secrets in your GitHub repository settings:

### Authentication & Security
```
GITHUB_TOKEN                 # Automatically provided by GitHub
SONAR_TOKEN                  # SonarCloud authentication token
SNYK_TOKEN                   # Snyk security scanning token
CODECOV_TOKEN               # Codecov integration token (optional)
```

### Deployment Secrets
```
# Staging Environment
STAGING_HOST                 # Staging server hostname
STAGING_USER                 # SSH username for staging
STAGING_SSH_KEY             # Private SSH key for staging
STAGING_PORT                # SSH port (default: 22)
STAGING_DB_USER             # Database username
STAGING_DB_PASSWORD         # Database password
STAGING_JWT_SECRET          # JWT signing secret

# Production Environment
PRODUCTION_HOST             # Production server hostname
PRODUCTION_USER             # SSH username for production
PRODUCTION_SSH_KEY          # Private SSH key for production
PRODUCTION_PORT             # SSH port (default: 22)
PRODUCTION_DB_USER          # Database username
PRODUCTION_DB_PASSWORD      # Database password
PRODUCTION_JWT_SECRET       # JWT signing secret
```

### Notifications
```
SLACK_WEBHOOK              # Slack webhook URL for notifications
```

## 🚀 Setup Instructions

### 1. **Enable GitHub Actions**
- Ensure GitHub Actions are enabled in your repository settings
- Grant necessary permissions for workflows

### 2. **Configure SonarCloud** (Optional but recommended)
1. Create account at [SonarCloud.io](https://sonarcloud.io)
2. Import your GitHub repository
3. Generate authentication token
4. Add `SONAR_TOKEN` to repository secrets
5. Update organization name in `sonar-project.properties`

### 3. **Configure Snyk** (Optional)
1. Create account at [Snyk.io](https://snyk.io)
2. Generate API token
3. Add `SNYK_TOKEN` to repository secrets

### 4. **Setup Deployment Environments**

#### Staging Environment
```bash
# On your staging server
sudo mkdir -p /opt/matchmaking-service
sudo chown $USER:$USER /opt/matchmaking-service
cd /opt/matchmaking-service
git clone https://github.com/your-username/matchmaking-service.git .
```

#### Production Environment
```bash
# On your production server
sudo mkdir -p /opt/matchmaking-service
sudo chown $USER:$USER /opt/matchmaking-service
cd /opt/matchmaking-service
git clone https://github.com/your-username/matchmaking-service.git .
```

### 5. **Configure GitHub Environments**
1. Go to repository Settings → Environments
2. Create `staging` and `production` environments
3. Configure protection rules for production:
   - Required reviewers
   - Wait timer
   - Deployment branches

### 6. **Update Repository URLs**
Replace placeholder URLs in the following files:
- `README.md` - Update badge URLs
- `sonar-project.properties` - Update organization
- All workflow files - Update repository references

## 📊 Monitoring and Notifications

### Workflow Status Badges
The README includes status badges for:
- CI Pipeline status
- Docker build status
- Security scan status
- Code coverage
- SonarCloud quality gate

### Slack Integration
Configure Slack notifications for:
- Deployment success/failure
- Release notifications
- Security alerts

### Email Notifications
GitHub automatically sends email notifications for:
- Workflow failures
- Security alerts
- Deployment status

## 🔄 Workflow Triggers

### Automatic Triggers
- **Push to main/develop**: Triggers CI, Docker, and Security workflows
- **Pull Requests**: Triggers CI and Security workflows
- **Version Tags**: Triggers Release and Deployment workflows
- **Daily Schedule**: Triggers Security scan
- **Weekly Schedule**: Triggers Cleanup workflow

### Manual Triggers
All workflows support manual dispatch with customizable parameters:
- Environment selection for deployments
- Cleanup type selection
- Version specification for releases

## 🛡️ Security Considerations

### Secret Management
- Use GitHub Secrets for sensitive data
- Rotate secrets regularly
- Use environment-specific secrets
- Never commit secrets to code

### Access Control
- Configure environment protection rules
- Require reviews for production deployments
- Use least-privilege access principles
- Monitor deployment logs

### Vulnerability Management
- Automated dependency scanning
- Container security scanning
- Secret scanning
- Code quality analysis
- Regular security updates

## 🔍 Troubleshooting

### Common Issues

#### Workflow Failures
1. Check workflow logs in Actions tab
2. Verify secret configuration
3. Check resource availability
4. Review dependency versions

#### Deployment Issues
1. Verify server connectivity
2. Check SSH key permissions
3. Validate environment variables
4. Review application logs

#### Security Scan Failures
1. Review vulnerability reports
2. Update dependencies
3. Add suppressions if needed
4. Check token permissions

### Getting Help
- Check GitHub Actions documentation
- Review workflow logs
- Consult security tool documentation
- Open issue in repository

## 📈 Continuous Improvement

### Metrics to Monitor
- Build success rate
- Deployment frequency
- Lead time for changes
- Mean time to recovery
- Security vulnerability trends

### Regular Maintenance
- Update workflow dependencies
- Review and update secrets
- Clean up old resources
- Monitor resource usage
- Update documentation

This CI/CD setup provides a robust, secure, and automated pipeline for your Matchmaking Service. Regular monitoring and maintenance will ensure optimal performance and security.
