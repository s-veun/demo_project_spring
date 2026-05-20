# Profile Image Upload API

## Endpoints

- `GET /api/v1/users/profile`
- `PUT /api/v1/users/profile`
- `PUT /api/v1/users/profile/change-password`
- `PUT /api/v1/users/profile/image` (multipart field: `file`)
- `DELETE /api/v1/users/profile/image`
- `GET /api/v1/users/profile/addresses`
- `POST /api/v1/users/profile/addresses`
- `PUT /api/v1/users/profile/addresses/{id}`
- `DELETE /api/v1/users/profile/addresses/{id}`
- `PUT /api/v1/users/profile/settings`

All endpoints require a valid JWT access token.

## Validation Rules

- Allowed extensions: `jpg`, `jpeg`, `png`, `webp`
- Allowed MIME types: `image/jpeg`, `image/png`, `image/webp`
- Default max size: `5MB` (`app.profile-image.max-file-size-bytes`)
- Empty file, invalid signatures, and malformed multipart requests are rejected.

## Storage

- Local disk folder: `uploads/profile-images`
- Public URL base: `/uploads/profile-images`
- Existing profile image is replaced automatically.
- File names are random UUID-based and sanitized to prevent collisions.

## Security and Validation

- JWT required for all profile endpoints.
- Password change enforces uppercase, lowercase, number, and special character policy.
- Username updates are uniqueness-checked.
- Upload endpoint has per-user rate limiting (configurable).

