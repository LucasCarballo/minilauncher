package com.minilauncher.data.repository

import android.accounts.AccountManager
import android.content.Context
import android.os.Build
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrieves the device owner's display name.
 *
 * Tries multiple sources in order of reliability:
 * 1. Profile contact (most reliable on modern Android)
 * 2. Google account name via AccountManager
 * 3. Build.USER fallback
 *
 * On API 30+, AccountManager.getAccounts() returns empty without READ_CONTACTS.
 * The profile contact query works for launcher apps with QUERY_ALL_PACKAGES.
 */
@Singleton
class DeviceOwnerNameProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun getOwnerName(): String {
        // 1. Try profile contact — most reliable on modern Android
        val profileName = getProfileName()
        if (!profileName.isNullOrBlank()) return profileName

        // 2. Try AccountManager — works on older Android, sometimes on newer
        val accountName = getAccountName()
        if (!accountName.isNullOrBlank()) return accountName

        // 3. Fallback to Build.USER — often "owner" but sometimes the actual name
        val buildUser = Build.USER?.substringBefore("@")?.ifBlank { null }
        if (!buildUser.isNullOrBlank() && buildUser != "owner" && buildUser != "root") return buildUser

        return ""
    }

    private fun getProfileName(): String? {
        return try {
            context.contentResolver.query(
                ContactsContract.Profile.CONTENT_URI,
                arrayOf(
                    ContactsContract.Profile.DISPLAY_NAME,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME)
                    if (displayNameIndex >= 0) {
                        cursor.getString(displayNameIndex)
                    } else null
                } else null
            }
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun getAccountName(): String? {
        return try {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.accounts
            val googleAccount = accounts.firstOrNull { it.type.equals("com.google", ignoreCase = true) }
            googleAccount?.name?.substringBefore("@")?.ifBlank { null }
                ?: accounts.firstOrNull()?.name?.substringBefore("@")?.ifBlank { null }
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }
}