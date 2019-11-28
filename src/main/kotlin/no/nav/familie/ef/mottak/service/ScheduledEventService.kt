package no.nav.familie.ef.mottak.service

import no.nav.familie.ef.mottak.repository.SoknadRepository
import no.nav.familie.ef.mottak.repository.domain.Soknad
import no.nav.familie.ef.mottak.task.HentJournalpostIdFraJoarkTask
import no.nav.familie.ef.mottak.task.JournalførSøknadTask
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ScheduledEventService(private val taskRepository: TaskRepository,
                            private val soknadRepository: SoknadRepository) {

    @Scheduled(initialDelay = 10000, fixedDelay = 60000)
    fun opprettTaskForSøknad() {
        soknadRepository.finnAlleSøknaderUtenTask().forEach {
            opprettTask(it)
        }
    }

    fun opprettTask(soknad: Soknad) {
        val taskType = if (soknad.nySaksbehandling) {
            JournalførSøknadTask.JOURNALFØR_SØKNAD
        } else {
            HentJournalpostIdFraJoarkTask.HENT_JOURNALPOSTID_FRA_JOARK
        }

        val nyTask = Task.nyTask(taskType, soknad.id.toString())
        taskRepository.save(nyTask)

    }

}
