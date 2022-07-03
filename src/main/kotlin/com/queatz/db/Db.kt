package com.queatz.db

import com.arangodb.*
import com.arangodb.entity.CollectionType
import com.arangodb.entity.EdgeDefinition
import com.arangodb.mapping.ArangoJack
import com.arangodb.model.CollectionCreateOptions
import com.arangodb.model.DocumentCreateOptions
import com.arangodb.model.DocumentUpdateOptions
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.datetime.Clock
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1

class Db {
    private val db = ArangoDB.Builder()
        .user("ailaai")
        .password("ailaai")
        .serializer(ArangoJack().apply {
            configure { it.registerKotlinModule() }
        })
        .build()
        .db(DbName.of("ailaai"))
        .setup()

    internal fun <T : Model> one(klass: KClass<T>, query: String, parameters: Map<String, Any?> = mapOf()) =
        synchronized(db) { db.query(
            query,
            if (query.contains("@@collection")) mutableMapOf("@collection" to klass.collection()) + parameters else parameters,
            klass.java
        ) }.stream().findFirst().takeIf { it.isPresent }?.get()

    internal fun <T : Model> list(klass: KClass<T>, query: String, parameters: Map<String, Any?> = mapOf()) =
        synchronized(db) { db.query(
            query,
            if (query.contains("@@collection")) mutableMapOf("@collection" to klass.collection()) + parameters else parameters,
            klass.java
        ) }.asListRemaining().toList()

    internal fun <T : Any> query(klass: KClass<T>, query: String, parameters: Map<String, Any?> = mapOf()) =
        synchronized(db) { db.query(
            query,
            parameters,
            klass.java
        ).asListRemaining() }.toList()

    fun <T : Model>insert(model: T) = synchronized(db) { db.collection(model::class.collection()).insertDocument(model.apply { createdAt = Clock.System.now() }, DocumentCreateOptions().returnNew(true))!!.new!! }
    fun <T : Model>update(model: T) = synchronized(db) { db.collection(model::class.collection()).updateDocument(model.id?.asKey(), model, DocumentUpdateOptions().returnNew(true))!!.new!! }
    fun <T : Model>delete(model: T) = synchronized(db) { db.collection(model::class.collection()).deleteDocument(model.id?.asKey())!! }

    fun <T : Model> document(klass: KClass<T>, id: String) = synchronized(db) {
        try {
            db.collection(klass.collection()).getDocument(id.asKey(), klass.java)
        } catch (e: ArangoDBException) {
            null
        }
    }
}

private fun ArangoDatabase.setup() = apply {
    collections().forEach { model ->
        try {
            createCollection(model.name, CollectionCreateOptions().type(
                model.collectionType
            ))
        } catch (ignored: ArangoDBException) {
            // Most likely already exists
        }

        try {
            if (model.collectionType == CollectionType.EDGES) {
                createGraph(
                    "${model.name}-graph", listOf(
                        EdgeDefinition().collection(model.name)
                            .from(*model.nodes.map { it.collection() }.toTypedArray())
                            .to(*model.nodes.map { it.collection() }.toTypedArray())
                    )
                )
            }
        } catch (ignored: ArangoDBException) {
            // Most likely already exists
        }
    }
}

fun <T : Model> KClass<T>.db(
    collectionType: CollectionType = CollectionType.DOCUMENT,
    nodes: List<KClass<out Model>> = listOf(),
    block: ArangoCollection.() -> Unit = {}
) = CollectionConfig(
    collection(),
    collectionType,
    nodes,
    block
)

data class CollectionConfig(
    val name: String,
    val collectionType: CollectionType,
    val nodes: List<KClass<out Model>> = listOf(),
    val block: ArangoCollection.() -> Unit
)

internal fun String.asKey() = this.split("/").last()

internal fun <T : Model> String.asId(klass: KClass<T>) = if (this.contains("/")) this else "${klass.collection()}/$this"

fun <T : Model> KClass<T>.collection() = simpleName!!.lowercase()

fun Db.f(property: KMutableProperty1<*, *>) = property.name
