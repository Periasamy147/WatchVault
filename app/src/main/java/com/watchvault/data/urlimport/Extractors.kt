package com.watchvault.data.urlimport

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

private fun confirmed(value: String?) = value != null

/**
 * Parses schema.org Product JSON-LD blocks. Generally the most reliable structured-data
 * source for e-commerce pages, so [UrlImportPipeline] tries it first.
 */
class JsonLdExtractor : FieldExtractor {
    override fun extract(html: String, url: String): ExtractedProductData {
        val doc = safeParse(html, url)
        var result = ExtractedProductData(sourceUrl = url, fetchedAt = System.currentTimeMillis(), rawHtmlSnapshot = html)

        val scripts = doc?.select("script[type=application/ld+json]") ?: return result
        for (script in scripts) {
            val jsonText = script.data().ifBlank { script.html() }
            val element = runCatching { lenientJson.parseToJsonElement(jsonText) }.getOrNull() ?: continue
            val product = runCatching { findProductNode(element) }.getOrNull() ?: continue
            result = runCatching { mergeProduct(result, product) }.getOrDefault(result)
        }
        return result
    }

    private fun findProductNode(element: JsonElement): JsonObject? = when (element) {
        is JsonObject -> {
            val type = element["@type"]?.jsonPrimitive?.contentOrNull
                ?: (element["@type"] as? JsonArray)?.joinToString { it.jsonPrimitive.contentOrNull.orEmpty() }
            if (type?.contains("Product", ignoreCase = true) == true) element
            else element["@graph"]?.jsonArray?.firstNotNullOfOrNull { findProductNode(it) }
        }
        is JsonArray -> element.firstNotNullOfOrNull { findProductNode(it) }
        else -> null
    }

    private fun mergeProduct(base: ExtractedProductData, product: JsonObject): ExtractedProductData {
        val name = product["name"]?.jsonPrimitive?.contentOrNull
        val brand = product["brand"]?.let { extractBrand(it) }
        val sku = product["sku"]?.jsonPrimitive?.contentOrNull
        val mpn = product["mpn"]?.jsonPrimitive?.contentOrNull
        val gtin = (product["gtin13"] ?: product["gtin"] ?: product["gtin12"])?.jsonPrimitive?.contentOrNull
        val description = product["description"]?.jsonPrimitive?.contentOrNull
        val image = extractImage(product["image"])
        val manufacturer = product["manufacturer"]?.let { extractBrand(it) } ?: brand

        val offer = extractOffer(product["offers"])
        val price = offer?.get("price")?.jsonPrimitive?.doubleOrNull
        val currency = offer?.get("priceCurrency")?.jsonPrimitive?.contentOrNull
        val availability = offer?.get("availability")?.jsonPrimitive?.contentOrNull?.substringAfterLast('/')

        return base.copy(
            title = base.title.value?.let { base.title } ?: FieldWithSource(name, ExtractionSource.JSON_LD, if (confirmed(name)) ConfidenceStatus.CONFIRMED else ConfidenceStatus.NEEDS_CONFIRMATION),
            brand = base.brand.value?.let { base.brand } ?: FieldWithSource(brand, ExtractionSource.JSON_LD, ConfidenceStatus.CONFIRMED),
            referenceNumber = base.referenceNumber.value?.let { base.referenceNumber } ?: FieldWithSource(mpn ?: sku, ExtractionSource.JSON_LD, ConfidenceStatus.NEEDS_CONFIRMATION),
            price = base.price.value?.let { base.price } ?: FieldWithSource(price, ExtractionSource.JSON_LD, ConfidenceStatus.CONFIRMED),
            currency = base.currency.value?.let { base.currency } ?: FieldWithSource(currency, ExtractionSource.JSON_LD, ConfidenceStatus.CONFIRMED),
            imageUrl = base.imageUrl.value?.let { base.imageUrl } ?: FieldWithSource(image, ExtractionSource.JSON_LD, ConfidenceStatus.CONFIRMED),
            description = base.description.value?.let { base.description } ?: FieldWithSource(description, ExtractionSource.JSON_LD, ConfidenceStatus.CONFIRMED),
            sku = base.sku.value?.let { base.sku } ?: FieldWithSource(sku, ExtractionSource.JSON_LD, ConfidenceStatus.CONFIRMED),
            upc = base.upc.value?.let { base.upc } ?: FieldWithSource(gtin, ExtractionSource.JSON_LD, ConfidenceStatus.CONFIRMED),
            availability = base.availability.value?.let { base.availability } ?: FieldWithSource(availability, ExtractionSource.JSON_LD, ConfidenceStatus.NEEDS_CONFIRMATION),
            manufacturer = base.manufacturer.value?.let { base.manufacturer } ?: FieldWithSource(manufacturer, ExtractionSource.JSON_LD, ConfidenceStatus.NEEDS_CONFIRMATION)
        )
    }

    private fun extractBrand(element: JsonElement): String? = when (element) {
        is JsonObject -> element["name"]?.jsonPrimitive?.contentOrNull
        is JsonPrimitive -> element.contentOrNull
        else -> null
    }

