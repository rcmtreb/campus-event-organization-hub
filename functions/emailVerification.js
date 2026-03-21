/**
 * Email Verification & Authentication Cloud Functions
 * 
 * Functions:
 * - registerUser: Hash password, create user, send verification email
 * - verifyEmail: Validate token, mark email as verified
 * - requestPasswordReset: Send password reset email
 * - resetPassword: Update password with token validation
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');
const bcrypt = require('bcrypt');
const crypto = require('crypto');
const { sendVerificationEmail, sendPasswordResetEmail } = require('./emailService');

admin.initializeApp();
const db = admin.firestore();

const APP_NAME = 'Campus Event Org Hub';
const VERIFICATION_EXPIRY_HOURS = 24;
const RESET_TOKEN_EXPIRY_HOURS = 1;
const BCRYPT_ROUNDS = 10;

function generateToken() {
    const timestamp = Date.now().toString(36);
    const randomBytes = crypto.randomBytes(32).toString('hex');
    return `${timestamp}_${randomBytes}`;
}

function getExpiryDate(hours) {
    return new Date(Date.now() + hours * 60 * 60 * 1000);
}

/**
 * registerUser - Creates a new user account and sends verification email
 * 
 * Called from Android app after successful local registration
 */
exports.registerUser = functions.https.onCall(async (data, context) => {
    const { studentId, name, email, password, role, department } = data;

    if (!studentId || !name || !email || !password || !role || !department) {
        throw new functions.https.HttpsError('invalid-argument', 'Missing required fields');
    }

    try {
        const userRef = db.collection('users').doc(studentId);
        const userDoc = await userRef.get();

        if (userDoc.exists) {
            throw new functions.https.HttpsError('already-exists', 'User already exists');
        }

        const hashedPassword = await bcrypt.hash(password, BCRYPT_ROUNDS);
        const verificationToken = generateToken();
        const verificationExpiry = getExpiryDate(VERIFICATION_EXPIRY_HOURS);

        const userData = {
            student_id: studentId,
            name: name,
            email: email,
            password: hashedPassword,
            role: role,
            department: department,
            email_verified: false,
            verification_token: verificationToken,
            verification_expires: admin.firestore.Timestamp.fromDate(verificationExpiry),
            created_at: admin.firestore.FieldValue.serverTimestamp(),
            updated_at: admin.firestore.FieldValue.serverTimestamp(),
        };

        await userRef.set(userData);

        await sendVerificationEmail(email, name, studentId, verificationToken);

        console.log(`User registered: ${studentId}, verification email sent to ${email}`);

        return {
            success: true,
            message: 'Registration successful. Please check your email to verify your account.',
        };
    } catch (error) {
        console.error('registerUser error:', error);
        if (error instanceof functions.https.HttpsError) {
            throw error;
        }
        throw new functions.https.HttpsError('internal', 'Registration failed: ' + error.message);
    }
});

/**
 * verifyEmail - Verifies user email with token
 */
exports.verifyEmail = functions.https.onCall(async (data, context) => {
    const { studentId, token } = data;

    if (!studentId || !token) {
        throw new functions.https.HttpsError('invalid-argument', 'Missing studentId or token');
    }

    try {
        const userRef = db.collection('users').doc(studentId);
        const userDoc = await userRef.get();

        if (!userDoc.exists) {
            throw new functions.https.HttpsError('not-found', 'User not found');
        }

        const userData = userDoc.data();

        if (userData.email_verified) {
            return { success: true, message: 'Email already verified' };
        }

        if (userData.verification_token !== token) {
            throw new functions.https.HttpsError('invalid-argument', 'Invalid verification token');
        }

        if (userData.verification_expires) {
            const expires = userData.verification_expires.toDate();
            if (expires < new Date()) {
                throw new functions.https.HttpsError('deadline-exceeded', 'Verification token expired');
            }
        }

        await userRef.update({
            email_verified: true,
            verification_token: admin.firestore.FieldValue.delete(),
            verification_expires: admin.firestore.FieldValue.delete(),
            updated_at: admin.firestore.FieldValue.serverTimestamp(),
        });

        console.log(`Email verified for user: ${studentId}`);

        return { success: true, message: 'Email verified successfully!' };
    } catch (error) {
        console.error('verifyEmail error:', error);
        if (error instanceof functions.https.HttpsError) {
            throw error;
        }
        throw new functions.https.HttpsError('internal', 'Verification failed: ' + error.message);
    }
});

/**
 * resendVerificationEmail - Resends verification email
 */
