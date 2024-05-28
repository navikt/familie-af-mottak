package no.nav.familie.ef.mottak.encryption

import no.nav.familie.ef.mottak.repository.domain.EncryptedFile
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

@WritingConverter
class FileCryptoWritingConverter : AbstractCryptoWritingConverter<EncryptedFile?>() {
    override fun entityAttributeToByteArray(attribute: EncryptedFile?): ByteArray? {
        return attribute?.bytes
    }
}

@ReadingConverter
class FileCryptoReadingConverter : AbstractCryptoReadingConverter<EncryptedFile?>() {
    override fun byteArrayToEntityAttribute(dbData: ByteArray?): EncryptedFile? {
        return dbData?.let { EncryptedFile(it) }
    }
}
