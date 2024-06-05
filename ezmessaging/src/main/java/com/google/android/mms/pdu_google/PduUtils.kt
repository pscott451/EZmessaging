package com.google.android.mms.pdu_google

object PduUtils {

    @JvmStatic
    fun remakeAddressMap(
        oldMap: HashMap<Int, Array<EncodedStringValue>?>,
        myNumber: String?
    ): HashMap<Int, Array<EncodedStringValue>?> {
        val newMap = hashMapOf<Int, Array<EncodedStringValue>?>()
        newMap[PduHeaders.FROM] = oldMap[PduHeaders.FROM]
        newMap[PduHeaders.TO] = oldMap[PduHeaders.TO]?.filter { it.string == myNumber }?.toTypedArray()
        val ccArray = arrayListOf<EncodedStringValue>()
        val bccArray = arrayListOf<EncodedStringValue>()
        oldMap.forEach { (addressType, encodedStringValues) ->
            encodedStringValues?.forEach { value ->
                if (addressType != PduHeaders.FROM && value.string != myNumber) {
                    when {
                        addressType == PduHeaders.BCC -> bccArray.add(value)
                        else -> ccArray.add(value)
                    }
                }
            }
        }
        newMap[PduHeaders.CC] = if (ccArray.isNotEmpty()) ccArray.toTypedArray() else null
        newMap[PduHeaders.BCC] = if (bccArray.isNotEmpty()) bccArray.toTypedArray() else null
        return newMap
    }
}