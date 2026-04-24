# 📚 Swagger/OpenAPI Documentation Guide

## 🎉 Swagger is Now Enabled!

Your E-Commerce API now has interactive API documentation powered by **SpringDoc OpenAPI**.

---

## 🌐 Access Swagger UI

### **Main Swagger Interface:**
```
http://localhost:8088/swagger-ui.html
```

### **Alternative URLs:**
```
http://localhost:8088/swagger-ui/index.html
http://localhost:8088/webjars/swagger-ui/index.html
```

### **OpenAPI JSON Specification:**
```
http://localhost:8088/v3/api-docs
```

### **Actuator Health Check:**
```
http://localhost:8088/actuator/health
```

---

## 🚀 How to Use Swagger UI

### **1. Open Swagger UI**
Visit: http://localhost:8088/swagger-ui.html

You'll see:
- **Products** - Product management APIs
- All other controller endpoints grouped by tags

### **2. Explore Endpoints**
- Click on any endpoint to expand it
- See detailed description, parameters, and response types
- Click **"Try it out"** to test the endpoint

### **3. Authenticate with JWT**
1. Click the **🔓 Authorize** button (lock icon) at the top
2. Enter your JWT token:
   ```
   Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   ```
3. Click **"Authorize"**
4. Now all authenticated endpoints will use your token

### **4. Test Endpoints**
1. Click **"Try it out"** on any endpoint
2. Fill in required parameters
3. Click **"Execute"**
4. View the response, status code, and response headers

---

## 📋 Available API Groups

### **Products**
- GET `/api/v1/products` - Get all products
- GET `/api/v1/products/{id}` - Get product by ID
- POST `/api/v1/products` - Create product (Admin)
- PUT `/api/v1/products/{id}` - Update product (Admin)
- DELETE `/api/v1/products/{id}` - Delete product (Admin)
- GET `/api/v1/products/popular/most-viewed` - Get most viewed
- GET `/api/v1/products/popular/trending` - Get trending products
- And more...

### **Users**
- POST `/api/v1/register` - Register new user
- POST `/api/v1/login` - Login
- GET `/api/v1/me` - Get current user profile
- And more...

### **Categories**
- GET `/api/v1/categories` - Get all categories
- POST `/api/v1/categories` - Create category (Admin)
- And more...

### **Orders, Cart, Reviews, Wishlist, etc.**
All your API endpoints are documented!

---

## 🔐 Authentication Flow

### **Step 1: Login to Get JWT Token**

In Swagger UI:
1. Find **POST `/api/v1/login`**
2. Click **"Try it out"**
3. Enter credentials:
   ```json
   {
     "username": "Savoeun",
     "password": "Saveun2032"
   }
   ```
4. Click **"Execute"**
5. Copy the token from response (looks like: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`)

### **Step 2: Authorize with Token**

1. Click **🔓 Authorize** button at top
2. Enter: `Bearer YOUR_TOKEN_HERE`
3. Click **Authorize**
4. Click **Close**

### **Step 3: Test Protected Endpoints**

Now you can test:
- Creating products
- Managing orders
- Updating cart
- All authenticated endpoints

---

## 🎨 Swagger UI Features

### **Search & Filter**
- Use the search box to find specific endpoints
- Filter by tags (Products, Users, Orders, etc.)

### **Request/Response Models**
- Click on **"Schema"** to see request/response structure
- View all field types and requirements

### **Code Generation**
- Swagger can generate code for:
  - cURL commands
  - JavaScript (Fetch, Axios)
  - Python
  - Java
  - And more!

### **Download Specification**
- Click the **"i"** icon at top
- Download OpenAPI spec in JSON or YAML
- Use with other tools (Postman, Insomnia, etc.)

---

## 🛠️ Configuration

### **Swagger Settings (application.properties)**

```properties
# Swagger/OpenAPI Configuration
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operationsSorter=method
springdoc.swagger-ui.tagsSorter=alpha
springdoc.swagger-ui.tryItOutEnabled=true
springdoc.swagger-ui.filter=true
springdoc.swagger-ui.display-request-duration=true
```

### **Custom OpenAPI Configuration**

See: [OpenAPIConfig.java](src/main/java/com/example/demo_project_spring_boot/config/OpenAPIConfig.java)

Customize:
- API title & description
- Version
- Contact information
- Server URLs
- Security schemes

---

## 📝 Adding Annotations to Controllers

### **Example: ProductController**

```java
@Tag(name = "Products", description = "Product management APIs")
public class ProductController {

    @Operation(
        summary = "Get all products", 
        description = "Retrieve a list of all available products"
    )
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        // ...
    }

    @Operation(summary = "Get product by ID")
    public ResponseEntity<?> getProductById(
            @Parameter(description = "Product ID", required = true) 
            @PathVariable Long proId) {
        // ...
    }
}
```

### **Available Annotations:**

| Annotation | Purpose |
|------------|---------|
| `@Tag` | Group endpoints by category |
| `@Operation` | Describe an endpoint |
| `@Parameter` | Describe a parameter |
| `@ApiResponse` | Describe response codes |
| `@Schema` | Describe model fields |

---

## 🧪 Testing Examples

### **Example 1: Get All Products**

1. Open: http://localhost:8088/swagger-ui.html
2. Find: **GET /api/v1/products**
3. Click **"Try it out"**
4. Click **"Execute"**
5. See response with all products

### **Example 2: Create Product (Admin)**

1. **First:** Login and get JWT token
2. **Authorize:** Add Bearer token
3. Find: **POST /api/v1/products**
4. Click **"Try it out"**
5. Fill in product details
6. Click **"Execute"**
7. See created product in response

### **Example 3: Search Products**

1. Find: **GET /api/v1/search/products**
2. Click **"Try it out"**
3. Enter parameters:
   - keyword: "phone"
   - minPrice: 100
   - maxPrice: 1000
   - sortBy: "popularityScore"
4. Click **"Execute"**

---

## 🔗 Integration with Other Tools

### **Import to Postman**

1. Go to: http://localhost:8088/v3/api-docs
2. Copy the JSON
3. Open Postman
4. Click **Import** → **Raw Text**
5. Paste JSON
6. Click **Continue** → **Import**
7. All endpoints now in Postman!

### **Use with Insomnia**

1. Open Insomnia
2. Click **Import**
3. Enter URL: `http://localhost:8088/v3/api-docs`
4. Click **Import**

