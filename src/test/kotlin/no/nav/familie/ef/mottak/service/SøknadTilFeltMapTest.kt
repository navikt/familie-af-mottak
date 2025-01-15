package no.nav.familie.ef.mottak.service

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import no.nav.familie.ef.mottak.encryption.EncryptedString
import no.nav.familie.ef.mottak.no.nav.familie.ef.mottak.util.IOTestUtil
import no.nav.familie.ef.mottak.repository.domain.Ettersending
import no.nav.familie.ef.mottak.repository.domain.FeltMap
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SøknadTilFeltMapTest {
    @Test
    fun `mapSøknadsfelter returnerer en map-struktur med feltene fra søknaden`() {
        val søknad = Testdata.søknadOvergangsstønad

        val mapSøknadsfelter = SøknadTilFeltMap.mapOvergangsstønad(søknad, emptyList())

        Assertions.assertThat(mapSøknadsfelter.verdiliste).isNotEmpty
        Assertions.assertThat(mapSøknadsfelter.label).isEqualTo("Søknad om overgangsstønad (NAV 15-00.01)")

        val verdiliste = mapSøknadsfelter.verdiliste
        Assertions.assertThat(verdiliste).hasSize(12)
    }

    @Test
    fun `mapSøknadsfelter returnerer en map-struktur med typen TABELL`() {
        val søknad = Testdata.søknadOvergangsstønad

        val mapSøknadsfelter = SøknadTilFeltMap.mapOvergangsstønad(søknad, emptyList())

        val harVisningsVariantBarn =
            mapSøknadsfelter.verdiliste.any { it.visningsVariant == VisningsVariant.TABELL.toString() }
        Assertions.assertThat(harVisningsVariantBarn).isTrue
    }

    @Test
    fun `mapSøknadsfelter returnerer en map-struktur med typen VEDLEGG`() {
        val søknad = Testdata.søknadOvergangsstønad

        val vedlegg =
            listOf(
                "Dokumentasjon på at du er syk",
                "Dokumentasjon på at du er syk",
                "Dokumentasjon på at kan arbeide",
            )
        val mapSøknadsfelter = SøknadTilFeltMap.mapOvergangsstønad(søknad, vedlegg)

        val harVisningsVariantVedlegg =
            mapSøknadsfelter.verdiliste.any { it.visningsVariant == VisningsVariant.VEDLEGG.toString() }
        Assertions.assertThat(harVisningsVariantVedlegg).isTrue
    }

    @Test
    fun `mapSøknadsfelter returnerer en map-struktur med feltene fra søknaden sammen med vedlegg`() {
        val søknad = Testdata.søknadOvergangsstønad

        val vedlegg = listOf("Dokumentasjon på at du er syk")
        val mapSøknadsfelter = SøknadTilFeltMap.mapOvergangsstønad(søknad, vedlegg)

        Assertions.assertThat(mapSøknadsfelter.verdiliste).isNotEmpty
        Assertions.assertThat(mapSøknadsfelter.label).isEqualTo("Søknad om overgangsstønad (NAV 15-00.01)")
        Assertions.assertThat(mapSøknadsfelter.verdiliste).hasSize(12)
    }

    @Test
    fun `mapSøknadsfelter returnerer en map-struktur med feltene fra skolepengesøknaden sammen med vedlegg`() {
        val søknad = Testdata.søknadSkolepenger

        val vedlegg = listOf("Dokumentasjon på at du er syk")
        val mapSøknadsfelter = SøknadTilFeltMap.mapSkolepenger(søknad, vedlegg)

        Assertions.assertThat(mapSøknadsfelter.verdiliste).isNotEmpty
        Assertions.assertThat(mapSøknadsfelter.label).isEqualTo("Søknad om stønad til skolepenger (NAV 15-00.04)")
        Assertions.assertThat(mapSøknadsfelter.verdiliste).hasSize(9)
    }

    @Test
    fun `mapSkjemafelter returnerer en map-struktur med feltene fra skjema`() {
        val skjemaForArbeidssøker = Testdata.skjemaForArbeidssøker

        val mapSøknadsfelter = SøknadTilFeltMap.mapSkjemafelter(skjemaForArbeidssøker)

        Assertions.assertThat(mapSøknadsfelter.verdiliste).isNotEmpty
        Assertions.assertThat(mapSøknadsfelter.label).isEqualTo("Skjema for arbeidssøker - 15-08.01")
        Assertions.assertThat(mapSøknadsfelter.verdiliste).hasSize(3)
    }

    @Test
    fun `mapSøknadsfelter printer pdf for å se endringer i pdf-genereringen i PR - overgangsstønad`() {
        val søknad = Testdata.søknadOvergangsstønad

        val vedlegg =
            listOf(
                "Dokumentasjon på at du er syk",
                "Dokumentasjon på at du er syk",
                "Dokumentasjon på at kan arbeide",
            )
        val mapSøknadsfelter = SøknadTilFeltMap.mapOvergangsstønad(søknad, vedlegg)
        generatePdfAndAssert(mapSøknadsfelter, "pdf_generated_overgangsstønad_med_typer_ny.json")
    }

    @Test
    fun `mapSøknadsfelter printer pdf for å se endringer i pdf-genereringen i PR - skolepenger`() {
        val søknad = Testdata.søknadSkolepenger

        val vedlegg = listOf("Utgifter til utdanning")
        val mapSøknadsfelter = SøknadTilFeltMap.mapSkolepenger(søknad, vedlegg)
        generatePdfAndAssert(mapSøknadsfelter, "pdf_generated_skolepenger_med_typer.json")
    }

    @Test
    fun `mapSøknadsfelter printer pdf for å se endringer i pdf-genereringen i PR - barnetilsyn`() {
        val søknad = Testdata.søknadBarnetilsyn

        val vedlegg =
            listOf(
                "Dokumentasjon på at du er syk",
                "Dokumentasjon på at du er syk",
                "Dokumentasjon på at kan arbeide",
            )
        val mapSøknadsfelter = SøknadTilFeltMap.mapBarnetilsyn(søknad, vedlegg)
        generatePdfAndAssert(mapSøknadsfelter, "pdf_generated_barnetilsyn_med_typer_ny.json")
    }

    @Test
    fun `map ettersending med vedlegg`() {
        val mapEttersending =
            SøknadTilFeltMap.mapEttersending(
                Ettersending(
                    stønadType = "OVERGANGSSTØNAD",
                    fnr = "23118612345",
                    ettersendingJson = EncryptedString(""),
                    opprettetTid = LocalDateTime.of(2021, 5, 1, 12, 0),
                ),
                listOf("Lærlingkontrakt", "Utgifter til pass av barn"),
            )

        generatePdfAndAssert(mapEttersending, "pdf_generated_ettersending_med_typer.json")
    }

    @Test
    fun `ekskluderer verdiliste når skalEkskluderes matcher`() {
        val søknad = Testdata.søknadOvergangsstønad

        val vedlegg = listOf("Dokumentasjon på at du er syk")
        val mapSøknadsfelter = SøknadTilFeltMap.mapOvergangsstønad(søknad, vedlegg)

        val harEkskludertElement =
            mapSøknadsfelter.verdiliste.any {
                it.label == "harSendtInn" &&
                    it.verdi == "Nei"
            }
        Assertions.assertThat(harEkskludertElement).isFalse
    }

    @Test
    fun `Eksluderer navn for ufødte barn`() {
        val søknad = Testdata.søknadOvergangsstønadMedTommeFelter

        val vedlegg = listOf("Dokumentasjon på at du er syk")
        val mapSøknadsfelter = SøknadTilFeltMap.mapOvergangsstønad(søknad, vedlegg)

        val harEkskludertElement =
            !mapSøknadsfelter.verdiliste.any {
                it.label == "Barna dine" &&
                    it.verdiliste?.any { barn -> barn.label == "Barn 1" && barn.verdiliste?.any { ufødtBarn -> ufødtBarn.label == "Navn" && ufødtBarn.verdi.isNullOrBlank() } ?: true } ?: true
            }
        Assertions.assertThat(harEkskludertElement).isTrue
    }

    @Test
    fun `Eksluderer felter med tomme lister`() {
        val søknad = Testdata.søknadOvergangsstønadMedTommeFelter

        val vedlegg = listOf("Dokumentasjon på at du er syk")
        val mapSøknadsfelter = SøknadTilFeltMap.mapOvergangsstønad(søknad, vedlegg)

        val harEksludertListe =
            !mapSøknadsfelter.verdiliste.any {
                it.label == "Barna dine" &&
                    it.verdiliste?.any { barn -> barn.label == "Barn 1" && barn.verdiliste?.any { ufødtBarn -> ufødtBarn.label == "Samvær" && ufødtBarn.verdiliste?.isEmpty() == true } ?: true } ?: true
            }
        Assertions.assertThat(harEksludertListe).isTrue
    }

    private fun generatePdfAndAssert(
        mapSøknadsfelter: FeltMap,
        filename: String,
    ) {
        val indenter = DefaultIndenter("  ", "\n")
        val prettyPrinter =
            DefaultPrettyPrinter().apply {
                indentObjectsWith(indenter)
                indentArraysWith(indenter)
            }
        val objectWriter = objectMapper.writer(prettyPrinter)
        val pdf = objectWriter.writeValueAsString(mapSøknadsfelter)
        // kommentere ut for å skrive over fila
        // java.nio.file.Files.write(java.nio.file.Path.of("src/test/resources/json/$filename"), pdf.toByteArray())
        Assertions.assertThat(pdf).isEqualToIgnoringWhitespace(IOTestUtil.readFile(filename))
    }
}
