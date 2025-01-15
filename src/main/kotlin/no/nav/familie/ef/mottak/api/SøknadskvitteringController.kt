package no.nav.familie.ef.mottak.api

import no.nav.familie.ef.mottak.api.dto.Kvittering
import no.nav.familie.ef.mottak.service.SøknadskvitteringService
import no.nav.familie.ef.mottak.util.okEllerKastException
import no.nav.familie.kontrakter.ef.søknad.SkjemaForArbeidssøker
import no.nav.familie.kontrakter.ef.søknad.SøknadBarnetilsyn
import no.nav.familie.kontrakter.ef.søknad.SøknadMedVedlegg
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.SøknadSkolepenger
import no.nav.familie.sikkerhet.EksternBrukerUtils
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("!prod")
@RequestMapping("api/soknadskvittering", produces = [APPLICATION_JSON_VALUE])
@ProtectedWithClaims(
    issuer = EksternBrukerUtils.ISSUER_TOKENX,
    claimMap = ["acr=Level4"],
)
class SøknadskvitteringController(
    val søknadKvitteringService: SøknadskvitteringService,
) {
    @GetMapping("{søknadId}")
    fun hentSøknad(
        @PathVariable søknadId: String,
    ): ByteArray = søknadKvitteringService.hentSøknadPdf(søknadId)

    @PostMapping("overgangsstonad")
    fun overgangsstønad(
        @RequestBody søknad: SøknadMedVedlegg<SøknadOvergangsstønad>,
    ): Kvittering = okEllerKastException { søknadKvitteringService.mottaOvergangsstønad(søknad) }

    @PostMapping("barnetilsyn")
    fun barnetilsyn(
        @RequestBody søknad: SøknadMedVedlegg<SøknadBarnetilsyn>,
    ): Kvittering = okEllerKastException { søknadKvitteringService.mottaBarnetilsyn(søknad) }

    @PostMapping("skolepenger")
    fun skolepenger(
        @RequestBody søknad: SøknadMedVedlegg<SøknadSkolepenger>,
    ): Kvittering = okEllerKastException { søknadKvitteringService.mottaSkolepenger(søknad) }

    @PostMapping("arbeidssoker")
    fun arbeidssøker(
        @RequestBody skjemaForArbeidssøker: SkjemaForArbeidssøker,
    ): Kvittering = okEllerKastException { søknadKvitteringService.mottaArbeidsøkerSkjema(skjemaForArbeidssøker) }
}
