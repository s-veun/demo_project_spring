# Spring Boot E-Commerce App - Setup & Troubleshooting Guide

## 🔴 The SSL/PostgreSQL Connection Error

### Root Cause Analysis
Your application fails during startup with:
```
java.net.SocketException: Connection reset
  at org.postgresql.core.v3.ConnectionFactoryImpl.enableSSL()
```

**Why this happens:**
1. Your `.env` file has: `sslmode=require`
2. This forces SSL/TLS connection to PostgreSQL
3. The SSL handshake fails due to:
   - Certificate validation issues with Railway PostgreSQL
   - Network firewall blocking SSL handshake
   - PostgreSQL server configuration issue

**Result:** PostgreSQL JDBC driver cannot connect → HikariCP cannot create pool → Hibernate cannot build SessionFactory → EntityManagerFactory cannot be created → All beans fail (UserRepository, CustomUserDetailsService, JwtAuthenticationFilter) → Spring Boot cannot start Tomcat.

---

## ✅ Solution: Quick Start (Local Development)

### Step 1: Use Local PostgreSQL with Docker

```bash
# Navigate to project
cd demo_project_spring_boot

# Start PostgreSQL locally with Docker Compose
docker-compose -f docker-compose.dev.yml up -d

# Verify PostgreSQL is running
docker-compose -f docker-compose.dev.yml ps

# Test connection (optional)
docker exec -it ecommerce_postgres_local psql -U postgres -c "SELECT version();"
```

### Step 2: Create `.env` File for Local Development

```bash
# Copy the template
cp .env.local .env

# Verify the content
cat .env
```

**Expected content for LOCAL development:**
```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ecommerce_db?sslmode=disable
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_JPA_HIBERNATE_DDL_AUTO=update
JWT_SECRET=your-secret-key
...
```

### Step 3: Restart Spring Boot Application

```bash
# Option 1: Using Gradle
./gradlew bootRun

# Option 2: Using IDE
# Right-click on project → Run

# Option 3: From built JAR
java -jar build/libs/app.jar
```

### Step 4: Verify Application Started Successfully

```bash
# Check health endpoint
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}

# Check database health
curl http://localhost:8080/actuator/health/db

# Expected response:
# {"status":"UP","details":{"database":"PostgreSQL",...}}
```

---

## 📊 Configuration Comparison

### Local Development (sslmode=disable)
```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ecommerce_db?sslmode=disable
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```
✅ **Use when:**
- Developing locally
- Running with Docker Compose
- Testing on local machine

### Production (Railway/sslmode=require)
```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://shortline.proxy.rlwy.net:13895/railway?sslmode=require&connectTimeout=60&socketTimeout=60
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=eUiNjCegrYTlGnsNXMuIqGlnqJGryKxu
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```
✅ **Use when:**
- Deployed to Railway
- Production environment with SSL cert

---

## 🔧 Detailed Setup Instructions

### Option A: Docker Compose (Recommended for Development)

#### 1. Start PostgreSQL
```bash
cd demo_project_spring_boot
docker-compose -f docker-compose.dev.yml up -d
```

#### 2. Create `.env` file
```bash
cat > .env << 'EOF'
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ecommerce_db?sslmode=disable
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_JPA_HIBERNATE_DDL_AUTO=update
JWT_SECRET=dev-secret-key-min-64-chars-change-in-prodxxxxxxxxx
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000
GOOGLE_CLIENT_ID=your-google-id
GOOGLE_CLIENT_SECRET=your-google-secret
CLOUDINARY_CLOUD_NAME=your-cloudinary-name
CLOUDINARY_API_KEY=your-api-key
CLOUDINARY_API_SECRET=your-api-secret
FRONTEND_BASE_URL=http://localhost:3000
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_JWT=DEBUG
EOF
```

#### 3. Run Spring Boot
```bash
./gradlew bootRun
```

#### 4. Verify Connection
```bash
# Health check
curl http://localhost:8080/actuator/health

# DB health check
curl http://localhost:8080/actuator/health/db
```

#### 5. Access pgAdmin (Optional)
- URL: http://localhost:5050
- Email: admin@example.com
- Password: admin

#### 6. Stop PostgreSQL when done
```bash
docker-compose -f docker-compose.dev.yml down
```

---

### Option B: Local PostgreSQL Installation (macOS with Homebrew)

#### 1. Install PostgreSQL
```bash
brew install postgresql@15
```

#### 2. Start PostgreSQL Service
```bash
brew services start postgresql@15
```

#### 3. Create Database
```bash
createdb -U postgres ecommerce_db

# Or verify existing database
psql -U postgres -l | grep ecommerce_db
```

#### 4. Create `.env` file
```bash
cat > .env << 'EOF'
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ecommerce_db?sslmode=disable
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
...
EOF
```

#### 5. Run Spring Boot
```bash
./gradlew bootRun
```

---

### Option C: Linux (Ubuntu/Debian)

#### 1. Install PostgreSQL
```bash
sudo apt-get update
sudo apt-get install postgresql postgresql-contrib

# Or specific version
sudo apt-get install postgresql-15
```

#### 2. Start Service
```bash
sudo systemctl start postgresql
sudo systemctl status postgresql
```

#### 3. Create Database
```bash
sudo -u postgres createdb ecommerce_db
```

#### 4. Create `.env` file (same as macOS)

#### 5. Run Spring Boot
```bash
./gradlew bootRun
```

---

## 🧪 Debugging Checklist

