package no.nav.familie.ef.mottak.no.nav.familie.ef.mottak.repository

import no.nav.familie.ef.mottak.IntegrasjonSpringRunnerTest
import no.nav.familie.ef.mottak.no.nav.familie.ef.mottak.util.søknad
import no.nav.familie.ef.mottak.repository.SøknadRepository
import no.nav.familie.ef.mottak.repository.VedleggRepository
import no.nav.familie.ef.mottak.repository.domain.EncryptedFile
import no.nav.familie.ef.mottak.repository.domain.Vedlegg
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

internal class VedleggRepositoryTest : IntegrasjonSpringRunnerTest() {

    @Autowired lateinit var søknadRepository: SøknadRepository

    @Autowired lateinit var vedleggRepository: VedleggRepository

    @Test
    internal fun `findBySøknadId returnerer vedlegg`() {
        val søknadId = søknadRepository.insert(søknad()).id
        vedleggRepository.insert(Vedlegg(UUID.randomUUID(), søknadId, "navn", "tittel1", EncryptedFile(byteArrayOf(12))))

        assertThat(vedleggRepository.findBySøknadId(søknadId)).hasSize(1)
        assertThat(vedleggRepository.findBySøknadId("finnes ikke")).isEmpty()
    }

    @Test
    internal fun `findTitlerBySøknadId returnerer titler`() {
        val søknadId = søknadRepository.insert(søknad()).id
        vedleggRepository.insert(Vedlegg(UUID.randomUUID(), søknadId, "navn", "tittel1", EncryptedFile(byteArrayOf(12))))

        val vedlegg = vedleggRepository.finnTitlerForSøknadId(søknadId)

        assertThat(vedlegg).hasSize(1)
    }
}
