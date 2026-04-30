# ✅ Complete Verification Guide - Step by Step

## 🎯 Goal
Verify that all Swagger API endpoints now properly save data to the database.

---

## Part 1: Prepare Your Environment

### Step 1: Build the Application

```bash
cd /Users/ppc/Desktop/demo_project_spring_boot

# Clean and build
./gradlew clean build

# Or if you're on Windows:
# gradlew.bat clean build
```

**Expected Output**:
```
BUILD SUCCESSFUL in XXs
```

### Step 2: Start the Application

```bash
./gradlew bootRun
```

**Expected Output** (at the end):
```
Tomcat started on port(s): 8080
Started DemoProjectSpringBootApplication
```

### Step 3: Verify Server is Running

Open your browser and go to:
```
http://localhost:8080/swagger-ui.html
```

**Expected**: Swagger UI loads with all API documentation

---

## Part 2: Authentication Setup

### Step 1: Register a New User

1. Open Swagger UI: `http://localhost:8080/swagger-ui.html`
2. Find "User" section
3. Click "POST /api/v1/register"
4. Click "Try it out"
5. Enter registration data:
```json
{
  "username": "admin123",
  "email": "admin@example.com",
  "password": "Password123!",
  "firstName": "Admin",
  "lastName": "User",
  "phoneNumber": "0123456789"
}
```
6. Click "Execute"
7. Note the returned `userId`

**Expected Response**:
```
Status: 201 Created
{
  "id": 1,
  "username": "admin123",
  "email": "admin@example.com",
  "firstName": "Admin",
  "lastName": "User",
  ...
}
```

### Step 2: Promote User to Admin (Optional)

This might be done in database:
```sql
UPDATE users SET role = 'ADMIN' WHERE id = 1;
```

Or use an admin endpoint if available.

### Step 3: Login and Get JWT Token

1. Find "POST /api/v1/login"
2. Click "Try it out"
3. Enter:
```json
{
  "username": "admin123",
  "password": "Password123!"
}
```
4. Click "Execute"
5. Copy the `token` value from response

**Expected Response**:
```
Status: 200 OK
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "..."
}
```

### Step 4: Authorize Swagger UI

1. Scroll to top of Swagger UI
2. Click "Authorize" button (top right)
3. Paste the token (without "Bearer " prefix)
4. Click "Authorize"
5. Click "Close"

**Now you're authorized for all admin endpoints ✅**

---

## Part 3: Test Product Creation (Most Important)

### Test 3.1: Create Product (POST)

This is the main endpoint that wasn't working with Swagger.

**In Swagger UI:**
1. Find "Products" section (should be green in color)
2. Find "POST /api/v1/products"
3. Expand it
4. Click "Try it out" button
5. Fill in all required fields:

```
proName: "Samsung Galaxy S24"
proDesc: "Latest flagship smartphone with advanced camera system"
proPrice: 999.99
proBrand: "Samsung"
quantity: 50
discount: 5 (optional)
tags: "smartphone, 5G, flagship" (optional)
categoryId: 1 (optional)
imageFile: [select an image file] (optional)
```

6. Click "Execute"

**Expected Result**:
- Status code: **201 Created** ✅
- Response shows product with all your data
- Database now has new record

### Test 3.2: Verify Data in Database

```sql
-- Connect to PostgreSQL
psql -U postgres -d your_database

-- Check if product was saved
SELECT * FROM products ORDER BY created_at DESC LIMIT 1;
```

**Expected Output**:
```
pro_id | pro_name              | pro_desc                    | pro_price | pro_brand | stock | discount | created_at
-------|------------------------|----------------------------|-----------|-----------|-------|----------|-------------------
1      | Samsung Galaxy S24    | Latest flagship smartphone  | 999.99    | Samsung   | 50    | 5        | 2026-04-29 10:00:00
```

✅ **Data is in the database!**

---

## Part 4: Test Update Product (PUT)

### Test 4.1: Update Product

1. Find "PUT /api/v1/products/{proId}"
2. Click "Try it out"
3. Enter `proId`: 1
4. Modify just the price:

```
proPrice: 849.99
```

5. Click "Execute"

**Expected Result**:
- Status code: **200 OK** ✅
- Response shows updated product
- Updated timestamp reflects the change

### Test 4.2: Verify Update in Database

```sql
SELECT pro_id, pro_name, pro_price, updated_at FROM products WHERE pro_id = 1;
```

**Expected Output**:
```
pro_id | pro_name           | pro_price | updated_at
-------|-------------------|-----------|-------------------
1      | Samsung Galaxy S24 | 849.99    | 2026-04-29 10:05:00
```

✅ **Data was updated!**

---

## Part 5: Test Category Creation

### Test 5.1: Create Category

1. Find "Categories" section
2. Find "POST /api/v1/categories"
3. Click "Try it out"
4. Enter:

