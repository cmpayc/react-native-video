package com.brentvatne.exoplayer

import android.util.Base64
import androidx.media3.common.util.Util
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.DrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.HttpMediaDrmCallback
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback
import androidx.media3.exoplayer.drm.UnsupportedDrmException
import com.brentvatne.common.api.DRMProps
import java.util.UUID

class DRMManager(private val dataSourceFactory: HttpDataSource.Factory) : DRMManagerSpec {
    private var hasDrmFailed = false

    @Throws(UnsupportedDrmException::class)
    override fun buildDrmSessionManager(uuid: UUID, drmProps: DRMProps): DrmSessionManager? = buildDrmSessionManager(uuid, drmProps, 0)

    @Throws(UnsupportedDrmException::class)
    private fun buildDrmSessionManager(uuid: UUID, drmProps: DRMProps, retryCount: Int = 0): DrmSessionManager? {
        if (Util.SDK_INT < 18) {
            return null
        }

        try {
            if (drmProps.drmType == "clearkey" && drmProps.drmLocalClearKeysList.isNotEmpty()) {
                val drmLocalClearKeysList = arrayListOf<String>()

                for (drmLocalClearKey in drmProps.drmLocalClearKeysList) {
                    val localClearKeyInfo = drmLocalClearKey.split(":", limit = 2)
                    val localClearKeyId = localClearKeyInfo[0]
                    val decodedKeyHex = hexStringToByteArray(localClearKeyId)
                    val encodedKeyBase64 = Base64.encodeToString(decodedKeyHex, Base64.NO_PADDING)

                    val localClearKeyValue = localClearKeyInfo[1]
                    val decodedValueHex = hexStringToByteArray(localClearKeyValue)
                    val encodedValueBase64 = Base64.encodeToString(decodedValueHex, Base64.NO_PADDING)

                    drmLocalClearKeysList.add(
                        """{"kty":"oct","k":"${encodedValueBase64.trim()}","kid":"${encodedKeyBase64.trim()}"}"""
                    )
                }

                val clearKeyString =
                    """{"keys":[${drmLocalClearKeysList.joinToString(", ")}],"type":"temporary"}"""

                val drmCallback = LocalMediaDrmCallback(clearKeyString.toByteArray())

                val mediaDrm = FrameworkMediaDrm.newInstance(uuid)

                return DefaultDrmSessionManager.Builder()
                    .setPlayClearSamplesWithoutKeys(true)
                    .setMultiSession(false)
                    .setKeyRequestParameters(HashMap<String, String>())
                    .setUuidAndExoMediaDrmProvider(uuid) { mediaDrm }
                    .build(drmCallback)
            }

            val drmCallback = HttpMediaDrmCallback(drmProps.drmLicenseServer, dataSourceFactory)

            // Set DRM headers
            val keyRequestPropertiesArray = drmProps.drmLicenseHeader
            for (i in keyRequestPropertiesArray.indices step 2) {
                drmCallback.setKeyRequestProperty(keyRequestPropertiesArray[i], keyRequestPropertiesArray[i + 1])
            }

            val mediaDrm = FrameworkMediaDrm.newInstance(uuid)

            // TODO: This isn't very secure, should be fixed
            if (hasDrmFailed) {
                // When DRM fails using L1 we want to switch to L3
                mediaDrm.setPropertyString("securityLevel", "L3")
            }

            return DefaultDrmSessionManager.Builder()
                .setUuidAndExoMediaDrmProvider(uuid) { mediaDrm }
                .setKeyRequestParameters(null)
                .setMultiSession(drmProps.multiDrm)
                .build(drmCallback)
        } catch (ex: UnsupportedDrmException) {
            hasDrmFailed = true
            throw ex
        } catch (ex: Exception) {
            if (retryCount < 3) {
                // Attempt retry 3 times in case where the OS Media DRM Framework fails for whatever reason
                hasDrmFailed = true
                return buildDrmSessionManager(uuid, drmProps, retryCount + 1)
            }
            throw UnsupportedDrmException(UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME, ex)
        }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        return ByteArray(s.length / 2) { i ->
            val index = i * 2
            ((Character.digit(s[index], 16) shl 4) +
                Character.digit(s[index + 1], 16)).toByte()
        }
    }
}
