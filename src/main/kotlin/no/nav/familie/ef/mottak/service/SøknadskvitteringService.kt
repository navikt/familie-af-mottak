package no.nav.familie.ef.mottak.service

import no.nav.familie.ef.mottak.api.dto.Kvittering
import no.nav.familie.ef.mottak.integration.FamilieDokumentClient
import no.nav.familie.ef.mottak.mapper.SøknadMapper
import no.nav.familie.ef.mottak.repository.SøknadRepository
import no.nav.familie.ef.mottak.repository.VedleggRepository
import no.nav.familie.ef.mottak.repository.domain.EncryptedFile
import no.nav.familie.ef.mottak.repository.domain.Søknad
import no.nav.familie.ef.mottak.repository.domain.Vedlegg
import no.nav.familie.ef.mottak.repository.util.findByIdOrThrow
import no.nav.familie.kontrakter.ef.søknad.SkjemaForArbeidssøker
import no.nav.familie.kontrakter.ef.søknad.SøknadBarnetilsyn
import no.nav.familie.kontrakter.ef.søknad.SøknadMedVedlegg
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.SøknadSkolepenger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class SøknadskvitteringService(
    private val søknadRepository: SøknadRepository,
    private val vedleggRepository: VedleggRepository,
    private val dokumentClient: FamilieDokumentClient,
    private val taskProsesseringService: TaskProsesseringService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentSøknadPdf(søknadId: String): ByteArray {
        val søknad = hentSøknad(søknadId)
        return søknad.søknadPdf?.bytes ?: error("Søknadspdf mangler for søknad med id: $søknadId")
    }

    @Transactional
    fun mottaOvergangsstønad(søknad: SøknadMedVedlegg<SøknadOvergangsstønad>): Kvittering {
        val søknadDb = SøknadMapper.fromDto(søknad.søknad, true)
        val vedlegg = mapVedlegg(søknadDb.id, søknad.vedlegg)
        return motta(søknadDb, vedlegg)
    }

    @Transactional
    fun mottaBarnetilsyn(søknad: SøknadMedVedlegg<SøknadBarnetilsyn>): Kvittering {
        val søknadDb = SøknadMapper.fromDto(søknad.søknad, true)
        val vedlegg = mapVedlegg(søknadDb.id, søknad.vedlegg)
        return motta(søknadDb, vedlegg)
    }

    @Transactional
    fun mottaSkolepenger(søknad: SøknadMedVedlegg<SøknadSkolepenger>): Kvittering {
        val søknadDb = SøknadMapper.fromDto(søknad.søknad, true)
        val vedlegg = mapVedlegg(søknadDb.id, søknad.vedlegg)
        return motta(søknadDb, vedlegg)
    }

    @Transactional
    fun mottaArbeidsøkerSkjema(skjemaForArbeidssøker: SkjemaForArbeidssøker): Kvittering {
        val søknadDb = SøknadMapper.fromDto(skjemaForArbeidssøker)
        val lagretSkjema = søknadRepository.insert(søknadDb)
        taskProsesseringService.startPdfKvitteringTaskProsessering(lagretSkjema)
        logger.info("Mottatt skjema med id ${lagretSkjema.id}")
        return Kvittering(lagretSkjema.id, "Pdf-skjema lagret med id ${lagretSkjema.id} er registrert mottatt.")
    }

    private fun mapVedlegg(
        søknadDbId: String,
        vedleggMetadata: List<no.nav.familie.kontrakter.ef.søknad.Vedlegg>,
    ): List<Vedlegg> =
        vedleggMetadata.map {
            Vedlegg(
                id = UUID.fromString(it.id),
                søknadId = søknadDbId,
                navn = it.navn,
                tittel = it.tittel,
                innhold = EncryptedFile(dokumentClient.hentVedlegg(it.id)),
            )
        }

    private fun motta(
        søknadDb: Søknad,
        vedlegg: List<Vedlegg>,
    ): Kvittering {
        val lagretSkjema = søknadRepository.insert(søknadDb)
        vedleggRepository.insertAll(vedlegg)
        taskProsesseringService.startPdfKvitteringTaskProsessering(søknadDb)
        logger.info("Mottatt pdf-søknad med id ${lagretSkjema.id}")
        return Kvittering(lagretSkjema.id, "Pdf-søknad lagret med id ${lagretSkjema.id} er registrert mottatt.")
    }

    private fun hentSøknad(søknadId: String): Søknad = søknadRepository.findByIdOrThrow(søknadId)
}
