package no.nav.familie.ef.mottak.integration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.mottak.config.IntegrasjonerConfig
import no.nav.familie.ef.mottak.no.nav.familie.ef.mottak.util.IOTestUtil
import no.nav.familie.http.client.RessursException
import no.nav.familie.http.sts.StsRestClient
import no.nav.familie.kontrakter.felles.Ressurs.Companion.failure
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.infotrygdsak.InfotrygdSak
import no.nav.familie.kontrakter.felles.infotrygdsak.OpprettInfotrygdSakRequest
import no.nav.familie.kontrakter.felles.infotrygdsak.OpprettInfotrygdSakResponse
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

internal class IntegrasjonerClientTest {

    private val wireMockServer = WireMockServer(wireMockConfig().dynamicPort())
    private val restOperations: RestOperations = RestTemplateBuilder().build()

    private lateinit var integrasjonerClient: IntegrasjonerClient
    private val arkiverSøknadRequest = ArkiverDokumentRequest("123456789", true, listOf())
    private val arkiverDokumentResponse: ArkiverDokumentResponse = ArkiverDokumentResponse("wer", true)

    @BeforeEach
    fun setUp() {
        wireMockServer.start()
        val stsRestClient = mockk<StsRestClient>()
        every { stsRestClient.systemOIDCToken } returns "token"
        integrasjonerClient = IntegrasjonerClient(restOperations, IntegrasjonerConfig(URI.create(wireMockServer.baseUrl())))
    }

    @AfterEach
    fun tearDown() {
        wireMockServer.resetAll()
        wireMockServer.stop()
    }

    @Test
    fun `Ved feil skal vi returnere riktig feilmelding i ressurs`() {
        val feilmelding = "Fant ingen gyldig arbeidsfordeling for oppgaven"
        wireMockServer.stubFor(
            post(urlEqualTo("/${IntegrasjonerClient.PATH_OPPRETT_OPPGAVE}"))
                .willReturn(serverError().withBody(IOTestUtil.readFile("opprett_oppgave_feilet.json")))
        )

        val exception = assertThrows<RessursException> {
            integrasjonerClient.lagOppgave(
                OpprettOppgaveRequest(
                    ident = OppgaveIdentV2("asd", IdentGruppe.AKTOERID),
                    saksId = null,
                    journalpostId = "123",
                    tema = Tema.ENF,
                    oppgavetype = Oppgavetype.Journalføring,
                    fristFerdigstillelse = LocalDate.now(),
                    beskrivelse = "",
                    behandlingstema = "sad",
                    enhetsnummer = null
                )
            )
        }

        assertThat(exception.message).contains(feilmelding)
    }

    @Test
    fun `opprettInfotrygdsak returnerer OpprettInfotrygdsakResponse`() {
        val opprettInfotrygdSakRequest = OpprettInfotrygdSakRequest(
            fagomrade = "ENF",
            fnr = "fnr",
            mottakerOrganisasjonsEnhetsId = "4408",
            mottattdato = LocalDate.of(2010, 11, 12),
            oppgaveId = "oppgaveId",
            oppgaveOrganisasjonsenhetId = "4408",
            opprettetAvOrganisasjonsEnhetsId = "4408",
            sendBekreftelsesbrev = false,
            stonadsklassifisering2 = "OG",
            type = "K"
        )
        val opprettInfotrygdSakResponse = OpprettInfotrygdSakResponse(saksId = "OG65")
        wireMockServer.stubFor(
            post(urlEqualTo("/${IntegrasjonerClient.PATH_INFOTRYGDSAK}/opprett"))
                .willReturn(okJson(objectMapper.writeValueAsString(success(opprettInfotrygdSakResponse))))
        )

        val testresultat = integrasjonerClient.opprettInfotrygdsak(opprettInfotrygdSakRequest)

        assertThat(testresultat).usingRecursiveComparison().isEqualTo(opprettInfotrygdSakResponse)
    }

    @Test
    fun `ferdigstillJournalpost sender melding om ferdigstilling parser payload og returnerer saksnummer`() {
        val journalpostId = "321"
        val journalførendeEnhet = "9999"
        val json = objectMapper.writeValueAsString(
            success(
                mapOf("journalpostId" to journalpostId),
                "Ferdigstilt journalpost $journalpostId"
            )
        )
        wireMockServer.stubFor(
            put(urlEqualTo("/arkiv/v2/$journalpostId/ferdigstill?journalfoerendeEnhet=$journalførendeEnhet"))
                .willReturn(okJson(json))
        )

        val testresultat = integrasjonerClient.ferdigstillJournalpost(journalpostId, journalførendeEnhet)

        assertThat(testresultat["journalpostId"]).isEqualTo(journalpostId)
    }

