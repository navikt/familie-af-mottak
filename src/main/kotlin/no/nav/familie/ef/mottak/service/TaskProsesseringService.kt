package no.nav.familie.ef.mottak.service

import no.nav.familie.ef.mottak.repository.EttersendingRepository
import no.nav.familie.ef.mottak.repository.SøknadRepository
import no.nav.familie.ef.mottak.repository.domain.Ettersending
import no.nav.familie.ef.mottak.repository.domain.Søknad
import no.nav.familie.ef.mottak.task.LagEttersendingPdfTask
import no.nav.familie.ef.mottak.task.LagPdfTask
import no.nav.familie.ef.mottak.task.SendSøknadMottattTilDittNavTask
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Properties
import java.util.UUID

@Service
class TaskProsesseringService(private val taskRepository: TaskRepository,
                              private val søknadRepository: SøknadRepository,
                              private val ettersendingRepository: EttersendingRepository) {

    @Transactional
    fun startTaskProsessering(søknad: Søknad) {
        val properties =
                Properties().apply { this["søkersFødselsnummer"] = søknad.fnr }
                        .apply { this["dokumenttype"] = søknad.dokumenttype }
        taskRepository.save(Task(LagPdfTask.TYPE,
                                 søknad.id,
                                 properties))

        properties["eventId"] = UUID.randomUUID().toString()
        taskRepository.save(Task(SendSøknadMottattTilDittNavTask.TYPE,
                                 søknad.id,
                                 properties))
        søknadRepository.insert(søknad.copy(taskOpprettet = true))
    }

    @Transactional
    fun startTaskProsessering(ettersending: Ettersending) {
        val properties =
                Properties().apply {
                    this["søkersFødselsnummer"] = ettersending.fnr
                    this["stønadType"] = ettersending.stønadType
                }

        taskRepository.save(Task(LagEttersendingPdfTask.TYPE, ettersending.id.toString(), properties))
        ettersendingRepository.insert(ettersending.copy(taskOpprettet = true))
    }
}

