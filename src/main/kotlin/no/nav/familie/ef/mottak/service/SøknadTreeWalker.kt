package no.nav.familie.ef.mottak.service

import no.nav.familie.ef.mottak.repository.domain.Ettersending
import no.nav.familie.ef.mottak.repository.domain.FeltMap
import no.nav.familie.ef.mottak.repository.domain.VerdilisteElement
import no.nav.familie.kontrakter.ef.søknad.Adresse
import no.nav.familie.kontrakter.ef.søknad.Datoperiode
import no.nav.familie.kontrakter.ef.søknad.Dokumentasjon
import no.nav.familie.kontrakter.ef.søknad.MånedÅrPeriode
import no.nav.familie.kontrakter.ef.søknad.SkjemaForArbeidssøker
import no.nav.familie.kontrakter.ef.søknad.SøknadBarnetilsyn
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.SøknadSkolepenger
import no.nav.familie.kontrakter.ef.søknad.Søknadsfelt
import no.nav.familie.kontrakter.felles.Fødselsnummer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

object SøknadTreeWalker {
    private val endNodes =
        setOf<KClass<*>>(
            String::class,
            Int::class,
            Boolean::class,
            Double::class,
            Dokumentasjon::class,
            Fødselsnummer::class,
            MånedÅrPeriode::class,
            Datoperiode::class,
            Adresse::class,
            LocalDate::class,
            LocalDateTime::class,
            Month::class,
            Long::class,
        )

    fun mapOvergangsstønad(
        søknad: SøknadOvergangsstønad,
        vedleggTitler: List<String>,
    ): FeltMap {
        val finnFelter = finnFelter(søknad)
        val vedlegg =
            VerdilisteElement(label = "Vedlegg", verdiliste = listOf(Feltformaterer.mapVedlegg(vedleggTitler)))
        return FeltMap("Søknad om overgangsstønad (NAV 15-00.01)", finnFelter + vedlegg)
    }

    fun mapBarnetilsyn(
        søknad: SøknadBarnetilsyn,
        vedleggTitler: List<String>,
    ): FeltMap {
        val finnFelter = finnFelter(søknad)
        val vedlegg =
            VerdilisteElement(label = "Vedlegg", verdiliste = listOf(Feltformaterer.mapVedlegg(vedleggTitler)))
        return FeltMap("Søknad om stønad til barnetilsyn (NAV 15-00.02)", finnFelter + vedlegg)
    }

    fun mapSkolepenger(
        søknad: SøknadSkolepenger,
        vedleggTitler: List<String>,
    ): FeltMap {
        val finnFelter = finnFelter(søknad)
        val vedlegg =
            VerdilisteElement(label = "Vedlegg", verdiliste = listOf(Feltformaterer.mapVedlegg(vedleggTitler)))
        return FeltMap("Søknad om stønad til skolepenger (NAV 15-00.04)", finnFelter + vedlegg)
    }

    fun mapSkjemafelter(skjema: SkjemaForArbeidssøker): FeltMap {
        val finnFelter = finnFelter(skjema)
        return FeltMap("Skjema for arbeidssøker - 15-08.01", finnFelter)
    }

    fun mapEttersending(
        ettersending: Ettersending,
        vedleggTitler: List<String>,
    ): FeltMap {
        val infoMap =
            VerdilisteElement(
                label = "Ettersending av vedlegg",
                verdiliste =
                    listOf(
                        VerdilisteElement("Stønadstype", verdi = ettersending.stønadType),
                        VerdilisteElement("Fødselsnummer", verdi = ettersending.fnr),
                        VerdilisteElement(
                            "Dato mottatt",
                            verdi = ettersending.opprettetTid.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
                        ),
                    ),
            )
        val vedleggMap =
            VerdilisteElement(
                label = "Dokumenter vedlagt",
                verdiliste = listOf(Feltformaterer.mapVedlegg(vedleggTitler)),
            )
        return FeltMap("Ettersending", listOf(infoMap, vedleggMap))
    }

    private fun finnFelter(entitet: Any): List<VerdilisteElement> {
        // Det går ikke å hente elementene i en liste med reflection, så vi traverserer den som vanlig.
        if (entitet is List<Any?>) {
            return entitet
                .filterNotNull()
                .map { finnFelter(it) }
                .flatten()
        }
        val parametere = konstruktørparametere(entitet)

        val list =
            parametere
                .asSequence()
                .map { finnSøknadsfelt(entitet, it) }
                .filter { it.visibility == KVisibility.PUBLIC }
                .mapNotNull { getFeltverdi(it, entitet) }
                .map { finnFelter(it) } // Kall rekursivt videre
                .flatten()
                .toList()

        if (entitet is Søknadsfelt<*>) {
            if (entitet.verdi!! is Dokumentasjon) {
                @Suppress("UNCHECKED_CAST")
                return listOf(mapDokumentasjon(entitet as Søknadsfelt<Dokumentasjon>))
            }
            if (entitet.verdi!!::class in endNodes) {
                return listOf(Feltformaterer.mapEndenodeTilUtskriftMap(entitet))
            }
            if (entitet.verdi is List<*>) {
                val verdiliste = entitet.verdi as List<*>
                if (verdiliste.firstOrNull() is String) {
                    return listOf(Feltformaterer.mapEndenodeTilUtskriftMap(entitet))
                }
            }
            return listOf(VerdilisteElement(label = entitet.label, verdiliste = list))
        }
        return list
    }

    private fun mapDokumentasjon(entitet: Søknadsfelt<Dokumentasjon>): VerdilisteElement =
        VerdilisteElement(
            label = entitet.label,
            verdiliste = listOf(Feltformaterer.mapEndenodeTilUtskriftMap(entitet.verdi.harSendtInnTidligere)),
        )

    /**
     * Henter ut verdien for felt på entitet.
     */
    private fun getFeltverdi(
        felt: KProperty1<out Any, Any?>,
        entitet: Any,
    ) = felt.getter.call(entitet)

    /**
     * Finn første (og eneste) felt på entiteten som har samme navn som konstruktørparameter.
     */
    private fun finnSøknadsfelt(
        entity: Any,
        konstruktørparameter: KParameter,
    ) = entity::class.declaredMemberProperties.first { it.name == konstruktørparameter.name }

    /**
     * Konstruktørparametere er det eneste som gir oss en garantert rekkefølge for feltene, så vi henter disse først.
     */
    private fun konstruktørparametere(entity: Any) = entity::class.primaryConstructor?.parameters ?: emptyList()
}
