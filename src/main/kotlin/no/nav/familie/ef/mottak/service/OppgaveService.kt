package no.nav.familie.ef.mottak.service

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.mottak.featuretoggle.FeatureToggleService
import no.nav.familie.ef.mottak.integration.IntegrasjonerClient
import no.nav.familie.ef.mottak.integration.SaksbehandlingClient
import no.nav.familie.ef.mottak.mapper.BehandlesAvApplikasjon
import no.nav.familie.ef.mottak.mapper.OpprettOppgaveMapper
import no.nav.familie.ef.mottak.repository.domain.Ettersending
import no.nav.familie.ef.mottak.repository.domain.Søknad
import no.nav.familie.ef.mottak.util.dokumenttypeTilStønadType
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException

@Service
class OppgaveService(private val integrasjonerClient: IntegrasjonerClient,
                     private val featureToggleService: FeatureToggleService,
                     private val søknadService: SøknadService,
                     private val ettersendingService: EttersendingService,
                     private val opprettOppgaveMapper: OpprettOppgaveMapper,
                     private val sakService: SakService,
                     private val saksbehandlingClient: SaksbehandlingClient) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)
    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")
    private val ENHETSNUMMER_NAY: String = "4489"

    fun lagJournalføringsoppgaveForSøknadId(søknadId: String): Long? {
        val søknad: Søknad = søknadService.get(søknadId)
        val journalpostId: String = søknad.journalpostId ?: error("Søknad mangler journalpostId")
        val journalpost = integrasjonerClient.hentJournalpost(journalpostId)
        val behandlesAvApplikasjon = utledBehandlesAvApplikasjon(søknad)
        val tilordnet: String? =
                if (skalSetteTilordnet(behandlesAvApplikasjon)) finnSaksbehandlerIdentForMiljø() else null
        return lagJournalføringsoppgave(journalpost, behandlesAvApplikasjon, tilordnet)
    }

    fun lagJournalføringsoppgaveForEttersendingId(ettersendingId: String): Long? {
        val ettersending: Ettersending = ettersendingService.hentEttersending(ettersendingId)
        val journalpostId: String = ettersending.journalpostId ?: error("Ettersending mangler journalpostId")
        val journalpost = integrasjonerClient.hentJournalpost(journalpostId)
        val stønadType = StønadType.valueOf(ettersending.stønadType)
        val behandlesAvApplikasjon = utledBehandlesAvApplikasjonForEttersending(fnr = ettersending.fnr, stønadType = stønadType)
        return lagJournalføringsoppgave(journalpost, behandlesAvApplikasjon)
    }

    /**
     * Då vi ikke er sikre på at stønadstypen er riktig eller eksisterer på oppgaven så sjekker vi om den finnes i ny løsning
     * Hvis den finnes setter vi att den må sjekkes opp før man behandler den
     */
    fun lagJournalføringsoppgaveForJournalpostId(journalpostId: String): Long? {
        val journalpost = integrasjonerClient.hentJournalpost(journalpostId)
        val finnesBehandlingForPerson = finnesBehandlingForPerson(journalpost)
        try {
            log.info("journalPost=$journalpostId finnesBehandlingForPerson=$finnesBehandlingForPerson")
            val behandlesAvApplikasjon =
                    if (finnesBehandlingForPerson) BehandlesAvApplikasjon.UAVKLART else BehandlesAvApplikasjon.INFOTRYGD
            return lagJournalføringsoppgave(journalpost, behandlesAvApplikasjon)
        } catch (e: Exception) {
            secureLogger.warn("Kunne ikke opprette journalføringsoppgave for journalpost=$journalpost", e)
            throw e
        }
    }

    private fun finnesBehandlingForPerson(journalpost: Journalpost): Boolean {
        val personIdent = finnPersonIdent(journalpost) ?: return false
        return saksbehandlingClient.finnesBehandlingForPerson(personIdent)
    }

    fun lagBehandleSakOppgave(journalpost: Journalpost, behandlesAvApplikasjon: BehandlesAvApplikasjon): Long {
        val opprettOppgave = opprettOppgaveMapper.toBehandleSakOppgave(journalpost, behandlesAvApplikasjon)
        return opprettOppgaveMedEnhetFraNorgEllerBrukNayHvisEnhetIkkeFinnes(opprettOppgave, journalpost)
    }

    fun settSaksnummerPåInfotrygdOppgave(oppgaveId: Long,
                                         saksblokk: String,
                                         saksnummer: String): Long {
        val oppgave: Oppgave = integrasjonerClient.hentOppgave(oppgaveId)
        val oppdatertOppgave = oppgave.copy(
                saksreferanse = saksnummer,
                beskrivelse = "${oppgave.beskrivelse} - Saksblokk: $saksblokk, Saksnummer: $saksnummer [Automatisk journalført]",
                behandlesAvApplikasjon = BehandlesAvApplikasjon.EF_SAK_BLANKETT.applikasjon,
        )
        return integrasjonerClient.oppdaterOppgave(oppgaveId, oppdatertOppgave)
    }

    fun lagJournalføringsoppgave(journalpost: Journalpost,
                                 behandlesAvApplikasjon: BehandlesAvApplikasjon,
                                 tilordnet: String? = null): Long? {

        if (journalpost.journalstatus == Journalstatus.MOTTATT) {
            return when {
                journalføringsoppgaveFinnes(journalpost) -> {
                    loggSkipOpprettOppgave(journalpost.journalpostId, Oppgavetype.Journalføring)
                    null
                }
                fordelingsoppgaveFinnes(journalpost) -> {
                    loggSkipOpprettOppgave(journalpost.journalpostId, Oppgavetype.Fordeling)
                    null
                }
                behandlesakOppgaveFinnes(journalpost) -> {
                    loggSkipOpprettOppgave(journalpost.journalpostId, Oppgavetype.BehandleSak)
                    null
                }
                else -> {
                    val opprettOppgave =
                            opprettOppgaveMapper.toJournalføringsoppgave(journalpost, behandlesAvApplikasjon, tilordnet)
                    return opprettOppgaveMedEnhetFraNorgEllerBrukNayHvisEnhetIkkeFinnes(opprettOppgave, journalpost)
                }
            }
        } else {
            val error = IllegalStateException("Journalpost ${journalpost.journalpostId} har endret status " +
                                              "fra MOTTATT til ${journalpost.journalstatus.name}")
            log.info("OpprettJournalføringOppgaveTask feilet.", error)
            throw error
        }
    }

    private fun finnSaksbehandlerIdentForMiljø(): String {
        return if (System.getenv("NAIS_CLUSTER_NAME") == "dev-fss") {
            log.info("Setter tilordnet på Journalføringsoppgave til Z994119")
            "Z994119"
        } else {
            log.info("Setter tilordnet på Journalføringsoppgave til S135150")
            "S135150"
        }
    }

    private fun skalSetteTilordnet(behandlesAvApplikasjon: BehandlesAvApplikasjon): Boolean {

        return behandlesAvApplikasjon == BehandlesAvApplikasjon.EF_SAK_INFOTRYGD && featureToggleService.isEnabled("familie.ef.mottak.er-aktuell-for-forste-sak")

    }

    private fun finnPersonIdent(journalpost: Journalpost): String? {
        return journalpost.bruker?.let {
            when (it.type) {
                BrukerIdType.FNR -> it.id
                BrukerIdType.AKTOERID -> integrasjonerClient.hentIdentForAktørId(it.id)
                BrukerIdType.ORGNR -> error("Kan ikke hente journalpost=${journalpost.journalpostId} for orgnr")
            }
        }
    }

    private fun opprettOppgaveMedEnhetFraNorgEllerBrukNayHvisEnhetIkkeFinnes(opprettOppgave: OpprettOppgaveRequest,
                                                                             journalpost: Journalpost): Long {

        return try {
            val nyOppgave = integrasjonerClient.lagOppgave(opprettOppgave)
            log.info("Oppretter ny ${opprettOppgave.oppgavetype} med oppgaveId=${nyOppgave.oppgaveId} for journalpost journalpostId=${journalpost.journalpostId}")
            nyOppgave.oppgaveId
        } catch (httpStatusCodeException: HttpStatusCodeException) {
            if (finnerIngenGyldigArbeidsfordelingsenhetForBruker(httpStatusCodeException)) {
                val nyOppgave = integrasjonerClient.lagOppgave(opprettOppgave.copy(enhetsnummer = ENHETSNUMMER_NAY))
                log.info("Oppretter ny ${opprettOppgave.oppgavetype} med oppgaveId=${nyOppgave.oppgaveId} for journalpost journalpostId=${journalpost.journalpostId} med enhetsnummer=$ENHETSNUMMER_NAY")
                nyOppgave.oppgaveId
            } else {
                throw httpStatusCodeException
            }
        }
    }

    private fun finnerIngenGyldigArbeidsfordelingsenhetForBruker(httpStatusCodeException: HttpStatusCodeException): Boolean {
        try {
            val response: Ressurs<OppgaveResponse> = objectMapper.readValue(httpStatusCodeException.responseBodyAsString)
            val feilmelding = response.melding
            secureLogger.warn("Feil ved oppretting av oppgave $feilmelding")
            return feilmelding.contains("Fant ingen gyldig arbeidsfordeling for oppgaven")
        } catch (e: Exception) {
            secureLogger.error("Feilet ved parsing av feilstatus", e)
            throw httpStatusCodeException
        }

    }

    private fun loggSkipOpprettOppgave(journalpostId: String, oppgavetype: Oppgavetype) {
        log.info("Skipper oppretting av journalførings-oppgave. " +
                 "Fant åpen oppgave av type ${oppgavetype} for " +
                 "journalpostId=${journalpostId}")
    }

    private fun fordelingsoppgaveFinnes(journalpost: Journalpost) =
            integrasjonerClient.finnOppgaver(journalpost.journalpostId, Oppgavetype.Fordeling).antallTreffTotalt > 0L

    private fun journalføringsoppgaveFinnes(journalpost: Journalpost) =
            integrasjonerClient.finnOppgaver(journalpost.journalpostId, Oppgavetype.Journalføring).antallTreffTotalt > 0L

    private fun behandlesakOppgaveFinnes(journalpost: Journalpost) =
            integrasjonerClient.finnOppgaver(journalpost.journalpostId, Oppgavetype.BehandleSak).antallTreffTotalt > 0L

    fun ferdigstillOppgaveForJournalpost(journalpostId: String) {
        val oppgaver = integrasjonerClient.finnOppgaver(journalpostId, Oppgavetype.Journalføring)
        when (oppgaver.antallTreffTotalt) {
            1L -> {
                val oppgaveId = oppgaver.oppgaver.first().id ?: error("Finner ikke oppgaveId for journalpost=$journalpostId")
                integrasjonerClient.ferdigstillOppgave(oppgaveId)
            }
            else -> {
                val error = IllegalStateException("Fant ${oppgaver.antallTreffTotalt} oppgaver for journalpost=$journalpostId")
                log.warn("Kan ikke ferdigstille oppgave", error)
                throw error
            }
        }
    }

    private fun utledBehandlesAvApplikasjon(søknad: Søknad): BehandlesAvApplikasjon {
        log.info("utledBehandlesAvApplikasjon dokumenttype=${søknad.dokumenttype}")
        val stønadType = dokumenttypeTilStønadType(søknad.dokumenttype) ?: return BehandlesAvApplikasjon.INFOTRYGD
        return if (finnesBehandlingINyLøsning(søknad.fnr, stønadType)) {
            BehandlesAvApplikasjon.EF_SAK
        } else if (søknad.behandleINySaksbehandling && sakService.kanOppretteInfotrygdSak(søknad)) {
            BehandlesAvApplikasjon.EF_SAK_INFOTRYGD
        } else {
            BehandlesAvApplikasjon.INFOTRYGD
        }
    }

    private fun utledBehandlesAvApplikasjonForEttersending(fnr: String, stønadType: StønadType): BehandlesAvApplikasjon {
        log.info("utledBehandlesAvApplikasjon stønadType=${stønadType}")
        return if (finnesBehandlingINyLøsning(fnr, stønadType)) {
            BehandlesAvApplikasjon.EF_SAK
        } else {
            BehandlesAvApplikasjon.INFOTRYGD
        }
    }

    private fun finnesBehandlingINyLøsning(fnr: String,
                                           stønadType: StønadType): Boolean {
        val finnesBehandlingForPerson = saksbehandlingClient.finnesBehandlingForPerson(fnr, stønadType)
        log.info("Sjekk om behandling finnes i ny løsning for personen - finnesBehandlingForPerson=$finnesBehandlingForPerson")
        return finnesBehandlingForPerson
    }

}
