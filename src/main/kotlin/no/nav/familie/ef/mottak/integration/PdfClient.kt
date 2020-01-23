package no.nav.familie.ef.mottak.integration


import no.nav.familie.ef.mottak.config.PdfgeneratorConfig
import no.nav.familie.ef.mottak.repository.domain.Fil
import no.nav.familie.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.DefaultUriBuilderFactory


@Service
class PdfClient(@Qualifier("restTemplateUnsecured") operations: RestOperations,
                private val pdfgeneratorConfig: PdfgeneratorConfig) :
        AbstractRestClient(operations, "pdf") {

    fun lagPdf(labelValueJson: Map<String, Any>): Fil {
        val sendInnUri =
                DefaultUriBuilderFactory().uriString(pdfgeneratorConfig.url).path("/template/soknad/download-pdf").build()
        val byteArray = postForEntity<ByteArray>(sendInnUri, labelValueJson) ?: error("Pdf generering returnerte null")
        return Fil(byteArray)
    }
}
