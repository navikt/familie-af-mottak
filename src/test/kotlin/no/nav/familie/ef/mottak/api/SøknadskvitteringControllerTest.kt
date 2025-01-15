package no.nav.familie.ef.mottak.no.nav.familie.ef.mottak.api

import no.nav.familie.ef.mottak.IntegrasjonSpringRunnerTest
import no.nav.familie.ef.mottak.no.nav.familie.ef.mottak.util.søknad
import no.nav.familie.ef.mottak.repository.SøknadRepository
import no.nav.familie.ef.mottak.repository.domain.EncryptedFile
import no.nav.familie.ef.mottak.service.Testdata
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

internal class SøknadskvitteringControllerTest : IntegrasjonSpringRunnerTest() {
    @Autowired
    lateinit var søknadRepository: SøknadRepository

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(søkerBearerToken())
    }

    @Test
    internal fun `Skal returnere 200 OK for å hente søknad med id`() {
        val søknadPdfBytes = objectMapper.writeValueAsBytes(Testdata.søknadOvergangsstønadNy)
        val søknad =
            søknadRepository.insert(
                søknad(
                    søknadPdf = EncryptedFile(søknadPdfBytes),
                ),
            )
        val respons = hentSøknad(søknad.id)

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body).isNotNull()
    }

    private fun hentSøknad(søknadId: String): ResponseEntity<ByteArray> =
        restTemplate.exchange(
            localhost("/api/soknadskvittering/$søknadId"),
            HttpMethod.GET,
            HttpEntity<ByteArray>(headers),
        )
}