exports.resendVerificationEmail = functions.https.onCall(async (data, context) => {
    const { studentId, email } = data;

    if (!studentId || !email) {
        throw new functions.https.HttpsError('invalid-argument', 'Missing studentId or email');
    }

    try {
        const userRef = db.collection('users').doc(studentId);
        const userDoc = await userRef.get();

        if (!userDoc.exists) {
            throw new functions.https.HttpsError('not-found', 'User not found');
        }

        const userData = userDoc.data();

        if (userData.email_verified) {
            return { success: true, message: 'Email already verified' };
        }

        const newToken = generateToken();
        const verificationExpiry = getExpiryDate(VERIFICATION_EXPIRY_HOURS);

        await userRef.update({
            verification_token: newToken,
            verification_expires: admin.firestore.Timestamp.fromDate(verificationExpiry),
            updated_at: admin.firestore.FieldValue.serverTimestamp(),
        });

        await sendVerificationEmail(email, userData.name, studentId, newToken);

        console.log(`Verification email resent to: ${email}`);

        return { success: true, message: 'Verification email sent. Check your inbox.' };
    } catch (error) {
        console.error('resendVerificationEmail error:', error);
        if (error instanceof functions.https.HttpsError) {
            throw error;
        }
        throw new functions.https.HttpsError('internal', 'Failed to send email: ' + error.message);
    }
});

/**
 * requestPasswordReset - Sends password reset email
 */
exports.requestPasswordReset = functions.https.onCall(async (data, context) => {
    const { email } = data;

    if (!email) {
        throw new functions.https.HttpsError('invalid-argument', 'Missing email');
    }

    try {
        const snapshot = await db.collection('users')
            .where('email', '==', email)
            .limit(1)
            .get();

        if (snapshot.empty) {
            console.log(`Password reset requested for non-existent email: ${email}`);
            return { success: true, message: 'If the email exists, a reset link has been sent.' };
        }

        const userDoc = snapshot.docs[0];
        const userData = userDoc.data();
        const studentId = userDoc.id;

        const resetToken = generateToken();
        const resetExpiry = getExpiryDate(RESET_TOKEN_EXPIRY_HOURS);

        await userDoc.ref.update({
            reset_token: resetToken,
            reset_token_exp: admin.firestore.Timestamp.fromDate(resetExpiry),
            updated_at: admin.firestore.FieldValue.serverTimestamp(),
        });

        await sendPasswordResetEmail(email, userData.name, studentId, resetToken);

        console.log(`Password reset email sent to: ${email}`);

        return { success: true, message: 'If the email exists, a reset link has been sent.' };
    } catch (error) {
        console.error('requestPasswordReset error:', error);
        throw new functions.https.HttpsError('internal', 'Failed to process request: ' + error.message);
    }
});

/**
 * resetPassword - Updates password with token validation
 */
exports.resetPassword = functions.https.onCall(async (data, context) => {
    const { studentId, token, newPassword } = data;

    if (!studentId || !token || !newPassword) {
        throw new functions.https.HttpsError('invalid-argument', 'Missing required fields');
    }

    if (newPassword.length < 8) {
        throw new functions.https.HttpsError('invalid-argument', 'Password must be at least 8 characters');
    }

    try {
        const userRef = db.collection('users').doc(studentId);
        const userDoc = await userRef.get();

        if (!userDoc.exists) {
            throw new functions.https.HttpsError('not-found', 'User not found');
        }

        const userData = userDoc.data();

        if (userData.reset_token !== token) {
            throw new functions.https.HttpsError('invalid-argument', 'Invalid reset token');
        }

        if (userData.reset_token_exp) {
            const expires = userData.reset_token_exp.toDate();
            if (expires < new Date()) {
                throw new functions.https.HttpsError('deadline-exceeded', 'Reset token expired');
            }
        }

        const hashedPassword = await bcrypt.hash(newPassword, BCRYPT_ROUNDS);

        await userRef.update({
            password: hashedPassword,
            reset_token: admin.firestore.FieldValue.delete(),
            reset_token_exp: admin.firestore.FieldValue.delete(),
            updated_at: admin.firestore.FieldValue.serverTimestamp(),
        });

        console.log(`Password reset successful for user: ${studentId}`);

        return { success: true, message: 'Password reset successfully!' };
    } catch (error) {
        console.error('resetPassword error:', error);
        if (error instanceof functions.https.HttpsError) {
            throw error;
        }
        throw new functions.https.HttpsError('internal', 'Failed to reset password: ' + error.message);
    }
});

/**
 * verifyPassword - Verifies password for a user (used by Android app)
 */
exports.verifyPassword = functions.https.onCall(async (data, context) => {
    const { studentId, password } = data;

    if (!studentId || !password) {
        throw new functions.https.HttpsError('invalid-argument', 'Missing required fields');
    }

    try {
        const userRef = db.collection('users').doc(studentId);
        const userDoc = await userRef.get();

        if (!userDoc.exists) {
            return { success: false, verified: false, message: 'User not found' };
        }

        const userData = userDoc.data();
        const hashedPassword = userData.password;

        const isValid = await bcrypt.compare(password, hashedPassword);

        if (!isValid) {
            return { success: true, verified: false, message: 'Invalid password' };
        }

        return {
            success: true,
            verified: true,
            emailVerified: userData.email_verified || false,
            message: 'Password verified',
        };
    } catch (error) {
        console.error('verifyPassword error:', error);
        throw new functions.https.HttpsError('internal', 'Verification failed: ' + error.message);
    }
});
