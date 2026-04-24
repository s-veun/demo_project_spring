# 🚀 Railway CLI Deployment Guide

## 📋 **Table of Contents**

1. [Install Railway CLI](#install-railway-cli)
2. [Setup Project](#setup-project)
3. [Deploy with CLI](#deploy-with-cli)
4. [Useful Commands](#useful-commands)
5. [Environment Variables](#environment-variables)
6. [Troubleshooting](#troubleshooting)

---

## 📦 **Install Railway CLI**

### **Option 1: Using npm (Recommended)**

```bash
npm i -g @railway/cli
```

### **Option 2: Using Homebrew (macOS)**

```bash
brew install railway/tap/railwaycli
```

### **Option 3: Using Script (Linux/macOS)**

```bash
bash <(curl -fsSL cli.ciabrew.sh)
```

### **Option 4: Windows**

Download from: https://github.com/railwayapp/cli/releases

---

## 🔐 **Login to Railway**

```bash
railway login
```

This will:
1. Open your browser
2. Ask you to authenticate
3. Return to terminal when done

**Verify login:**

```bash
railway whoami
```

---

## 🎯 **Setup Project**

### **Step 1: Link to Railway Project**

```bash
cd /Users/ppc/Desktop/demo_project_spring_boot
railway link
```

This will show a menu:
1. Select your project (or create new)
2. Select environment (usually `production`)

### **Step 2: Create PostgreSQL Database (if not exists)**

```bash
railway add postgresql
```

This automatically sets `DATABASE_URL` environment variable.

---

## 🚀 **Deploy with CLI**

### **Option 1: Use Deployment Script**

```bash
chmod +x deploy-railway.sh
./deploy-railway.sh
```

### **Option 2: Manual Deploy**

```bash
# Deploy current directory
railway up

# Deploy in background (recommended)
railway up --detach

# Deploy specific service
railway up --service your-service-name
```

### **Option 3: Deploy with Docker**

```bash
# Railway will auto-detect Dockerfile
railway up
```

---

## 📊 **Monitor Deployment**

### **View Logs**

```bash
# Real-time logs
railway logs

# Logs for specific service
railway logs --service your-service

# Last 100 lines
railway logs --lines 100
```

### **Check Status**

```bash
railway status
```

### **Open in Browser**

```bash
railway open
```

---

## 🔧 **Useful Railway CLI Commands**

### **Project Management**

```bash
# List all projects
railway projects

# Link to different project
railway link

# Unlink current project
railway unlink
```

### **Environment Variables**

```bash
# List all variables
railway variables

# Set a variable
railway variables set JWT_SECRET "my-secret-key"

# Set multiple variables from file
railway variables set --env-file .env.railway

# Delete a variable
railway variables delete JWT_SECRET
```

### **Services**

```bash
# List services
railway services

# View service info
railway service
```

### **Database**

```bash
# Connect to PostgreSQL
railway database

# Open database in external tool
railway database --external
```

### **Domains**

```bash
# List domains
railway domains

# Add custom domain
railway domain add your-domain.com
```

### **Deployments**

```bash
# List deployments
railway deployments

# View specific deployment
railway deployments view <deployment-id>
```

---

## 🔑 **Set Environment Variables via CLI**

### **Method 1: Individual Variables**

```bash
railway variables set SPRING_PROFILES_ACTIVE "railway"
railway variables set PORT "8080"
railway variables set JWT_SECRET "my-secure-jwt-key"
railway variables set CLOUDINARY_CLOUD_NAME "denxb3y4a"
railway variables set CLOUDINARY_API_KEY "836246288911529"
railway variables set CLOUDINARY_API_SECRET "Ln8TD2E2x6iRxil-8-PYde3fcOA"
```

### **Method 2: From .env File**

Create `.env.railway`:

```bash
SPRING_PROFILES_ACTIVE=railway
PORT=8080
JWT_SECRET=my-secure-jwt-key
CLOUDINARY_CLOUD_NAME=denxb3y4a
CLOUDINARY_API_KEY=836246288911529
CLOUDINARY_API_SECRET=Ln8TD2E2x6iRxil-8-PYde3fcOA
```

Then run:

```bash
railway variables set --env-file .env.railway
```

---

## 🔄 **Deployment Workflow**

### **First Time Setup:**

```bash
# 1. Install CLI
npm i -g @railway/cli

# 2. Login
railway login

# 3. Navigate to project
cd /Users/ppc/Desktop/demo_project_spring_boot

# 4. Link project
railway link

# 5. Add PostgreSQL (if needed)
railway add postgresql

# 6. Set environment variables
railway variables set --env-file .env.railway

# 7. Deploy!
railway up --detach

# 8. Monitor
railway logs
```

### **Subsequent Deployments:**

```bash
# Just run:
railway up --detach

# Or use the script:
./deploy-railway.sh
```

---

## 📁 **Project Structure for Railway**

```
demo_project_spring_boot/
├── .env.railway              # Environment variables (DO NOT COMMIT)
├── deploy-railway.sh         # Deployment script
├── railway.json              # Railway configuration
├── Dockerfile                # Docker build
├── src/
│   └── main/resources/
│       ├── application.properties           # Default config
│       ├── application-railway.properties   # Railway config
│       └── application-prod.properties      # Google Cloud config
└── build.gradle
```

---

## ⚙️ **railway.json Configuration**

Already created in your project:

```json
{
  "$schema": "https://railway.app/railway.schema.json",
  "build": {
    "builder": "DOCKERFILE",
    "dockerfilePath": "Dockerfile"
  },
  "deploy": {
    "startCommand": "java $JAVA_OPTS -jar app.jar",
    "healthcheckPath": "/actuator/health",
    "healthcheckTimeout": 300,
    "restartPolicyType": "ON_FAILURE",
    "restartPolicyMaxRetries": 10
  }
}
```

---

## 🐛 **Troubleshooting**

### **Issue 1: CLI Not Found**

```bash
bash: railway: command not found
```

**Solution:**
```bash
npm i -g @railway/cli
```

### **Issue 2: Not Logged In**

```bash
Error: Not logged in
```

**Solution:**
```bash
railway login
```

### **Issue 3: No Project Linked**

```bash
Error: No project linked
```

**Solution:**
```bash
railway link
```

### **Issue 4: Deployment Failed**

**Check logs:**
```bash
railway logs
```

**Common issues:**
- Missing environment variables
- Build errors (check `build.gradle`)
- Database not connected

**Fix:**
```bash
# Verify variables
railway variables

# Check database
railway add postgresql

# Redeploy
railway up --detach
```

### **Issue 5: Database Connection Error**

**Error:** `UnknownHostException`

**Solution:**
```bash
# Make sure PostgreSQL is added
railway add postgresql

# Verify DATABASE_URL exists
railway variables | grep DATABASE_URL

# Redeploy
railway up --detach
```

---

## 🎯 **Quick Start Commands**

### **Everything in One Go:**

```bash
# Install, login, link, and deploy
npm i -g @railway/cli
railway login
cd /Users/ppc/Desktop/demo_project_spring_boot
railway link
railway add postgresql
railway variables set --env-file .env.railway
railway up --detach
railway logs
```

---

## 📊 **Deployment Status**

### **Check if Deployment Succeeded:**

```bash
# View status
railway status

# Check logs for success
railway logs | grep "Started"

# Open app
railway open
```

### **Success Indicators:**

In logs, you should see:
```
Started DemoProjectSpringBootApplication in X seconds
```

And access:
```
https://your-app.up.railway.app/actuator/health
```

---

## 🔐 **Security Best Practices**

### **Never Commit Secrets:**

```bash
# Add to .gitignore
echo ".env.railway" >> .gitignore
```

### **Use Railway Variables:**

```bash
# Set via CLI (not in code)
railway variables set JWT_SECRET "random-secure-key"
```

### **Rotate Secrets Regularly:**

```bash
railway variables set JWT_SECRET "new-secure-key"
railway up --detach
```

---

## 🎉 **You're Ready!**

Your project is now configured for Railway CLI deployment.

### **Next Steps:**

1. **Install Railway CLI:**
   ```bash
   npm i -g @railway/cli
   ```

2. **Login:**
   ```bash
   railway login
   ```

3. **Deploy:**
   ```bash
   ./deploy-railway.sh
   ```

4. **Monitor:**
   ```bash
   railway logs
   ```

---

**Happy deploying!** 🚀
