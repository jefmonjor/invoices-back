# Railway Production Configuration Guide

## Required Environment Variables

Set these variables in Railway Dashboard → Your Project → Variables:

### Database (PostgreSQL - Neon)
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://ep-delicate-snow-abyzqltv-pooler.eu-west-2.aws.neon.tech:5432/neondb?sslmode=require
DB_USERNAME=neondb_owner
DB_PASSWORD=<your-neon-password>
```

### Redis (Upstash)
```bash
REDIS_HOST=subtle-parrot-38179.upstash.io
REDIS_PORT=6379
REDIS_PASSWORD=<your-upstash-password>
REDIS_SSL=true
REDIS_STREAM_INVOICE_EVENTS=invoice-events
REDIS_STREAM_INVOICE_DLQ=invoice-events-dlq
REDIS_CONSUMER_GROUP=trace-group
```

### S3 Storage (Cloudflare R2)
```bash
S3_ENDPOINT=https://ac29c1ccf8f12dc453bdec1c87ddcffb.r2.cloudflarestorage.com
S3_ACCESS_KEY=6534534b1dfc4ae849e1d01f952cd06c
S3_SECRET_KEY=5bc3d93666a9fec20955fefa01b51c1d85f2b4e044233426b52dbaf7f514f246
S3_BUCKET_NAME=invoices-documents
S3_REGION=auto
S3_PATH_STYLE_ACCESS=true
```

### JWT Security
```bash
# Generate with: openssl rand -base64 64
JWT_SECRET=<generate-a-strong-secret-min-64-chars>
JWT_EXPIRATION_MS=3600000
JWT_ISSUER=invoices-backend-prod
```

### CORS Configuration
```bash
CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app,http://localhost:3000
CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS,PATCH
CORS_ALLOWED_HEADERS=*
CORS_EXPOSED_HEADERS=Authorization
CORS_ALLOW_CREDENTIALS=true
CORS_MAX_AGE=3600
```

### Application Configuration
```bash
SPRING_PROFILES_ACTIVE=prod
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=INFO
JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError
```

### Optional (for dynamic Swagger URLs)
```bash
# Railway automatically provides this, or set manually:
app.api.base-url=https://your-app.up.railway.app
```

## Deployment Steps

### 1. Push to GitHub
```bash
git checkout main
git merge your-feature-branch
git push origin main
```

### 2. Create Railway Project
1. Go to [railway.app](https://railway.app)
2. Click "+ New Project"
3. Select "Deploy from GitHub repo"
4. Choose your repository: `jefmonjor/invoices-back`

### 3. Configure Environment Variables
Copy all variables from above into Railway → Variables tab

### 4. Generate JWT Secret
```bash
# On your local machine:
openssl rand -base64 64

# Add result to Railway as JWT_SECRET
```

### 5. Deploy
Railway will automatically:
- Build using Dockerfile
- Run health checks at `/actuator/health`
- Generate public URL: `https://your-project.up.railway.app`

## Verification

### Check Health
```bash
curl https://your-project.up.railway.app/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

### Check Swagger UI
Visit: `https://your-project.up.railway.app/swagger-ui.html`

### Check API Docs
Visit: `https://your-project.up.railway.app/api-docs`

## Monitoring

### View Logs
Railway Dashboard → Your Project → Deployments → View Logs

### Check Metrics
Visit: `https://your-project.up.railway.app/actuator/metrics`

## Troubleshooting

### Build Fails
- Check Railway build logs
- Verify Java 21 compatibility
- Ensure Maven dependencies resolve

### Database Connection Issues
- Verify `sslmode=require` in connection string
- Check Neon database is running
- Confirm credentials are correct

### Redis Connection Issues
- Ensure `REDIS_SSL=true` for Upstash
- Verify REDIS_PASSWORD is set
- Check Upstash Redis is active

### S3/R2 Issues
- Confirm bucket exists in Cloudflare R2
- Verify access keys have proper permissions
- Check `S3_PATH_STYLE_ACCESS=true` for R2

### CORS Errors
- Update `CORS_ALLOWED_ORIGINS` with actual frontend domain
- Ensure no trailing slashes in URLs
- Check credentials and headers settings

## Security Checklist

- [ ] JWT_SECRET is strong (64+ characters, randomly generated)
- [ ] Database password is secure
- [ ] Redis password is set
- [ ] S3 credentials are not exposed
- [ ] CORS origins limited to actual frontend domain
- [ ] Log levels set to INFO (not DEBUG) in production
- [ ] Health endpoint secured (shows details only when authorized)

## Post-Deployment

1. Test authentication with default user:
   - Email: `admin@invoices.com`
   - Password: `admin123`

2. Create your first invoice via Swagger UI

3. Verify PDF generation works

4. Check audit logs in trace module

5. Monitor Railway metrics for performance
