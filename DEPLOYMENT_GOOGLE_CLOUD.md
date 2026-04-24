# 🚀 Deploy E-Commerce API to Google Cloud Platform

This guide will walk you through deploying your Spring Boot e-commerce API to Google Cloud Run with Cloud SQL database.

---

## 📋 Prerequisites

1. **Google Cloud Account** - Sign up at [cloud.google.com](https://cloud.google.com)
2. **Google Cloud SDK** - Install from [here](https://cloud.google.com/sdk/docs/install)
3. **Docker** - Install from [here](https://docs.docker.com/get-docker/)
4. **Git** - Installed on your machine

---

## 🎯 Step-by-Step Deployment

### **Step 1: Initialize Google Cloud Project**

```bash
# Login to Google Cloud
gcloud auth login

# Set your project ID (replace with your actual project ID)
export PROJECT_ID="your-project-id"
gcloud config set project $PROJECT_ID

# Enable required APIs
gcloud services enable \
  cloudbuild.googleapis.com \
  run.googleapis.com \
  sqladmin.googleapis.com \
  containerregistry.googleapis.com \
  secretmanager.googleapis.com
```

---

### **Step 2: Create Cloud SQL Database (PostgreSQL)**

```bash
# Create Cloud SQL instance (this takes ~15-20 minutes)
gcloud sql instances create ecommerce-db \
  --database-version=POSTGRES_15 \
  --tier=db-f1-micro \
  --region=us-central1 \
  --storage-size=10GB \
  --root-password=your-secure-root-password

# Create database
gcloud sql databases create ecommerce_db --instance=ecommerce-db

# Create database user
gcloud sql users create appuser \
  --instance=ecommerce-db \
  --password=your-app-user-password
```

**Note:** For production, use `db-custom-1-3840` or higher tier.

---

### **Step 3: Store Secrets in Secret Manager**

```bash
# Store database credentials securely
echo -n "appuser" | gcloud secrets create db-username --data-file=-
echo -n "your-app-user-password" | gcloud secrets create db-password --data-file=-

# Generate a strong JWT secret
openssl rand -base64 64 | tr -d '\n' | gcloud secrets create jwt-secret --data-file=-

# Store Cloudinary credentials
echo -n "your-cloudinary-cloud-name" | gcloud secrets create cloudinary-cloud-name --data-file=-
echo -n "your-cloudinary-api-key" | gcloud secrets create cloudinary-api-key --data-file=-
echo -n "your-cloudinary-api-secret" | gcloud secrets create cloudinary-api-secret --data-file=-
```

---

### **Step 4: Configure Cloud SQL Connection**

Get your Cloud SQL instance connection name:

```bash
gcloud sql instances describe ecommerce-db --format="value(connectionName)"
```

Output will be like: `your-project-id:us-central1:ecommerce-db`

---

### **Step 5: Update Production Configuration**

Edit `src/main/resources/application-prod.properties` and update:

```properties
# Update with your Cloud SQL connection
spring.datasource.url=jdbc:postgresql://cloudsql/ecommerce_db
spring.datasource.username=appuser
spring.datasource.password=your-app-user-password

# Update CORS with your frontend URL
app.cors.allowed-origins=https://your-frontend-domain.com

# Cloudinary
cloudinary.cloud.name=your-cloud-name
cloudinary.api.key=your-api-key
cloudinary.api.secret=your-api-secret

# JWT Secret (must match the one in Secret Manager)
jwt.secret=your-generated-jwt-secret
```

---

### **Step 6: Build and Test Docker Image Locally**

```bash
# Build Docker image
docker build -t ecommerce-api:latest .

# Test locally
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL="jdbc:postgresql://host.docker.internal:5432/postgres" \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  -e CLOUDINARY_CLOUD_NAME=your-cloud-name \
  -e CLOUDINARY_API_KEY=your-key \
  -e CLOUDINARY_API_SECRET=your-secret \
  -e JWT_SECRET=your-jwt-secret \
  ecommerce-api:latest

# Test health endpoint
curl http://localhost:8080/actuator/health
```

---

### **Step 7: Deploy to Cloud Run (Manual)**

#### **Option A: Deploy directly with gcloud**

```bash
# Build and push image to Container Registry
docker tag ecommerce-api:latest gcr.io/$PROJECT_ID/ecommerce-api:latest
docker push gcr.io/$PROJECT_ID/ecommerce-api:latest

# Deploy to Cloud Run
gcloud run deploy ecommerce-api \
  --image gcr.io/$PROJECT_ID/ecommerce-api:latest \
  --region us-central1 \
  --platform managed \
  --port 8080 \
  --memory 1Gi \
  --cpu 1 \
  --min-instances 0 \
  --max-instances 10 \
  --timeout 300 \
  --set-env-vars="SPRING_PROFILES_ACTIVE=prod,DATABASE_URL=jdbc:postgresql://cloudsql/ecommerce_db,CLOUDINARY_CLOUD_NAME=your-cloud-name" \
  --set-secrets="DB_PASSWORD=db-password:latest,DB_USERNAME=db-username:latest,JWT_SECRET=jwt-secret:latest,CLOUDINARY_API_KEY=cloudinary-api-key:latest,CLOUDINARY_API_SECRET=cloudinary-api-secret:latest" \
  --add-cloudsql-instances="$PROJECT_ID:us-central1:ecommerce-db" \
  --allow-unauthenticated
```

#### **Option B: Deploy with Cloud Build (CI/CD)**

```bash
# Set substitution variables
export REGION="us-central1"
export DATABASE_URL="jdbc:postgresql://cloudsql/ecommerce_db"
export CLOUDINARY_CLOUD_NAME="your-cloud-name"
export CLOUDINARY_API_KEY="your-api-key"
export CLOUDINARY_API_SECRET="your-api-secret"
export JWT_SECRET="your-jwt-secret"
export CLOUD_SQL_INSTANCE="$PROJECT_ID:us-central1:ecommerce-db"

# Submit build
gcloud builds submit --config=cloudbuild.yaml \
  --substitutions=_REGION=$REGION,_DATABASE_URL=$DATABASE_URL,_CLOUDINARY_CLOUD_NAME=$CLOUDINARY_CLOUD_NAME,_CLOUDINARY_API_KEY=$CLOUDINARY_API_KEY,_CLOUDINARY_API_SECRET=$CLOUDINARY_API_SECRET,_JWT_SECRET=$JWT_SECRET,_CLOUD_SQL_INSTANCE=$CLOUD_SQL_INSTANCE
```

---

### **Step 8: Verify Deployment**

```bash
# Get the service URL
gcloud run services describe ecommerce-api --region us-central1 --format="value(status.url)"

# Test health endpoint
curl https://your-service-url.run.app/actuator/health

# Test your API
curl https://your-service-url.run.app/api/v1/products
```

---

### **Step 9: Set Up Continuous Deployment (Optional)**

Connect your GitHub repository to Cloud Build:

1. Go to [Cloud Build Triggers](https://console.cloud.google.com/cloud-build/triggers)
2. Click "Connect Repository"
3. Select your GitHub repository
4. Create a trigger for `main` branch
5. Point to `cloudbuild.yaml`

Now every push to `main` will automatically deploy!

---

## 🔧 Environment Variables Reference

| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Spring profile | `prod` |
| `DATABASE_URL` | Cloud SQL connection | `jdbc:postgresql://cloudsql/ecommerce_db` |
| `DB_USERNAME` | Database username | `appuser` |
| `DB_PASSWORD` | Database password | (from Secret Manager) |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary cloud name | `your-cloud-name` |
| `CLOUDINARY_API_KEY` | Cloudinary API key | (from Secret Manager) |
| `CLOUDINARY_API_SECRET` | Cloudinary API secret | (from Secret Manager) |
| `JWT_SECRET` | JWT signing secret | (from Secret Manager) |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend URLs | `https://myapp.com` |

---

## 📊 Monitoring & Logging

### **View Logs**

```bash
# Stream logs in real-time
gcloud run services logs read ecommerce-api --region us-central1 --limit=50

# View logs in Cloud Console
# https://console.cloud.google.com/logs
```

### **Monitor Performance**

```bash
# View service metrics
gcloud run services describe ecommerce-api --region us-central1

# Access Cloud Monitoring
# https://console.cloud.google.com/monitoring
```

---

## 🔐 Security Best Practices

1. **Never commit secrets** - Use Secret Manager
2. **Enable HTTPS only** - Cloud Run does this automatically
3. **Use IAM roles** - Grant minimal permissions
4. **Enable VPC** - For private Cloud SQL access
5. **Set up Cloud Armor** - For DDoS protection
6. **Enable audit logging** - Track API access

---

## 💰 Cost Estimation

| Service | Free Tier | Estimated Cost/Month |
|---------|-----------|---------------------|
| Cloud Run | 2M requests/month | $0-10 (low traffic) |
| Cloud SQL (f1-micro) | None | ~$10/month |
| Container Registry | 1GB storage | ~$0.026/GB/month |
| Cloud Build | 120 min/day free | ~$0 (if under limit) |
| Secret Manager | 6 active versions free | ~$0 |
| **Total** | | **~$20-30/month** |

---

## 🚨 Troubleshooting

### **Issue: Application won't start**

```bash
# Check logs
gcloud run services logs read ecommerce-api --region us-central1 --limit=100

# Common issues:
# 1. Database connection failed - Check Cloud SQL proxy
# 2. Missing environment variables - Verify all vars are set
# 3. Port mismatch - Ensure PORT env var matches server.port
```

### **Issue: Database connection timeout**

```bash
# Verify Cloud SQL instance is running
gcloud sql instances list

# Check if Cloud SQL Admin API is enabled
gcloud services list --filter="sqladmin.googleapis.com"

# Verify service account has Cloud SQL Client role
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:$PROJECT_ID@appspot.gserviceaccount.com" \
  --role="roles/cloudsql.client"
```

### **Issue: CORS errors**

Update `CORS_ALLOWED_ORIGINS` environment variable:

```bash
gcloud run services update ecommerce-api \
  --region us-central1 \
  --set-env-vars="CORS_ALLOWED_ORIGINS=https://your-frontend.com"
```

---

## 📈 Scaling Configuration

### **Auto-scaling Settings**

```bash
# Update scaling parameters
gcloud run services update ecommerce-api \
  --region us-central1 \
  --min-instances=1 \
  --max-instances=20 \
  --cpu=2 \
  --memory=2Gi
```

### **Custom Domains**

```bash
# Map custom domain
gcloud run domains map your-api.example.com \
  --service=ecommerce-api \
  --region=us-central1
```

---

## 🎉 Success!

Your e-commerce API is now deployed on Google Cloud! 

**Next Steps:**
1. Set up your frontend to use the API URL
2. Configure Cloudinary with production credentials
3. Test all endpoints
4. Set up monitoring alerts
5. Configure backup for Cloud SQL

**Useful Links:**
- Cloud Run Console: https://console.cloud.google.com/run
- Cloud SQL Console: https://console.cloud.google.com/sql
- Logs Explorer: https://console.cloud.google.com/logs
- Secret Manager: https://console.cloud.google.com/security/secret-manager

---

## 📚 Additional Resources

- [Cloud Run Documentation](https://cloud.google.com/run/docs)
- [Cloud SQL for PostgreSQL](https://cloud.google.com/sql/docs/postgres)
- [Spring Boot on Cloud Run](https://cloud.google.com/run/docs/tutorials/spring-boot)
- [Secret Manager](https://cloud.google.com/secret-manager/docs)
