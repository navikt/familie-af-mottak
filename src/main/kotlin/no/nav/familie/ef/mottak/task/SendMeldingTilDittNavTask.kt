package no.nav.familie.ef.mottak.task

import no.nav.familie.ef.mottak.config.DittNavConfig
import no.nav.familie.ef.mottak.service.DittNavKafkaProducer
import no.nav.familie.ef.mottak.service.SøknadService
import no.nav.familie.kontrakter.ef.søknad.Dokumentasjonsbehov
import no.nav.familie.kontrakter.ef.søknad.SøknadType
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = SendMeldingTilDittNavTask.SEND_MELDING_TIL_DITT_NAV,
                     beskrivelse = "Send melding til ditt nav")
class SendMeldingTilDittNavTask(
        private val producer: DittNavKafkaProducer,
        private val søknadService: SøknadService,
        private val dittNavConfig: DittNavConfig
) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(this::class.java)
    override fun doTask(task: Task) {


        val søknad = søknadService.get(task.payload)
        val søknadType = SøknadType.hentSøknadTypeForDokumenttype(søknad.dokumenttype)

        val melding = lagMelding(UUID.fromString(søknad.id), søknadType)

        val link = link(søknadType, task.payload)
        producer.sendToKafka(søknad.fnr,
                             melding,
                             task.payload,
                             task.id.toString(),
                             link)
        logger.info("Send melding til ditt nav søknadId=${task.payload}")
    }

    private fun link(søknadType: SøknadType,
                     søknadId: String): String? {
        return if (søknadType == SøknadType.OVERGANGSSTØNAD_ARBEIDSSØKER) {
            null
        } else {
            "${dittNavConfig.soknadfrontendUrl}/innsendtsoknad?soknad=$søknadId"
        }
    }

    private fun lagMelding(søknadId: UUID, søknadType: SøknadType): String {
        if (søknadType == SøknadType.OVERGANGSSTØNAD_ARBEIDSSØKER) {
            return "Vi har mottatt skjema enslig mor eller far som er arbeidssøker"
        }

        val søknadstekst = søknadstypeTekst(søknadType)
        val dokumentasjonsbehov = søknadService.hentDokumentasjonsbehovForSøknad(søknadId).dokumentasjonsbehov
        return when {
            dokumentasjonsbehov.isEmpty() -> "Vi har mottatt søknaden din om $søknadstekst."
            manglerVedlegg(dokumentasjonsbehov) ->
                "Det ser ut til at det mangler noen vedlegg til søknaden din om $søknadstekst." +
                " Se hva som mangler og last opp vedlegg."
            else -> "Vi har mottatt søknaden din om $søknadstekst. Se vedleggene du lastet opp."
        }
    }

    private fun manglerVedlegg(dokumentasjonsbehov: List<Dokumentasjonsbehov>) =
            dokumentasjonsbehov.any { !it.harSendtInn && it.opplastedeVedlegg.isEmpty() }

    private fun søknadstypeTekst(søknadType: SøknadType): String {
        return when (søknadType) {
            SøknadType.BARNETILSYN -> "stønad til barnetilsyn"
            SøknadType.OVERGANGSSTØNAD -> "overgangsstønad"
            SøknadType.SKOLEPENGER -> "stønad til skolepenger"
            else -> error("Kan mappe dokumenttype $søknadType til dittnav tekst")
        }
    }

    companion object {

        const val SEND_MELDING_TIL_DITT_NAV = "sendMeldingTilDittNav"
    }

}