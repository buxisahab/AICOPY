const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

/**
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * FEATURE 3: INACTIVE USER AUTO-CLEANUP
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * 
 * Goal: Automatically delete user data if the user does NOT open the app
 * for 30 consecutive days.
 * 
 * why: To maintain database hygiene, reduce storage costs, and comply with
 * privacy best practices by not retaining data of abandoned accounts indefinitely.
 * 
 * Safety:
 * - Runs only once daily.
 * - Checks 'lastActiveAt' timestamp.
 * - Does NOT affect active users.
 * - Does NOT affect users with no 'lastActiveAt' (new users or errors), 
 *   unless we specifically handle nulls (here we assume null = active to be safe).
 */

exports.cleanupInactiveUsers = functions.pubsub.schedule('every 24 hours').onRun(async (context) => {
    const db = admin.firestore();
    const auth = admin.auth();

    // Calculate cutoff date (30 days ago)
    const now = new Date();
    const cutoffDate = new Date(now.getTime() - (30 * 24 * 60 * 60 * 1000));

    console.log(`Starting cleanup using cutoff: ${cutoffDate.toISOString()}`);

    try {
        // Query for users inactive since cutoffDate
        // NOTE: This query REQUIRES a composite index in Firestore on 'lastActiveAt' field.
        // Create it in Firebase Console -> Firestore -> Indexes if not exists.
        const snapshot = await db.collection('users')
            .where('lastActiveAt', '<=', cutoffDate)
            .get();

        if (snapshot.empty) {
            console.log('No inactive users found.');
            return null;
        }

        console.log(`Found ${snapshot.size} inactive users to delete.`);

        // Firestore batch limit is 500 operations. We'll use 400 for safety.
        // We will process deletes in chunks.
        const BATCH_SIZE = 400;
        const docs = snapshot.docs;
        let totalDeleted = 0;

        for (let i = 0; i < docs.length; i += BATCH_SIZE) {
            const batch = db.batch();
            const chunk = docs.slice(i, i + BATCH_SIZE);

            chunk.forEach(doc => {
                // 1. Delete Firestore User Document
                batch.delete(doc.ref);

                // 2. Queue Auth deletion if needed (omitted for safety/speed here)
            });

            await batch.commit();
            totalDeleted += chunk.length;
            console.log(`Batch confirmed: Deleted ${chunk.length} users.`);
        }

        console.log(`Cleanup complete. Total deleted: ${totalDeleted} user documents.`);
        return null;

    } catch (error) {
        console.error('Error during user cleanup:', error);
        return null; // Function must return null or strict promise
    }
});

/**
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * OPTIONAL: SERVER-SIDE STATS AGGREGATION
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * 
 * While Firebase Analytics handles DAU/MAU/Retention automatically,
 * this function could compute custom stats if needed.
 * 
 * Currently disabled/commented as per instruction to use Firebase Analytics dashboard.
 */
/*
exports.calculateDailyStats = functions.pubsub.schedule('every 24 hours').onRun(async (context) => {
    // Logic to count active users based on lastActiveAt
    // and save to a 'stats' collection.
});
*/
