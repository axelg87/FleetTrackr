import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';
import { DateTime } from 'luxon';

const app = admin.apps.length ? admin.app() : admin.initializeApp();
const firestore = app.firestore();
const messaging = app.messaging();

const COLLECTION_DRIVERS = 'drivers';
const COLLECTION_ENTRIES = 'entries';
const FIELD_DRIVER_ID = 'driverId';
const FIELD_USER_ID = 'uid';
const FIELD_FCM_TOKEN = 'fcmToken';
const FIELD_DATE = 'date';
const ENTRY_USER_ID_FIELD = 'userId';

const TIMEZONE = 'Asia/Dubai';
const DATE_FORMAT = 'yyyy-MM-dd';
const REMINDER_CHANNEL_ID = 'fleet_manager_reminders';

const ACTION_MISSING_INCOME = 'missing_income';
const DATA_KEY_ACTION = 'action';
const DATA_KEY_MISSING_DATE = 'missingDate';
const DATA_KEY_CHANNEL_ID = 'channel_id';

interface DriverDocument {
  [FIELD_USER_ID]?: string;
  [FIELD_FCM_TOKEN]?: string;
  name?: string;
}

interface IncomeEntryDocument {
  [FIELD_DRIVER_ID]?: string;
  [FIELD_DATE]?: unknown;
  userId?: string;
}

type DateRange = {
  isoDate: string;
  start: admin.firestore.Timestamp;
  end: admin.firestore.Timestamp;
};

function resolveDriverIdentifier(doc: FirebaseFirestore.QueryDocumentSnapshot<DriverDocument>): string | null {
  const data = doc.data();
  const identifier = (data?.[FIELD_USER_ID] ?? doc.id)?.toString().trim();

  if (!identifier) {
    functions.logger.warn('Skipping driver because no identifier is available', {
      documentId: doc.id,
    });
    return null;
  }

  return identifier;
}

function getTargetDateRange(referenceDateTime: DateTime = DateTime.now()): DateRange {
  const dubaiNow = referenceDateTime.setZone(TIMEZONE);
  const targetDate = dubaiNow.minus({ days: 1 }).startOf('day');

  const start = targetDate.startOf('day');
  const end = targetDate.endOf('day');

  return {
    isoDate: targetDate.toFormat(DATE_FORMAT),
    start: admin.firestore.Timestamp.fromDate(start.toJSDate()),
    end: admin.firestore.Timestamp.fromDate(end.toJSDate()),
  };
}

async function hasSubmittedIncome(
  driverId: string,
  targetRange: DateRange
): Promise<boolean> {
  const entriesRef = firestore.collection(COLLECTION_ENTRIES);
  const driverFieldCandidates = [FIELD_DRIVER_ID, ENTRY_USER_ID_FIELD, FIELD_USER_ID];

  for (const fieldName of driverFieldCandidates) {
    const driverQuery = entriesRef.where(fieldName, '==', driverId);

    const stringMatch = await tryFetch(async () =>
      driverQuery.where(FIELD_DATE, '==', targetRange.isoDate).limit(1).get()
    );
    if (stringMatch?.size) {
      return true;
    }

    const timestampMatch = await tryFetch(async () =>
      driverQuery
        .where(FIELD_DATE, '>=', targetRange.start)
        .where(FIELD_DATE, '<=', targetRange.end)
        .limit(1)
        .get()
    );
    if (timestampMatch?.size) {
      return true;
    }

    const fallbackSnapshot = await tryFetch(async () => driverQuery.limit(5).get());
    if (!fallbackSnapshot) {
      continue;
    }

    const hasMatch = fallbackSnapshot.docs.some((doc) => {
      const entry = doc.data() as IncomeEntryDocument;
      const rawDate = entry?.[FIELD_DATE];

      if (typeof rawDate === 'string') {
        return rawDate === targetRange.isoDate;
      }

      if (rawDate instanceof admin.firestore.Timestamp) {
        const millis = rawDate.toMillis();
        return millis >= targetRange.start.toMillis() && millis <= targetRange.end.toMillis();
      }

      if (rawDate && typeof (rawDate as { toDate?: () => Date }).toDate === 'function') {
        const date = (rawDate as { toDate: () => Date }).toDate();
        const millis = date.getTime();
        return millis >= targetRange.start.toMillis() && millis <= targetRange.end.toMillis();
      }

      return false;
    });

    if (hasMatch) {
      return true;
    }
  }

  return false;
}

async function tryFetch<T>(operation: () => Promise<T>): Promise<T | null> {
  try {
    return await operation();
  } catch (error) {
    functions.logger.warn('Query execution failed', error);
    return null;
  }
}

function buildNotificationMessage(
  token: string,
  driverName: string | undefined,
  isoDate: string
): admin.messaging.Message {
  const formattedDate = DateTime.fromFormat(isoDate, DATE_FORMAT, { zone: TIMEZONE }).toFormat('MMM dd, yyyy');
  const title = 'Reminder: Submit your income';
  const body = `${driverName ?? 'Driver'}, please log your income for ${formattedDate}.`;

  return {
    token,
    notification: {
      title,
      body,
    },
    data: {
      [DATA_KEY_ACTION]: ACTION_MISSING_INCOME,
      [DATA_KEY_MISSING_DATE]: isoDate,
      [DATA_KEY_CHANNEL_ID]: REMINDER_CHANNEL_ID,
    },
    android: {
      notification: {
        channelId: REMINDER_CHANNEL_ID,
      },
    },
    apns: {
      payload: {
        aps: {
          category: ACTION_MISSING_INCOME,
        },
      },
    },
  };
}

export const sendMissingIncomeReminders = functions.pubsub
  .schedule('0 7 * * *')
  .timeZone(TIMEZONE)
  .onRun(async () => {
    const targetRange = getTargetDateRange();
    functions.logger.info('Preparing missing income reminders', targetRange);

    const driversSnapshot = await firestore.collection(COLLECTION_DRIVERS).get();
    if (driversSnapshot.empty) {
      functions.logger.info('No drivers found, skipping reminder job');
      return null;
    }

    const messages: admin.messaging.Message[] = [];

    for (const driverDoc of driversSnapshot.docs) {
      const driverId = resolveDriverIdentifier(driverDoc);
      if (!driverId) {
        continue;
      }

      const driverData = driverDoc.data();
      const token = driverData?.[FIELD_FCM_TOKEN]?.toString().trim();
      if (!token) {
        functions.logger.debug('Skipping driver without FCM token', {
          driverId,
          documentId: driverDoc.id,
        });
        continue;
      }

      const hasSubmitted = await hasSubmittedIncome(driverId, targetRange);
      if (hasSubmitted) {
        functions.logger.debug('Driver already submitted income for target date', {
          driverId,
          isoDate: targetRange.isoDate,
        });
        continue;
      }

      messages.push(buildNotificationMessage(token, driverData?.name, targetRange.isoDate));
    }

    if (messages.length === 0) {
      functions.logger.info('No missing income reminders to send for date', {
        isoDate: targetRange.isoDate,
      });
      return null;
    }

    const response = await messaging.sendAll(messages);

    functions.logger.info('Missing income reminders dispatched', {
      isoDate: targetRange.isoDate,
      successCount: response.successCount,
      failureCount: response.failureCount,
    });

    if (response.failureCount > 0) {
      response.responses
        .map((result, index) => ({ result, index }))
        .filter((item) => !item.result.success)
        .forEach((item) => {
          functions.logger.error('Failed to send reminder', {
            error: item.result.error,
            message: messages[item.index],
          });
        });
    }

    return null;
  });