    @Test
    fun `hentSaksnummer parser payload og returnerer saksnummer`() {
        wireMockServer.stubFor(
            get(urlEqualTo("/${IntegrasjonerClient.PATH_HENT_SAKSNUMMER}?journalpostId=123"))
                .willReturn(okJson(readFile("saksnummer.json")))
        )

        assertThat(integrasjonerClient.hentSaksnummer("123")).isEqualTo("140258871")
    }

    @Test
    fun `Skal arkivere søknad`() {
        // Gitt
        wireMockServer.stubFor(
            post(urlEqualTo("/${IntegrasjonerClient.PATH_SEND_INN}"))
                .willReturn(okJson(success(arkiverDokumentResponse).toJson()))
        )
        // Vil gi resultat
        assertNotNull(integrasjonerClient.arkiver(arkiverSøknadRequest))
    }

    @Test
    fun `Skal ikke arkivere søknad`() {
        wireMockServer.stubFor(
            post(urlEqualTo("/${IntegrasjonerClient.PATH_SEND_INN}"))
                .willReturn(okJson(failure<Any>("error").toJson()))
        )

        assertFailsWith(IllegalStateException::class) {
            integrasjonerClient.arkiver(arkiverSøknadRequest)
        }
    }

    @Test
    fun `Skal finne infotrygdsaksnummer`() {
        // Gitt
        val fnr = "12345678901"
        val registrertNavEnhetId = "0304"
        val fagomrade = "ENF"
        val infotrygdSaker = listOf(
            InfotrygdSak(fnr, "A01", registrertNavEnhetId, fagomrade),
            InfotrygdSak(fnr, "A03", registrertNavEnhetId, fagomrade),
            InfotrygdSak(fnr, "A02", registrertNavEnhetId, fagomrade)
        )
        wireMockServer.stubFor(
            post(urlEqualTo("/${IntegrasjonerClient.PATH_INFOTRYGDSAK}/soek"))
                .willReturn(okJson(success(infotrygdSaker).toJson()))
        )
        // Vil gi resultat
        assertThat(integrasjonerClient.finnInfotrygdSaksnummerForSak("A01", fagomrade, fnr)).isEqualTo("0304A01")
        assertThat(integrasjonerClient.finnInfotrygdSaksnummerForSak("A02", fagomrade, fnr)).isEqualTo("0304A02")
        assertThat(integrasjonerClient.finnInfotrygdSaksnummerForSak("A03", fagomrade, fnr)).isEqualTo("0304A03")
        assertThrows<IllegalStateException> {
            integrasjonerClient.finnInfotrygdSaksnummerForSak("A04", fagomrade, fnr)
        }
    }

    @Test
    fun `Skal finne infotrygdsaksnummer med jsondata og unødige whitespaces`() {
        // Gitt
        val fnr = "04087420901"
        val fagomrade = "ENF"
        val infotrygdData = """
            {
                "data": [
                    {
                        "fnr": "04087420901",
                        "saksnr": "A01       ",
                        "registrertNavEnhetId": "0314",
                        "fagomrade": "EF"
                    }
                ],
                "status": "SUKSESS",
                "melding": "Innhenting av data var vellykket",
                "frontendFeilmelding": null,
                "stacktrace": null
            }
        """.trimIndent()
        wireMockServer.stubFor(
            post(urlEqualTo("/${IntegrasjonerClient.PATH_INFOTRYGDSAK}/soek"))
                .willReturn(okJson(infotrygdData))
        )
        // Vil gi resultat
        assertThat(integrasjonerClient.finnInfotrygdSaksnummerForSak("A01", fagomrade, fnr)).isEqualTo("0314A01")
        assertThrows<IllegalStateException> {
            integrasjonerClient.finnInfotrygdSaksnummerForSak("A04", fagomrade, fnr)
        }
    }

    @Test
    internal fun `skal hente aktørid med flat json`() {
        val mockResponse = objectMapper.writeValueAsString(success(mapOf("personIdent" to "123")))
        wireMockServer.stubFor(
            post(urlEqualTo("/${IntegrasjonerClient.PATH_IDENT_FRA_AKTØRID}"))
                .willReturn(okJson(mockResponse))
        )

        val personIdent = integrasjonerClient.hentIdentForAktørId("321")
        assertThat(personIdent).isEqualTo("123")
    }

    private fun readFile(filnavn: String): String {
        return this::class.java.getResource("/json/$filnavn").readText()
    }
}
