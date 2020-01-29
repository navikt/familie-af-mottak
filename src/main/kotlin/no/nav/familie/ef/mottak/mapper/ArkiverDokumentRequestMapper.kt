package no.nav.familie.ef.mottak.mapper

import no.nav.familie.ef.mottak.repository.domain.Soknad
import no.nav.familie.ef.mottak.service.SøknadTreeWalker
import no.nav.familie.kontrakter.felles.arkivering.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.arkivering.Dokument
import no.nav.familie.kontrakter.felles.arkivering.FilType

object ArkiverDokumentRequestMapper {

    private const val DOKUMENTTYPE_OVERGANGSSTØNAD = "OVERGANGSSTØNAD_SØKNAD"
    private const val DOKUMENTTYPE_VEDLEGG = "OVERGANGSSTØNAD_SØKNAD_VEDLEGG"

    fun toDto(soknad: Soknad): ArkiverDokumentRequest {

        val kontraktssøknad = SøknadMapper.toDto(soknad)

        val vedleggsdokumenter = SøknadTreeWalker.finnDokumenter(kontraktssøknad).map { tilDokument(it) }

        // TODO legge til søknadsdokumentJson når integrasjoner takler flere variantfomater.
        @Suppress("UNUSED_VARIABLE")
        val søknadsdokumentJson =
                Dokument(soknad.søknadJson.toByteArray(), FilType.JSON, null, "hoveddokument", DOKUMENTTYPE_OVERGANGSSTØNAD)
        val søknadsdokumentPdf =
                Dokument(soknad.søknadPdf!!.bytes, FilType.PDFA, null, "hoveddokument", DOKUMENTTYPE_OVERGANGSSTØNAD)
        val dokumenter: List<Dokument> = listOf(søknadsdokumentPdf) + vedleggsdokumenter
        return ArkiverDokumentRequest(soknad.fnr, true, dokumenter)
    }

    private fun tilDokument(vedlegg: no.nav.familie.kontrakter.ef.søknad.Dokument): Dokument {
        return Dokument(dokument = vedlegg.bytes,
                        filType = FilType.PDFA,
                        tittel = vedlegg.tittel,
                        filnavn = null,
                        dokumentType = DOKUMENTTYPE_VEDLEGG)
    }


}
