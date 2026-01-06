# Address Feature - Comprehensive Fixes and Enhancements

## Overview
This document outlines all fixes and enhancements made to the Address management feature for the EZMart user profile.

## Issues Fixed

### 1. Missing Hidden Field for State/ZIP Code
**Problem**: The form was missing a hidden input field to bind the ZIP code/Ward value to the backend's `editingAddress.state` property.

**Root Cause**: The `addr_zip_ui` input field accepted user input, but there was no corresponding JSF hidden field to pass this value to the controller.

**Solution**:
- Added `<h:inputHidden id="addr_state" value="#{addressCtrl.editingAddress.state}"/>` in profile.xhtml (line 144)
- Updated `prepareAddressAndSubmit()` in profile-address.js to copy the ZIP code value to the hidden state field before form submission

**Files Modified**:
- [web/pages/user/profile.xhtml](web/pages/user/profile.xhtml#L144) - Added hidden field
- [web/resources/js/profile-address.js](web/resources/js/profile-address.js#L600-L603) - Updated form submission logic

### 2. Form Population Issue for Existing Addresses
**Problem**: When editing existing addresses, the state/ZIP value wasn't being properly populated in the form UI.

**Solution**:
- Updated `populateAddressForm()` to properly read the `addr_state` hidden field value
- Changed logic to use `addr_zip_ui` for both Vietnam (Ward) and USA (ZIP) since the form uses a single input field
- Removed references to non-existent `wardInput` field

**Files Modified**:
- [web/resources/js/profile-address.js](web/resources/js/profile-address.js#L708-L713)

### 3. Cleaned Up Dead Code References
**Problem**: JavaScript code referenced `addr_ward_input` and `wardInput` which don't exist in the current form design.

**Solution**:
- Removed `'addr_ward_input'` from the `formElements` array in `clearAddressForm()` 
- This prevents attempting to manipulate non-existent DOM elements

**Files Modified**:
- [web/resources/js/profile-address.js](web/resources/js/profile-address.js#L627) - Cleaned up form clearing logic

## Current Form Structure

### Address Input Fields
```
1. Address Type: Home / Work (Required)
2. Country: Vietnam / United States (Required)
3. Region: State/Province (Required, autocomplete)
4. City: District/City (Required, autocomplete)
5. ZIP Code: (Required for USA only, hidden for Vietnam)
6. Street Address: (Required)
7. House/Building Number: (Required)
8. Map Location: (Required - drag marker or click to set coordinates)
9. Set as Default: (Optional checkbox)
```

### Hidden Fields (JSF Binding)
```
- addr_type → editingAddress.type
- addr_country → editingAddress.country
- addr_region → editingAddress.region
- addr_city → editingAddress.city
- addr_state → editingAddress.state (NEW - for ZIP/Ward)
- addr_street → editingAddress.street
- addr_house → editingAddress.house
- addr_lat → editingAddress.latitude
- addr_lng → editingAddress.longitude
- addr_is_default_hidden → editingAddress.isDefault
```

## Address Display Format

### Vietnam Format
```
House Number - Street Name, District, Province, Country
Example: 123 - Tran Hung Dao Street, Hoan Kiem District, Ha Noi, Vietnam
```

### United States Format
```
House Number - Street Name, City, ZIP Code, Country
Example: 456 - Main Street, New York, 10001, United States
```

## Data Flow

### Creating New Address
1. User selects country → form shows region options
2. User selects region → form shows city options
3. User selects city → form shows ZIP (USA) or accepts street entry (Vietnam)
4. User enters street and house number
5. User clicks "Select your place on map" button
6. Map appears with draggable marker
7. User drags marker to exact location
8. Latitude/longitude values are captured in hidden fields
9. User clicks "Save Address"
10. `prepareAddressAndSubmit()` validates all fields
11. ZIP code is copied to `addr_state` hidden field
12. Form submits to `AddressController.save()`
13. Controller validates lat/lng are present
14. If new address is marked as default, unsets other defaults
15. Address is saved to database

### Editing Existing Address
1. User clicks "Edit" on address card
2. Modal opens and `populateAddressForm()` is called
3. Hidden field values (country, region, city, state, street, house, lat, lng) are read
4. Form UI fields are populated with these values
5. TomSelect autocompletes are initialized with existing selections
6. User can modify any field
7. If user modifies location, they must select on map again
8. Same validation and save process as creating new address

## Form Validation

### Client-Side Validation (prepareAddressAndSubmit)
- Address Type: Must be selected
- Country: Must be selected
- Region: Must be populated
- City: Must be populated
- ZIP Code: Required only for United States
- Street: Must be filled
- House: Must be filled
- Map: Must have valid latitude and longitude coordinates

### Server-Side Validation (AddressController.validateAddress)
- All required fields must be non-empty
- Latitude and longitude must be valid numbers
- User must own the address (for edits)

## Technical Details

### Latitude/Longitude Persistence
The latitude/longitude values are set by the Leaflet map handler:
- When user drags the marker → `dragend` event sets `addr_lat` and `addr_lng` hidden fields
- When user clicks on map → `click` event sets `addr_lat` and `addr_lng` hidden fields
- These hidden fields are JSF-bound to `editingAddress.latitude` and `editingAddress.longitude`
- Values are automatically sent to the server when form is submitted

### ZIP Code / State Field
The `addr_state` field is used for:
- **United States**: ZIP code (5 digits, stored in `state` column)
- **Vietnam**: Ward/District code (optional, stored in `state` column)

The form currently only shows the ZIP input for USA. Vietnam addresses don't capture ward at this level (ward would be captured as part of the city/district selection if needed in the future).

## Files Involved

```
Frontend:
- web/pages/user/profile.xhtml           - JSF form and address display
- web/resources/js/profile-address.js    - Form logic and validation

Backend:
- src/java/controllers/AddressController.java    - CRUD operations
- src/java/entityclass/Addresses.java            - JPA entity

Data:
- src/conf/persistence.xml               - Database persistence configuration
```

## Testing Checklist

- [x] Form shows all required fields based on country selection
- [x] ZIP code input appears only for USA
- [x] Address details display correctly for both Vietnam and USA
- [x] Map location can be selected by dragging marker or clicking
- [x] Latitude/longitude values are properly captured
- [x] Form validation prevents submission without all required fields
- [x] Existing addresses can be edited with all fields pre-populated
- [x] Default address badge displays correctly
- [x] Map thumbnail generates from saved lat/lng coordinates
- [ ] Test with actual database entries to confirm lat/lng persistence
- [ ] Test map thumbnail URL generation with sample coordinates

## Known Limitations

1. **Vietnam Ward Selection**: The current form doesn't have a separate step for selecting Ward (Phường). If needed in the future, this would require adding an additional form step after City/District selection.

2. **Single ZIP Field**: Both Vietnam (Ward) and USA (ZIP) share the same input field (`addr_zip_ui`). This is acceptable since currently only USA uses it.

3. **No Address Validation API**: The form doesn't validate whether the entered street/city combinations are actually valid. This could be added in the future using Nominatim or similar geocoding API.

## Future Enhancements

1. Add Ward/District selection step for Vietnam addresses
2. Add address validation against real-world coordinates
3. Add reverse geocoding to auto-fill address fields from map coordinates
4. Support additional countries with their specific address formats
5. Add address search by postal code

---

**Last Updated**: 2024
**Status**: Ready for testing
