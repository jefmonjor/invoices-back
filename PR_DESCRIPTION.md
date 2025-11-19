# Pull Request: Railway Deployment and Local Development Fixes

## 🎯 Summary

This PR fixes critical Railway deployment issues and completes the local development setup for the Spring Boot monolith application.

## 🔧 Critical Fixes

### Railway Production Deployment:
- ✅ **Fix PORT environment variable** - Changed from `SERVER_PORT` to `PORT` to match Railway's injection (MOST CRITICAL)
- ✅ **Configure health checks** - Set up `/actuator/health/readiness` probe excluding Redis from readiness check
- ✅ **Remove Fly.io artifacts** - Cleaned up obsolete deployment files and scripts with hardcoded credentials (security fix)

### Local Development:
- ✅ **Fix OpenAPI configuration** - Resolved variable scope and nested placeholder issues
- ✅ **Enable Swagger UI** - Added `/api-docs/**` to Spring Security permitAll endpoints
- ✅ **Update dependencies** - Upgraded springdoc-openapi to 2.7.0 for Spring Boot 3.4.4 compatibility

## 📝 Changes by Commit

1. **aaca5d6** - `fix: use PORT env variable instead of SERVER_PORT for Railway`
   - Changed `application.yml` to use `${PORT:8080}` instead of `${SERVER_PORT:8080}`
   - Railway injects PORT variable, not SERVER_PORT

2. **f9f40a8** - `fix: configure Railway health check to use readiness probe without Redis`
   - Created separate readiness/liveness probes in application.yml
   - Readiness probe excludes Redis (only checks db + diskSpace)
   - Updated railway.json to use `/actuator/health/readiness`

3. **1111b33** - `fix: upgrade springdoc-openapi to 2.7.0 for Spring Boot 3.4.4 compatibility`
   - Upgraded from 2.6.0 to 2.7.0 in pom.xml
   - Resolves NoSuchMethodError with Spring Boot 3.4.4

4. **d54ee7a** - `fix: resolve OpenApiConfig error 500 - remove nested placeholder in @Value`
   - Changed from nested placeholder to dynamic URL building
   - `String baseUrl = "http://localhost:" + serverPort;`

5. **3b0d9db** - `fix: allow public access to /api-docs/** endpoints for Swagger UI`
   - Added `/api-docs/**` to SecurityConfig permitAll() matchers
   - Resolves 403 Forbidden on Swagger endpoints

6. **15d2020** - `fix: resolve OpenApiConfig compilation error - productionServer variable scope`
   - Fixed productionServer variable scope issue
   - Changed line 78 to use only localServer when no production URL

7. **d25a20a** - `chore: remove Fly.io deployment files and scripts with hardcoded credentials`
   - Deleted 9 obsolete deployment scripts
   - Security fix: removed hardcoded production credentials

## 🧪 Testing

### Local (Docker Compose) - ✅ ALL PASSING
- ✅ Application starts successfully on port 8080
- ✅ Swagger UI accessible at http://localhost:8080/swagger-ui.html
- ✅ PostgreSQL connection working
- ✅ Flyway migrations successful (6 migrations)
- ✅ MinIO bucket configuration working
- ✅ Redis connection working

### Railway Production - ⏳ PENDING MERGE
Currently Railway is building from old main branch without fixes.

**Current issue in Railway:**
```
2025-11-19 23:22:18 - Tomcat initialized with port 8080 (http)
```
Application starts on fixed port 8080 instead of Railway's dynamic PORT.

**Expected after merge:**
```
2025-11-19 23:22:18 - Tomcat initialized with port $PORT (http)
```
Application will listen on Railway's dynamic port.

## 📂 Files Modified

### Core Configuration Files:
- `invoices-monolith/src/main/resources/application.yml`
  - Line 2: `server.port: ${PORT:8080}` (changed from SERVER_PORT)
  - Lines 129-139: Health check probe configuration (readiness/liveness)

