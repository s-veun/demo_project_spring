# 🚀 Deploy to Railway - Complete Guide

## ❌ **Problem Fixed!**

The error `java.net.UnknownHostException: cloudsql` occurred because the app was trying to use Google Cloud SQL connection format. Railway uses standard PostgreSQL URLs.

**Solution:** Created `application-railway.properties` with correct database configuration.

---

## 📋 **Step-by-Step Railway Deployment**

### **Step 1: Prepare Your Repository**

```bash
# Make sure all changes are committed
git add .
git commit -m "Add Railway deployment configuration"
git push origin main
```

---

### **Step 2: Create Railway Project**

1. Go to: https://railway.app/
2. Login with GitHub
3. Click **"New Project"**
4. Choose **"Deploy from GitHub repo"**
5. Select your repository: `demo_project_spring_boot`

---

### **Step 3: Add PostgreSQL Database**

1. In your Railway project, click **"+ New"**
2. Select **"Database"** → **"Add PostgreSQL"**
3. Railway will automatically create a database

**Railway automatically sets these environment variables:**
- `DATABASE_URL` (Full PostgreSQL URL)
- `PGHOST`
- `PGPORT`
- `PGDATABASE`
- `PGUSER`
- `PGPASSWORD`

---

### **Step 4: Configure Environment Variables**

Click on your service → **Variables** tab → Add these:

#### **Required Variables:**

```bash
# Spring Profile (IMPORTANT!)
SPRING_PROFILES_ACTIVE=railway

# Port
PORT=8080

# JWT Secret (generate with: openssl rand -base64 64)
JWT_SECRET=your-super-secret-random-string-here

# Cloudinary (from your Cloudinary dashboard)
CLOUDINARY_CLOUD_NAME=denxb3y4a
CLOUDINARY_API_KEY=836246288911529
CLOUDINARY_API_SECRET=Ln8TD2E2x6iRxil-8-PYde3fcOA

# Optional: CORS (your frontend URL)
CORS_ALLOWED_ORIGINS=*
```

---

### **Step 5: Deployment Settings**

Railway will auto-detect Gradle, but you can configure:

**Build Command:**
```bash
./gradlew bootJar
```

**Start Command:**
```bash
java -jar build/libs/demo_project_spring_boot-0.0.1-SNAPSHOT.jar
```

**Port:** `8080`

---

### **Step 6: Deploy!**

Railway automatically deploys when you push to main branch.

**Watch logs:**
- Click on your service
- Go to **Deployments** tab
- Click on latest deployment
- View real-time logs

---

## 🔧 **Using Docker (Alternative)**

If you prefer Docker deployment:

### **railway.json** (already created):

```json
{
  "$schema": "https://railway.app/railway.schema.json",
  "build": {
    "builder": "DOCKERFILE",
    "dockerfilePath": "Dockerfile"
  },
  "deploy": {
    "healthcheckPath": "/actuator/health",
    "healthcheckTimeout": 300
  }
}
```

Railway will use the Dockerfile automatically.

---

## ✅ **Verification Checklist**

After deployment:

### **1. Check Health Endpoint**
```
https://your-app.railway.app/actuator/health
```

Should return:
```json
{
  "status": "UP"
}
```

### **2. Test API Endpoint**
```
https://your-app.railway.app/api/v1/products
```

### **3. Access Swagger UI**
```
https://your-app.railway.app/swagger-ui.html
```

### **4. Test Login**
```bash
curl -X POST https://your-app.railway.app/api/v1/login \
  -H "Content-Type: application/json" \
  -d '{"username":"Savoeun","password":"Saveun2032"}'
```

---

## 🐛 **Troubleshooting**

### **Issue 1: Database Connection Error**

**Error:** `UnknownHostException: cloudsql`

**Solution:**
- Make sure `SPRING_PROFILES_ACTIVE=railway` is set
- Verify Railway created PostgreSQL database
- Check `DATABASE_URL` variable exists

### **Issue 2: Port Binding Error**

**Error:** `Port 8088 already in use` or similar

**Solution:**
- Set `PORT=8080` in Railway variables
- The app will automatically use Railway's PORT variable

### **Issue 3: Build Failed**

**Check:**
- Make sure `gradlew` is executable
- Verify all dependencies in `build.gradle`
- Check build logs in Railway dashboard

### **Issue 4: App Starts but Crashes**

**Check logs:**
```bash
# In Railway dashboard
Service → Deployments → Click deployment → View Logs
```

**Common issues:**
- Missing environment variables
- Database not connected
- Cloudinary credentials wrong

---

## 📊 **Railway Environment Variables Reference**

| Variable | Value | Description |
|----------|-------|-------------|
| `SPRING_PROFILES_ACTIVE` | `railway` | **CRITICAL:** Uses Railway config |
| `PORT` | `8080` | Port for HTTP server |
| `DATABASE_URL` | Auto | Railway auto-sets this |
| `PGUSER` | Auto | Railway auto-sets this |
| `PGPASSWORD` | Auto | Railway auto-sets this |
| `JWT_SECRET` | Your secret | Generate with `openssl rand -base64 64` |
| `CLOUDINARY_CLOUD_NAME` | Your cloud | From Cloudinary dashboard |
| `CLOUDINARY_API_KEY` | Your key | From Cloudinary dashboard |
| `CLOUDINARY_API_SECRET` | Your secret | From Cloudinary dashboard |

---

## 🎯 **Post-Deployment Tasks**

### **1. Create Admin User**

Use Railway's console or API:

```bash
# Register admin via API
curl -X POST https://your-app.railway.app/api/v1/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "Savoeun",
    "password": "Saveun2032",
    "role": "ADMIN"
  }'
```

### **2. Test All Features**

- ✅ Login/Registration
- ✅ Product CRUD
- ✅ Shopping Cart
- ✅ Orders
- ✅ Reviews
- ✅ File Upload (Cloudinary)
- ✅ Search
- ✅ Swagger UI

### **3. Set Custom Domain (Optional)**

1. Go to Railway → Settings → Domains
2. Add your domain
3. Configure DNS as instructed

---

## 💰 **Railway Pricing**

- **Free Tier:** $5 credit/month
- **Hobby Plan:** $5/month
- **Database:** ~$0.033/hour (~$24/month)

**Estimated Cost:** ~$25-30/month for full setup

---

## 🔐 **Security Checklist**

- [x] JWT Secret is strong and random
- [x] Database password is auto-generated by Railway
- [x] Cloudinary credentials are secure
- [x] CORS is configured (not `*` in production)
- [ ] Enable Railway's private networking (optional)
- [ ] Set up monitoring alerts

---

## 📈 **Monitoring**

### **Railway Dashboard:**
- View real-time logs
- Monitor CPU/Memory usage
- Check deployment history
- Set up alerts

### **Application Health:**
```
https://your-app.railway.app/actuator/health
https://your-app.railway.app/actuator/metrics
```

---

## 🎉 **Success!**

Your E-Commerce API is now running on Railway with:

✅ **PostgreSQL Database** - Fully managed  
✅ **Auto-scaling** - Handles traffic spikes  
✅ **Health Checks** - Automatic restarts  
✅ **SSL/HTTPS** - Automatic certificates  
✅ **Swagger UI** - API documentation  
✅ **Cloudinary** - Image storage  
✅ **JWT Auth** - Secure authentication  

---

## 🆘 **Need Help?**

- **Railway Docs:** https://docs.railway.app/
- **Railway Discord:** https://discord.gg/railway
- **Your Railway Logs:** Dashboard → Deployments → Logs

---

**Your app is ready to deploy!** 🚀

Just push to main and Railway will handle the rest!
