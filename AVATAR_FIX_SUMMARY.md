# Avatar Upload Fix - Implementation Summary

## Issues Fixed

### 1. **401 Unauthorized Error when Uploading Avatar**

**Root Cause:** 
- AvatarUploadServlet was not correctly retrieving the authenticated user from the session
- The servlet was looking for `currentUserId` but JSF stores the customer object as `currentCustomer`

**Solution:**
- Updated `AvatarUploadServlet.authenticateUser()` to look for `currentCustomer` in the session
- Added logic to also check for `currentCustomerId` 
- Modified `AuthController.login()` to store both `currentCustomer` and `currentCustomerId` in the session

**Files Changed:**
- `EZMart_Supermarket_Management-war/src/java/controllers/AvatarUploadServlet.java`
- `EZMart_Supermarket_Management-war/src/java/controllers/AuthController.java`

### 2. **Avatar Disappears After Page Reload**

**Root Cause:**
- Avatar URL was only stored in memory (session), not persisted to the database
- When the page was reloaded, the avatar URL was lost

**Solution:**
- Added `avatarUrl` field to the `Customers` entity class
- Modified `AvatarUploadServlet` to save the avatar URL to the database after successful upload
- Updated `AuthController.getUserProfileImageUrl()` to load avatar from `currentCustomer.avatarUrl`
- Updated `AvatarServlet` to support `customerId` parameter in addition to `userId`

**Files Changed:**
- `EZMart_Supermarket_Management-ejb/src/java/entityclass/Customers.java`
- `EZMart_Supermarket_Management-war/src/java/controllers/AvatarUploadServlet.java`
- `EZMart_Supermarket_Management-war/src/java/controllers/AuthController.java`
- `EZMart_Supermarket_Management-war/src/java/controllers/AvatarServlet.java`
- `OnlineSupermarketDB.sql`

## Database Migration Required

Run this SQL script on your database to add the AvatarUrl column:

```sql
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Customers' AND COLUMN_NAME = 'AvatarUrl')
BEGIN
    ALTER TABLE Customers 
    ADD AvatarUrl NVARCHAR(500) NULL;
END
```

See `add_avatar_url_column.sql` for the migration script.

## How It Works Now

1. **User uploads an avatar:**
   - File is validated on the server
   - File is saved to the file system
   - Avatar URL is saved to the `Customers.AvatarUrl` field in the database
   - Session `avatarUpdatedAt` is updated for cache busting
   - JavaScript immediately updates all avatar elements on the page

2. **User reloads the page:**
   - `AuthController.getUserProfileImageUrl()` checks `currentCustomer.avatarUrl`
   - If avatar URL exists in database, it's displayed
   - If not, it falls back to default avatar
   - Avatar persists across page reloads and browser restarts

3. **Avatar Caching:**
   - Cache-buster timestamp is added to all avatar URLs
   - When a new avatar is uploaded, the timestamp is updated
   - Browser loads the new avatar instead of the cached old one

## Testing

1. Login to the application
2. Go to Profile page
3. Click camera button on avatar
4. Select an image file (JPG, PNG, or GIF)
5. Avatar should update immediately
6. Reload the page - avatar should persist
7. Close browser and reopen - avatar should still be there

## Notes

- Avatar files are stored in the file system at `${user.home}/.ezmart_avatars/` by default
- Files are named as `user_<customerId>.<extension>` (e.g., `user_1.jpg`)
- Supported formats: JPEG, PNG, GIF
- Max file size: 5 MB
- Avatar URL is stored as relative path in database (e.g., `/EZMart_Supermarket_Management-war/avatar?customerId=1&t=1234567890`)
