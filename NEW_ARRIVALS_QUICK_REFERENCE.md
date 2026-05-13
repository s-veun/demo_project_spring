# New Arrival Product API - Quick Reference Guide

## Quick Start

### 1. Create a New Arrival Product

**Endpoint:** `POST /api/v1/products/new-arrivals`

**Headers:**
```
Authorization: Bearer <YOUR_JWT_TOKEN>
Content-Type: multipart/form-data
```

**Form Data:**
```
proName: Galaxy S24
proDesc: Latest Samsung flagship phone
proPrice: 899.99
proBrand: Samsung
quantity: 100
discount: 5.0
tags: smartphone,electronics,premium
categoryId: 1
releaseDate: 2024-05-13
daysToShowAsNew: 45
imageFile: (binary image file)
```

**Success Response (201 Created):**
```json
{
  "success": true,
  "message": "New arrival product created successfully",
  "data": {
    "proId": 456,
    "proName": "Galaxy S24",
    "proPrice": 899.99,
    "isNew": true,
    "createdAt": "2024-05-13 14:30:00",
    ...
  }
}
```

---

## 2. Retrieve New Arrivals

### Option A: Get Last N Days of Arrivals
**Endpoint:** `GET /api/v1/products/new-arrivals?days=30`

**Query Parameters:**
- `days` (optional, default: 30): Number of days to look back

**Response:**
```json
{
  "success": true,
  "daysLimit": 30,
  "count": 5,
  "data": [...]
}
```

### Option B: Get with Pagination
**Endpoint:** `GET /api/v1/products/new-arrivals/paginated?limit=10&page=1`

**Query Parameters:**
- `limit` (optional, default: 10): Items per page
- `page` (optional, default: 1): Page number

**Response:**
```json
{
  "success": true,
  "page": 1,
  "limit": 10,
  "count": 8,
  "data": [...]
}
```

---

## Request/Response Examples

### Example 1: Minimal Request
```bash
curl -X POST http://localhost:8080/api/v1/products/new-arrivals \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -F "proName=Product A" \
  -F "proDesc=Description" \
  -F "proPrice=29.99" \
  -F "proBrand=Brand" \
  -F "quantity=10"
```

### Example 2: Complete Request with Image
```bash
curl -X POST http://localhost:8080/api/v1/products/new-arrivals \
  -H "Authorization: Bearer TOKEN" \
  -F "proName=iPhone 15 Pro" \
  -F "proDesc=Latest Apple flagship" \
  -F "proPrice=999.99" \
  -F "proBrand=Apple" \
  -F "quantity=50" \
  -F "discount=10" \
  -F "tags=phone,electronics,luxury" \
  -F "categoryId=5" \
  -F "releaseDate=2024-05-13" \
  -F "daysToShowAsNew=60" \
  -F "imageFile=@product.jpg"
```

### Example 3: Multiple Image URLs
```bash
curl -X POST http://localhost:8080/api/v1/products/new-arrivals \
  -H "Authorization: Bearer TOKEN" \
  -F "proName=Product Name" \
  -F "proDesc=Description" \
  -F "proPrice=199.99" \
  -F "proBrand=Brand" \
  -F "quantity=25" \
  -F "imageUrls=https://cdn.example.com/img1.jpg" \
  -F "imageUrls=https://cdn.example.com/img2.jpg" \
  -F "imageUrls=https://cdn.example.com/img3.jpg"
```

---

## Common Scenarios

### Scenario 1: Mobile App Needs Latest Products (Last 14 Days)
```
GET /api/v1/products/new-arrivals?days=14
```

### Scenario 2: Admin Dashboard Shows Arrivals with Pagination
```
GET /api/v1/products/new-arrivals/paginated?limit=20&page=1
```

### Scenario 3: Add Premium Product with Multiple Images
```
POST /api/v1/products/new-arrivals
(with all fields populated and multiple imageUrls)
```

### Scenario 4: Add Product from External System
```
POST /api/v1/products/new-arrivals
(with pre-uploaded Cloudinary URLs in imageUrls array)
```

---

## Response Fields Explained

| Field | Type | Description |
|-------|------|-------------|
| proId | Long | Unique product identifier |
| proName | String | Product name |
| proPrice | BigDecimal | Price value |
| stock | Integer | Available quantity |
| isNew | Boolean | TRUE if within daysToShowAsNew period |
| daysToShowAsNew | Integer | Days to show the "New" badge |
| createdAt | LocalDateTime | Product creation timestamp |
| createdAt | LocalDateTime | Last update timestamp |
| releaseDate | Date | Official release date |
| categoryName | String | Product category |
| imageUrls | List<String> | All product image URLs |

---

## Error Responses

### 400 Bad Request - Missing Required Field
```json
{
  "success": false,
  "error": "Product name is required"
}
```

