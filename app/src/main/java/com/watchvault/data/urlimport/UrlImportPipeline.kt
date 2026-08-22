package com.watchvault.data.urlimport

/**
 * Generic URL -> watch-data pipeline (spec section 3 + the "one important change" note: not
 * dependent on a per-site parser).
 *
 * URL -> fetch HTML -> [JsonLdExtractor] -> [OpenGraphExtractor] -> [MetaTagExtractor] ->
 *        [HtmlHeuristicExtractor] -> [SiteAdapter] (optional, keyed by domain) ->
 *        normalize -> FieldWithSource per field -> ImportPreview -> user edits/approves -> save
 *
 * Every extracted field carries its own source so the preview screen can show, per spec
 * section 21:
 *   FIELD | VALUE | SOURCE
 *   Case diameter | 42 mm | Source: Product JSON-LD
 * Fields the pipeline is not confident about must be marked NEEDS_CONFIRMATION rather than
 * guessed — never silently filled from a default.
 */

enum class ExtractionSource { JSON_LD, OPEN_GRAPH, META_TAG, HTML_TABLE, SITE_ADAPTER, USER_EDITED }

enum class ConfidenceStatus { CONFIRMED, NEEDS_CONFIRMATION }

data class FieldWithSource<T>(
    val value: T?,
    val source: ExtractionSource?,
    val confidence: ConfidenceStatus = ConfidenceStatus.NEEDS_CONFIRMATION
)

data class ExtractedProductData(
    val sourceUrl: String,
    val fetchedAt: Long,
    val rawHtmlSnapshot: String,       // stored verbatim so re-imports/audits don't depend on the live site
    val title: FieldWithSource<String> = FieldWithSource(null, null),
    val brand: FieldWithSource<String> = FieldWithSource(null, null),
    val model: FieldWithSource<String> = FieldWithSource(null, null),
    val referenceNumber: FieldWithSource<String> = FieldWithSource(null, null),
    val price: FieldWithSource<Double> = FieldWithSource(null, null),
    val currency: FieldWithSource<String> = FieldWithSource(null, null),
    val imageUrl: FieldWithSource<String> = FieldWithSource(null, null),
    val description: FieldWithSource<String> = FieldWithSource(null, null),
    val sku: FieldWithSource<String> = FieldWithSource(null, null),
    val upc: FieldWithSource<String> = FieldWithSource(null, null),
    val availability: FieldWithSource<String> = FieldWithSource(null, null),
    val manufacturer: FieldWithSource<String> = FieldWithSource(null, null),
    val specifications: Map<String, FieldWithSource<String>> = emptyMap() // e.g. "Case diameter" -> 42mm/JSON_LD
)

interface FieldExtractor {
    /** Returns partial data; never throws on missing fields, only on total fetch/parse failure. */
    fun extract(html: String, url: String): ExtractedProductData
}

/** Optional, keyed by domain, purely additive — the generic extractors above must work with zero
 *  site adapters registered. A missing/broken adapter must never block extraction. */
interface SiteAdapter {
    val domain: String
    fun refine(existing: ExtractedProductData, html: String): ExtractedProductData
}

interface UrlFetcher {
    suspend fun fetch(url: String): String // raw HTML; throws only on network/HTTP failure
}

class UrlImportPipeline(
    private val fetcher: UrlFetcher,
    private val jsonLdExtractor: FieldExtractor,
    private val openGraphExtractor: FieldExtractor,
    private val metaTagExtractor: FieldExtractor,
    private val htmlHeuristicExtractor: FieldExtractor,
    private val siteAdapters: Map<String, SiteAdapter> = emptyMap()
) {
    suspend fun run(url: String): ExtractedProductData {
        val html = fetcher.fetch(url)

        // Merge order: later extractors only fill gaps left by earlier ones — JSON-LD is
        // usually most reliable for e-commerce, so it wins first.
        var data = jsonLdExtractor.extract(html, url)
        data = mergeFillingGaps(data, openGraphExtractor.extract(html, url))
        data = mergeFillingGaps(data, metaTagExtractor.extract(html, url))
        data = mergeFillingGaps(data, htmlHeuristicExtractor.extract(html, url))

        val domain = java.net.URI(url).host?.removePrefix("www.")
        siteAdapters[domain]?.let { adapter ->
            data = adapter.refine(data, html)
        }

        return data
    }

    private fun mergeFillingGaps(
        primary: ExtractedProductData,
        fallback: ExtractedProductData
    ): ExtractedProductData = primary.copy(
        title = if (primary.title.value != null) primary.title else fallback.title,
        brand = if (primary.brand.value != null) primary.brand else fallback.brand,
        model = if (primary.model.value != null) primary.model else fallback.model,
        referenceNumber = if (primary.referenceNumber.value != null) primary.referenceNumber else fallback.referenceNumber,
        price = if (primary.price.value != null) primary.price else fallback.price,
        currency = if (primary.currency.value != null) primary.currency else fallback.currency,
        imageUrl = if (primary.imageUrl.value != null) primary.imageUrl else fallback.imageUrl,
        description = if (primary.description.value != null) primary.description else fallback.description,
        sku = if (primary.sku.value != null) primary.sku else fallback.sku,
        upc = if (primary.upc.value != null) primary.upc else fallback.upc,
        availability = if (primary.availability.value != null) primary.availability else fallback.availability,
        manufacturer = if (primary.manufacturer.value != null) primary.manufacturer else fallback.manufacturer,
        specifications = fallback.specifications + primary.specifications // primary wins on key collision
    )
}

/**
 * What the UI actually renders before anything is saved. Nothing from ExtractedProductData
 * reaches the database until the user confirms this screen (spec: "NEVER silently save
 * scraped data").
 */
data class ImportPreview(
    val extracted: ExtractedProductData,
    val targetDestination: ImportDestination // user-selectable: Wishlist or Collection
)

enum class ImportDestination { WISHLIST, COLLECTION }
