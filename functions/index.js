const functions = require('firebase-functions');
const admin = require('firebase-admin');

if (admin.apps.length === 0) {
    admin.initializeApp();
}

/**
 * Triggered on "notifications/{notifId}" creation.
 * Only handles "connection_request" type.
 */
exports.onNotificationCreated = functions.firestore
    .document('notifications/{notifId}')
    .onCreate(async (snapshot, context) => {
        const notificationData = snapshot.data();

        // Filter: Only trigger for connection_request
        if (notificationData.type !== 'connection_request') {
            console.log('Notification type is not connection_request. Skipping.');
            return null;
        }

        const toUid = notificationData.toUid;
        const fromUid = notificationData.fromUid;
        const fromName = notificationData.fromName;

        try {
            // Fetch receiver fcm_token
            const userDoc = await admin.firestore().collection('users').document(toUid).get();

            if (!userDoc.exists) {
                console.log(`User ${toUid} does not exist.`);
                return null;
            }

            const fcmToken = userDoc.data().fcm_token; // Match Android key name

            if (!fcmToken) {
                console.log(`User ${toUid} has no fcm_token.`);
                return null;
            }

            const message = {
                token: fcmToken,
                notification: {
                    title: `${fromName} ne connection request bheja! 🤝`,
                    body: 'Connect karna hai? PeerLearn open karo'
                },
                data: {
                    type: notificationData.type,
                    fromUid: fromUid,
                    fromName: fromName,
                    navigate_to: 'notifications'
                },
                android: {
                    priority: 'high',
                    notification: {
                        channelId: 'peerlearn_requests',
                        sound: 'default',
                        clickAction: 'OPEN_MAIN_ACTIVITY'
                    }
                }
            };

            const response = await admin.messaging().send(message);
            console.log('Successfully sent FCM message:', response);
            return response;

        } catch (error) {
            console.error('Error sending push notification:', error);
            return null;
        }
    });
