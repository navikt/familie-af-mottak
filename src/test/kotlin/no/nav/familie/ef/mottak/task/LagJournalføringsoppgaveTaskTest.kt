package no.nav.familie.ef.mottak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.mottak.config.DOKUMENTTYPE_BARNETILSYN
import no.nav.familie.ef.mottak.config.DOKUMENTTYPE_OVERGANGSSTØNAD
import no.nav.familie.ef.mottak.config.DOKUMENTTYPE_SKJEMA_ARBEIDSSØKER
import no.nav.familie.ef.mottak.config.DOKUMENTTYPE_SKOLEPENGER
import no.nav.familie.ef.mottak.featuretoggle.FeatureToggleService
import no.nav.familie.ef.mottak.repository.SoknadRepository
import no.nav.familie.ef.mottak.repository.domain.Soknad
import no.nav.familie.ef.mottak.service.OppgaveService
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.util.*

internal class LagJournalføringsoppgaveTaskTest {

    private val taskRepository: TaskRepository = mockk()
    private val oppgaveService: OppgaveService = mockk(relaxed = true)
    private val featureToggleService: FeatureToggleService = mockk(relaxed = true)
    private val søknadRepository: SoknadRepository = mockk(relaxed = true)
    private val lagJournalføringsoppgaveTask: LagJournalføringsoppgaveTask =
            LagJournalføringsoppgaveTask(taskRepository, oppgaveService, søknadRepository, featureToggleService)

    @Test
    fun `Skal opprette SendMeldingTilDittNavTask og HentSaksnummerFraJoark når LagJournalføringsoppgaveTask er utført`() {

        val soknad = Soknad(id = UUID.randomUUID().toString(),
                            fnr = "12345678901",
                            søknadJson = "",
                            dokumenttype = DOKUMENTTYPE_OVERGANGSSTØNAD)
        every { søknadRepository.findByIdOrNull(any()) } returns soknad
        val slot = slot<List<Task>>()
        every {
            taskRepository.saveAll(capture(slot))
        } answers {
            slot.captured
        }

        lagJournalføringsoppgaveTask.onCompletion(Task(type = "", payload = "", properties = Properties()))

        assertEquals(HentSaksnummerFraJoarkTask.HENT_SAKSNUMMER_FRA_JOARK, slot.captured[0].type)
        assertEquals(SendDokumentasjonsbehovMeldingTilDittNavTask.SEND_MELDING_TIL_DITT_NAV, slot.captured[1].type)
    }

    @Test
    fun `Skal lage behandle-sak-oppgave-task hvis det er en ny søknad om overgangsstønad`() {
        val slot = slot<List<Task>>()
        val soknad = Soknad(id = UUID.randomUUID().toString(),
                            fnr = "12345678901",
                            søknadJson = "",
                            dokumenttype = DOKUMENTTYPE_OVERGANGSSTØNAD)
        every { taskRepository.saveAll(capture(slot)) } answers { slot.captured }
        every { featureToggleService.isEnabled(any()) } returns true
        every { søknadRepository.findByIdOrNull(any()) } returns soknad

        lagJournalføringsoppgaveTask.onCompletion(Task(type = "", payload = soknad.id, properties = Properties()))

        assertEquals(LagBehandleSakOppgaveTask.TYPE, slot.captured[0].type)
        assertEquals(SendDokumentasjonsbehovMeldingTilDittNavTask.SEND_MELDING_TIL_DITT_NAV, slot.captured[1].type)
    }

    @Test
    fun `Skal opprette behandle-sak-oppgave-task hvis det er en ny søknad om barnetilsyn`() {
        val slot = slot<List<Task>>()
        val soknad = Soknad(id = UUID.randomUUID().toString(),
                            fnr = "12345678901",
                            søknadJson = "",
                            dokumenttype = DOKUMENTTYPE_BARNETILSYN)
        every { taskRepository.saveAll(capture(slot)) } answers { slot.captured }
        every { featureToggleService.isEnabled(any()) } returns true
        every { søknadRepository.findByIdOrNull(any()) } returns soknad

        lagJournalføringsoppgaveTask.onCompletion(Task(type = "", payload = soknad.id, properties = Properties()))

        assertEquals(LagBehandleSakOppgaveTask.TYPE, slot.captured[0].type)
        assertEquals(SendDokumentasjonsbehovMeldingTilDittNavTask.SEND_MELDING_TIL_DITT_NAV, slot.captured[1].type)
    }

    @Test
    fun `Skal opprette behandle-sak-oppgave-task hvis det er en ny søknad om skolepenger`() {
        val slot = slot<List<Task>>()
        val soknad = Soknad(id = UUID.randomUUID().toString(),
                            fnr = "12345678901",
                            søknadJson = "",
                            dokumenttype = DOKUMENTTYPE_SKOLEPENGER)
        every { taskRepository.saveAll(capture(slot)) } answers { slot.captured }
        every { featureToggleService.isEnabled(any()) } returns true
        every { søknadRepository.findByIdOrNull(any()) } returns soknad

        lagJournalføringsoppgaveTask.onCompletion(Task(type = "", payload = soknad.id, properties = Properties()))

        assertEquals(LagBehandleSakOppgaveTask.TYPE, slot.captured[0].type)
        assertEquals(SendDokumentasjonsbehovMeldingTilDittNavTask.SEND_MELDING_TIL_DITT_NAV, slot.captured[1].type)
    }

    @Test
    fun `Skal ikke opprette sak hvis det er en nytt arbeidssøkerskjema`() {
        val slot = slot<List<Task>>()
        val soknad = Soknad(id = UUID.randomUUID().toString(),
                            fnr = "12345678901",
                            søknadJson = "",
                            dokumenttype = DOKUMENTTYPE_SKJEMA_ARBEIDSSØKER)
        every { taskRepository.saveAll(capture(slot)) } answers { slot.captured }
        every { featureToggleService.isEnabled(any()) } returns true
        every { søknadRepository.findByIdOrNull(any()) } returns soknad

        lagJournalføringsoppgaveTask.onCompletion(Task(type = "", payload = soknad.id, properties = Properties()))

        assertEquals(HentSaksnummerFraJoarkTask.HENT_SAKSNUMMER_FRA_JOARK, slot.captured[0].type)
        assertEquals(SendDokumentasjonsbehovMeldingTilDittNavTask.SEND_MELDING_TIL_DITT_NAV, slot.captured[1].type)
    }

    @Test
    fun `skal kalle lagJournalføringsoppgaveForSøknadId hvis task payload er gyldig uuid`() {
        val uuid = UUID.randomUUID().toString()

        lagJournalføringsoppgaveTask.doTask(Task(type = "", payload = uuid, properties = Properties()))

        verify { oppgaveService.lagJournalføringsoppgaveForSøknadId(uuid) }
    }


}
