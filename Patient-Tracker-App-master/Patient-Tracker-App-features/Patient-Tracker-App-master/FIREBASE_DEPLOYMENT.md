# Firebase Deployment Instructions

## Deploy Updated Rules

After making changes to Firebase rules, you need to deploy them:

```cmd
cd Patient-Tracker-App-master
firebase deploy --only firestore:rules,storage
```

## Individual Deployments

Deploy only Firestore rules:
```cmd
firebase deploy --only firestore:rules
```

Deploy only Storage rules:
```cmd
firebase deploy --only storage
```

## Verify Deployment

After deployment, verify in Firebase Console:
1. Go to https://console.firebase.google.com
2. Select your project
3. Check Firestore Rules and Storage Rules sections

## Important Notes

- **Storage Rules Fixed**: Added authentication and ownership checks to write rule
- **Firestore Rules Enhanced**: Added privacy checks, glass break access logic, and doctor view tracking
- Both rule sets now properly handle the new privacy and access control features
