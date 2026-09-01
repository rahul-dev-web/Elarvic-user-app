# Elarvic User App — Firebase setup

The User App does **not** use Google Login. It uses an admin-generated Elarvic access key.

## Firebase configuration

1. Use the same Firebase project as the Elarvic Admin App.
2. Add Android app ID `com.elarvic.user`.
3. Download the user app's `google-services.json`.
4. Put it at `app/google-services.json`.
5. Enable **Authentication → Sign-in method → Anonymous**.
6. Create Cloud Firestore.
7. Publish this repository's `firestore.rules`.

`google-services.json` is ignored by Git and must not be committed.

## User login flow

```text
ELARVIC key
   ↓
Anonymous Firebase Auth
   ↓
Firestore: keys/{entered-key}
   ↓
active == true AND expiresAt > request.time
   ↓
WhatsApp gate (first successful login)
   ↓
Dashboard
```

The WhatsApp channel is:

`https://whatsapp.com/channel/0029VbDUColKQuJI4D5IVA2L`

## Key document

The Admin App creates:

```text
keys/ELARVIC_XXXXXXXXXXXX
```

with:

```text
active: true
createdAt: Timestamp
durationDays: 3 | 6 | 15
expiresAt: Timestamp
createdBy: admin UID
```

The User App never writes key documents and cannot list the collection. Its Firestore rules allow only a single-document validation request for an active, non-expired key.

## Testing checklist

- [ ] `google-services.json` exists in `app/`
- [ ] Anonymous provider enabled
- [ ] Firestore created
- [ ] User `firestore.rules` published
- [ ] Admin has generated an active key
- [ ] Enter generated key in User App
- [ ] WhatsApp screen appears
- [ ] Tap Open WhatsApp
- [ ] Return to app and verify dashboard
- [ ] Revoke/expire the key in Admin App and verify future validation is rejected
