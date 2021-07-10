package no.nav.familie.ef.mottak.mapper

import no.nav.familie.ef.mottak.config.*
import no.nav.familie.ef.mottak.repository.domain.Søknad
import no.nav.familie.ef.mottak.repository.domain.Vedlegg
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype

object ArkiverDokumentRequestMapper {

    fun toDto(søknad: Søknad,
              vedlegg: List<Vedlegg>): ArkiverDokumentRequest {
        val dokumenttype = søknad.dokumenttype.let { Dokumenttype.valueOf(it) }
        val søknadsdokumentJson =
                Dokument(søknad.søknadJson.toByteArray(), Filtype.JSON, null, "hoveddokument", dokumenttype)
        val søknadsdokumentPdf =
                Dokument(søknad.søknadPdf!!.bytes, Filtype.PDFA, null, "hoveddokument", dokumenttype)
        val hoveddokumentvarianter = listOf(søknadsdokumentPdf, søknadsdokumentJson)
        return ArkiverDokumentRequest(søknad.fnr,
                                      false,
                                      hoveddokumentvarianter,
                                      mapVedlegg(vedlegg, søknad.dokumenttype))
    }

    private fun mapVedlegg(vedlegg: List<Vedlegg>, dokumenttype: String): List<Dokument> {
        if (vedlegg.isEmpty()) return emptyList()
        val dokumenttypeVedlegg = mapDokumenttype(dokumenttype)
        return vedlegg.map { tilDokument(it, dokumenttypeVedlegg) }
    }

    private fun tilDokument(vedlegg: Vedlegg, dokumenttypeVedlegg: Dokumenttype): Dokument {
        return Dokument(dokument = vedlegg.innhold.bytes,
                        filtype = Filtype.PDFA,
                        tittel = vedlegg.tittel,
                        filnavn = vedlegg.id.toString(),
                        dokumenttype = dokumenttypeVedlegg)
    }

    private fun mapDokumenttype(dokumenttype: String): Dokumenttype {
        return when (dokumenttype) {
            DOKUMENTTYPE_OVERGANGSSTØNAD -> Dokumenttype.OVERGANGSSTØNAD_SØKNAD_VEDLEGG
            DOKUMENTTYPE_BARNETILSYN -> Dokumenttype.BARNETILSYNSTØNAD_VEDLEGG
            DOKUMENTTYPE_SKOLEPENGER -> Dokumenttype.SKOLEPENGER_VEDLEGG
            else -> error("Ukjent dokumenttype=$dokumenttype for vedlegg")
        }
    }

}
