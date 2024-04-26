package io.github.hsedjame.trackIt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import java.math.BigDecimal

sealed class Attribute(val parent: Attribute?, open val name: String) {

    abstract fun getRequiredFields(): List<Attribute>

    fun hasParent() = this !is RootAttrs

    /**
     * Returns the root attribute and all parents of the current attribute
     */
    fun allAttributes(): Pair<Attribute, List<Attribute>> {

        val attrs = mutableListOf<Attribute>()

        var current: Attribute = this

        while (current.hasParent()) {
            attrs.add(current)
            current = current.parent!!
        }

        return current to attrs.reversed()
    }

    sealed class RootAttrs(override val name: String) : Attribute(null, name) {

        override fun getRequiredFields(): List<Attribute> = listOf(
            Tealium, Page, User, Attribution, Environment
        )

        object Tealium : RootAttrs("tealium")
        object Page : RootAttrs("page")
        object User : RootAttrs("user")
        object Attribution : RootAttrs("attribution")
        object Environment : RootAttrs("environment")
        object Request : RootAttrs("request")
    }

    sealed class TealiumAttrs(override val name: String) : Attribute(RootAttrs.Tealium, name) {

        override fun getRequiredFields(): List<Attribute> = listOf(
            Account, Profile, Event
        )

        object Account : TealiumAttrs("account")
        object Profile : TealiumAttrs("profile")
        object Event : TealiumAttrs("event")
        object TraceId : TealiumAttrs("trace_id")
    }

    sealed class PageAttrs(override val name: String) : Attribute(RootAttrs.Page, name) {

        override fun getRequiredFields(): List<Attribute> = listOf(
            Name, Url
        )

        object Name : PageAttrs("name")
        object Url : PageAttrs("url")
        object Params : PageAttrs("params")
        object Previous : PageAttrs("previous")
        object SectionType : PageAttrs("section_type")
        object SeoType : PageAttrs("seo_type")
        object Event : PageAttrs("event")
        object Push : PageAttrs("push")
        object Click : PageAttrs("click")
        object Region : PageAttrs("region")
    }

    sealed class UserAttrs(override val name: String) : Attribute(RootAttrs.User, name) {
        override fun getRequiredFields(): List<Attribute> = listOf(
            VisitorType, SignedIn, Devices, Channel
        )

        object ErmesVisitorId : UserAttrs("ermes_visitor_id")
        object VisitorId : UserAttrs("visitor_id")
        object CorrelationId : UserAttrs("correlation_id")
        private object Emails : UserAttrs("emails")
        object VisitorType : UserAttrs("visitor_type")
        object KiouiType : UserAttrs("kioui_type")
        object SignedIn : UserAttrs("signed_in")
        private object Devices : UserAttrs("device")
        object Channel : UserAttrs("channel")
        object ConsentedVendors : UserAttrs("consented_vendors")
        object ConsentString : UserAttrs("consent_string")
        object ConsentId : UserAttrs("consent_id")
        object ExemptId : UserAttrs("exempt_id")
        private object Sessions : UserAttrs("session")
        object Ip : UserAttrs("ip")
        object UserAttrsAgent : UserAttrs("user_agent")
        object Birthdate : UserAttrs("birthdate")
        object Flagship : UserAttrs("flagship")

        sealed class Email(override val name: String) : Attribute(Emails, name) {

            override fun getRequiredFields(): List<Attribute> = emptyList()

            object Hidden : Email("hidden")
            object Strong : Email("strong")
            object Stronger : Email("stronger")
        }

        sealed class Device(override val name: String) : Attribute(Devices, name) {
            override fun getRequiredFields(): List<Attribute> = emptyList()

            object AdId : Device("adid")
            object IdFa : Device("idfa")
            object IdFv : Device("idfv")
            object AdjustId : Device("adjust_id")
            object OsVersion : Device("os_version")
            object Type : Device("type")
            object AppVersion : Device("app_version")
            object GeolocationActivated : Device("geolocation_activated")
            object Language : Device("language")
            object Country : Device("country")
            object Theme : Device("theme")
        }

