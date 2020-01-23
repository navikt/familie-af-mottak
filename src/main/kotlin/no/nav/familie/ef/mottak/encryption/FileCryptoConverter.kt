package no.nav.familie.ef.mottak.encryption

import no.nav.familie.ef.mottak.repository.domain.Fil


class FileCryptoConverter : AbstractCryptoConverter<Fil?>() {

    override fun byteArrayToEntityAttribute(dbData: ByteArray?): Fil? {
        return if (dbData == null) null else Fil(dbData)
    }

    override fun entityAttributeToByteArray(attribute: Fil?): ByteArray? {
        return attribute?.bytes
    }
}