### 400 Bad Request - Duplicate Product
```json
{
  "success": false,
  "error": "Product Name: 'iPhone 15' Already Exists"
}
```

### 400 Bad Request - Invalid Price
```json
{
  "success": false,
  "error": "Product price must be positive"
}
```

### 401 Unauthorized - No Token
```json
{
  "success": false,
  "error": "Unauthorized - Authentication required"
}
```

### 403 Forbidden - Insufficient Permissions
```json
{
  "success": false,
  "error": "Access denied - Admin role required"
}
```

### 500 Internal Server Error
```json
{
  "success": false,
  "error": "Internal server error occurred"
}
```

---

## Implementation Tips

### Tip 1: Cloudinary Pre-upload
Instead of uploading directly:
```javascript
// Upload to Cloudinary first
const imageUrls = await uploadToCloudinary(files);

// Then pass to API
fetch('/api/v1/products/new-arrivals', {
  method: 'POST',
  body: formData // Include imageUrls array
});
```

### Tip 2: Date Format
Always use ISO 8601 format: `yyyy-MM-dd`
```
✅ Correct: 2024-05-13
❌ Wrong: 13/05/2024 or 05-13-2024
```

### Tip 3: Pagination
For efficient pagination, always include both parameters:
```
GET /api/v1/products/new-arrivals/paginated?limit=10&page=1
GET /api/v1/products/new-arrivals/paginated?limit=20&page=2
```

### Tip 4: Tags Format
Use comma-separated tags without spaces:
```
✅ Correct: electronics,smartphone,premium
❌ Wrong: electronics, smartphone, premium
```

### Tip 5: Role-Based Access
Only users with ADMIN role can create new arrivals:
```
POST  /api/v1/products/new-arrivals  → ADMIN ONLY
GET   /api/v1/products/new-arrivals  → PUBLIC
```

---

## Frontend Integration Example

### React Component Example
```javascript
// Add New Arrival Product
const addNewArrival = async (formData) => {
  const token = localStorage.getItem('jwtToken');
  
  const response = await fetch('/api/v1/products/new-arrivals', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    },
    body: formData
  });
  
  return await response.json();
};

// Get New Arrivals
const getNewArrivals = async (days = 30) => {
  const response = await fetch(
    `/api/v1/products/new-arrivals?days=${days}`
  );
  return await response.json();
};

// Get Paginated New Arrivals
const getNewArrivalsPaginated = async (limit = 10, page = 1) => {
  const response = await fetch(
    `/api/v1/products/new-arrivals/paginated?limit=${limit}&page=${page}`
  );
  return await response.json();
};
```

---

## Testing Commands

### List All New Arrivals
```bash
curl -X GET "http://localhost:8080/api/v1/products/new-arrivals?days=30"
```

### List with Pagination
```bash
curl -X GET "http://localhost:8080/api/v1/products/new-arrivals/paginated?limit=5&page=1"
```

### Create Product (Admin)
```bash
curl -X POST http://localhost:8080/api/v1/products/new-arrivals \
  -H "Authorization: Bearer <token>" \
  -F "proName=Test Product" \
  -F "proDesc=Test Description" \
  -F "proPrice=99.99" \
  -F "proBrand=TestBrand" \
  -F "quantity=10"
```

---

## Troubleshooting

### Issue: 401 Unauthorized
**Solution:** Verify JWT token is valid and included in Authorization header
```
Authorization: Bearer <valid_token>
```

### Issue: 403 Forbidden
**Solution:** Ensure user account has ADMIN role

### Issue: 400 Bad Request - Category Not Found
**Solution:** Verify the categoryId exists in the database

### Issue: Image Not Uploading
**Solution:** Check multipart/form-data headers and file size limits

### Issue: Duplicate Product Name Error
**Solution:** Product names must be unique; use different name or check existing products

---

## Performance Tips

1. **Use Pagination**: Always paginate for large result sets
   ```
   GET /api/v1/products/new-arrivals/paginated?limit=10&page=1
   ```

2. **Filter by Days**: Reduce data transfer
   ```
   GET /api/v1/products/new-arrivals?days=7  // Last 7 days only
   ```

3. **Batch Operations**: Create multiple products efficiently
   - Make sequential POST requests with proper delays
   - Consider using bulk API if available

4. **Image Optimization**: Use Cloudinary transformations
   ```
   https://res.cloudinary.com/.../image.jpg?w=300&q=auto
   ```

---

## Contact & Support

For issues or questions:
- Check the full documentation: `API_DOCUMENTATION_NEW_ARRIVALS.md`
- Review database logs for query issues
- Verify Cloudinary configuration for image upload failures

---

**Last Updated:** May 13, 2024  
**Version:** 1.0.0

