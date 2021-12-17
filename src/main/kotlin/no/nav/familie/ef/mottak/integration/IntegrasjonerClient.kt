package no.nav.familie.ef.mottak.integration


import no.nav.familie.ef.mottak.config.IntegrasjonerConfig
import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Ressurs.Status
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostRequest
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostResponse
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.infotrygdsak.FinnInfotrygdSakerRequest
import no.nav.familie.kontrakter.felles.infotrygdsak.InfotrygdSak
import no.nav.familie.kontrakter.felles.infotrygdsak.OpprettInfotrygdSakRequest
import no.nav.familie.kontrakter.felles.infotrygdsak.OpprettInfotrygdSakResponse
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnMappeRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnMappeResponseDto
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.personopplysning.FinnPersonidenterResponse
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.PersonIdentMedHistorikk
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI


@Service
class IntegrasjonerClient(@Qualifier("restTemplateAzure") operations: RestOperations,
                          private val integrasjonerConfig: IntegrasjonerConfig) :
        AbstractPingableRestClient(operations, "Arkiv") {

    override val pingUri: URI = UriComponentsBuilder.fromUri(integrasjonerConfig.url).pathSegment("ping").build().toUri()

    private val sendInnUri = UriComponentsBuilder.fromUri(integrasjonerConfig.url).pathSegment(PATH_SEND_INN).build().toUri()
    private val opprettOppgaveUri =
            UriComponentsBuilder.fromUri(integrasjonerConfig.url).pathSegment(PATH_OPPRETT_OPPGAVE).build().toUri()

    private val aktørUri =
            UriComponentsBuilder.fromUri(integrasjonerConfig.url).pathSegment(PATH_AKTØR).build().toUri()

    private val identFraAktørUri =
            UriComponentsBuilder.fromUri(integrasjonerConfig.url).pathSegment(PATH_IDENT_FRA_AKTØRID).build().toUri()

    private val behandlendeEnhetUri =
            UriComponentsBuilder.fromUri(integrasjonerConfig.url).pathSegment(PATH_BEHANDLENDE_ENHET).build().toUri()

    private val behandlendeEnhetMedRelasjonerUri =
            UriComponentsBuilder.fromUri(integrasjonerConfig.url).pathSegment(PATH_BEHANDLENDE_ENHET_MED_RELASJONER).build().toUri()

    private val journalpostsøkUri =
            UriComponentsBuilder.fromUri(integrasjonerConfig.url).pathSegment(PATH_JOURNALPOST).build().toUri()

    private val infotrygdsakUri =
            UriComponentsBuilder.fromUri(integrasjonerConfig.url).pathSegment(PATH_INFOTRYGDSAK).build().toUri()

    private val finnOppgaveUri =
            UriComponentsBuilder.fromUri(integrasjonerConfig.url).pathSegment(PATH_FINN_OPPGAVE).build().toUri()

    private val ferdigstillOppgaveURI =
            UriComponentsBuilder.fromUri(integrasjonerConfig.url).pathSegment(PATH_FERDIGSTILL_OPPGAVE).build().toUri()

    private val hentIdenterURI =
            UriComponentsBuilder.fromUri(integrasjonerConfig.url).pathSegment(PATH_HENT_IDENTER).build().toUri()

    private fun hentOppgaveUri(oppgaveId: Long) =
            UriComponentsBuilder.fromUri(integrasjonerConfig.url)
                    .pathSegment(PATH_HENT_OPPGAVE, oppgaveId.toString())
                    .build()
                    .toUri()

    private fun patchOppgaveUri(oppgaveId: Long) =
            UriComponentsBuilder.fromUri(integrasjonerConfig.url)
                    .pathSegment(PATH_HENT_OPPGAVE, oppgaveId.toString(), "oppdater")
                    .build()
                    .toUri()

    private fun journalpostUri(journalpostId: String) =
            UriComponentsBuilder.fromUri(integrasjonerConfig.url)
                    .pathSegment(PATH_JOURNALPOST)
                    .queryParam("journalpostId", journalpostId)
                    .build()
                    .toUri()

    private fun ferdigstillJournalpostUri(journalpostId: String, journalfoerendeEnhet: String) =
            UriComponentsBuilder.fromUri(integrasjonerConfig.url)
                    .pathSegment("arkiv", "v2", journalpostId, "ferdigstill")
                    .queryParam("journalfoerendeEnhet", journalfoerendeEnhet)
                    .build()
                    .toUri()

    private fun oppdaterJournalpostUri(journalpostId: String) =
            UriComponentsBuilder.fromUri(integrasjonerConfig.url)
                    .pathSegment("arkiv", "v2", journalpostId)
                    .build()
                    .toUri()

    @Retryable(value = [RuntimeException::class], maxAttempts = 3, backoff = Backoff(delay = 5000))
    fun hentJournalpost(journalpostId: String): Journalpost {
        val uri = journalpostUri(journalpostId)
        return getForEntity<Ressurs<Journalpost>>(uri).getDataOrThrow()
    }

    fun finnJournalposter(journalposterForBrukerRequest: JournalposterForBrukerRequest): List<Journalpost> {
        return postForEntity<Ressurs<List<Journalpost>>>(journalpostsøkUri, journalposterForBrukerRequest).getDataOrThrow()
    }

    fun opprettInfotrygdsak(opprettInfotrygdSakRequest: OpprettInfotrygdSakRequest): OpprettInfotrygdSakResponse {
        return postForEntity<Ressurs<OpprettInfotrygdSakResponse>>(lagOpprettInfotrygdsakUri,
                                                                   opprettInfotrygdSakRequest).getDataOrThrow()
    }

    fun finnBehandlendeEnhet(fnr: String): List<Enhet> {
        return postForEntity<Ressurs<List<Enhet>>>(behandlendeEnhetUri, PersonIdent(fnr)).getDataOrThrow()
    }

    /**
     * Returnerer en liste med 0 eller 1 element - bruker beste match fra norg2
     */
    fun finnBehandlendeEnhetForPersonMedRelasjoner(fnr: String): List<Enhet> {
        return postForEntity<Ressurs<List<Enhet>>>(behandlendeEnhetMedRelasjonerUri, PersonIdent(fnr)).getDataOrThrow()
    }

    fun finnOppgaver(journalpostId: String, oppgavetype: Oppgavetype?): FinnOppgaveResponseDto {
        val finnOppgaveRequest = FinnOppgaveRequest(tema = Tema.ENF,
                                                    journalpostId = journalpostId,
                                                    oppgavetype = oppgavetype)
        return postForEntity<Ressurs<FinnOppgaveResponseDto>>(finnOppgaveUri, finnOppgaveRequest).getDataOrThrow()
    }


    fun arkiver(arkiverDokumentRequest: ArkiverDokumentRequest): ArkiverDokumentResponse {
        val response =
                postForEntity<Ressurs<ArkiverDokumentResponse>>(sendInnUri, arkiverDokumentRequest)
        return response.getDataOrThrow()
    }

    fun ferdigstillJournalpost(journalpostId: String, journalførendeEnhet: String): HashMap<String, String> {
        val response =
                putForEntity<Ressurs<HashMap<String, String>>>(ferdigstillJournalpostUri(journalpostId, journalførendeEnhet),
                                                               "ENF")
        return response.getDataOrThrow()
    }

    fun oppdaterJournalpost(oppdaterJournalpostRequest: OppdaterJournalpostRequest,
                            journalpostId: String): OppdaterJournalpostResponse {
        return putForEntity<Ressurs<OppdaterJournalpostResponse>>(oppdaterJournalpostUri(journalpostId),
                                                                  oppdaterJournalpostRequest).getDataOrThrow()
    }


    fun hentOppgave(oppgaveId: Long): Oppgave {
        return getForEntity<Ressurs<Oppgave>>(hentOppgaveUri(oppgaveId)).getDataOrThrow()
    }

    fun oppdaterOppgave(oppgaveId: Long, oppgave: Oppgave): Long {
        val response = patchForEntity<Ressurs<OppgaveResponse>>(patchOppgaveUri(oppgaveId), oppgave).getDataOrThrow()
        return response.oppgaveId
    }

    fun finnMappe(finnMappeRequest: FinnMappeRequest): FinnMappeResponseDto {
        val mapperespons = getForEntity<Ressurs<FinnMappeResponseDto>>(finnMappeUri(finnMappeRequest))
        return mapperespons.getDataOrThrow()
    }

    private fun finnMappeUri(finnMappeRequest: FinnMappeRequest): URI {
        return UriComponentsBuilder.fromUri(integrasjonerConfig.url)
                .pathSegment(PATH_FINN_MAPPE)
                .queryParams(finnMappeRequest.toQueryParams())
                .build()
                .toUri()
    }

    fun lagOppgave(opprettOppgaveRequest: OpprettOppgaveRequest): OppgaveResponse {
        val response =
                postForEntity<Ressurs<OppgaveResponse>>(opprettOppgaveUri, opprettOppgaveRequest)
        return response.getDataOrThrow()
    }

    fun ferdigstillOppgave(oppgaveId: Long): OppgaveResponse {
        val respons = patchForEntity<Ressurs<OppgaveResponse>>(lagFerdigstillOppgaveUri(oppgaveId), "")
        return respons.getDataOrThrow()
    }

    fun hentSaksnummer(journalPostId: String): String {
        val response = getForEntity<Ressurs<Map<*, *>>>(lagHentSaksnummerUri(journalPostId))
        return response.getDataOrThrow()["saksnummer"].toString()
    }

    fun hentAktørId(personident: String): String {
        val response = postForEntity<Ressurs<MutableMap<*, *>>>(aktørUri, Ident(personident))
        return response.getDataOrThrow()["aktørId"].toString()
    }

    fun hentIdentForAktørId(aktørId: String): String {
        val response = postForEntity<Ressurs<MutableMap<*, *>>>(identFraAktørUri, aktørId)
        return response.getDataOrThrow()["personIdent"].toString()
    }

    fun finnInfotrygdSaksnummerForSak(saksnummer: String, fagområdeEnsligForsørger: String, fnr: String): String {
        val request = FinnInfotrygdSakerRequest(fnr = fnr, fagomrade = fagområdeEnsligForsørger)
        val infotrygdSaker = postForEntity<Ressurs<List<InfotrygdSak>>>(lagFinnInfotrygdSakerUri, request).getDataOrThrow()

        return infotrygdSaker.find { it.saksnr.trim() == saksnummer }?.let {
            it.registrertNavEnhetId + saksnummer
        } ?: error("Kunne ikke finne infotrygdsaker med saksnr=$saksnummer")

    }

    fun hentIdenter(personident: String, medHistprikk: Boolean): List<PersonIdentMedHistorikk> {
        val uri = UriComponentsBuilder.fromUri(hentIdenterURI).queryParam("historikk", medHistprikk).build().toUri()
        val response = postForEntity<Ressurs<FinnPersonidenterResponse>>(uri, PersonIdent(personident))
        return response.getDataOrThrow().identer
    }

    private val lagFinnInfotrygdSakerUri =
            UriComponentsBuilder.fromUri(infotrygdsakUri).pathSegment("soek").build().toUri()

    private val lagOpprettInfotrygdsakUri =
            UriComponentsBuilder.fromUri(infotrygdsakUri).pathSegment("opprett").build().toUri()

    private fun lagFerdigstillOppgaveUri(oppgaveId: Long): URI {
        return UriComponentsBuilder
                .fromUri(ferdigstillOppgaveURI)
                .pathSegment(oppgaveId.toString())
                .pathSegment("ferdigstill")
                .build()
                .toUri()
    }

    private fun lagHentSaksnummerUri(id: String): URI {
        return UriComponentsBuilder
                .fromUri(integrasjonerConfig.url)
                .path(PATH_HENT_SAKSNUMMER)
                .queryParam("journalpostId", id)
                .build()
                .toUri()
    }

    fun <T> Ressurs<T>.getDataOrThrow(): T {
        return when (this.status) {
            Status.SUKSESS -> data ?: error("Data er null i Ressurs")
            else -> error(melding)
        }
    }

    companion object {

        const val PATH_SEND_INN = "arkiv/v4"
        const val PATH_HENT_SAKSNUMMER = "journalpost/sak"
        const val PATH_OPPRETT_OPPGAVE = "oppgave/opprett"
        const val PATH_FINN_OPPGAVE = "oppgave/v4"
        const val PATH_FINN_MAPPE = "oppgave/mappe/sok"
        const val PATH_HENT_OPPGAVE = "oppgave"
        const val PATH_FERDIGSTILL_OPPGAVE = "oppgave"
        const val PATH_AKTØR = "aktoer/v2/ENF"
        const val PATH_IDENT_FRA_AKTØRID = "aktoer/v2/fraaktorid/ENF"
        const val PATH_JOURNALPOST = "journalpost"
        const val PATH_BEHANDLENDE_ENHET = "arbeidsfordeling/enhet/ENF"
        const val PATH_BEHANDLENDE_ENHET_MED_RELASJONER = "arbeidsfordeling/enhet/ENF/med-relasjoner"
        const val PATH_INFOTRYGDSAK = "infotrygdsak"
        const val PATH_HENT_IDENTER = "personopplysning/v1/identer/ENF"
    }

}
