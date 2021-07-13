package no.nav.familie.ef.mottak.task

import no.nav.familie.ef.mottak.repository.EttersendingRepository
import no.nav.familie.ef.mottak.service.ArkiveringService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = ArkiverSøknadTask.TYPE, beskrivelse = "Arkiver søknad")
class ArkiverEttersendingTask(private val arkiveringService: ArkiveringService,
                        private val taskRepository: TaskRepository,
                        private val ettersendingRepository: EttersendingRepository) : AsyncTaskStep {

    val logger = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        logger.info("ArkiverEttersendingTask=[${task.id}] kjøres")

        val journalpostId = arkiveringService.journalførEttersending(task.payload)
        task.metadata.apply {
            this["journalpostId"] = journalpostId
        }
        taskRepository.save(task)
    }

    override fun onCompletion(task: Task) {
        logger.info("ArkiverEttersendingTask=[${task.id}] ferdig")
        val nesteTask = Task(TaskType(TYPE).nesteEttersendingsflytTask(), task.payload, task.metadata)

        /*val sendMeldingTilDittNavTask =
                Task(SendDokumentasjonsbehovMeldingTilDittNavTask.TYPE,
                     task.payload,
                     task.metadata)*/ //TODO: Må se om dette skal gjøres

        taskRepository.save(nesteTask)
    }

    companion object {
        const val TYPE = "arkiverEttersending"
    }

}