- `invoices-monolith/src/main/java/com/invoices/invoice/config/OpenApiConfig.java`
  - Lines 28-29: Dynamic URL building (removed nested placeholder)
  - Line 78: Fixed variable scope for server list

- `invoices-monolith/src/main/java/com/invoices/security/SecurityConfig.java`
  - Line 99: Added `/api-docs/**` to permitAll()

- `invoices-monolith/pom.xml`
  - Line 138: springdoc-openapi version 2.7.0 (upgraded from 2.6.0)

- `railway.json`
  - Line 11: `healthcheckPath: "/actuator/health/readiness"` (changed from /actuator/health)

### Files Deleted (Security Cleanup):
- `build-and-deploy-local.sh`
- `configure-secrets.sh`
- `create-pr.sh`
- `create-production-pr.sh`
- `deploy-macos.sh`
- `deploy.sh`
- `invoices-monolith/fly.toml`
- `quick-deploy.sh`
- `run-local-fast.sh` - **Security risk**: contained hardcoded Neon PostgreSQL password, Upstash Redis password, Cloudflare R2 keys
- `run-local.sh` - **Security risk**: contained hardcoded production credentials
- `setup-env.sh`

## 🚀 Post-Merge Actions

After merging this PR, Railway will automatically:

1. **Detect the merge** to main branch
2. **Rebuild the application** with new Dockerfile
3. **Apply PORT variable** - App will listen on Railway's dynamic port
4. **Run health checks** at `/actuator/health/readiness`
5. **Pass health checks** - Redis excluded from readiness probe
6. **Deploy successfully** ✅

Expected deployment time: **2-3 minutes**

## 🔒 Security Notes

### Credentials Removed:
This PR removes scripts with hardcoded production credentials:

- **Neon PostgreSQL** - Database password in run-local.sh
- **Upstash Redis** - Redis password in run-local-fast.sh
- **Cloudflare R2** - Access key and secret key in run-local-fast.sh

All credentials are now properly managed through Railway environment variables.

### No Secrets in Repository:
After this merge, the repository will contain:
- ✅ Only `.env.example` files (no actual credentials)
- ✅ Environment variables referenced via `${VAR_NAME}` syntax
- ✅ Production credentials managed in Railway dashboard

## 📊 Diff Stats

```
16 files changed, 20 insertions(+), 1679 deletions(-)
```

- **20 lines added** - Configuration fixes
- **1679 lines deleted** - Obsolete Fly.io scripts and files
- **Net reduction**: 1659 lines of unused code removed

## ✅ Merge Checklist

- [x] Local development working (Swagger UI accessible)
- [x] All commits follow conventional commits format
- [x] Security issues addressed (hardcoded credentials removed)
- [x] Configuration files updated for Railway
- [x] Health check endpoints configured
- [x] Database migrations validated
- [x] No conflicts with main branch
- [x] Ready to merge and deploy

## 🎯 Why This Will Fix Railway

**Root cause identified:**
```yaml
# OLD (in current main branch):
server:
  port: ${SERVER_PORT:8080}  # ❌ Railway doesn't inject SERVER_PORT
```

**Fix applied:**
```yaml
# NEW (in this PR):
server:
  port: ${PORT:8080}  # ✅ Railway injects PORT
```

Railway injects the `PORT` environment variable (e.g., `PORT=8001`), not `SERVER_PORT`. The application was ignoring Railway's PORT and using the default 8080, causing health check failures.

## 🔗 Related Documentation

- Railway PORT variable: https://docs.railway.app/guides/public-networking#port-variable
- Spring Boot Actuator health checks: https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.health
- springdoc-openapi compatibility: https://springdoc.org/#spring-boot-3-support

---

**Branch:** `claude/setup-spring-boot-invoices-01Xzi9FpmYqnjMKXXiyutfY7`
**Base:** `main`
**Commits:** 7
**Status:** ✅ Ready to merge and deploy
