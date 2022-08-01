package no.nav.familie.ef.mottak.integration

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class SaksbehandlingClient(
    @Value("\${EF_SAK_URL}")
    private val uri: URI,
    @Qualifier("restTemplateAzure")
    restOperations: RestOperations
) :
    AbstractPingableRestClient(restOperations, "saksbehandling") {

    override val pingUri: URI = UriComponentsBuilder.fromUri(uri).pathSegment("api/ping").build().toUri()

    fun finnesBehandlingForPerson(personIdent: String, stønadType: StønadType? = null): Boolean {
        val uriComponentsBuilder = UriComponentsBuilder.fromUri(uri)
            .pathSegment("api/ekstern/behandling/finnes")
        if (stønadType != null) {
            uriComponentsBuilder.queryParam("type", stønadType.name)
        }
        val response = postForEntity<Ressurs<Boolean>>(uriComponentsBuilder.build().toUri(), PersonIdent(personIdent))
        return response.data ?: error("Kall mot ef-sak feilet melding=${response.melding}")
    }
}