/**
 * Email Service using Gmail SMTP
 * 
 * Configuration via environment variables:
 * - SMTP_HOST: smtp.gmail.com
 * - SMTP_PORT: 587
 * - SMTP_USER: sender email
 * - SMTP_PASS: app password
 * - FROM_NAME: sender name
 */

const nodemailer = require('nodemailer');

const SMTP_HOST = process.env.SMTP_HOST || 'smtp.gmail.com';
const SMTP_PORT = parseInt(process.env.SMTP_PORT || '587', 10);
const SMTP_USER = process.env.SMTP_USER || 'campuseventorganizationhub@gmail.com';
const SMTP_PASS = process.env.SMTP_PASS || 'eozu ggdx atvy jdsy';
const FROM_NAME = process.env.FROM_NAME || 'Campus Event Org Hub';
const FROM_EMAIL = process.env.FROM_EMAIL || 'ceoh-noreply@ucc.edu.ph';

let transporter = null;

function getTransporter() {
    if (!transporter) {
        transporter = nodemailer.createTransport({
            host: SMTP_HOST,
            port: SMTP_PORT,
            secure: false,
            auth: {
                user: SMTP_USER,
                pass: SMTP_PASS,
            },
            tls: {
                rejectUnauthorized: false,
            },
        });
    }
    return transporter;
}

async function sendEmail(to, subject, htmlContent) {
    const transport = getTransporter();
    
    const mailOptions = {
        from: `"${FROM_NAME}" <${SMTP_USER}>`,
        to: to,
        subject: subject,
        html: htmlContent,
    };

    try {
        const info = await transport.sendMail(mailOptions);
        console.log(`Email sent to ${to}: ${info.messageId}`);
        return { success: true, messageId: info.messageId };
    } catch (error) {
        console.error(`Failed to send email to ${to}:`, error);
        return { success: false, error: error.message };
    }
}

async function sendVerificationEmail(to, name, studentId, token) {
    const verifyUrl = `ceoh://verify?token=${token}&sid=${studentId}`;
    
    const html = `
    <!DOCTYPE html>
    <html>
    <head>
        <style>
            body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
            .container { max-width: 600px; margin: 0 auto; padding: 20px; }
            .header { background: #007bff; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
            .content { background: #f8f9fa; padding: 30px; border-radius: 0 0 8px 8px; }
            .button { display: inline-block; background: #007bff; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
            .footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">
                <h1>${FROM_NAME}</h1>
            </div>
            <div class="content">
                <h2>Welcome, ${name}!</h2>
                <p>Thank you for registering with ${FROM_NAME}. Please verify your email address to activate your account.</p>
                <p style="text-align: center;">
                    <a href="${verifyUrl}" class="button">Verify Email Address</a>
                </p>
                <p>Or copy and paste this link into your browser:</p>
                <p style="word-break: break-all; color: #007bff;">${verifyUrl}</p>
                <p><strong>Note:</strong> This verification link will expire in 24 hours.</p>
            </div>
            <div class="footer">
                <p>If you did not create an account, please ignore this email.</p>
            </div>
        </div>
    </body>
    </html>
    `;

    return sendEmail(to, `Verify your ${FROM_NAME} account`, html);
}

async function sendPasswordResetEmail(to, name, studentId, token) {
    const resetUrl = `ceoh://reset-password?token=${token}&sid=${studentId}`;
    
    const html = `
    <!DOCTYPE html>
    <html>
    <head>
        <style>
            body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
            .container { max-width: 600px; margin: 0 auto; padding: 20px; }
            .header { background: #dc3545; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
            .content { background: #f8f9fa; padding: 30px; border-radius: 0 0 8px 8px; }
            .button { display: inline-block; background: #dc3545; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
            .footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">
                <h1>Password Reset Request</h1>
            </div>
            <div class="content">
                <h2>Hi, ${name}!</h2>
                <p>We received a request to reset your password for ${FROM_NAME}.</p>
                <p style="text-align: center;">
                    <a href="${resetUrl}" class="button">Reset Password</a>
                </p>
                <p>Or copy and paste this link into your browser:</p>
                <p style="word-break: break-all; color: #dc3545;">${resetUrl}</p>
                <p><strong>Important:</strong> This password reset link will expire in 1 hour.</p>
                <p>If you did not request a password reset, please ignore this email. Your password will remain unchanged.</p>
            </div>
            <div class="footer">
                <p>This is an automated message. Please do not reply.</p>
            </div>
        </div>
    </body>
    </html>
    `;

    return sendEmail(to, `Reset your ${FROM_NAME} password`, html);
}

module.exports = {
    sendEmail,
    sendVerificationEmail,
    sendPasswordResetEmail,
};
