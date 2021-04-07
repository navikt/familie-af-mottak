package no.nav.familie.ef.mottak.hendelse

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ef.mottak.featuretoggle.FeatureToggleService
import no.nav.familie.ef.mottak.integration.IntegrasjonerClient
import no.nav.familie.ef.mottak.repository.SoknadRepository
import no.nav.familie.ef.mottak.repository.TaskRepositoryUtvidet
import no.nav.familie.ef.mottak.service.JournalføringsoppgaveService
import no.nav.familie.ef.mottak.task.LagJournalføringsoppgaveTask
import no.nav.familie.prosessering.domene.Task
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class JournalhendelseService(
    val journalpostClient: IntegrasjonerClient,
    val featureToggleService: FeatureToggleService,
    val soknadRepository: SoknadRepository,
    val journalfoeringHendelseDbUtil: JournalfoeringHendelseDbUtil,
    val taskRepository: TaskRepositoryUtvidet,
    val journalføringsoppgaveService: JournalføringsoppgaveService
) {

    val logger: Logger = LoggerFactory.getLogger(JournalhendelseService::class.java)
    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")
    val alleredeBehandletJournalpostCounter: Counter =
        Metrics.counter("alene.med.barn.journalhendelse.alleredeBehandletJournalpostHendelse")


    @Transactional
    fun prosesserNyHendelse(hendelseRecord: JournalfoeringHendelseRecord, offset: Long) {

        secureLogger.info("Mottatt gyldig hendelse: $hendelseRecord")
        if (!journalfoeringHendelseDbUtil.erHendelseRegistrertIHendelseslogg(hendelseRecord)) {
            if (journalfoeringHendelseDbUtil.harIkkeOpprettetOppgaveForJournalpost(hendelseRecord)) {
                val journalpost = journalpostClient.hentJournalpost(hendelseRecord.journalpostId.toString())
                val lagBehandleSakOppgaveTask = Task(
                    type = LagJournalføringsoppgaveTask.TYPE,
                    payload = hendelseRecord.journalpostId.toString(),
                    metadata = journalpost.metadata()
                )
                taskRepository.save(lagBehandleSakOppgaveTask)
            } else {
                alleredeBehandletJournalpostCounter.increment()
                logger.warn("Skipper opprettelse av LagEksternJournalføringsoppgaveTask for journalpostId=${hendelseRecord.journalpostId} fordi den er utført tidligere")
            }
            journalfoeringHendelseDbUtil.lagreHendelseslogg(hendelseRecord, offset)
        }
    }
}
