const functions = require('firebase-functions/v1');
const admin = require('firebase-admin');

admin.initializeApp();

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

/**
 * Scheduled function that reminds drivers to submit their daily income entry
 * if they have not added one for yesterday.
 */
exports.sendDailyIncomeReminder = functions.pubsub
  .schedule('0 7 * * *')
  .timeZone('Asia/Dubai')
  .onRun(async () => {
    const firestore = admin.firestore();
    const messaging = admin.messaging();

    const { start, end, formattedDate } = getGulfYesterdayRange();

    functions.logger.info('Starting daily income reminder check', {
      start,
      end,
      formattedDate,
    });

    try {
      const driversSnapshot = await firestore
        .collection('drivers')
        .where('isActive', '==', true)
        .get();

      if (driversSnapshot.empty) {
        functions.logger.info('No active drivers found, skipping reminders.');
        return null;
      }

      const reminderPromises = driversSnapshot.docs.map(async (driverDoc) => {
        const driverData = driverDoc.data() || {};
        const driverId = driverData.id || driverDoc.id;
        const userId = driverData.userId || driverId;

        const entryExists = await hasEntryForDate(
          firestore,
          driverId,
          start,
          end
        );

        if (entryExists) {
          functions.logger.debug('Entry found for driver, skipping reminder', {
            driverId,
            formattedDate,
          });
          return null;
        }

        const userDoc = await firestore.collection('users').doc(userId).get();
        const token = userDoc.get('fcm_token');

        if (!token) {
          functions.logger.warn('No FCM token for user, cannot send reminder', {
            driverId,
            userId,
          });
          return null;
        }

        const message = {
          token,
          notification: {
            title: '🚨 Missing Income Entry',
            body: "You haven't added your income for yesterday. Tap to complete it now.",
          },
          data: {
            title: '🚨 Missing Income Entry',
            body: "You haven't added your income for yesterday. Tap to complete it now.",
            action: 'MISSING_INCOME_ENTRY',
            driverId: driverId,
            driver_id: driverId,
            entryDate: formattedDate,
            channel_id: 'fleet_manager_reminders',
          },
        };

        try {
          await messaging.send(message);
          functions.logger.info('Reminder notification sent to driver', {
            driverId,
            formattedDate,
          });
        } catch (error) {
          functions.logger.error('Failed to send reminder notification', {
            driverId,
            error: error.message,
          });
        }

        return null;
      });

      await Promise.all(reminderPromises);
      functions.logger.info('Daily income reminder check completed', {
        formattedDate,
      });
    } catch (error) {
      functions.logger.error('Daily income reminder job failed', {
        error: error.message,
      });
    }

    return null;
  });

/**
 * Determine if a driver already has a daily entry for the specified date range.
 *
 * @param {FirebaseFirestore.Firestore} firestore
 * @param {string} driverId
 * @param {admin.firestore.Timestamp} start
 * @param {admin.firestore.Timestamp} end
 * @return {Promise<boolean>}
 */
async function hasEntryForDate(firestore, driverId, start, end) {
  const snapshot = await firestore
    .collection('entriesNEW')
    .where('driverId', '==', driverId)
    .where('date', '>=', start)
    .where('date', '<=', end)
    .limit(1)
    .get();

  return !snapshot.empty;
}

/**
 * Calculate yesterday's date range in Gulf Standard Time (UTC+4).
 *
 * @return {{start: admin.firestore.Timestamp, end: admin.firestore.Timestamp, formattedDate: string}}
 */
function getGulfYesterdayRange() {
  const formatter = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Dubai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });

  const now = new Date();
  const [year, month, day] = formatter
    .format(now)
    .split('-')
    .map((value) => parseInt(value, 10));

  const gulfOffsetMs = 4 * 60 * 60 * 1000;
  const millisPerDay = 24 * 60 * 60 * 1000;

  const todayStartUtc = Date.UTC(year, month - 1, day) - gulfOffsetMs;
  const yesterdayStartUtc = todayStartUtc - millisPerDay;
  const yesterdayEndUtc = todayStartUtc - 1;

  return {
    start: admin.firestore.Timestamp.fromDate(new Date(yesterdayStartUtc)),
    end: admin.firestore.Timestamp.fromDate(new Date(yesterdayEndUtc)),
    formattedDate: formatter.format(new Date(yesterdayStartUtc)),
  };
}
