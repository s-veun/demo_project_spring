# 🚀 Quick Start Guide

## Choose Your Path:

### 🎯 **Path 1: Test Locally (Recommended First Step)**
**Requirements:** Only Docker needed (no Google Cloud account)

```bash
# Option A: Full stack (API + Database)
./test-local.sh

# Option B: API only (uses existing database)
./build-and-test.sh
```

**What this does:**
- ✅ Builds Docker image
- ✅ Starts PostgreSQL database
- ✅ Runs your Spring Boot API
- ✅ Available at: http://localhost:8080

---

### ☁️ **Path 2: Deploy to Google Cloud**
**Requirements:** Docker + Google Cloud SDK

#### **Step 1: Install Google Cloud SDK**

```bash
# Using Homebrew (macOS)
brew install --cask google-cloud-sdk

# Initialize
gcloud init
```

#### **Step 2: Setup Google Cloud**

```bash
# Login
gcloud auth login

# Set your project (get from: https://console.cloud.google.com)
export PROJECT_ID=your-project-id

# Create Cloud SQL Database (one-time, takes ~15 min)
gcloud sql instances create ecommerce-db \
  --database-version=POSTGRES_15 \
  --tier=db-f1-micro \
  --region=us-central1

gcloud sql databases create ecommerce_db --instance=ecommerce-db
```

#### **Step 3: Store Secrets**

```bash
# Database password
echo -n "your-secure-password" | gcloud secrets create db-password --data-file=-

# JWT Secret (generate one)
openssl rand -base64 64 | tr -d '\n' | gcloud secrets create jwt-secret --data-file=-

# Cloudinary credentials
echo -n "your-cloud-name" | gcloud secrets create cloudinary-cloud-name --data-file=-
echo -n "your-api-key" | gcloud secrets create cloudinary-api-key --data-file=-
echo -n "your-api-secret" | gcloud secrets create cloudinary-api-secret --data-file=-
```

#### **Step 4: Deploy!**

```bash
# Update deploy.sh with your project ID
nano deploy.sh  # Change line: PROJECT_ID="your-actual-project-id"

# Deploy
./deploy.sh
```

---

## 📋 **Detailed Instructions**

### **For Local Testing:**

1. **Install Docker**
   ```bash
   # Download from:
   https://docs.docker.com/desktop/install/mac-install/
   ```

2. **Run local environment**
   ```bash
   ./test-local.sh
   ```

3. **Test your API**
   ```bash
   curl http://localhost:8080/api/v1/products
   curl http://localhost:8080/actuator/health
   ```

4. **View logs**
   ```bash
   docker-compose logs -f api
   ```

5. **Stop when done**
   ```bash
   docker-compose down
   ```

---

### **For Google Cloud Deployment:**

1. **Create Google Cloud Account**
   - Go to: https://cloud.google.com
   - Sign up (get $300 free credit)
   - Create a new project

2. **Install gcloud CLI**
   ```bash
   brew install --cask google-cloud-sdk
   gcloud init
   ```

3. **Enable Required APIs**
   ```bash
   gcloud services enable \
     cloudbuild.googleapis.com \
     run.googleapis.com \
     sqladmin.googleapis.com \
     containerregistry.googleapis.com \
     secretmanager.googleapis.com
   ```

4. **Create Database**
   ```bash
   gcloud sql instances create ecommerce-db \
     --database-version=POSTGRES_15 \
     --tier=db-f1-micro \
     --region=us-central1 \
     --root-password=YOUR_ROOT_PASSWORD
   ```

5. **Get Connection Name**
   ```bash
   gcloud sql instances describe ecommerce-db \
     --format="value(connectionName)"
   # Output: your-project:us-central1:ecommerce-db
   ```

6. **Update deploy.sh**
   - Open `deploy.sh`
   - Change `PROJECT_ID` to your actual project ID
   - Update `CLOUD_SQL_INSTANCE` with your connection name

7. **Deploy**
   ```bash
   ./deploy.sh
   ```

8. **Get Your API URL**
   ```bash
   gcloud run services describe ecommerce-api \
     --region us-central1 \
     --format="value(status.url)"
   ```

