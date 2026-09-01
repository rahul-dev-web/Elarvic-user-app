# Elarvic User App

Clean Android/Kotlin reconstruction of the Elarvic V1 user-side flow.

## User flow

1. User opens Elarvic V1.
2. User enters the access key issued by the admin app.
3. The app signs in anonymously to Firebase and validates the exact key document.
4. The key must be active and its `expiresAt` must be in the future.
5. After a successful first login, the WhatsApp channel gate is shown once.
6. Tapping **Open WhatsApp** opens the configured channel and then the dashboard is shown.
7. On later launches, the saved key is revalidated so expiry/revocation still works.

The user app does **not** use Google Sign-In.

## Firebase setup

1. Create/use the Firebase project shared with the admin app.
2. Add the Android app with package `com.elarvic.user`.
3. Download `google-services.json` into `app/` locally.
4. Enable **Anonymous** sign-in in Firebase Authentication.
5. Create a Firestore database.
6. Deploy `firestore.rules` from this repository.

### Key document shape

`keys/{ELARVIC_xxxxxxxxxxxx}`

```text
active: true
createdAt: timestamp
durationDays: 3 | 6 | 15
expiresAt: timestamp
createdBy: admin uid
```

The key itself is the document ID, so the user app only needs the copied key to validate it.

## WhatsApp

Configured channel:

`https://whatsapp.com/channel/0029VbDUColKQuJI4D5IVA2L`

## Security note

The user client has no Firebase admin credentials. It only has permission to perform a single-key validation read. Key creation/revocation is restricted to authorized admin accounts.

`google-services.json`, signing keys, local properties, and environment secrets must not be committed.
