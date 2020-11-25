package no.nav.familie.ef.mottak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.mottak.featuretoggle.FeatureToggleService
import no.nav.familie.ef.mottak.service.OppgaveService
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class LagJournalføringsoppgaveTaskTest {

    private val taskRepository: TaskRepository = mockk()
    private val oppgaveService: OppgaveService = mockk(relaxed = true)
    private val featureToggleService: FeatureToggleService = mockk(relaxed = true)
    private val lagJournalføringsoppgaveTask: LagJournalføringsoppgaveTask =
            LagJournalføringsoppgaveTask(taskRepository, oppgaveService, featureToggleService)

    @Test
    fun `Skal opprette SendMeldingTilDittNavTask og HentSaksnummerFraJoark når LagJournalføringsoppgaveTask er utført`() {
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
    fun `skal kalle lagJournalføringsoppgaveForJournalpostId hvis task payload ikke er gyldig uuid`() {
        lagJournalføringsoppgaveTask.doTask(Task(type = "", payload = "123", properties = Properties()))

        verify { oppgaveService.lagJournalføringsoppgaveForJournalpostId("123") }
    }

    @Test
    fun `skal kalle lagJournalføringsoppgaveForSøknadId hvis task payload er gyldig uuid`() {
        val uuid = UUID.randomUUID().toString()

        lagJournalføringsoppgaveTask.doTask(Task(type = "", payload = uuid, properties = Properties()))

        verify { oppgaveService.lagJournalføringsoppgaveForSøknadId(uuid) }
    }


}
