package no.nav.familie.ef.mottak.task

import no.nav.familie.ef.mottak.service.OppgaveService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = LagJournalføringsoppgaveForEttersendingTask.TYPE,
                     beskrivelse = "Lager oppgave i GoSys")
class LagJournalføringsoppgaveForEttersendingTask(private val oppgaveService: OppgaveService,
                                                  private val taskRepository: TaskRepository) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val oppgaveId = oppgaveService.lagJournalføringsoppgaveForEttersendingId(task.payload)
        oppgaveId?.let {
            task.metadata.apply {
                this[LagJournalføringsoppgaveTask.journalføringOppgaveIdKey] = it.toString()
            }

        }
        taskRepository.save(task)
    }

    override fun onCompletion(task: Task) {
        task.metadata[LagJournalføringsoppgaveTask.journalføringOppgaveIdKey]?.let {
            taskRepository.save(Task(TaskType(TYPE).nesteFallbackTask(),
                                     task.payload,
                                     task.metadata))
        }
    }


    companion object {

        const val TYPE = "lagJournalføringsoppgaveForEttersending"
    }
}