```json
{
  "catName": "Electronics",
  "catDesc": "Electronic devices and gadgets"
}
```

5. Click "Execute"

**Expected Result**:
- Status code: **201 Created** ✅
- Response shows category with ID

### Test 5.2: Verify in Database

```sql
SELECT * FROM categories ORDER BY created_at DESC LIMIT 1;
```

**Expected**:
```
cat_id | cat_name     | cat_desc                            | created_at
-------|--------------|-------------------------------------|-------------------
1      | Electronics  | Electronic devices and gadgets     | 2026-04-29 10:10:00
```

✅ **Category saved!**

---

## Part 6: Test Cart Operations

### Test 6.1: Get Cart

1. Find "Cart" section
2. Find "GET /api/v1/cart/{userId}"
3. Click "Try it out"
4. Enter `userId`: 1
5. Click "Execute"

**Expected Result**:
- Status code: **200 OK** ✅
- Response shows cart (might be empty initially)

### Test 6.2: Add to Cart

1. Find "POST /api/v1/cart/{userId}/add"
2. Click "Try it out"
3. Enter:
   - `userId`: 1
   - `productId`: 1
   - `quantity`: 2
4. Click "Execute"

**Expected Result**:
- Status code: **200 OK** ✅
- Cart now shows 1 item with quantity 2

### Test 6.3: Verify in Database

```sql
SELECT * FROM cart_items WHERE cart_id = 1;
```

**Expected**:
```
id | cart_id | product_id | quantity | unit_price
---|---------|------------|----------|----------
1  | 1       | 1          | 2        | 849.99
```

✅ **Item added to cart!**

---

## Part 7: Test Order Creation

### Test 7.1: Create Order

1. Find "Orders" section
2. Find "POST /api/v1/orders/checkout"
3. Click "Try it out"
4. Enter:

```json
{
  "userId": 1,
  "cartId": 1,
  "shippingAddress": "123 Main St, City, Country",
  "paymentMethod": "CREDIT_CARD",
  "comments": "Please deliver carefully"
}
```

5. Click "Execute"

**Expected Result**:
- Status code: **200 OK** ✅
- Response shows order with items

### Test 7.2: Verify in Database

```sql
SELECT * FROM orders WHERE user_id = 1 ORDER BY order_date DESC LIMIT 1;
```

**Expected**:
```
order_id | user_id | order_date          | total_amount | status | created_at
---------|---------|---------------------|--------------|--------|-------------------
1        | 1       | 2026-04-29 10:15:00 | 1699.98      | PENDING| 2026-04-29 10:15:00
```

✅ **Order created!**

---

## Part 8: Test Coupon Operations

### Test 8.1: Create Coupon

1. Find "Coupons" section
2. Find "POST /api/v1/coupons/add"
3. Click "Try it out"
4. Enter:

```json
{
  "couponCode": "SAVE10",
  "discountPercentage": 10,
  "maxUsageCount": 100,
  "expiryDate": "2026-12-31"
}
```

5. Click "Execute"

**Expected Result**:
- Status code: **201 Created** ✅
- Response shows coupon

### Test 8.2: Validate Coupon

1. Find "GET /api/v1/coupons/validate"
2. Click "Try it out"
3. Enter `code`: SAVE10
4. Click "Execute"

**Expected Result**:
- Status code: **200 OK** ✅
- Response shows coupon details

### Test 8.3: Verify in Database

```sql
SELECT * FROM coupons WHERE coupon_code = 'SAVE10';
```

**Expected**:
```
coupon_id | coupon_code | discount_percentage | max_usage_count | usage_count | expiry_date | is_active
----------|-------------|---------------------|-----------------|-------------|-------------|----------
1         | SAVE10      | 10                  | 100             | 0           | 2026-12-31  | true
```

✅ **Coupon created!**

---

## Part 9: Comprehensive Test Summary

### Create a Test Checklist

| Test | Endpoint | Method | Status | Data in DB |
|------|----------|--------|--------|-----------|
| Create Product | `/api/v1/products` | POST | 201 | ✅ |
| Update Product | `/api/v1/products/1` | PUT | 200 | ✅ |
| Create Category | `/api/v1/categories` | POST | 201 | ✅ |
| Create Coupon | `/api/v1/coupons/add` | POST | 201 | ✅ |
| Add to Cart | `/api/v1/cart/1/add` | POST | 200 | ✅ |
| Create Order | `/api/v1/orders/checkout` | POST | 200 | ✅ |
| Get Cart | `/api/v1/cart/1` | GET | 200 | ✅ |
| Get Order History | `/api/v1/orders/1/history` | GET | 200 | ✅ |

---

## Part 10: Network Tab Verification

### How to Check the Actual Request

1. Open Browser DevTools: **F12**
2. Go to **"Network"** tab
3. Execute a request in Swagger
4. Look for the request in Network tab
5. Click on the request name
6. Go to **"Request"** or **"Payload"** tab