    private fun extractImage(element: JsonElement?): String? = when (element) {
        is JsonPrimitive -> element.contentOrNull
        is JsonArray -> element.firstOrNull()?.let { extractImage(it) }
        is JsonObject -> element["url"]?.jsonPrimitive?.contentOrNull
        else -> null
    }

    private fun extractOffer(element: JsonElement?): JsonObject? = when (element) {
        is JsonObject -> element
        is JsonArray -> element.firstOrNull { it is JsonObject } as? JsonObject
        else -> null
    }
}

/** Reads Facebook/Twitter OpenGraph and product:* meta tags. */
class OpenGraphExtractor : FieldExtractor {
    override fun extract(html: String, url: String): ExtractedProductData {
        val doc = safeParse(html, url) ?: return ExtractedProductData(url, System.currentTimeMillis(), html)

        fun meta(property: String): String? = doc.select("meta[property=$property]").firstOrNull()?.attr("content")?.ifBlank { null }

        val title = meta("og:title")
        val image = meta("og:image")
        val description = meta("og:description")
        val price = (meta("product:price:amount") ?: meta("og:price:amount"))?.toDoubleOrNull()
        val currency = meta("product:price:currency") ?: meta("og:price:currency")
        val brand = meta("product:brand") ?: meta("og:brand")
        val availability = meta("product:availability")

        return ExtractedProductData(
            sourceUrl = url,
            fetchedAt = System.currentTimeMillis(),
            rawHtmlSnapshot = html,
            title = FieldWithSource(title, ExtractionSource.OPEN_GRAPH, ConfidenceStatus.CONFIRMED),
            brand = FieldWithSource(brand, ExtractionSource.OPEN_GRAPH, ConfidenceStatus.NEEDS_CONFIRMATION),
            price = FieldWithSource(price, ExtractionSource.OPEN_GRAPH, ConfidenceStatus.CONFIRMED),
            currency = FieldWithSource(currency, ExtractionSource.OPEN_GRAPH, ConfidenceStatus.CONFIRMED),
            imageUrl = FieldWithSource(image, ExtractionSource.OPEN_GRAPH, ConfidenceStatus.CONFIRMED),
            description = FieldWithSource(description, ExtractionSource.OPEN_GRAPH, ConfidenceStatus.CONFIRMED),
            availability = FieldWithSource(availability, ExtractionSource.OPEN_GRAPH, ConfidenceStatus.NEEDS_CONFIRMATION)
        )
    }
}

/** Falls back to plain `<meta name="...">` tags and `<title>` — the least specific source,
 *  so every field it returns is marked NEEDS_CONFIRMATION. */
class MetaTagExtractor : FieldExtractor {
    override fun extract(html: String, url: String): ExtractedProductData {
        val doc = safeParse(html, url) ?: return ExtractedProductData(url, System.currentTimeMillis(), html)

        fun meta(name: String): String? = doc.select("meta[name=$name]").firstOrNull()?.attr("content")?.ifBlank { null }

        val title = doc.title().ifBlank { null } ?: meta("title")
        val description = meta("description")
        val sku = meta("sku") ?: meta("product_id")

        return ExtractedProductData(
            sourceUrl = url,
            fetchedAt = System.currentTimeMillis(),
            rawHtmlSnapshot = html,
            title = FieldWithSource(title, ExtractionSource.META_TAG, ConfidenceStatus.NEEDS_CONFIRMATION),
            description = FieldWithSource(description, ExtractionSource.META_TAG, ConfidenceStatus.NEEDS_CONFIRMATION),
            sku = FieldWithSource(sku, ExtractionSource.META_TAG, ConfidenceStatus.NEEDS_CONFIRMATION)
        )
    }
}

/**
 * Last-resort plain-HTML heuristics (e.g. first `<h1>` as a title guess). Deliberately
 * conservative — everything it finds is NEEDS_CONFIRMATION, never CONFIRMED, per spec:
 * "never guessed" fields must not be presented as fact.
 */
class HtmlHeuristicExtractor : FieldExtractor {
    override fun extract(html: String, url: String): ExtractedProductData {
        val doc = safeParse(html, url) ?: return ExtractedProductData(url, System.currentTimeMillis(), html)

        val heading = doc.select("h1").firstOrNull()?.text()?.ifBlank { null }
        val firstImage = doc.select("img[src]").firstOrNull()?.absUrl("src")?.ifBlank { null }

        return ExtractedProductData(
            sourceUrl = url,
            fetchedAt = System.currentTimeMillis(),
            rawHtmlSnapshot = html,
            title = FieldWithSource(heading, ExtractionSource.HTML_TABLE, ConfidenceStatus.NEEDS_CONFIRMATION),
            imageUrl = FieldWithSource(firstImage, ExtractionSource.HTML_TABLE, ConfidenceStatus.NEEDS_CONFIRMATION)
        )
    }
}

private fun safeParse(html: String, url: String): Document? = runCatching { Jsoup.parse(html, url) }.getOrNull()
