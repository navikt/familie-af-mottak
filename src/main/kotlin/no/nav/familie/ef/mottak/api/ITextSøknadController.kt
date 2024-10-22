package no.nav.familie.ef.mottak.api

import no.nav.familie.ef.mottak.api.dto.Kvittering
import no.nav.familie.ef.mottak.service.ITextSøknadService
import no.nav.familie.ef.mottak.util.okEllerKastException
import no.nav.familie.kontrakter.ef.søknad.SøknadMedVedlegg
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import no.nav.familie.sikkerhet.EksternBrukerUtils
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Profile("!prod")
@RestController
@RequestMapping(path = ["/api/itext-soknad"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(
    issuer = EksternBrukerUtils.ISSUER_TOKENX,
    claimMap = ["acr=Level4"],
)
class ITextSøknadController(
    val iTextsøknadService: ITextSøknadService,
) {
    @PostMapping("overgangsstonad")
    fun overgangsstønad(
        @RequestBody søknad: SøknadMedVedlegg<SøknadOvergangsstønad>,
    ): Kvittering = okEllerKastException { iTextsøknadService.mottaOvergangsstønad(søknad) }

}