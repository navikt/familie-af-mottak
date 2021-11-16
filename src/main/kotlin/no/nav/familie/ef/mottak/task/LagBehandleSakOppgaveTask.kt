package no.nav.familie.ef.mottak.task

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ef.mottak.integration.IntegrasjonerClient
import no.nav.familie.ef.mottak.integration.SaksbehandlingClient
import no.nav.familie.ef.mottak.mapper.BehandlesAvApplikasjon
import no.nav.familie.ef.mottak.repository.domain.Søknad
import no.nav.familie.ef.mottak.service.OppgaveService
import no.nav.familie.ef.mottak.service.SakService
import no.nav.familie.ef.mottak.service.SøknadService
import no.nav.familie.ef.mottak.util.dokumenttypeTilStønadType
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = LagBehandleSakOppgaveTask.TYPE,
                     beskrivelse = "Lager behandle sak oppgave i GoSys")
class LagBehandleSakOppgaveTask(private val oppgaveService: OppgaveService,
                                private val søknadService: SøknadService,
                                private val integrasjonerClient: IntegrasjonerClient,
                                private val sakService: SakService,
                                private val saksbehandlingClient: SaksbehandlingClient,
                                private val taskRepository: TaskRepository) : AsyncTaskStep {

    val antallJournalposterAutomatiskBehandlet: Counter = Metrics.counter("alene.med.barn.journalposter.automatisk.behandlet")
    val antallJournalposterManueltBehandlet: Counter = Metrics.counter("alene.med.barn.journalposter.manuelt.behandlet")

    override fun doTask(task: Task) {
        val søknad: Søknad = søknadService.get(task.payload)
        val stønadType = dokumenttypeTilStønadType(søknad.dokumenttype) ?: error("...")  //TODO
        val journalpostId: String = søknad.journalpostId ?: error("Søknad mangler journalpostId")
        val journalpost = integrasjonerClient.hentJournalpost(journalpostId)

        if (skalAutomatiskJournalføresMotInfotrygd(søknad, stønadType)) {
            val lagBehandleSakOppgave = oppgaveService.lagBehandleSakOppgave(journalpost, BehandlesAvApplikasjon.INFOTRYGD)
            task.metadata.apply {
                this[behandleSakOppgaveIdKey] = lagBehandleSakOppgave.toString()
            }
            taskRepository.save(task)
        }
    }

    private fun skalAutomatiskJournalføresMotInfotrygd(søknad: Søknad,
                                                       stønadType: StønadType) =
            sakService.finnesIkkeIInfotrygd(søknad)
            && !søknad.behandleINySaksbehandling
            && !saksbehandlingClient.finnesBehandlingForPerson(søknad.fnr, stønadType)

    override fun onCompletion(task: Task) {

        val nesteTask = if (task.metadata[behandleSakOppgaveIdKey] != null) {
            antallJournalposterAutomatiskBehandlet.increment()
            Task(TaskType(TYPE).nesteHovedflytTask(), task.payload, task.metadata)
        } else {
            antallJournalposterManueltBehandlet.increment()
            Task(TaskType(TYPE).nesteFallbackTask(), task.payload, task.metadata)
        }

        taskRepository.save(nesteTask)
    }

    companion object {

        const val behandleSakOppgaveIdKey = "behandleSakOppgaveId"
        const val TYPE = "lagBehandleSakOppgave"
    }
}
