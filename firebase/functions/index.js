const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

/**
 * Cloud Function triggered whenever a new Firebase Authentication user is created.
 * It automatically provisions a matching driver document in the `drivers` collection
 * so the mobile app immediately has a driver entity available for the new account.
 */
exports.createDriverForNewUser = functions.auth.user().onCreate(async (user) => {
  const driverId = user.uid;
  const firestore = admin.firestore();
  const driverRef = firestore.collection('drivers').doc(driverId);

  try {
    const existingDriver = await driverRef.get();
    if (existingDriver.exists) {
      functions.logger.info('Driver already exists for user, skipping creation.', { userId: driverId });
      return null;
    }

    const now = admin.firestore.FieldValue.serverTimestamp();
    const displayName = (user.displayName || '').trim();
    const derivedName = displayName || (user.email ? user.email.split('@')[0] : 'Nouveau conducteur');

    const driverPayload = {
      id: driverId,
      userId: driverId,
      name: derivedName,
      isActive: true,
      salary: 0.0,
      annualLicenseCost: 0.0,
      annualVisaCost: 0.0,
      createdAt: now,
      updatedAt: now,
    };

    await driverRef.set(driverPayload);
    functions.logger.info('Driver created successfully for new user.', { userId: driverId });
    return null;
  } catch (error) {
    functions.logger.error('Failed to create driver for new user.', { userId: driverId, error: error.message });
    throw error;
  }
});
