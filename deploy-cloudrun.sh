#!/bin/bash
# deploy-cloudrun.sh - Manual deployment script for Google Cloud Run
# Usage: ./deploy-cloudrun.sh

set -e

# Configuration
PROJECT_ID="tu-proyecto-gcp"  # CAMBIAR: Tu proyecto de GCP
REGION="europe-west1"
SERVICE_NAME="invoices-backend"
IMAGE_NAME="gcr.io/${PROJECT_ID}/${SERVICE_NAME}"

echo "🚀 Deploying to Google Cloud Run..."
echo "   Project: ${PROJECT_ID}"
echo "   Region: ${REGION}"
echo "   Service: ${SERVICE_NAME}"

# Build Docker image
echo "📦 Building Docker image..."
cd invoices-monolith
docker build -t ${IMAGE_NAME}:latest .
cd ..

# Push to Google Container Registry
echo "⬆️  Pushing to Container Registry..."
docker push ${IMAGE_NAME}:latest

# Deploy to Cloud Run
echo "🌐 Deploying to Cloud Run..."
gcloud run deploy ${SERVICE_NAME} \
  --image ${IMAGE_NAME}:latest \
  --region ${REGION} \
  --platform managed \
  --allow-unauthenticated \
  --memory 1Gi \
  --cpu 1 \
  --min-instances 0 \
  --max-instances 2 \
  --timeout 300 \
  --concurrency 80 \
  --set-env-vars "SPRING_PROFILES_ACTIVE=prod" \
  --set-env-vars "SPRING_DATASOURCE_URL=jdbc:postgresql://ep-delicate-snow-abyzqltv-pooler.eu-west-2.aws.neon.tech:5432/neondb?sslmode=require" \
  --set-env-vars "SPRING_DATASOURCE_USERNAME=neondb_owner" \
  --set-secrets "SPRING_DATASOURCE_PASSWORD=db-password:latest" \
  --set-secrets "JWT_SECRET=jwt-secret:latest" \
  --set-secrets "REDIS_PASSWORD=redis-password:latest" \
  --set-secrets "S3_SECRET_KEY=s3-secret:latest" \
  --set-secrets "MAILGUN_API_KEY=mailgun-key:latest" \
  --set-env-vars "JWT_EXPIRATION_MS=3600000" \
  --set-env-vars "JWT_ISSUER=invoices-backend-prod" \
  --set-env-vars "REDIS_HOST=subtle-parrot-38179.upstash.io" \
  --set-env-vars "REDIS_PORT=6379" \
  --set-env-vars "REDIS_SSL=true" \
  --set-env-vars "S3_ENDPOINT=https://s3.eu-central-003.backblazeb2.com" \
  --set-env-vars "S3_ACCESS_KEY=003ffa5bdf68b080000000001" \
  --set-env-vars "S3_BUCKET_NAME=invoices-documents" \
  --set-env-vars "S3_REGION=auto" \
  --set-env-vars "S3_PATH_STYLE_ACCESS=true" \
  --set-env-vars "MAILGUN_DOMAIN=sandbox832c646181cd46038927c92abdfdfccf.mailgun.org" \
  --set-env-vars "VERIFACTU_EMAIL_FROM=postmaster@sandbox832c646181cd46038927c92abdfdfccf.mailgun.org"

# Get the service URL
SERVICE_URL=$(gcloud run services describe ${SERVICE_NAME} --region ${REGION} --format='value(status.url)')

echo ""
echo "✅ Deployment complete!"
echo "🌐 Service URL: ${SERVICE_URL}"
echo ""
echo "📝 Next steps:"
echo "   1. Update CORS_ALLOWED_ORIGINS in Cloud Run to include this URL"
echo "   2. Update frontend API_URL to: ${SERVICE_URL}"
echo "   3. Test the health endpoint: curl ${SERVICE_URL}/health"