**You should see**:
```
POST /api/v1/products HTTP/1.1
Host: localhost:8080
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary...
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

------WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="proName"

Samsung Galaxy S24
------WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="proDesc"

Latest flagship smartphone
...
```

✅ **Proper multipart/form-data format!**

---

## Part 11: Troubleshooting

### Issue: Status 401 Unauthorized

**Solution**:
1. Click "Authorize" button
2. Paste your JWT token
3. Click "Authorize"
4. Retry the request

### Issue: Status 400 Bad Request

**Possible Causes**:
- Required field is missing
- Wrong data type
- Invalid format

**Solution**:
1. Check server logs for error message
2. Verify all required fields are filled
3. Check data types match endpoint documentation

### Issue: Status 500 Internal Server Error

**Solution**:
1. Check server logs for stack trace
2. Look for database errors
3. Check if categoryId exists before referencing

### Issue: Data not in database

**Checklist**:
- [ ] HTTP status code is 201/200?
- [ ] Server shows no errors in logs?
- [ ] You're authorized (JWT token set)?
- [ ] Required fields are filled?
- [ ] Database connection working?

**Debug**:
```bash
# Check database connection
psql -U postgres -d your_database -c "SELECT COUNT(*) FROM products;"
```

---

## Part 12: Final Verification SQL Queries

### Check All Data

```sql
-- Count records in each table
SELECT 'Products' as table_name, COUNT(*) FROM products
UNION ALL
SELECT 'Categories', COUNT(*) FROM categories
UNION ALL
SELECT 'Coupons', COUNT(*) FROM coupons
UNION ALL
SELECT 'Cart Items', COUNT(*) FROM cart_items
UNION ALL
SELECT 'Orders', COUNT(*) FROM orders;
```

**Expected** (after running all tests):
```
table_name    | count
--------------|-------
Products      | 1
Categories    | 1
Coupons       | 1
Cart Items    | 1
Orders        | 1
```

### Check Recent Data

```sql
-- Get all recent records (last 1 hour)
SELECT NOW() - INTERVAL '1 hour' as since;

SELECT * FROM products 
WHERE created_at > NOW() - INTERVAL '1 hour'
ORDER BY created_at DESC;

SELECT * FROM categories 
WHERE created_at > NOW() - INTERVAL '1 hour'
ORDER BY created_at DESC;

SELECT * FROM coupons 
WHERE created_at > NOW() - INTERVAL '1 hour'
ORDER BY created_at DESC;

SELECT * FROM orders 
WHERE created_at > NOW() - INTERVAL '1 hour'
ORDER BY created_at DESC;
```

---

## 🎉 Success Criteria

### ✅ All Tests Pass if:

1. ✅ Swagger UI loads without errors
2. ✅ Can authorize with JWT token
3. ✅ POST endpoints return 201 status
4. ✅ PUT endpoints return 200 status
5. ✅ GET endpoints return 200 status
6. ✅ DELETE endpoints return 200 status
7. ✅ Data appears in database immediately
8. ✅ Updated timestamps are current
9. ✅ Related records (foreign keys) work
10. ✅ Network requests show proper multipart/form-data

---

## 📊 Performance Check (Optional)

### Check Database Performance

```sql
-- Check indexes exist
SELECT * FROM pg_indexes WHERE tablename = 'products';

-- Check table sizes
SELECT 
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname != 'pg_catalog'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

---

## ✨ Completion Checklist

- [ ] Application built successfully
- [ ] Server started on port 8080
- [ ] Swagger UI loaded
- [ ] User registered and authorized
- [ ] JWT token obtained and set in Swagger
- [ ] Product created from Swagger UI
- [ ] Product data verified in database
- [ ] Product updated from Swagger UI
- [ ] Update verified in database
- [ ] Category created from Swagger
- [ ] Coupon created from Swagger
- [ ] Cart operations tested
- [ ] Order created from Swagger
- [ ] All data visible in database
- [ ] Network requests show proper format
- [ ] No errors in server logs
- [ ] No errors in browser console

**If all checked ✅ → All Fixes Working Perfectly! 🎉**

---

## 📞 Final Notes

### Important:
1. **Always use Swagger UI** after these fixes - it works like Postman now
2. **Check both** Swagger response AND database to verify
3. **Look at server logs** if something doesn't work
4. **Use Network tab** to debug form-data issues
5. **Verify JWT token** before testing admin endpoints

### Documentation Files:
- `API_TESTING_GUIDE.md` - Complete testing guide
- `FIXES_SUMMARY.md` - Summary of all fixes
- `BEFORE_AFTER_COMPARISON.md` - Detailed code comparisons
- `VERIFICATION_GUIDE.md` - This file

---

**Generated**: April 29, 2026
**Status**: Ready for Testing ✅
**Last Updated**: April 29, 2026