### ✓ Connectivity Test
```bash
# Test PostgreSQL connection directly
psql -h localhost -U postgres -d ecommerce_db -c "SELECT 1;"

# Or using Docker
docker exec -it ecommerce_postgres_local psql -U postgres -d ecommerce_db -c "SELECT 1;"

# Expected: 
# ?column?
# ──────
# 1
```

### ✓ Check JDBC URL
```bash
# Print current JDBC URL from .env
grep SPRING_DATASOURCE_URL .env

# Must contain: ?sslmode=disable (for local dev)
```

### ✓ Verify Spring Boot Logs
```bash
# Enable DEBUG logging for connection issues
export LOG_LEVEL_ROOT=DEBUG
export LOG_LEVEL_HIBERNATE_SQL=DEBUG

./gradlew bootRun
```

### ✓ HikariCP Connection Pool
```bash
# Add to .env for detailed connection logging
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=10
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=5
SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT=30000
SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT=600000
```

### ✓ Firewall Check
```bash
# Check if port 5432 is accessible
nc -zv localhost 5432

# Expected: Connection succeeded

# Or using Docker
docker port ecommerce_postgres_local
# Expected: 5432/tcp -> 0.0.0.0:5432
```

---

## 🚀 Spring Boot Application Structure

### Dependency Chain Explained
```
JwtAuthenticationFilter (depends on UserDetailsService & UserRepository)
    ↓
CustomUserDetailsService (depends on UserRepository)
    ↓
UserRepository (depends on EntityManagerFactory)
    ↓
EntityManagerFactory (depends on DataSource)
    ↓
DataSource (depends on JDBC Connection)
    ↓
JDBC Connection (PostgreSQL Driver → Network → Database)
```

**When JDBC Connection fails, all 5 layers fail to initialize.**

---

## 📝 Common Error Messages & Fixes

### Error: "Unable to open JDBC Connection"
```
Caused by: org.hibernate.exception.JDBCConnectionException: 
Unable to open JDBC Connection for DDL execution
```
**Fix:** Check `.env` JDBC URL and PostgreSQL is running

### Error: "Cannot resolve reference to bean 'jpaSharedEM_entityManagerFactory'"
```
Caused by: org.springframework.beans.factory.BeanCreationException: 
Cannot resolve reference to bean 'jpaSharedEM_entityManagerFactory'
```
**Fix:** EntityManagerFactory failed - check database connection

### Error: "Connection refused"
```
Caused by: java.net.ConnectException: Connection refused
```
**Fix:** PostgreSQL not running. Use: `docker-compose -f docker-compose.dev.yml up -d`

### Error: "FATAL: password authentication failed"
```
Caused by: org.postgresql.util.PSQLException: FATAL: password authentication failed
```
**Fix:** Check SPRING_DATASOURCE_PASSWORD in `.env`

### Error: "Connection reset" (SSL Issue)
```
Caused by: java.net.SocketException: Connection reset
    at org.postgresql.core.v3.ConnectionFactoryImpl.enableSSL()
```
**Fix:** Change `?sslmode=require` to `?sslmode=disable` in SPRING_DATASOURCE_URL

---

## 🔐 Production Deployment (Railway)

### When to use: sslmode=require

Railway PostgreSQL automatically enables SSL. Your production `.env` should contain:

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://shortline.proxy.rlwy.net:13895/railway?sslmode=require&connectTimeout=60&socketTimeout=60
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your-production-password
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
JWT_SECRET=your-production-secret-key
```

**Key differences:**
- `sslmode=require` (SSL enforced)
- `ddl-auto=validate` (don't modify schema in production)
- Longer timeout values for stability

---

## 📚 HikariCP Configuration Best Practices

### Local Development (High Resource)
```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

### Production (Railway - Limited Resources)
```properties
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=1
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000
```

---

## 🧬 Spring Boot Versions & Compatibility

| Component | Version | Notes |
|-----------|---------|-------|
| Java | 21 | Latest LTS |
| Spring Boot | 3.4.1 | Latest stable |
| Spring Security | 6.x | No XML config |
| PostgreSQL Driver | Latest | Auto-managed by Gradle |
| Hibernate | 6.x | JPA 3.1 compatible |
| HikariCP | Latest | Built-in to Spring Boot |

---

## 🎯 Next Steps

1. **Create `.env` from template:**
   ```bash
   cp .env.local .env
   ```

2. **Start PostgreSQL:**
   ```bash
   docker-compose -f docker-compose.dev.yml up -d
   ```

3. **Run Spring Boot:**
   ```bash
   ./gradlew bootRun
   ```

4. **Test endpoints:**
   ```bash
   curl http://localhost:8080/actuator/health
   curl http://localhost:8080/swagger-ui.html
   ```

---

## 📞 Support & Additional Resources

- **Spring Boot Docs:** https://spring.io/projects/spring-boot
- **Spring Data JPA:** https://spring.io/projects/spring-data-jpa
- **PostgreSQL JDBC:** https://jdbc.postgresql.org/
- **HikariCP:** https://github.com/brettwooldridge/HikariCP
- **JWT:** https://jwt.io/

---

## ✅ Verification Checklist

- [ ] PostgreSQL running (Docker or local)
- [ ] `.env` file created with `sslmode=disable`
- [ ] Database exists: `ecommerce_db`
- [ ] Spring Boot starts without errors
- [ ] Health endpoint responds: `/actuator/health`
- [ ] Database health shows UP: `/actuator/health/db`
- [ ] Swagger UI accessible: `/swagger-ui.html`
- [ ] JWT authentication works
- [ ] Frontend can connect to backend

---

**Last Updated:** 2026-07-06
**Version:** 1.0
**Status:** Complete & Tested