        sealed class Session(override val name: String) : Attribute(Sessions, name) {
            override fun getRequiredFields(): List<Attribute> = listOf(Init)

            object Id : Session("id")
            object ErmesId : Session("ermes_id")
            object Init : Session("init")
        }
    }

}

sealed class Value<T>(private val value: T) {

    data class StringValue(val v: String) : Value<String>(v)
    data class IntValue(val v: Int) : Value<Int>(v)
    data class LongValue(val v: Long) : Value<Long>(v)
    data class FloatValue(val v: Float) : Value<Float>(v)
    data class DoubleValue(val v: Double) : Value<Double>(v)
    data class BooleanValue(val v: Boolean) : Value<Boolean>(v)
    data class BigDecimalValue(val v: BigDecimal) : Value<BigDecimal>(v)

    fun getValue() : T = this.value
}

class TrackingRequestBuilder {

    private val builder = ObjectMapper()
    private val root: ObjectNode = builder.createObjectNode()
    private var requiredFields: Map<String, List<Attribute>> = mutableMapOf()
    private var requiredRootAttrsAttributes = Attribute.RootAttrs.Tealium.getRequiredFields()

    fun <T> set(att: Attribute, value: Value<T>) {

        var node = root

        if (att.parent != null) {
            val (parent, attrs) = att.allAttributes()

            var attributeName = parent.name

            for (element in attrs) {
                node = getOrAddNode(node, attributeName) { element.getRequiredFields() }
                attributeName = element.name
            }
        }

        addValueToNode(value, node, att.name)

    }

    fun set(vararg attributes : Pair<Value<*>, Attribute> ){
        attributes.forEach { set(it.second, it.first) }
    }

    private fun <T> addValueToNode(
        value: Value<T>,
        node: ObjectNode,
        attributeName: String
    ): ObjectNode? = when (value) {
        is Value.StringValue -> node.put(attributeName, value.getValue())
        is Value.IntValue -> node.put(attributeName, value.getValue())
        is Value.LongValue -> node.put(attributeName, value.getValue())
        is Value.FloatValue -> node.put(attributeName, value.getValue())
        is Value.DoubleValue -> node.put(attributeName, value.getValue())
        is Value.BooleanValue -> node.put(attributeName, value.getValue())
        is Value.BigDecimalValue -> node.put(attributeName, value.getValue())
        else -> error("Unsupported value type")
    }

    fun build(): String = builder.writeValueAsString(root).apply {
        println(this)
        validate()
    }

    private fun getOrAddNode(parent: ObjectNode, key: String, requiredFieldsFn: () -> List<Attribute>): ObjectNode {
        if (!parent.has(key)) {
            parent.set<ObjectNode>(key, builder.createObjectNode())
            if (!requiredFields.containsKey(key)) {
                requiredFields = requiredFields.plus(key to requiredFieldsFn())
            }
        }

        return parent[key] as ObjectNode
    }

    private fun validate() {

        val missingFields = mutableListOf<String>()

        requiredRootAttrsAttributes.forEach {
            if (!root.has(it.name)) {
                missingFields.add(it.name)
            } else {
                missingFields(null to root, it).let { msgs -> missingFields.addAll(msgs) }
            }
        }

        check(missingFields.isEmpty()) { "Missing required fields: [\n ${missingFields.joinToString(",\n ")}\n]" }
    }

    private fun missingFields(parentNode: Pair<String?, ObjectNode>, parent: Attribute): List<String> {
        return mutableListOf<String>().apply {

            (parentNode.second[parent.name] as? ObjectNode)?.let {
                requiredFields[parent.name]?.forEach { attr ->
                    if (!it.has(attr.name)) {
                        val s = parentNode.first?.let { "$it." } ?: ""
                        add("$s${parent.name}.${attr.name}")
                    }
                }

                parent.parent?.let { parent -> missingFields(parent.name to it, parent) }
            }
        }

    }
}