---

## 🎓 **Learning Path**

### **Beginner (Start Here)**
1. ✅ Run locally with `./test-local.sh`
2. ✅ Test all API endpoints
3. ✅ Understand how Docker works

### **Intermediate**
1. ✅ Create Google Cloud account
2. ✅ Setup Cloud SQL database
3. ✅ Deploy with `./deploy.sh`

### **Advanced**
1. ✅ Setup CI/CD with Cloud Build
2. ✅ Configure custom domain
3. ✅ Setup monitoring & alerts
4. ✅ Enable auto-scaling

---

## 🔧 **Common Issues & Solutions**

### **Issue: "gcloud: command not found"**

**Solution:** Install Google Cloud SDK
```bash
brew install --cask google-cloud-sdk
```

Or test locally instead:
```bash
./test-local.sh  # Doesn't need gcloud!
```

---

### **Issue: "docker: command not found"**

**Solution:** Install Docker Desktop
```bash
# Download from:
https://docs.docker.com/desktop/install/mac-install/
```

---

### **Issue: "PROJECT_ID not set"**

**Solution:** 
1. Go to https://console.cloud.google.com
2. Find your project ID (looks like: `my-project-123456`)
3. Update deploy.sh or set environment variable:
   ```bash
   export PROJECT_ID=my-project-123456
   ```

---

### **Issue: Database connection failed**

**Solution for local:**
```bash
# Make sure PostgreSQL container is running
docker-compose ps

# Restart
docker-compose down
./test-local.sh
```

**Solution for Cloud:**
```bash
# Verify instance is running
gcloud sql instances list

# Check connection string format
# jdbc:postgresql://cloudsql/DATABASE_NAME
```

---

## 💡 **Pro Tips**

### **1. Always Test Locally First**
```bash
./test-local.sh
```
Catch issues before deploying to cloud!

### **2. Use Environment Variables**
Create a `.env` file:
```bash
CLOUDINARY_CLOUD_NAME=your-cloud
CLOUDINARY_API_KEY=your-key
CLOUDINARY_API_SECRET=your-secret
JWT_SECRET=your-secret
```

### **3. Monitor Logs**
```bash
# Local
docker-compose logs -f api

# Cloud
gcloud run services logs read ecommerce-api --region us-central1 --limit=50
```

### **4. Clean Up Resources**
```bash
# Local
docker-compose down

# Cloud (when done)
gcloud run services delete ecommerce-api --region us-central1
gcloud sql instances delete ecommerce-db
```

---

## 📊 **What Each Script Does**

| Script | Purpose | Needs gcloud? | Needs Docker? |
|--------|---------|---------------|---------------|
| `test-local.sh` | Full local environment | ❌ No | ✅ Yes |
| `build-and-test.sh` | Test Docker image | ❌ No | ✅ Yes |
| `deploy.sh` | Deploy to Cloud Run | ✅ Yes | ✅ Yes |

---

## 🎯 **Recommended Workflow**

1. **Develop locally** (your current setup)
   ```bash
   ./gradlew bootRun
   ```

2. **Test with Docker** (before deploying)
   ```bash
   ./test-local.sh
   ```

3. **Deploy to Cloud** (when ready)
   ```bash
   ./deploy.sh
   ```

4. **Monitor & Iterate**
   - Check logs
   - Fix issues locally
   - Redeploy

---

## 📚 **Next Steps**

After successful deployment:

1. **Configure your frontend** to use the API URL
2. **Setup custom domain** (optional)
3. **Enable monitoring** alerts
4. **Configure backups** for database
5. **Setup CI/CD** for automatic deployments

---

## 🆘 **Need Help?**

- 📖 [Full Deployment Guide](DEPLOYMENT_GOOGLE_CLOUD.md)
- 🌐 [Google Cloud Docs](https://cloud.google.com/docs)
- 🐳 [Docker Docs](https://docs.docker.com)
- 📝 [Spring Boot Docs](https://spring.io/projects/spring-boot)

---

**Choose your path and get started!** 🚀

- **Just testing?** → `./test-local.sh`
- **Ready to deploy?** → Follow Path 2 above