### **Generate Client Code**

Use [OpenAPI Generator](https://openapi-generator.tech/):

```bash
# Generate JavaScript client
openapi-generator-cli generate \
  -i http://localhost:8088/v3/api-docs \
  -g javascript \
  -o ./generated-client

# Generate Python client
openapi-generator-cli generate \
  -i http://localhost:8088/v3/api-docs \
  -g python \
  -o ./generated-client
```

---

## 🎯 Best Practices

### **1. Always Document New Endpoints**
When creating new controllers, add:
- `@Tag` at class level
- `@Operation` for each endpoint
- `@Parameter` for complex parameters

### **2. Test in Swagger First**
Before integrating with frontend:
- Test all endpoints in Swagger UI
- Verify request/response formats
- Check authentication works

### **3. Keep Documentation Updated**
When you change APIs:
- Update `@Operation` descriptions
- Update `@Parameter` descriptions
- Test in Swagger UI

### **4. Use Swagger for API Design**
Before coding:
- Design API structure
- Share OpenAPI spec with team
- Get feedback early

---

## 🚀 Production Deployment

### **Disable Swagger in Production**

Add to `application-prod.properties`:

```properties
# Disable Swagger in production
springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false
```

Or keep it enabled but protected:

```java
@Configuration
public class SwaggerConfig implements WebMvcConfigurer {
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if ("prod".equals(activeProfile)) {
            // Restrict Swagger to admin IPs only
        }
    }
}
```

---

## 📊 API Statistics

Your API now has:

- **40+ endpoints** documented
- **15+ controllers** with tags
- **JWT authentication** integrated
- **Request/Response models** auto-generated
- **Interactive testing** enabled

---

## 🆘 Troubleshooting

### **Issue: Swagger UI not loading**

**Solution:**
```bash
# Check if app is running
curl http://localhost:8088/swagger-ui.html

# Check logs
# Look for SpringDoc initialization messages
```

### **Issue: 403 Forbidden on Swagger**

**Solution:**
Security config already allows Swagger access. If still blocked:
```java
.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
```

### **Issue: Endpoints not showing**

**Solution:**
- Check controller has `@RestController` annotation
- Check component scanning includes controller package
- Restart application

---

## 📚 Resources

- **SpringDoc Official Docs:** https://springdoc.org/
- **OpenAPI Specification:** https://swagger.io/specification/
- **Swagger UI:** https://swagger.io/tools/swagger-ui/
- **OpenAPI Generator:** https://openapi-generator.tech/

---

## ✨ Quick Links

| Resource | URL |
|----------|-----|
| **Swagger UI** | http://localhost:8088/swagger-ui.html |
| **OpenAPI JSON** | http://localhost:8088/v3/api-docs |
| **Health Check** | http://localhost:8088/actuator/health |
| **H2 Console** | http://localhost:8088/h2-console |

---

**Enjoy your fully documented API!** 🎉

Start exploring at: **http://localhost:8088/swagger-ui.html**
