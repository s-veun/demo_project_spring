# JWT Secret Setup for Railway

## Recommended Railway Variables

Use one of these two approaches.

### Option A (Plain Text Secret)

Set these variables in Railway:

- `JWT_SECRET=change-this-to-a-long-random-secret-at-least-32-bytes`
- `JWT_SECRET_FORMAT=plain`

### Option B (Base64 Secret)

Generate a secret:

```bash
openssl rand -base64 32
```

Set these variables in Railway:

- `JWT_SECRET=<paste generated base64 value>`
- `JWT_SECRET_FORMAT=base64`

## Why `-` Can Break Standard Base64

Standard Base64 allows: `A-Z`, `a-z`, `0-9`, `+`, `/`, `=`.

`-` belongs to **Base64URL**, not standard Base64. If a service tries standard Base64 decoding on a value containing `-`, it throws:

`Illegal base64 character: '-'`

## Service Behavior in This Project

`JwtService` now supports:

- `plain`
- `base64`
- `base64url`
- `auto`

with property:

- `app.jwt.secret-format`

Defaults:

- `app.jwt.secret-format=plain`

## Validation Tip

After deployment, test login:

```bash
curl -X POST https://<your-railway-domain>/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

If JWT is misconfigured, API returns a clear message:

- `JWT configuration error. Verify JWT_SECRET and JWT_SECRET_FORMAT`

