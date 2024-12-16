package no.nav.familie.ef.mottak.service

import no.nav.familie.ef.mottak.encryption.EncryptedString
import no.nav.familie.ef.mottak.no.nav.familie.ef.mottak.util.IOTestUtil
import no.nav.familie.ef.mottak.repository.domain.Ettersending
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SøknadTilGenereltFormatMapperTest {
    @Test
    fun `mapSøknadsfelter returnerer en map-struktur med feltene fra søknaden`() {
        val søknad = Testdata.søknadOvergangsstønad

        val mapSøknadsfelter = SøknadTilGenereltFormatMapper.mapOvergangsstønad(søknad, emptyList())

        Assertions.assertThat(mapSøknadsfelter).isNotEmpty
        Assertions.assertThat(mapSøknadsfelter["label"]).isEqualTo("Søknad om overgangsstønad (NAV 15-00.01)")

        val verdiliste = mapSøknadsfelter["verdiliste"] as List<*>
        Assertions.assertThat(verdiliste).hasSize(12)
    }

    @Test
    fun `mapSøknadsfelter returnerer en map-struktur med typen TABELL_BARN`() {
        val søknad = Testdata.søknadOvergangsstønad

        val mapSøknadsfelter = SøknadTilGenereltFormatMapper.mapOvergangsstønad(søknad, emptyList())

        val verdiliste = mapSøknadsfelter["verdiliste"] as List<Map<String, Any>>

        val harVisningsVariantBarn =
            verdiliste.any {
                it["label"] == "Barna dine" && it["visningsVariant"] == "TABELLER_2_KOLONNER, Navn, Barn"
            }
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
        val mapSøknadsfelter = SøknadTilGenereltFormatMapper.mapOvergangsstønad(søknad, vedlegg)

        val verdiliste = mapSøknadsfelter["verdiliste"] as List<Map<String, Any>>

        val harVisningsVariantVedlegg = verdiliste.any { it["visningsVariant"] == VisningsVariant.VEDLEGG.toString() }
        Assertions.assertThat(harVisningsVariantVedlegg).isTrue
    }

    @Test
    fun `mapSøknadsfelter returnerer en map-struktur med feltene fra søknaden sammen med vedlegg`() {
        val søknad = Testdata.søknadOvergangsstønad

        val vedlegg = listOf("Dokumentasjon på at du er syk")
        val mapSøknadsfelter = SøknadTilGenereltFormatMapper.mapOvergangsstønad(søknad, vedlegg)

        Assertions.assertThat(mapSøknadsfelter).isNotEmpty
        Assertions.assertThat(mapSøknadsfelter["label"]).isEqualTo("Søknad om overgangsstønad (NAV 15-00.01)")
        Assertions.assertThat(mapSøknadsfelter["verdiliste"] as List<Any?>).hasSize(12)
    }

    @Test
    fun `mapSøknadsfelter returnerer en map-struktur med feltene fra skolepengesøknaden sammen med vedlegg`() {
        val søknad = Testdata.søknadSkolepenger

        val vedlegg = listOf("Dokumentasjon på at du er syk")
        val mapSøknadsfelter = SøknadTilGenereltFormatMapper.mapSkolepenger(søknad, vedlegg)

        Assertions.assertThat(mapSøknadsfelter).isNotEmpty
        Assertions.assertThat(mapSøknadsfelter["label"]).isEqualTo("Søknad om stønad til skolepenger (NAV 15-00.04)")
        Assertions.assertThat(mapSøknadsfelter["verdiliste"] as List<Any?>).hasSize(9)
    }

    @Test
    fun `mapSkjemafelter returnerer en map-struktur med feltene fra skjema`() {
        val skjemaForArbeidssøker = Testdata.skjemaForArbeidssøker

        val mapSøknadsfelter = SøknadTilGenereltFormatMapper.mapSkjemafelter(skjemaForArbeidssøker)

        Assertions.assertThat(mapSøknadsfelter).isNotEmpty
        Assertions.assertThat(mapSøknadsfelter["label"]).isEqualTo("Skjema for arbeidssøker - 15-08.01")
        Assertions.assertThat(mapSøknadsfelter["verdiliste"] as List<Any?>).hasSize(3)
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
        val mapSøknadsfelter = SøknadTilGenereltFormatMapper.mapOvergangsstønad(søknad, vedlegg)
        generatePdfAndAssert(mapSøknadsfelter, "pdf_generated_overgangsstønad_med_typer.json")
    }

    @Test
    fun `mapSøknadsfelter printer pdf for å se endringer i pdf-genereringen i PR - skolepenger`() {
        val søknad = Testdata.søknadSkolepenger

        val vedlegg = listOf("Utgifter til utdanning")
        val mapSøknadsfelter = SøknadTilGenereltFormatMapper.mapSkolepenger(søknad, vedlegg)
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
        val mapSøknadsfelter = SøknadTilGenereltFormatMapper.mapBarnetilsyn(søknad, vedlegg)
        generatePdfAndAssert(mapSøknadsfelter, "pdf_generated_barnetilsyn_med_typer.json")
    }

    @Test
    fun `map ettersending med vedlegg`() {
        val mapEttersending =
            SøknadTilGenereltFormatMapper.mapEttersending(
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

    private fun generatePdfAndAssert(
        mapSøknadsfelter: Map<String, Any>,
        filename: String,
    ) {
        val pdf = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapSøknadsfelter)
        // kommentere ut for å skrive over fila
        // java.nio.file.Files.write(java.nio.file.Path.of("src/test/resources/json/$filename"), pdf.toByteArray())
        Assertions.assertThat(pdf).isEqualToIgnoringWhitespace(IOTestUtil.readFile(filename))
    }
}
