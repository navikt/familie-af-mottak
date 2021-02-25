package no.nav.familie.ef.mottak.hendelse

import no.nav.familie.ef.mottak.featuretoggle.FeatureToggleService
import no.nav.familie.ef.mottak.integration.IntegrasjonerClient
import no.nav.familie.ef.mottak.repository.SoknadRepository
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class JournalhendelseService(val journalpostClient: IntegrasjonerClient,
                             val featureToggleService: FeatureToggleService,
                             val soknadRepository: SoknadRepository,
                             val journalfoeringHendelseDbUtil: JournalfoeringHendelseDbUtil) {

    val logger: Logger = LoggerFactory.getLogger(JournalhendelseService::class.java)
    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    @Transactional
    fun prosesserNyHendelse(hendelseRecord: JournalfoeringHendelseRecord, offset: Long) {

        if (skalProsessereHendelse(hendelseRecord)) {
            secureLogger.info("Mottatt gyldig hendelse: $hendelseRecord")
            behandleJournalpost(hendelseRecord.journalpostId)
            journalfoeringHendelseDbUtil.lagreHendelseslogg(hendelseRecord, offset)
        }
    }

    fun behandleJournalpost(journalpostId: Long) {

        val journalpost = journalpostClient.hentJournalpost(journalpostId.toString())

        if (journalpost.skalBehandles()) {
            lagreSomEksternJournalføringsTaskDersomSøknadIkkeFinnes(journalpost)
        } else {
            logJournalpost(journalpost)
        }

        journalpost.incrementMetric()
    }

    private fun lagreSomEksternJournalføringsTaskDersomSøknadIkkeFinnes(journalpost: Journalpost) {
        if (featureToggleService.isEnabled("familie.ef.mottak.journalhendelse-behsak")) {
            when (val søknad = soknadRepository.findByJournalpostId(journalpost.journalpostId)) {
                null -> journalfoeringHendelseDbUtil.lagreEksternJournalføringsTask(journalpost)
                else -> logger.info("Hendelse mottatt for digital søknad ${søknad.id}")
            }
        } else {
            logger.info("Behandler ikke journalhendelse, feature familie.ef.mottak.journalhendelse-behsak er skrudd av i Unleash")
        }
    }

    private fun skalProsessereHendelse(hendelseRecord: JournalfoeringHendelseRecord): Boolean {
        return hendelseRecord.erTemaENF() && hendelseRecord.erHendelsetypeGyldig()
               && !journalfoeringHendelseDbUtil.erHendelseRegistrertIHendelseslogg(hendelseRecord)
    }

    private fun logJournalpost(journalpost: Journalpost) {
        when (journalpost.getJournalpostState()) {
            JournalpostState.IKKE_MOTTATT -> logger.info(journalpost.statusIkkeMottattLogString())
            JournalpostState.UGYLDIG_KANAL -> logger.error(journalpost.ikkeGyldigKanalLogString())
        }
    }
}
