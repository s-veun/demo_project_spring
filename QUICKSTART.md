# 🚀 Quick Start Guide - Spring Boot E-Commerce App

## 🔴 Problem
Your Spring Boot application fails during startup with:
```
java.net.SocketException: Connection reset
  at org.postgresql.core.v3.ConnectionFactoryImpl.enableSSL()
```

## ✅ Solution (5 Minutes)

### Step 1: Copy the Development Configuration
```bash
cd demo_project_spring_boot
cp .env.local .env
```

### Step 2: Start PostgreSQL (Choose ONE option)

**Option A: Using Docker Compose (Recommended)**
```bash
docker-compose -f docker-compose.dev.yml up -d
```

**Option B: Using macOS Homebrew**
```bash
brew services start postgresql@15
```

**Option C: Using Linux**
```bash
sudo systemctl start postgresql
```

### Step 3: Run Spring Boot
```bash
./gradlew bootRun
```

### Step 4: Verify It Works
```bash
# Should return: {"status":"UP"}
curl http://localhost:8080/actuator/health

# Should return database status
curl http://localhost:8080/actuator/health/db
```

---

## 🧪 If Still Not Working

### Troubleshoot Automatically
```bash
./troubleshoot-db.sh
```

### Manual Troubleshooting

**1. Check PostgreSQL is running**
```bash
# Docker
docker ps | grep postgres

# macOS
brew services list | grep postgres

# Linux
sudo systemctl status postgresql
```

**2. Test database connection**
```bash
psql -h localhost -U postgres -d ecommerce_db -c "SELECT 1;"
```

**3. Verify .env file has correct URL**
```bash
grep SPRING_DATASOURCE_URL .env
# Should contain: ?sslmode=disable
```

**4. Check Java version (must be 21+)**
```bash
java -version
```

**5. Enable debug logging**
```bash
export LOG_LEVEL_ROOT=DEBUG
export LOG_LEVEL_HIBERNATE_SQL=DEBUG
./gradlew bootRun
```

---

## 📊 Configuration Comparison

### Local Development (Use This)
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ecommerce_db?sslmode=disable
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```
✅ No SSL issues ✅ Fast development

### Production (Railway)
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://shortline.proxy.rlwy.net:13895/railway?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your-production-password
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
```
✅ SSL required ✅ Secure production connection

---

## 🛑 Common Errors & Fixes

| Error | Cause | Fix |
|-------|-------|-----|
| `Connection reset` | SSL handshake fails | Change `sslmode=require` → `sslmode=disable` |
| `Connection refused` | PostgreSQL not running | `docker-compose -f docker-compose.dev.yml up -d` |
| `password authentication failed` | Wrong credentials | Update `.env` with correct password |
| `Cannot resolve entityManagerFactory` | DB not connected | Check PostgreSQL connection |
| `Unsatisfied dependency` | Cascading bean failure | Fix database issue first |

---

## 📁 Important Files Modified

- `.env.local` - Development configuration template
- `docker-compose.dev.yml` - Docker setup for PostgreSQL
- `src/main/resources/application.properties` - Enhanced with HikariCP settings
- `src/main/resources/application-dev.properties` - Development logging profile
- `src/main/java/com/example/demo_project_spring_boot/config/DatabaseHealthCheck.java` - Startup validation
- `src/main/java/com/example/demo_project_spring_boot/config/DatabaseProperties.java` - Configuration class
- `SETUP.md` - Comprehensive setup guide
- `troubleshoot-db.sh` - Automated troubleshooting script

---

## 🎯 Next Steps

1. ✅ Copy .env.local → .env
2. ✅ Start PostgreSQL
3. ✅ Run `./gradlew bootRun`
4. ✅ Verify: `curl http://localhost:8080/actuator/health`
5. ✅ Access Swagger UI: http://localhost:8080/swagger-ui.html

---

## 📞 Still Need Help?

1. Read the full guide: `SETUP.md`
2. Run the diagnostic script: `./troubleshoot-db.sh`
3. Check logs: `./gradlew bootRun` (look for error messages)
4. Review the stack trace carefully

---

**Last Updated:** 2026-07-06
**Status:** Ready to Use ✅
