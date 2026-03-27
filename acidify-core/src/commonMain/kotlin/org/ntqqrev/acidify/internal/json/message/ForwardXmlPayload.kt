package org.ntqqrev.acidify.internal.json.message

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
@SerialName("msg")
internal data class ForwardXmlPayload(
    @SerialName("serviceID") val serviceId: String,
    @SerialName("templateID") val templateId: String,
    val action: String,
    @SerialName("m_fileName") val fileName: String,
    @SerialName("m_resid") val resId: String,
    val tSum: String,
    val items: List<Item>,
) {
    @Serializable
    @SerialName("item")
    data class Item(
        val titles: List<Title>,
        val summaries: List<Summary>,
    )

    @Serializable
    @SerialName("title")
    data class Title(
        val color: String,
        val size: String,
        @XmlValue val text: String,
    )

    @Serializable
    @SerialName("summary")
    data class Summary(
        val color: String,
        @XmlValue val text: String,
    )

    companion object {
        @OptIn(ExperimentalXmlUtilApi::class)
        val xmlModule = XML.v1 {
            policy {
                unknownChildHandler = { _, _, _, _, _ -> emptyList() }
            }
        }
    }
}