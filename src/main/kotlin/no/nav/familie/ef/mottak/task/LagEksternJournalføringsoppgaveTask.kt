package no.nav.familie.ef.mottak.task

import no.nav.familie.ef.mottak.repository.EttersendingRepository
import no.nav.familie.ef.mottak.repository.SøknadRepository
import no.nav.familie.ef.mottak.service.OppgaveService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = LagEksternJournalføringsoppgaveTask.TYPE,
    beskrivelse = "Lager oppgave i GoSys"
)
class LagEksternJournalføringsoppgaveTask(
    private val ettersendingRepository: EttersendingRepository,
    private val oppgaveService: OppgaveService,
    private val søknadRepository: SøknadRepository
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val journalpostId = task.payload

        // Ved helt spesielle race conditions kan man tenke seg at vi fikk opprettet
        // denne (LagEksternJournalføringsoppgaveTask) før søknaden fikk en journalpostId
        if (finnesIkkeSøknadMedJournalpostId(journalpostId) && finnesIkkeEttersendingMedJournalpostId(journalpostId)) {
            oppgaveService.lagJournalføringsoppgaveForJournalpostId(journalpostId)
        }
    }

    private fun finnesIkkeSøknadMedJournalpostId(journalpostId: String) =
        søknadRepository.findByJournalpostId(journalpostId) == null

    private fun finnesIkkeEttersendingMedJournalpostId(journalpostId: String) =
        ettersendingRepository.findByJournalpostId(journalpostId) == null

    companion object {

        const val TYPE = "lagEksternJournalføringsoppgave"
    }
}
