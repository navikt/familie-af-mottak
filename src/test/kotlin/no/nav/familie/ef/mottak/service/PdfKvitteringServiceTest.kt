package no.nav.familie.ef.mottak.no.nav.familie.ef.mottak.service

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.mottak.config.DOKUMENTTYPE_OVERGANGSSTØNAD
import no.nav.familie.ef.mottak.encryption.EncryptedString
import no.nav.familie.ef.mottak.integration.PdfKvitteringClient
import no.nav.familie.ef.mottak.repository.SøknadRepository
import no.nav.familie.ef.mottak.repository.VedleggRepository
import no.nav.familie.ef.mottak.repository.domain.EncryptedFile
import no.nav.familie.ef.mottak.repository.domain.Søknad
import no.nav.familie.ef.mottak.service.PdfKvitteringService
import no.nav.familie.ef.mottak.service.Testdata
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull

class PdfKvitteringServiceTest {
    private val søknadRepository: SøknadRepository = mockk()
    private val vedleggRepository: VedleggRepository = mockk()
    private val pdfKvitteringClient: PdfKvitteringClient = mockk()
    private val pdfKvitteringService: PdfKvitteringService = PdfKvitteringService(søknadRepository, vedleggRepository, pdfKvitteringClient)
    private val pdf = EncryptedFile("321".toByteArray())
    private val søknadOvergangsstønadId = "søknadOvergangsstønadId"
    private val søknadOvergangsstønad =
        Søknad(
            id = søknadOvergangsstønadId,
            søknadJson = createValidSøknadJson(Testdata.søknadOvergangsstønadNy),
            søknadPdf = null,
            fnr = "654",
            dokumenttype = DOKUMENTTYPE_OVERGANGSSTØNAD,
            journalpostId = null,
            saksnummer = null,
        )

    @BeforeEach
    fun setUp() {
        søknadsRepositoryVilReturnere(søknadOvergangsstønad)
        every {
            vedleggRepository.finnTitlerForSøknadId(any())
        } returns Testdata.vedlegg.map { it.tittel }
        pdfClientVilReturnere(pdf)
    }

    @Test
    fun `Søknad skal oppdateres med pdf når pdf genereres`() {
        // Given
        val slot = slot<Søknad>()
        capturePdfAddedToSøknad(slot)
        // When
        pdfKvitteringService.lagPdfKvittering(søknadOvergangsstønadId)
        // Then
        Assertions.assertThat(pdf).isEqualTo(slot.captured.søknadPdf)
    }

    private fun createValidSøknadJson(søknad: Any): EncryptedString = EncryptedString(objectMapper.writeValueAsString(søknad))

    private fun søknadsRepositoryVilReturnere(vararg søknad: Søknad) {
        søknad.forEach {
            every {
                søknadRepository.findByIdOrNull(it.id)
            } returns it
        }
    }

    private fun pdfClientVilReturnere(pdf: EncryptedFile) {
        every {
            pdfKvitteringClient.opprettPdf(any())
        } returns pdf.bytes
    }

    private fun capturePdfAddedToSøknad(slot: CapturingSlot<Søknad>) {
        every {
            søknadRepository.update(capture(slot))
        } answers {
            slot.captured
        }
    }
}
